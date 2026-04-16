package com.nortcali.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SaleItemRequest {

    @NotNull(message = "El platillo es obligatorio")
    private Long menuItemId;

    private Long variantId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1)
    private Integer quantity;

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.0")
    private BigDecimal subtotal;

    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
