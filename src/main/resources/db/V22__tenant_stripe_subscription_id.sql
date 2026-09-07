-- Fase 3 (plan SaaS) — fix de cambio de plan: sin guardar la suscripción vigente de
-- Stripe, el backend no tenía forma de saber que ya existía una suscripción activa y
-- creaba una Checkout Session nueva en cada cambio de plan, apilando suscripciones
-- duplicadas (y cobros duplicados) para el mismo tenant.
ALTER TABLE tenants
    ADD COLUMN stripe_subscription_id VARCHAR(255) NULL AFTER stripe_customer_id;
