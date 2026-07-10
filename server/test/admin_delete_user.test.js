// Regression tests for DELETE /api/admin/users/:id  (admin.deleteUser)
//
// Framework: node:test + node:assert (same convention as admin_assets.test.js).
// The project does NOT use vitest/miniflare here; existing tests inject a hand-
// written mock Env.  To actually exercise the cascade SQL, we use a *stateful*
// in-memory mock DB whose `prepare().bind().first/all/run` and `batch()` truly
// mutate a shared store, replicating SQLite/D1 semantics (a role-less row takes
// the column DEFAULT, here 'readonly' -- verified separately with sqlite3).
//
// Note: compiled handler lives in ../.tmp-test (produced by the project's tsc
// compile step).  Run with:  node --test test/admin_delete_user.test.js

const test = require('node:test');
const assert = require('node:assert/strict');

const { handleAdminRoutes } = require('../.tmp-test/handlers/admin.js');
const { generateJWT } = require('../.tmp-test/crypto.js');

// ---------------------------------------------------------------------------
// Stateful in-memory mock DB
// ---------------------------------------------------------------------------
function makeMockEnv() {
  // Shared store.  Each table is a plain array of row objects.
  const store = {
    users: [],
    level_unlock: [],
    user_items: [],
    exchange_record: [],
    point_record: [],
    sign_record: [],
    user_task: [],
    leaderboard: [],
    backup_save_log: [],
    admin_audit_log: [],
    login_token: [],
  };

  function deleteByUserId(table, uid) {
    if (store[table]) store[table] = store[table].filter((r) => r.user_id !== uid);
  }

  const DB = {
    prepare(sql) {
      const s = String(sql).toLowerCase();
      return {
        bind(...args) {
          return {
            first: async () => {
              // requireAdmin: SELECT is_banned FROM users WHERE id = ?
              if (s.includes('select is_banned from users')) {
                const u = store.users.find((x) => x.id === args[0]);
                return u ? { is_banned: u.is_banned ?? 0 } : null;
              }
              // deleteUser: SELECT id, phone, username, role FROM users WHERE id = ?
              if (s.includes('from users') && s.includes('where id')) {
                return store.users.find((x) => x.id === args[0]) || null;
              }
              // COUNT(*) fallback (not used by deleteUser, safe default)
              if (s.includes('count(')) return { c: 0 };
              return null;
            },
            all: async () => ({ results: [] }),
            run: async () => {
              // deleteUser: DELETE FROM users WHERE id = ?
              if (s.startsWith('delete from users where id')) {
                const before = store.users.length;
                store.users = store.users.filter((u) => u.id !== args[0]);
                return { success: true, meta: { changes: before - store.users.length } };
              }
              // deleteUser cascade: DELETE FROM <t> WHERE user_id = ?
              const m = s.match(/delete from (\w+) where user_id/);
              if (m) {
                deleteByUserId(m[1], args[0]);
                return { success: true };
              }
              // writeAudit INSERT (ignored for assertions, just succeed)
              if (s.startsWith('insert into admin_audit_log')) {
                store.admin_audit_log.push({ id: store.admin_audit_log.length + 1 });
                return { success: true };
              }
              // deleteUser cleanup (post-fix): DELETE FROM login_token WHERE phone = ?
              if (s.startsWith('delete from login_token where phone')) {
                if (store.login_token) store.login_token = store.login_token.filter((r) => r.phone !== args[0]);
                return { success: true };
              }
              return { success: true };
            },
          };
        },
      };
    },
    batch(statements) {
      // D1 batch executes each prepared (bound) statement.
      return (async () => {
        for (const st of statements) await st.run();
        return { results: statements.map(() => ({ success: true })) };
      })();
    },
  };

  return { env: { DB }, store };
}

// Add a user to the mock store.  role defaults to 'user' to faithfully
// mirror the (post-fix) DB DEFAULT (auth.ts/game.ts now write role='user').
function addUser(store, id, phone, username, role = 'user', isBanned = 0) {
  store.users.push({ id, phone, username, role, is_banned: isBanned, points: 0, created_at: 1700000000000 });
}

function makeReq(method, pathname, token, body) {
  return {
    method,
    headers: {
      get: (h) => (String(h).toLowerCase() === 'authorization' ? `Bearer ${token}` : null),
    },
    json: async () => body || {},
    url: 'https://example.com' + pathname,
  };
}

