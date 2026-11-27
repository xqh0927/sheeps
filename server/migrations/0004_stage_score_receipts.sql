CREATE TABLE IF NOT EXISTS stage_score_receipts (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    run_id TEXT NOT NULL,
    attempt_id TEXT NOT NULL UNIQUE,
    PRIMARY KEY (user_id, run_id)
);
