package com.nortcali.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class OrderItemRequest {

    @NotNull(message = "El platillo es obligatorio")
    private Long menuItemId;

    private Long variantId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal unitPrice;

    @Size(max = 100)
    private String groupLabel;

    @Valid
    private List<OrderItemModifierRequest> modifiers;

    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getGroupLabel() { return groupLabel; }
    public void setGroupLabel(String groupLabel) { this.groupLabel = groupLabel; }

    public List<OrderItemModifierRequest> getModifiers() { return modifiers; }
    public void setModifiers(List<OrderItemModifierRequest> modifiers) { this.modifiers = modifiers; }
}
