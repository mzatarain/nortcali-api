-- Fase 1 del plan SaaS (ver ../../../../../../SAAS-TASKS.md) — fundamento de multi-tenancy.
-- Un tenant puede tener N restaurantes (cadenas/multi-sucursal desde el día 1).

CREATE TABLE tenants (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    name               VARCHAR(150) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'active',
    plan               VARCHAR(50)  NULL,
    stripe_customer_id VARCHAR(255) NULL,
    trial_ends_at      DATETIME     NULL,
    created_at         TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Backfill: tenant único para los datos existentes, sin pérdida de datos ni downtime.
INSERT INTO tenants (name, status, created_at)
VALUES ('NortCali', 'active', NOW());

ALTER TABLE restaurants ADD COLUMN tenant_id BIGINT NULL;

UPDATE restaurants SET tenant_id = (SELECT id FROM tenants WHERE name = 'NortCali' LIMIT 1);

ALTER TABLE restaurants MODIFY COLUMN tenant_id BIGINT NOT NULL;

ALTER TABLE restaurants
    ADD CONSTRAINT fk_restaurant_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id);
