-- V33 — Fase 13 (go-live): limpia los IDs de Stripe del tenant 1 (NortCali).
--
-- Son del sandbox de pruebas (`NortCali · sandbox`, acct_1UCmmuJ7gw4BVDFw) y no existen en la
-- cuenta live: con ellos BillingPage mostraba "Administrar facturación"/"Cancelar" y cualquier
-- llamada a Stripe live fallaba. NortCali está exento de cobro (V32), así que no necesita
-- customer ni suscripción en Stripe.
--
-- Se condiciona a los IDs exactos del sandbox: si el tenant ya tuviera otros IDs (live), no se
-- tocan.

UPDATE tenants
SET stripe_customer_id     = NULL,
    stripe_subscription_id = NULL,
    cancel_at_period_end   = 0
WHERE id = 1
  AND name = 'NortCali'
  AND stripe_customer_id = 'cus_VDKieQEvbuOF2N'
  AND stripe_subscription_id = 'sub_1UCuBDJ7gw4BVDFwKDqO8D8y';

-- Verificación: stripe_customer_id / stripe_subscription_id en NULL
SELECT id, name, status, plan, billing_exempt, stripe_customer_id, stripe_subscription_id, cancel_at_period_end
FROM tenants WHERE id = 1;
