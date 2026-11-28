const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');

test('a fresh database can load the full schema and initial game content', () => {
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(readFileSync(path.resolve(__dirname, '../schema.sql'), 'utf8'));
    for (const table of ['notice', 'shop_items', 'task']) {
      assert.ok(db.prepare(`SELECT COUNT(*) AS n FROM ${table}`).get().n > 0, table);
    }
  } finally {
    db.close();
  }
});

const standardShopNames = [
  ['UNDO', '乾坤符', 'Undo'], ['MOVEOUT', '缩地咒', 'MoveOut'],
  ['SHUFFLE', '流沙契', 'Shuffle'], ['REVIVE', '还魂丹', 'Revive'],
  ['HINT', '天眼符', 'Hint'], ['BOMB', '雷震子', 'Bomb'],
  ['JOKER', '太极牌', 'Joker'], ['DOUBLE_POINTS', '双倍符', 'Double'],
];

test('fresh shop items have their original Simplified Chinese names', () => {
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(readFileSync(path.resolve(__dirname, '../schema.sql'), 'utf8'));
    const select = db.prepare('SELECT name FROM shop_items WHERE item_type = ?');
    for (const [type, name] of standardShopNames) assert.equal(select.get(type).name, name, type);
  } finally {
    db.close();
  }
});

test('Chinese shop migration matches the item type, exact old name, module, locale and field', () => {
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(readFileSync(path.resolve(__dirname, '../schema.sql'), 'utf8'));
    const migration = readFileSync(path.resolve(__dirname, '../migrations/0003_zh_shop_item_names.sql'), 'utf8');
    const insertItem = db.prepare('INSERT INTO shop_items(name,item_type,points_price) VALUES (?,?,10) RETURNING id');
    const translation = db.prepare('INSERT INTO i18n_strings(str_key,locale,module,value) VALUES (?,?,?,?) RETURNING id');
    const renamed = [];
    const translated = [];
    const preserved = [];
    for (const [type, name, en] of standardShopNames) {
      const old = `${name} (${en})`;
      const id = insertItem.get(old, type).id;
      renamed.push([id, name]);
      const key = `shop_items.${id}.name`;
      translated.push([translation.get(key, 'zh', 'shop_items', old).id, name]);
      for (const locale of ['en','tw','ja','ko']) {
        preserved.push([translation.get(key, locale, 'shop_items', old).id, old]);
      }
      preserved.push([translation.get(`shop_items.${id}.description`, 'zh', 'shop_items', old).id, old]);
      preserved.push([translation.get(key, 'zh', 'other', old).id, old]);
    }
    const customId = insertItem.get('自定义乾坤符 (Undo)', 'UNDO').id;
    const wrongType = insertItem.get('乾坤符 (Undo)', 'CUSTOM').id;
    const standardId = insertItem.get('乾坤符 (Undo)', 'UNDO').id;
    preserved.push([translation.get(`shop_items.${standardId}.name`, 'zh', 'shop_items', '管理员自定义名称').id, '管理员自定义名称']);
    preserved.push([translation.get(`shop_items.${wrongType}.name`, 'zh', 'shop_items', '乾坤符 (Undo)').id, '乾坤符 (Undo)']);
    const beforeCount = db.prepare('SELECT COUNT(*) AS n FROM i18n_strings').get().n;

    db.exec(migration);
    const byId = db.prepare('SELECT name FROM shop_items WHERE id = ?');
    assert.equal(byId.get(customId).name, '自定义乾坤符 (Undo)');
    assert.equal(byId.get(wrongType).name, '乾坤符 (Undo)');
    for (const [id, name] of renamed) assert.equal(byId.get(id).name, name);
    const selectValue = db.prepare('SELECT value FROM i18n_strings WHERE id = ?');
    for (const [id, value] of [...translated, ...preserved]) assert.equal(selectValue.get(id).value, value);
    assert.equal(db.prepare('SELECT COUNT(*) AS n FROM i18n_strings').get().n, beforeCount);
    const changes = db.prepare('SELECT total_changes() AS n').get().n;
    db.exec(migration);
    assert.equal(db.prepare('SELECT total_changes() AS n').get().n, changes);
  } finally {
    db.close();
  }
});

test('stage health migration preserves old scores without assigning them a layout or run', () => {
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(readFileSync(path.resolve(__dirname, '../schema.sql'), 'utf8'));
    db.exec(`
      DROP TABLE stage_attempts;
      DROP TABLE stage_run_scores;
      INSERT INTO users(id, username, created_at) VALUES ('old', 'Old player', 100);
      INSERT INTO stage_score_receipts VALUES ('old', 'offline-run', 'receipt');
      INSERT INTO leaderboard(user_id, level_id, score, clear_time_ms, achieved_at)
        VALUES ('old', 5, 90, 1000, 101);
    `);
    const migrate = readFileSync(path.resolve(__dirname, '../migrations/0008_stage_attempt_health.sql'), 'utf8');
    db.exec(migrate);
    db.exec(migrate);
    assert.equal(db.prepare('SELECT count(*) AS n FROM stage_run_scores').get().n, 0);
    assert.equal(db.prepare('SELECT count(*) AS n FROM stage_attempts').get().n, 0);
    assert.equal(db.prepare('SELECT score FROM leaderboard').get().score, 90);
    assert.equal(db.prepare('SELECT run_id FROM stage_score_receipts').get().run_id, 'offline-run');
  } finally {
    db.close();
  }
});

test('email auth migration adds email column and verification table while preserving legacy phone users', () => {
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(`
      CREATE TABLE users (
        id TEXT PRIMARY KEY,
        phone TEXT UNIQUE,
        username TEXT NOT NULL,
        points INTEGER DEFAULT 0,
        password_hash TEXT,
        avatar_url TEXT,
        created_at INTEGER NOT NULL,
        role TEXT DEFAULT 'user',
        is_banned INTEGER DEFAULT 0
      );
      INSERT INTO users(id, phone, username, created_at) VALUES ('u1', '13800000001', 'Legacy', 100);
    `);
    const migrate = readFileSync(path.resolve(__dirname, '../migrations/0015_email_auth.sql'), 'utf8');
    db.exec(migrate);
    db.prepare('UPDATE users SET email = ? WHERE id = ?').run('legacy@example.com', 'u1');
    db.prepare('INSERT INTO email_verification_codes(email, code, created_at) VALUES (?, ?, ?)').run('legacy@example.com', '123456', 200);
    assert.equal(db.prepare('SELECT email FROM users WHERE id = ?').get('u1').email, 'legacy@example.com');
    assert.equal(db.prepare('SELECT code FROM email_verification_codes WHERE email = ?').get('legacy@example.com').code, '123456');
  } finally {
    db.close();
  }
});
