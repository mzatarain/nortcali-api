-- V31 — Fase 13 (go-live): borra los tenants de prueba 3, 8 y 9 y todo lo que cuelga de ellos.
--
-- Datos de prueba creados durante el desarrollo del SaaS (no clientes reales):
--   3 "Tacos El Compa"  → restaurante 3, empleado 5 (juanpereze2etest)
--   8 "Fase7 Test Biz"  → restaurante 8, empleado 10 (fase7test…)
--   9 "El vagabundo"    → restaurante 9, empleado 11 (daniel)
-- Hard-delete deliberado: son datos de prueba, no aplica la regla de soft-delete.
--
-- Seguridad para correrlo en RDS:
--   * Los objetivos se resuelven por id **y** nombre — si en otra DB el id 3/8/9 es otro
--     negocio, ese tenant no se toca.
--   * Todo va en una transacción. Las FK con RESTRICT/NO ACTION hacen fallar el script si
--     algún tenant tuviera datos no contemplados aquí (órdenes, ventas, etc.); el cliente
--     mysql se detiene en el primer error y la transacción no se confirma.
--   * Nota de schema: en employee_restaurants, `restaurant_id` es el id del EMPLEADO y
--     `restaurants_id` el del restaurante (nombres heredados).
--
-- Respaldo previo: mysqldump nortcali > backup_pre_v31.sql (local) / snapshot manual (RDS).

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_test_tenants AS
SELECT id FROM tenants
WHERE (id, name) IN ((3, 'Tacos El Compa'), (8, 'Fase7 Test Biz'), (9, 'El vagabundo'));

CREATE TEMPORARY TABLE tmp_test_restaurants AS
SELECT id FROM restaurants WHERE tenant_id IN (SELECT id FROM tmp_test_tenants);

CREATE TEMPORARY TABLE tmp_test_employees AS
SELECT id FROM employees WHERE tenant_id IN (SELECT id FROM tmp_test_tenants);

-- Lo que se va a borrar (para revisar en la salida)
SELECT 'tenants' AS objetivo, COUNT(*) AS filas FROM tmp_test_tenants
UNION ALL SELECT 'restaurants', COUNT(*) FROM tmp_test_restaurants
UNION ALL SELECT 'employees', COUNT(*) FROM tmp_test_employees;

-- Dependencias de empleados
DELETE FROM sessions              WHERE employee_id IN (SELECT id FROM tmp_test_employees);
DELETE FROM password_reset_tokens WHERE employee_id IN (SELECT id FROM tmp_test_employees);
DELETE FROM impersonation_audit   WHERE employee_id IN (SELECT id FROM tmp_test_employees)
                                     OR tenant_id IN (SELECT id FROM tmp_test_tenants);
DELETE FROM employee_restaurants  WHERE restaurant_id  IN (SELECT id FROM tmp_test_employees)
                                     OR restaurants_id IN (SELECT id FROM tmp_test_restaurants);
DELETE FROM employees             WHERE id IN (SELECT id FROM tmp_test_employees);

-- Catálogos por restaurante / tenant (sembrados por el onboarding)
DELETE FROM menu_categories WHERE restaurant_id IN (SELECT id FROM tmp_test_restaurants);
DELETE FROM sales_sources   WHERE tenant_id     IN (SELECT id FROM tmp_test_tenants);
DELETE FROM units           WHERE tenant_id     IN (SELECT id FROM tmp_test_tenants);

-- Restaurantes y tenants
DELETE FROM restaurants WHERE id IN (SELECT id FROM tmp_test_restaurants);
DELETE FROM tenants     WHERE id IN (SELECT id FROM tmp_test_tenants);

-- Verificación: deben quedar 0
SELECT 'tenants restantes' AS chequeo, COUNT(*) AS filas FROM tenants WHERE id IN (3, 8, 9)
UNION ALL SELECT 'restaurants restantes', COUNT(*) FROM restaurants WHERE tenant_id IN (3, 8, 9)
UNION ALL SELECT 'employees restantes', COUNT(*) FROM employees WHERE tenant_id IN (3, 8, 9);

DROP TEMPORARY TABLE tmp_test_employees;
DROP TEMPORARY TABLE tmp_test_restaurants;
DROP TEMPORARY TABLE tmp_test_tenants;

COMMIT;
