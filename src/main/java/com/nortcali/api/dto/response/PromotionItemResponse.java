package com.nortcali.api.dto.response;

public record PromotionItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Long variantId,
        String variantName
) {}
