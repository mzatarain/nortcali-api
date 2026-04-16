package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String method,
        BigDecimal amount,
        String reference,
        Long registeredBy,
        LocalDateTime createdAt
) {}
