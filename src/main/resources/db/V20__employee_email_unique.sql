-- Fase 2 del plan SaaS (ver ../../../../../../SAAS-TASKS.md) — login por email.
--
-- Antes de aplicar en un ambiente con datos reales, verificar que no haya empleados sin
-- email o con email duplicado — el ALTER falla si los hay:
--
--   SELECT email, COUNT(*) FROM employees GROUP BY email HAVING COUNT(*) > 1;
--   SELECT id, username FROM employees WHERE email IS NULL OR email = '';
--
-- Verificado en local (2026-09-05): los 4 empleados existentes tienen email poblado y
-- único, aplica sin backfill previo.

ALTER TABLE employees MODIFY COLUMN email VARCHAR(150) NOT NULL;

ALTER TABLE employees ADD CONSTRAINT uq_employees_email UNIQUE (email);
