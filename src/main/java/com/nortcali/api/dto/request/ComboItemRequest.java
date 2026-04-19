package com.nortcali.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ComboItemRequest {

    @NotNull(message = "El menu_item_id es obligatorio")
    private Long menuItemId;

    private Long variantId;

    @Min(value = 1, message = "La cantidad mínima es 1")
    private int quantity = 1;

    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
