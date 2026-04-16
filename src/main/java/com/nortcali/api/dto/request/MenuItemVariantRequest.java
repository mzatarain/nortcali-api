package com.nortcali.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class MenuItemVariantRequest {

    @NotBlank(message = "El nombre de la variante es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String name;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo")
    private BigDecimal salePrice;

    private boolean isActive = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}
