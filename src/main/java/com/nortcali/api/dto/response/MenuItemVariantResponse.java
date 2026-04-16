package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record MenuItemVariantResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        String name,
        BigDecimal salePrice,
        boolean isActive
) {}
