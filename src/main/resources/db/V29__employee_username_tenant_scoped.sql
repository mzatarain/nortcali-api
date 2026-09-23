-- `employees.username` ya no es el identificador de login (Fase 2 lo reemplazó por `email`),
-- pero conservaba una constraint UNIQUE global heredada del diseño anterior — bloqueaba el alta
-- de un empleado en un tenant si CUALQUIER otro tenant ya tenía ese username, aunque no hubiera
-- ninguna relación entre los dos negocios. Se escopa la unicidad a nivel tenant: se denormaliza
-- `tenant_id` en `employees` (mismo patrón ya usado con `restaurant_id`) y la constraint pasa a
-- ser compuesta (tenant_id, username).
ALTER TABLE employees ADD COLUMN tenant_id BIGINT NULL AFTER restaurant_id;

UPDATE employees e
    JOIN restaurants r ON r.id = e.restaurant_id
    SET e.tenant_id = r.tenant_id;

ALTER TABLE employees MODIFY COLUMN tenant_id BIGINT NOT NULL;
ALTER TABLE employees ADD CONSTRAINT fk_employee_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id);

ALTER TABLE employees DROP INDEX username;
ALTER TABLE employees ADD CONSTRAINT uq_employees_tenant_username UNIQUE (tenant_id, username);
