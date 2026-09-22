-- Cancelación de suscripción desde la app: la suscripción de Stripe se marca con
-- `cancel_at_period_end` en vez de cancelarse de inmediato — el tenant conserva acceso
-- hasta que termine el periodo ya pagado. Esta columna refleja ese estado en la DB para
-- que el frontend pueda mostrarlo sin depender de una consulta directa a Stripe.
ALTER TABLE tenants
    ADD COLUMN cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE AFTER stripe_subscription_id;
