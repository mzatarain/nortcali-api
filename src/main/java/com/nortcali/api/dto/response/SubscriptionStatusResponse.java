package com.nortcali.api.dto.response;

import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
        String status,
        String plan,
        LocalDateTime trialEndsAt,
        boolean hasStripeCustomer,
        /** Fase 7 (plan SaaS): {@code true} solo cuando el checkout (con tarjeta) ya se completó. */
        boolean hasSubscription,
        /** {@code true} cuando la suscripción fue cancelada pero sigue activa hasta el fin del periodo pagado. */
        boolean cancelAtPeriodEnd,
        /** Fase 13 (go-live): {@code true} para un tenant exento de cobro (NortCali) — "plan cortesía". */
        boolean billingExempt
) {}
