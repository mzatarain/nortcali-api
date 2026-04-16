-- ─────────────────────────────────────────────────────────────────────────────
-- V3: Catálogo de roles de empleados
-- Aplicar antes de arrancar la app:
--   mysql -u root -p nortcali < src/main/resources/db/V3__employee_roles.sql
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS employee_roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_employee_roles_name (name)
);

-- Roles iniciales del sistema
INSERT INTO employee_roles (name, description, is_active) VALUES
    ('ADMIN',    'Administrador del sistema con acceso total',        TRUE),
    ('MANAGER',  'Gerente de restaurante',                            TRUE),
    ('CASHIER',  'Cajero — gestiona pagos y corte de caja',           TRUE),
    ('WAITER',   'Mesero — toma y gestiona órdenes en sala',          TRUE),
    ('KITCHEN',  'Personal de cocina — ve órdenes confirmadas',       TRUE),
    ('DELIVERY', 'Repartidor — gestiona entregas a domicilio',        TRUE);
