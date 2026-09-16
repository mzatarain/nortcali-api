-- Mesas de comedor por restaurante, con ocupación automática desde el flujo de órdenes
-- dine_in (ver TASKS.md, sección "Mesas de comedor con ocupación automática").
-- status: 'available' | 'occupied' | 'reserved' | 'out_of_service'

CREATE TABLE dining_tables (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    restaurant_id BIGINT      NOT NULL,
    name          VARCHAR(40) NOT NULL,
    capacity      INT         NULL,
    status        VARCHAR(15) NOT NULL DEFAULT 'available',
    is_active     TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_dining_table_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Nullable porque las órdenes takeout/delivery no usan mesa.
ALTER TABLE orders ADD COLUMN table_id BIGINT NULL,
    ADD CONSTRAINT fk_order_table FOREIGN KEY (table_id) REFERENCES dining_tables (id);