function adminToken(userId, role) {
  return generateJWT({ userId, phone: '13900000000', role, type: 'access', exp: Date.now() + 100000 });
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test('readonly admin is blocked by write guard (403) before any delete', async () => {
  const { env, store } = makeMockEnv();
  addUser(store, 'adminRO', '13900000001', 'readonly_admin', 'readonly');
  addUser(store, 'victim', '13800000099', 'victim_user', 'user'); // a normal user

  const token = await adminToken('adminRO', 'readonly');
  const req = makeReq('DELETE', '/api/admin/users/victim', token);
  const url = new URL(req.url);
  const res = await handleAdminRoutes(req, env, url.pathname, url);

  assert.equal(res.status, 403);
  const data = await res.json();
  assert.equal(data.error, '无写权限（当前为只读角色）');
  // victim untouched
  assert.ok(store.users.find((u) => u.id === 'victim'));
});

test('happy path: a correctly-roled (non-admin) user is deleted with full cascade', async () => {
  const { env, store } = makeMockEnv();
  addUser(store, 'adminA', '13900000000', 'super_admin', 'super');
  addUser(store, 'u1', '13800000001', 'normal_user', 'user'); // non-admin role

  // seed related rows for u1
  store.user_items.push({ user_id: 'u1', item_type: 'UNDO', count: 3 });
  store.point_record.push({ user_id: 'u1', type: 'IN', amount: 10, source: 'X', remaining_points: 10, created_at: 1 });
  store.leaderboard.push({ user_id: 'u1', level_id: 1, score: 100, clear_time_ms: 1, game_mode: 0, achieved_at: 1 });
  store.level_unlock.push({ user_id: 'u1', level_id: 2, unlocked_at: 1 });

  const token = await adminToken('adminA', 'super');
  const req = makeReq('DELETE', '/api/admin/users/u1', token);
  const url = new URL(req.url);
  const res = await handleAdminRoutes(req, env, url.pathname, url);

  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.success, true);

  // users row gone
  assert.equal(store.users.find((u) => u.id === 'u1'), undefined);
  // cascade tables cleaned for u1
  assert.equal(store.user_items.filter((r) => r.user_id === 'u1').length, 0);
  assert.equal(store.point_record.filter((r) => r.user_id === 'u1').length, 0);
  assert.equal(store.leaderboard.filter((r) => r.user_id === 'u1').length, 0);
  assert.equal(store.level_unlock.filter((r) => r.user_id === 'u1').length, 0);
  // audit logged
  assert.equal(store.admin_audit_log.length, 1);
});

test('guard: deleting an admin-role user (super/operator/readonly) returns 403', async () => {
  const { env, store } = makeMockEnv();
  addUser(store, 'adminA', '13900000000', 'super_admin', 'super');
  addUser(store, 'adminTarget', '13900000002', 'target_admin', 'super');

  const token = await adminToken('adminA', 'super');
  const req = makeReq('DELETE', '/api/admin/users/adminTarget', token);
  const url = new URL(req.url);
  const res = await handleAdminRoutes(req, env, url.pathname, url);

  assert.equal(res.status, 403);
  const data = await res.json();
  assert.equal(data.error, '不能移除管理员账户');
  // target NOT deleted
  assert.ok(store.users.find((u) => u.id === 'adminTarget'));
});

test('guard: admin deleting their own account returns 403 (self-guard)', async () => {
  const { env, store } = makeMockEnv();
  // target id equals the caller's id, role is admin -> role guard fires first
  addUser(store, 'adminA', '13900000000', 'self_admin', 'super');

  const token = await adminToken('adminA', 'super');
  const req = makeReq('DELETE', '/api/admin/users/adminA', token);
  const url = new URL(req.url);
  const res = await handleAdminRoutes(req, env, url.pathname, url);

  assert.equal(res.status, 403);
  // Note: because the admin-role guard precedes the self guard, the returned
  // message is '不能移除管理员账户' rather than '不能移除当前登录账户'.
  const data = await res.json();
  assert.equal(data.error, '不能移除管理员账户');
  // caller still present (deletion blocked)
  assert.ok(store.users.find((u) => u.id === 'adminA'));
});

test('404: deleting a non-existent user', async () => {
  const { env, store } = makeMockEnv();
  addUser(store, 'adminA', '13900000000', 'super_admin', 'super');

  const token = await adminToken('adminA', 'super');
  const req = makeReq('DELETE', '/api/admin/users/ghost', token);
  const url = new URL(req.url);
  const res = await handleAdminRoutes(req, env, url.pathname, url);

  assert.equal(res.status, 404);
  const data = await res.json();
  assert.equal(data.error, '用户不存在');
});

// REGRESSION GUARD (post-fix)
// After the role-separation fix, game users created by /api/auth/login and
// /api/auth/register take the schema DEFAULT 'user' (auth.ts/game.ts now write
// role='user'; schema default is also 'user').  The delete guard only blocks
// users whose role is in [super, operator, readonly], so a normal game user
// (role='user') MUST be deletable.  This test encodes that REQUIRED behaviour
// and must stay green after the fix.
test('REGRESSION: a normal game user (role defaults to user) is deletable', async () => {
  const { env, store } = makeMockEnv();
  addUser(store, 'adminA', '13900000000', 'super_admin', 'super');
  // exactly how auth.ts creates a player: no role -> store defaults to 'user'
  addUser(store, 'gameUser', '13800000123', '国风玩家_0123'); // role omitted => 'user' (new default)
  // also seed the login_token row that the fix now cleans up by phone
  store.login_token.push({ phone: '13800000123', code: '123456', created_at: 1 });
  store.user_items.push({ user_id: 'gameUser', item_type: 'UNDO', count: 1 });

  const token = await adminToken('adminA', 'super');
  const req = makeReq('DELETE', '/api/admin/users/gameUser', token);
  const url = new URL(req.url);
  const res = await handleAdminRoutes(req, env, url.pathname, url);

  // REQUIRED: game user should be removed (status 200) with cascade cleanup.
  assert.equal(res.status, 200, `expected game user to be deletable, got ${res.status} (${JSON.stringify(await res.json())})`);
  assert.equal(store.users.find((u) => u.id === 'gameUser'), undefined);
  assert.equal(store.user_items.filter((r) => r.user_id === 'gameUser').length, 0);
  // the fix's new login_token cleanup must have removed the stale token by phone
  assert.equal(store.login_token.filter((r) => r.phone === '13800000123').length, 0);
});
