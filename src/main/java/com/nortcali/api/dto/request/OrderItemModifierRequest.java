package com.nortcali.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OrderItemModifierRequest {

    @NotNull(message = "El modificador es obligatorio")
    private Long modifierId;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal price;

    public Long getModifierId() { return modifierId; }
    public void setModifierId(Long modifierId) { this.modifierId = modifierId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
