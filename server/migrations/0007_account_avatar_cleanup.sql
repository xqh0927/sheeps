-- The retry job must survive deletion of the corresponding users row.
CREATE TABLE IF NOT EXISTS account_avatar_cleanup (
    user_id TEXT PRIMARY KEY NOT NULL,
    deleted_at INTEGER NOT NULL,
    revision TEXT NOT NULL,
    last_attempt_at INTEGER NOT NULL DEFAULT 0
);
