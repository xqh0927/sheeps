/**
 * 管理后台首个超级管理员种子脚本
 *
 * 用法（在项目 server/ 目录下）：
 *   ADMIN_PHONE=13800000000 ADMIN_PASSWORD=YourStrongPass node scripts/seed-admin.mjs
 *
 * 行为：幂等 upsert 首个 super 账号
 *   - 若手机号已存在：确保 role='super'、刷新密码（若提供了 ADMIN_PASSWORD）
 *   - 若不存在：创建新用户，role='super'，username 取手机号
 *
 * 密码哈希与 server/src/auth-utils.ts 的 hashPassword 完全一致：
 *   PBKDF2-HMAC-SHA256, 100000 迭代, 随机 32 字节盐, 存储格式 "salt_hex:hash_hex"
 *
 * 依赖：@cloudflare/workers-types 不在 Node 运行时需要；此处仅使用 Web Crypto（全局 crypto.subtle）。
 * 运行环境：Node 18+（含全局 crypto.subtle）。需本地 D1 或本地 sqlite 时，请通过 wrangler 执行：
 *   npx wrangler d1 execute my-app-db --local --file=...   (本脚本改为导出函数供调用)
 * 本脚本默认直接打印 SQL，便于手动执行；如需直连，可改用 wrangler 方式。
 */

const PBKDF2_ITERATIONS = 100000;
const SALT_LENGTH = 32;
const HASH_LENGTH = 32;

function bytesToHex(bytes) {
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

async function hashPassword(password) {
  const salt = crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(password),
    'PBKDF2',
    false,
    ['deriveBits']
  );
  const derivedBits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt, iterations: PBKDF2_ITERATIONS, hash: 'SHA-256' },
    keyMaterial,
    HASH_LENGTH * 8
  );
  return `${bytesToHex(salt)}:${bytesToHex(new Uint8Array(derivedBits))}`;
}

async function main() {
  const phone = process.env.ADMIN_PHONE;
  const password = process.env.ADMIN_PASSWORD;
  if (!phone || !password) {
    console.error('缺少环境变量 ADMIN_PHONE 或 ADMIN_PASSWORD');
    process.exit(1);
  }
  const passwordHash = await hashPassword(password);
  const userId = `admin_${phone}`;
  const username = phone;

  // 生成幂等 upsert SQL（兼容 D1 / SQLite）
  const sql = `
-- 若账号已存在则升级为 super 并刷新密码
UPDATE users SET role = 'super', password_hash = '${passwordHash}', username = '${username}' WHERE phone = '${phone}';
-- 若不存在则插入
INSERT INTO users (id, phone, username, role, password_hash, points, created_at)
SELECT '${userId}', '${phone}', '${username}', 'super', '${passwordHash}', 0, ${Date.now()}
WHERE NOT EXISTS (SELECT 1 FROM users WHERE phone = '${phone}');
`;

  if (process.env.SQL_ONLY === 'true') {
    console.log(sql.trim());
    return;
  }

  console.log('=== 请将以下 SQL 在 D1 上执行（或集成进部署流程） ===');
  console.log(sql);
  console.log('=== 提示 ===');
  console.log(`管理员手机号: ${phone}`);
  console.log('角色: super（后续可在后台「账户管理」中调整）');
  console.log('注：本脚本生成 SQL 文本，便于 Code Review 与手动执行；');
  console.log('    如需自动化，可改为通过 wrangler d1 execute 调用。');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
