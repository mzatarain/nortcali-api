package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RecipeResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Long variantId,
        String variantName,
        Integer portions,
        boolean isActive,
        List<RecipeIngredientResponse> ingredients,
        BigDecimal totalCost
) {}
