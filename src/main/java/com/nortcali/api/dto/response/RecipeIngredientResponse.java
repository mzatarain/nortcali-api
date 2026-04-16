package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record RecipeIngredientResponse(
        Long id,
        Long supplyId,
        String supplyName,
        BigDecimal quantity,
        Long unitId,
        String unitAbbreviation,
        BigDecimal calculatedCost
) {}
