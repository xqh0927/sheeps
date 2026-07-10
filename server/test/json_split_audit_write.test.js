// ===========================================================================
// 回归测试：writeAudit 必须真正把快照写入 admin_audit_changes 子表
//
// 触发路径：DELETE /api/admin/users/:id → deleteUser → writeAudit
//           → 取 info.lastRowId 作为 changeId → writeAuditChanges
//
// 用真实 sqlite D1 替身（与 D1 一致：.run() 返回 { meta:{ last_row_id } }）。
// 若 writeAudit 仍写 `(info as any).lastRowId`（D1 实际不存在该字段，应为
// meta.last_row_id），则 changeId 为 undefined，writeAuditChanges 被跳过，
// admin_audit_changes 为空 —— 本测试会失败，从而定位该源码 Bug。
//
// 运行：node --test test/json_split_audit_write.test.js
// ===========================================================================

const { test, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');

const { handleAdminRoutes } = require('../.tmp-test/handlers/admin.js');
const { generateJWT } = require('../.tmp-test/crypto.js');
const { createTestEnv } = require('./helpers/d1_adapter.cjs');

let env;
beforeEach(() => { env = createTestEnv(); });
afterEach(() => { env.close(); });

function seedUser(id, phone, username, role, isBanned = 0) {
  // users 表：id(PK), phone, username NOT NULL, created_at NOT NULL, role DEFAULT 'user'
  env.DB.prepare(
    'INSERT INTO users (id, phone, username, created_at, role, is_banned) VALUES (?,?,?,?,?,?)'
  ).bind(id, phone, username, 1700000000000, role, isBanned).run();
}

function makeReq(method, pathname, token) {
  return {
    method,
    headers: { get: (h) => (String(h).toLowerCase() === 'authorization' ? `Bearer ${token}` : null) },
    json: async () => ({}),
    url: 'https://example.com' + pathname,
  };
}

function adminToken(userId, role) {
  return generateJWT({ userId, phone: '13900000000', role, type: 'access', exp: Date.now() + 100000 });
}

test('DELETE 用户后，writeAudit 应将快照写入 admin_audit_changes 子表（验证 lastRowId 修复）', async () => {
  seedUser('adminA', '13900000000', 'super_admin', 'super');
  seedUser('u1', '13800000001', 'normal_user', 'user'); // 普通用户，可删

  const token = await adminToken('adminA', 'super');
  const req = makeReq('DELETE', '/api/admin/users/u1', token);
  const url = new URL(req.url);
  const res = await handleAdminRoutes(req, env, url.pathname, url);

  // 1) 删除本身成功
  assert.equal(res.status, 200, '删除应成功');

  // 2) 主表审计行已落库
  const auditRows = await env.DB.prepare('SELECT COUNT(*) as c FROM admin_audit_log').bind().first();
  assert.ok(auditRows.c >= 1, 'admin_audit_log 应有审计主表行');

  // 3) 关键断言：快照拆分子表必须被写入（writeAuditChanges 经 changeId 写入）
  const kvRows = await env.DB.prepare('SELECT COUNT(*) as c FROM admin_audit_changes').bind().first();
  assert.ok(
    kvRows.c >= 1,
    'writeAudit 应通过 changeId 将 before/after 写入 admin_audit_changes；' +
    '当前为 0，说明 info.lastRowId 未取到（D1 应为 meta.last_row_id），writeAuditChanges 被跳过'
  );

  // 4) 重组出的快照应是合法 JSON 且含被删用户的字段（端到端闭环）
  const { reassembleAuditSnapshots } = require('../.tmp-qa/compiled/lib/audit.js');
  const ids = (await env.DB.prepare('SELECT id FROM admin_audit_log').bind().all()).results.map((r) => r.id);
  const map = await reassembleAuditSnapshots(env, ids);
  const snap = map.get(ids[0]);
  assert.ok(snap && typeof snap.before === 'string', 'reassemble 应返回字符串快照');
  const before = JSON.parse(snap.before);
  assert.ok('phone' in before || 'username' in before, '快照应包含被删用户字段');
});
