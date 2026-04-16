package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record OrderItemExtraResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        BigDecimal unitPrice
) {}
