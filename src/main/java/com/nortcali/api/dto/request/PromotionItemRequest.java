package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotNull;

public class PromotionItemRequest {

    @NotNull(message = "El menu_item_id es obligatorio")
    private Long menuItemId;

    private Long variantId;

    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
}
