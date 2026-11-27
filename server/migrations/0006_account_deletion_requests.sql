CREATE TABLE IF NOT EXISTS account_deletion_requests (
    user_id TEXT PRIMARY KEY NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requested_at INTEGER NOT NULL
);
