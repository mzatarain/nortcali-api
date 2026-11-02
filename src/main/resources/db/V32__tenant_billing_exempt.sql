-- V32 — Fase 13 (go-live): exención de cobro explícita para NortCali (tenant 1).
--
-- Decisión del usuario (2026-11-02): el tenant de NortCali no paga, sin límites de plan
-- (restaurantes/empleados ilimitados) y todas sus sucursales gratis. Si en el futuro se venden
-- franquicias a otros dueños, cada una se da de alta como OTRO tenant (que sí paga).
--
-- `billing_exempt = TRUE` hace que el backend:
--   * no aplique el bloqueo de solo lectura por pago (past_due / trial sin tarjeta) — una
--     suspensión manual del superadmin (suspended) sí sigue aplicando
--   * no aplique límites de plan (PlanLimits)
--   * rechace checkout / portal / cancelación de Stripe y lo ignore en los webhooks
--   * lo excluya del MRR del panel de superadmin
--
-- El objetivo se resuelve por id **y** nombre (igual que V31) — si en otra DB el id 1 es otro
-- negocio, no se marca nada. Script idempotente salvo el ALTER (falla si la columna ya existe,
-- lo cual detiene el cliente mysql antes de tocar datos).

ALTER TABLE tenants
    ADD COLUMN billing_exempt BOOLEAN NOT NULL DEFAULT FALSE AFTER plan;

UPDATE tenants SET billing_exempt = TRUE WHERE id = 1 AND name = 'NortCali';

-- Verificación: debe devolver exactamente 1 fila (NortCali, billing_exempt = 1)
SELECT id, name, status, plan, billing_exempt FROM tenants WHERE billing_exempt = TRUE;
