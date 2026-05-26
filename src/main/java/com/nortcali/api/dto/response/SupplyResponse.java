package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record SupplyResponse(
        Long id,
        Long restaurantId,
        String name,
        String supplier,
        Long unitId,
        String unitName,
        String unitAbbreviation,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        BigDecimal unitCost,
        boolean isActive,
        boolean isBelowMinimum
) {}
