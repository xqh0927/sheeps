CREATE TABLE IF NOT EXISTS shop_exchange_receipts (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id TEXT NOT NULL,
    attempt_id TEXT NOT NULL UNIQUE,
    shop_item_id INTEGER NOT NULL,
    item_type TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    points_cost INTEGER NOT NULL,
    remaining_points INTEGER NOT NULL,
    new_count INTEGER NOT NULL,
    PRIMARY KEY (user_id, operation_id)
);
