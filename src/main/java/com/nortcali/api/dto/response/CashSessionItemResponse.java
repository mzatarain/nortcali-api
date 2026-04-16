package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record CashSessionItemResponse(
        Long id,
        String method,
        BigDecimal expectedAmount,
        BigDecimal countedAmount,
        BigDecimal difference
) {}
