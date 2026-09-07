-- `restaurants.address_line` era NOT NULL en la BD pero opcional en la entidad `Restaurant` y en
-- `RestaurantRequest` (nunca se exige al crear un restaurante). Bloqueaba con
-- `SQLIntegrityConstraintViolationException` cualquier alta sin dirección — incluida "Nueva
-- sucursal" (BranchesPage, Fase 1 del plan SaaS) y el registro de un tenant nuevo (Fase 4),
-- descubierto al probar el flujo de registro end-to-end por primera vez en esta sesión.

ALTER TABLE restaurants MODIFY COLUMN address_line VARCHAR(255) NULL;
