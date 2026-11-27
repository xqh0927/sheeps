-- Generated stages have no levels row. Only observed starts carry layout provenance.
CREATE TABLE IF NOT EXISTS stage_attempts (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    run_id TEXT NOT NULL,
    level_id INTEGER NOT NULL,
    layout_version TEXT NOT NULL,
    started_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, run_id)
);
CREATE INDEX IF NOT EXISTS idx_stage_attempts_version ON stage_attempts(level_id, layout_version, user_id, started_at, run_id);
CREATE INDEX IF NOT EXISTS idx_stage_attempts_retry ON stage_attempts(user_id, level_id, started_at, run_id);

-- Older leaderboard entries have no run identity. Never infer one from a timestamp or score.
CREATE TABLE IF NOT EXISTS stage_run_scores (
    user_id TEXT NOT NULL,
    run_id TEXT NOT NULL,
    score_id INTEGER NOT NULL UNIQUE REFERENCES leaderboard(id) ON DELETE CASCADE,
    abandoned INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, run_id),
    FOREIGN KEY (user_id, run_id) REFERENCES stage_score_receipts(user_id, run_id) ON DELETE CASCADE
);
