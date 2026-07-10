// ---------------------------------------------------------------------------
// D1 形态适配层（测试替身）
//
// 用 Node 22 内置的 node:sqlite (DatabaseSync) 模拟 Cloudflare D1，使业务代码
// 中 `env.DB.prepare(sql).bind(...).run()/.all()/.first()` 与 `env.DB.batch([...])`
// 的调用形态完全兼容。返回结构对齐 D1：
//   - run()  -> { success, meta: { changes, last_row_id } }
//   - all()  -> { results: [...] }
//   - first()-> 首行对象 或 null
//   - batch()-> { results: [ ... ] }（顺序执行每个已绑定语句）
//
// 注意：这是「测试双」而非生产 D1，原子性/事务以顺序执行为近似；业务 SQL 逻辑
// （拆表/回填/重组）在真实 SQLite 引擎上执行，能真实暴露 SQL 语义错误。
// ---------------------------------------------------------------------------
const fs = require('fs');
const path = require('path');
const { DatabaseSync } = require('node:sqlite');

// 把 schema.sql 拆成独立语句（跳过 -- 注释与空语句），兼容 node:sqlite 的 exec。
function splitSchema(sql) {
  const out = [];
  let cur = '';
  let inStr = false;
  let i = 0;
  while (i < sql.length) {
    const ch = sql[i];
    if (inStr) {
      cur += ch;
      if (ch === "'") {
        // 处理 '' 转义
        if (sql[i + 1] === "'") { cur += "'"; i += 2; continue; }
        inStr = false;
      }
      i += 1;
      continue;
    }
    if (ch === "'") { inStr = true; cur += ch; i += 1; continue; }
    if (ch === '-' && sql[i + 1] === '-') {
      // 行注释：跳到行尾
      while (i < sql.length && sql[i] !== '\n') i += 1;
      continue;
    }
    if (ch === ';') {
      const stmt = cur.trim();
      if (stmt) out.push(stmt);
      cur = '';
      i += 1;
      continue;
    }
    cur += ch;
    i += 1;
  }
  const tail = cur.trim();
  if (tail) out.push(tail);
  return out;
}

function makeD1(db) {
  function makeBound(sql, args) {
    return {
      _sql: sql,
      _args: args,
      run() {
        const r = db.prepare(sql).run(...args);
        return {
          success: true,
          meta: { changes: r.changes, last_row_id: r.lastInsertRowid },
        };
      },
      all() {
        const rows = db.prepare(sql).all(...args);
        return { results: rows };
      },
      first() {
        const rows = db.prepare(sql).all(...args);
        return rows.length ? rows[0] : null;
      },
      get() {
        return db.prepare(sql).get(...args);
      },
    };
  }

  function prep(sql) {
    // 对齐 D1：prepare() 即可直接 .run()/.all()/.first()（无参数时无需 .bind()）；
    // 也支持 .bind(...args) 返回已绑定语句。
    const direct = makeBound(sql, []);
    return {
      bind(...args) {
        return makeBound(sql, args);
      },
      run: direct.run,
      all: direct.all,
      first: direct.first,
      get: direct.get,
    };
  }

  const d1 = {
    _raw: db,
    prepare: prep,
    // D1 batch：顺序执行每个已绑定语句；任一语句抛错会以 rejected promise 形式向上传播。
    async batch(statements) {
      const results = [];
      for (const st of statements) {
        results.push(await st.run());
      }
      return { results };
    },
    exec(sqlText) {
      db.exec(sqlText);
    },
  };
  return d1;
}

/**
 * 创建一个全新的内存测试环境（每次调用互相隔离，互不影响）。
 * @param {object} [opts]
 * @param {string} [opts.schemaPath] schema.sql 路径，默认指向 server/schema.sql
 * @returns {{ env: { DB: any }, db: any, close: () => void }}
 */
function createTestEnv(opts = {}) {
  const db = new DatabaseSync(':memory:');
  db.exec('PRAGMA foreign_keys = OFF;'); // 测试不依赖外键级联，关闭以放宽断言
  const schemaPath = opts.schemaPath || path.resolve(__dirname, '../../schema.sql');
  const statements = splitSchema(fs.readFileSync(schemaPath, 'utf8'));
  for (const s of statements) {
    try {
      db.exec(s);
    } catch (e) {
      // schema 中个别语句在 node:sqlite 不兼容时记录但不阻断（应极少发生）
      console.error('[schema exec warning]', s.slice(0, 80), '->', e.message);
    }
  }
  const DB = makeD1(db);
  // 返回扁平结构：env.DB / env.db / env.close()
  return {
    DB,
    db,
    close() {
      try { db.close(); } catch { /* noop */ }
    },
  };
}

module.exports = { createTestEnv, makeD1, splitSchema };
