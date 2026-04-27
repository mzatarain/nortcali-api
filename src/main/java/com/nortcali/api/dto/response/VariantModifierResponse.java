package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record VariantModifierResponse(
        Long modifierId,
        String modifierName,
        Long groupId,
        String groupName,
        BigDecimal price
) {}
