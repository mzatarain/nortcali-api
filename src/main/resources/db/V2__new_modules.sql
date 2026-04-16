-- =============================================================================
-- NortCali API — V2: Módulos nuevos
-- Ejecutar contra la base de datos nortcali después del schema inicial (V1).
-- Todos los enums se almacenan en lowercase (ej. 'pending', 'open').
-- =============================================================================

-- ============================================================
-- SESIONES JWT (tabla sessions puede ya existir; IF NOT EXISTS)
-- ============================================================
CREATE TABLE IF NOT EXISTS sessions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT       NOT NULL,
    token       VARCHAR(512) NOT NULL,
    ip_address  VARCHAR(45)  NULL,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    expires_at  DATETIME     NULL,
    CONSTRAINT fk_sessions_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

-- ============================================================
-- MÓDULO MENÚ
-- ============================================================
CREATE TABLE IF NOT EXISTS menu_categories (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT      NOT NULL,
    name          VARCHAR(80) NOT NULL,
    display_order INT         NOT NULL DEFAULT 0,
    is_active     TINYINT(1)  NOT NULL DEFAULT 1,
    CONSTRAINT fk_menu_cat_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

CREATE TABLE IF NOT EXISTS menu_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT       NOT NULL,
    category_id   BIGINT       NOT NULL,
    name          VARCHAR(120) NOT NULL,
    description   TEXT         NULL,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT fk_menu_item_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT fk_menu_item_category   FOREIGN KEY (category_id)   REFERENCES menu_categories (id)
);

CREATE TABLE IF NOT EXISTS menu_item_variants (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY,
    menu_item_id BIGINT         NOT NULL,
    name         VARCHAR(60)    NOT NULL,
    sale_price   DECIMAL(10, 2) NOT NULL,
    is_active    TINYINT(1)     NOT NULL DEFAULT 1,
    CONSTRAINT fk_variant_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id)
);

