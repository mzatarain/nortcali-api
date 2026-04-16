package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record SalesSourceResponse(
        Long id,
        String name,
        BigDecimal commissionPct,
        boolean isActive
) {}
