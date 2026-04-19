CREATE TABLE IF NOT EXISTS combos (
    id            BIGINT         AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT         NOT NULL,
    name          VARCHAR(150)   NOT NULL,
    description   VARCHAR(400)   NULL,
    sale_price    DECIMAL(10, 2) NOT NULL,
    is_active     TINYINT(1)     NOT NULL DEFAULT 1,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_combo_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

CREATE TABLE IF NOT EXISTS combo_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    combo_id     BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    variant_id   BIGINT NULL,
    quantity     INT    NOT NULL DEFAULT 1,
    CONSTRAINT fk_combo_item_combo    FOREIGN KEY (combo_id)     REFERENCES combos (id),
    CONSTRAINT fk_combo_item_menu     FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT fk_combo_item_variant  FOREIGN KEY (variant_id)   REFERENCES menu_item_variants (id)
);

CREATE TABLE IF NOT EXISTS promotions (
    id             BIGINT         AUTO_INCREMENT PRIMARY KEY,
    restaurant_id  BIGINT         NOT NULL,
    name           VARCHAR(150)   NOT NULL,
    description    VARCHAR(400)   NULL,
    type           VARCHAR(20)    NOT NULL,
    discount_value DECIMAL(10, 2) NULL,
    start_date     DATE           NOT NULL,
    end_date       DATE           NOT NULL,
    is_active      TINYINT(1)     NOT NULL DEFAULT 1,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

CREATE TABLE IF NOT EXISTS promotion_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    variant_id   BIGINT NULL,
    CONSTRAINT fk_promo_item_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (id),
    CONSTRAINT fk_promo_item_menu      FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT fk_promo_item_variant   FOREIGN KEY (variant_id)   REFERENCES menu_item_variants (id)
);
