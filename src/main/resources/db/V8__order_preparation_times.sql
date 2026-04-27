ALTER TABLE orders
    ADD COLUMN preparing_at             DATETIME NULL,
    ADD COLUMN ready_at                 DATETIME NULL,
    ADD COLUMN preparation_time_seconds INT      NULL;
