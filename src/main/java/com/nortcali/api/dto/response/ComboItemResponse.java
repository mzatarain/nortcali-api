package com.nortcali.api.dto.response;

public record ComboItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Long variantId,
        String variantName,
        int quantity
) {}