-- ============================================================
-- MÓDULO INVENTARIO
-- ============================================================
CREATE TABLE IF NOT EXISTS units (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(40) NOT NULL,
    abbreviation VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS supplies (
    id            BIGINT          AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT          NOT NULL,
    unit_id       BIGINT          NOT NULL,
    name          VARCHAR(100)    NOT NULL,
    current_stock DECIMAL(12, 4)  NOT NULL DEFAULT 0,
    minimum_stock DECIMAL(12, 4)  NOT NULL DEFAULT 0,
    unit_cost     DECIMAL(10, 4)  NOT NULL DEFAULT 0,
    is_active     TINYINT(1)      NOT NULL DEFAULT 1,
    CONSTRAINT fk_supply_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT fk_supply_unit       FOREIGN KEY (unit_id)       REFERENCES units (id)
);

-- movement_type: 'entrada' | 'salida' | 'merma' | 'ajuste'
CREATE TABLE IF NOT EXISTS inventory_movements (
    id            BIGINT         AUTO_INCREMENT PRIMARY KEY,
    supply_id     BIGINT         NOT NULL,
    employee_id   BIGINT         NULL,
    movement_type VARCHAR(10)    NOT NULL,
    quantity      DECIMAL(12, 4) NOT NULL,
    created_at    DATETIME       NOT NULL,
    CONSTRAINT fk_inv_mov_supply   FOREIGN KEY (supply_id)   REFERENCES supplies (id),
    CONSTRAINT fk_inv_mov_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

-- ============================================================
-- MÓDULO RECETAS
-- ============================================================
CREATE TABLE IF NOT EXISTS recipes (
    id           BIGINT     AUTO_INCREMENT PRIMARY KEY,
    menu_item_id BIGINT     NOT NULL,
    variant_id   BIGINT     NULL,
    portions     INT        NOT NULL DEFAULT 1,
    is_active    TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_recipe_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT fk_recipe_variant   FOREIGN KEY (variant_id)   REFERENCES menu_item_variants (id)
);

CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    recipe_id       BIGINT         NOT NULL,
    supply_id       BIGINT         NOT NULL,
    unit_id         BIGINT         NOT NULL,
    quantity        DECIMAL(12, 4) NOT NULL,
    calculated_cost DECIMAL(10, 4) NOT NULL DEFAULT 0,
    CONSTRAINT fk_ing_recipe  FOREIGN KEY (recipe_id)  REFERENCES recipes (id),
    CONSTRAINT fk_ing_supply  FOREIGN KEY (supply_id)  REFERENCES supplies (id),
    CONSTRAINT fk_ing_unit    FOREIGN KEY (unit_id)    REFERENCES units (id)
);

-- ============================================================
-- MÓDULO CLIENTES Y DELIVERY
-- ============================================================
CREATE TABLE IF NOT EXISTS customers (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT      NOT NULL,
    first_name    VARCHAR(80) NOT NULL,
    phone         VARCHAR(20) NOT NULL,
    address       VARCHAR(255) NULL,
    total_orders  BIGINT      NOT NULL DEFAULT 0,
    is_active     TINYINT(1)  NOT NULL DEFAULT 1,
    CONSTRAINT fk_customer_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

CREATE TABLE IF NOT EXISTS delivery_drivers (
    id            BIGINT     AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT     NOT NULL,
    first_name    VARCHAR(80) NOT NULL,
    phone         VARCHAR(20) NOT NULL,
    vehicle       VARCHAR(60) NULL,
    is_active     TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_driver_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

-- ============================================================
-- MÓDULO ÓRDENES
-- order_type:     'dine_in' | 'takeout' | 'delivery'
-- source:         'pos' | 'whatsapp' | 'phone' | 'rappi' | 'uber_eats' | 'web'
-- status:         'pending' | 'confirmed' | 'preparing' | 'ready' | 'delivered' | 'cancelled'
-- payment_method: 'efectivo' | 'tarjeta_credito' | 'tarjeta_debito' | 'transferencia' | 'rappi' | 'uber_eats' | 'otro'
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id             BIGINT         AUTO_INCREMENT PRIMARY KEY,
    restaurant_id  BIGINT         NOT NULL,
    customer_id    BIGINT         NULL,
    employee_id    BIGINT         NOT NULL,
    driver_id      BIGINT         NULL,
    folio          VARCHAR(40)    NOT NULL UNIQUE,
    order_type     VARCHAR(10)    NOT NULL,
    source         VARCHAR(15)    NOT NULL,
    status         VARCHAR(15)    NOT NULL DEFAULT 'pending',
    total          DECIMAL(10, 2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(25)    NULL,
    created_at     DATETIME       NOT NULL,
    CONSTRAINT fk_order_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT fk_order_customer   FOREIGN KEY (customer_id)   REFERENCES customers (id),
    CONSTRAINT fk_order_employee   FOREIGN KEY (employee_id)   REFERENCES employees (id),
    CONSTRAINT fk_order_driver     FOREIGN KEY (driver_id)     REFERENCES delivery_drivers (id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    menu_item_id BIGINT         NOT NULL,
    variant_id   BIGINT         NULL,
    quantity     INT            NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    subtotal     DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_item_order     FOREIGN KEY (order_id)     REFERENCES orders (id),
    CONSTRAINT fk_order_item_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT fk_order_item_variant   FOREIGN KEY (variant_id)   REFERENCES menu_item_variants (id)
);

CREATE TABLE IF NOT EXISTS order_item_extras (
    id            BIGINT         AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT         NOT NULL,
    menu_item_id  BIGINT         NOT NULL,
    unit_price    DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_extra_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT fk_extra_menu_item  FOREIGN KEY (menu_item_id)  REFERENCES menu_items (id)
);

CREATE TABLE IF NOT EXISTS order_status_history (
    id          BIGINT   AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT   NOT NULL,
    employee_id BIGINT   NULL,
    from_status VARCHAR(15) NULL,
    to_status   VARCHAR(15) NOT NULL,
    changed_at  DATETIME    NOT NULL,
    CONSTRAINT fk_history_order    FOREIGN KEY (order_id)    REFERENCES orders (id),
    CONSTRAINT fk_history_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS payments (
    id            BIGINT         AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT         NOT NULL,
    registered_by BIGINT         NULL,
    method        VARCHAR(25)    NOT NULL,
    amount        DECIMAL(10, 2) NOT NULL,
    reference     VARCHAR(100)   NULL,
    created_at    DATETIME       NOT NULL,
    CONSTRAINT fk_payment_order    FOREIGN KEY (order_id)      REFERENCES orders (id),
    CONSTRAINT fk_payment_employee FOREIGN KEY (registered_by) REFERENCES employees (id)
);

-- ============================================================
-- MÓDULO GASTOS
-- ============================================================
CREATE TABLE IF NOT EXISTS expense_categories (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT      NOT NULL,
    name          VARCHAR(80) NOT NULL,
    type          VARCHAR(40) NULL,
    CONSTRAINT fk_exp_cat_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

CREATE TABLE IF NOT EXISTS expenses (
    id            BIGINT         AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT         NOT NULL,
    category_id   BIGINT         NOT NULL,
    employee_id   BIGINT         NULL,
    concept       VARCHAR(200)   NOT NULL,
    amount        DECIMAL(10, 2) NOT NULL,
    expense_date  DATE           NOT NULL,
    is_active     TINYINT(1)     NOT NULL DEFAULT 1,
    CONSTRAINT fk_expense_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT fk_expense_category   FOREIGN KEY (category_id)   REFERENCES expense_categories (id),
    CONSTRAINT fk_expense_employee   FOREIGN KEY (employee_id)   REFERENCES employees (id)
);

-- ============================================================
-- MÓDULO INGRESOS
-- ============================================================
CREATE TABLE IF NOT EXISTS income_categories (
    id            BIGINT     AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT     NOT NULL,
    name          VARCHAR(80) NOT NULL,
    description   TEXT        NULL,
    is_active     TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_inc_cat_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

CREATE TABLE IF NOT EXISTS incomes (
    id             BIGINT         AUTO_INCREMENT PRIMARY KEY,
    restaurant_id  BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    employee_id    BIGINT         NULL,
    concept        VARCHAR(200)   NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL,
    income_date    DATE           NOT NULL,
    payment_method VARCHAR(25)    NULL,
    is_active      TINYINT(1)     NOT NULL DEFAULT 1,
    CONSTRAINT fk_income_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT fk_income_category   FOREIGN KEY (category_id)   REFERENCES income_categories (id),
    CONSTRAINT fk_income_employee   FOREIGN KEY (employee_id)   REFERENCES employees (id)
);

-- ============================================================
-- MÓDULO VENTAS
-- ============================================================
CREATE TABLE IF NOT EXISTS sales_sources (
    id             BIGINT         AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(60)    NOT NULL,
    commission_pct DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    is_active      TINYINT(1)     NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS sales (
    id            BIGINT         AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT         NOT NULL,
    source_id     BIGINT         NOT NULL,
    employee_id   BIGINT         NULL,
    folio         VARCHAR(40)    NULL,
    total         DECIMAL(10, 2) NOT NULL DEFAULT 0,
    commission    DECIMAL(10, 2) NOT NULL DEFAULT 0,
    sale_date     DATE           NOT NULL,
    is_active     TINYINT(1)     NOT NULL DEFAULT 1,
    CONSTRAINT fk_sale_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT fk_sale_source     FOREIGN KEY (source_id)     REFERENCES sales_sources (id),
    CONSTRAINT fk_sale_employee   FOREIGN KEY (employee_id)   REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS sale_items (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY,
    sale_id      BIGINT         NOT NULL,
    menu_item_id BIGINT         NOT NULL,
    variant_id   BIGINT         NULL,
    quantity     INT            NOT NULL,
    subtotal     DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_sale_item_sale      FOREIGN KEY (sale_id)      REFERENCES sales (id),
    CONSTRAINT fk_sale_item_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT fk_sale_item_variant   FOREIGN KEY (variant_id)   REFERENCES menu_item_variants (id)
);

-- ============================================================
-- MÓDULO CAJA
-- status: 'open' | 'closed'
-- ============================================================
CREATE TABLE IF NOT EXISTS cash_sessions (
    id             BIGINT         AUTO_INCREMENT PRIMARY KEY,
    restaurant_id  BIGINT         NOT NULL,
    opened_by      BIGINT         NOT NULL,
    closed_by      BIGINT         NULL,
    opening_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    expected_cash  DECIMAL(10, 2) NULL     DEFAULT 0,
    counted_cash   DECIMAL(10, 2) NULL     DEFAULT 0,
    difference     DECIMAL(10, 2) NULL     DEFAULT 0,
    total_sales    DECIMAL(10, 2) NULL     DEFAULT 0,
    total_expenses DECIMAL(10, 2) NULL     DEFAULT 0,
    total_incomes  DECIMAL(10, 2) NULL     DEFAULT 0,
    status         VARCHAR(10)    NOT NULL DEFAULT 'open',
    opened_at      DATETIME       NOT NULL,
    closed_at      DATETIME       NULL,
    CONSTRAINT fk_cash_session_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT fk_cash_session_opened_by  FOREIGN KEY (opened_by)     REFERENCES employees (id),
    CONSTRAINT fk_cash_session_closed_by  FOREIGN KEY (closed_by)     REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS cash_session_items (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    cash_session_id BIGINT         NOT NULL,
    method          VARCHAR(25)    NOT NULL,
    expected_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    counted_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    difference      DECIMAL(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_cash_item_session FOREIGN KEY (cash_session_id) REFERENCES cash_sessions (id)
);

-- ============================================================
-- MÓDULO FINANCIERO
-- period_type: 'daily' | 'weekly' | 'monthly'
-- status: 'open' | 'closed'
-- ============================================================
CREATE TABLE IF NOT EXISTS financial_periods (
    id                BIGINT         AUTO_INCREMENT PRIMARY KEY,
    restaurant_id     BIGINT         NOT NULL,
    period_type       VARCHAR(10)    NOT NULL,
    period_label      VARCHAR(30)    NOT NULL,
    start_date        DATE           NOT NULL,
    end_date          DATE           NOT NULL,
    gross_income      DECIMAL(12, 2) NULL DEFAULT 0,
    total_commissions DECIMAL(12, 2) NULL DEFAULT 0,
    total_expenses    DECIMAL(12, 2) NULL DEFAULT 0,
    net_profit        DECIMAL(12, 2) NULL DEFAULT 0,
    payment_breakdown JSON           NULL,
    status            VARCHAR(10)    NOT NULL DEFAULT 'open',
    CONSTRAINT fk_fin_period_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

-- ============================================================
-- Datos iniciales — fuentes de venta por defecto
-- ============================================================
INSERT IGNORE INTO sales_sources (name, commission_pct, is_active) VALUES
    ('Mostrador', 0.00, 1),
    ('WhatsApp',  0.00, 1),
    ('Rappi',    18.00, 1),
    ('Uber Eats', 25.00, 1);
