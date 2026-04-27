package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record OrderItemModifierResponse(
        Long id,
        Long modifierId,
        String modifierName,
        String groupName,
        BigDecimal price
) {}
