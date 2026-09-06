package com.nortcali.api.dto.response;

import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
        String status,
        String plan,
        LocalDateTime trialEndsAt,
        boolean hasStripeCustomer
) {}
