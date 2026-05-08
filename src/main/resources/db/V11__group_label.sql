ALTER TABLE order_items
    ADD COLUMN group_label VARCHAR(100) NULL AFTER subtotal;

ALTER TABLE sale_items
    ADD COLUMN group_label VARCHAR(100) NULL AFTER subtotal;
