-- 邮箱验证码登录与邮箱/手机号双绑定支持
-- 1. users 表新增 email 列（SQLite 允许 UNIQUE 索引存在多个 NULL，老手机号账号/游客/管理员不受影响）
ALTER TABLE users ADD COLUMN email TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 2. 邮箱验证码表（60s 冷却 + 5min 有效期 + 核销即删）
CREATE TABLE IF NOT EXISTS email_verification_codes (
    email TEXT PRIMARY KEY,
    code TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
