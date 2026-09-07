-- Fase 4 (plan SaaS) — antes de permitir el registro de tenants nuevos, `units` y
-- `sales_sources` deben dejar de ser catálogos globales compartidos: sin esto, un tenant
-- nuevo vería y podría editar/borrar las mismas unidades y fuentes de venta (con su
-- commission_pct) que NortCali Pizzeria usa hoy en producción real.
ALTER TABLE units ADD COLUMN tenant_id BIGINT NULL AFTER id;
ALTER TABLE sales_sources ADD COLUMN tenant_id BIGINT NULL AFTER id;

-- Backfill: hoy solo existe un tenant (NortCali) — se le asignan todos los registros
-- existentes. Debe correr antes de que exista un segundo tenant.
UPDATE units SET tenant_id = (SELECT id FROM tenants ORDER BY id LIMIT 1) WHERE tenant_id IS NULL;
UPDATE sales_sources SET tenant_id = (SELECT id FROM tenants ORDER BY id LIMIT 1) WHERE tenant_id IS NULL;

ALTER TABLE units MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE sales_sources MODIFY COLUMN tenant_id BIGINT NOT NULL;

ALTER TABLE units ADD CONSTRAINT fk_unit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE sales_sources ADD CONSTRAINT fk_sales_source_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
