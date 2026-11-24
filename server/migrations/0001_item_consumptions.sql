-- Retried offline operations retain the same receipt for the lifetime of the account.
CREATE TABLE IF NOT EXISTS item_consumptions (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id TEXT NOT NULL,
    PRIMARY KEY (user_id, operation_id)
);
