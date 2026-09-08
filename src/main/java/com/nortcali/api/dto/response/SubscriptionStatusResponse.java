package com.nortcali.api.dto.response;

import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
        String status,
        String plan,
        LocalDateTime trialEndsAt,
        boolean hasStripeCustomer,
        /** Fase 7 (plan SaaS): {@code true} solo cuando el checkout (con tarjeta) ya se completó. */
        boolean hasSubscription
) {}
