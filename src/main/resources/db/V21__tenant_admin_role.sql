-- Fase 2 del plan SaaS (ver ../../../../../../SAAS-TASKS.md) — catálogo únicamente.
--
-- TENANT_ADMIN es un nivel adicional por encima de ADMIN: administra la suscripción/
-- facturación del tenant (Fase 3, Stripe). No reemplaza a ADMIN — la gestión operativa del
-- restaurante sigue igual. Se agrega el catálogo aquí para no bloquear Fase 3; todavía no se
-- asigna a ningún empleado ni se referencia en SecurityConfig/@PreAuthorize — eso llega junto
-- con el primer caso de uso real de facturación.

INSERT INTO employee_roles (name, description, is_active) VALUES
    ('TENANT_ADMIN', 'Administra la suscripción y facturación del tenant (por encima de ADMIN)', TRUE);
