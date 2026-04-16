package com.nortcali.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RecipeIngredientRequest {

    @NotNull(message = "El insumo es obligatorio")
    private Long supplyId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
    private BigDecimal quantity;

    @NotNull(message = "La unidad de medida es obligatoria")
    private Long unitId;

    public Long getSupplyId() { return supplyId; }
    public void setSupplyId(Long supplyId) { this.supplyId = supplyId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
}
