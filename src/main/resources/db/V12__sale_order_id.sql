ALTER TABLE sales
    ADD COLUMN order_id BIGINT NULL,
    ADD CONSTRAINT fk_sales_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE SET NULL,
    ADD UNIQUE INDEX uq_sales_order_id (order_id);
