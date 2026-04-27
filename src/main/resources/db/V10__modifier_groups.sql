CREATE TABLE modifier_groups (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT        NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    is_active     TINYINT(1)    NOT NULL DEFAULT 1,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

CREATE TABLE modifiers (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id  BIGINT        NOT NULL,
    name      VARCHAR(100)  NOT NULL,
    is_active TINYINT(1)    NOT NULL DEFAULT 1,
    FOREIGN KEY (group_id) REFERENCES modifier_groups(id)
);

CREATE TABLE variant_modifiers (
    id          BIGINT         AUTO_INCREMENT PRIMARY KEY,
    variant_id  BIGINT         NOT NULL,
    modifier_id BIGINT         NOT NULL,
    price       DECIMAL(10,2)  NOT NULL,
    UNIQUE KEY uk_variant_modifier (variant_id, modifier_id),
    FOREIGN KEY (variant_id)  REFERENCES menu_item_variants(id),
    FOREIGN KEY (modifier_id) REFERENCES modifiers(id)
);

CREATE TABLE order_item_modifiers (
    id             BIGINT         AUTO_INCREMENT PRIMARY KEY,
    order_item_id  BIGINT         NOT NULL,
    modifier_id    BIGINT         NULL,
    modifier_name  VARCHAR(100)   NOT NULL,
    group_name     VARCHAR(100)   NOT NULL,
    price          DECIMAL(10,2)  NOT NULL,
    FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    FOREIGN KEY (modifier_id)   REFERENCES modifiers(id)
);
