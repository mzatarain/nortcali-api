package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Long variantId,
        String variantName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        List<OrderItemModifierResponse> modifiers
) {}
