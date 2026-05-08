package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Long variantId,
        String variantName,
        Integer quantity,
        BigDecimal subtotal,
        String groupLabel
) {}
