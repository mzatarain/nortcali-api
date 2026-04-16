package com.nortcali.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OpenCashSessionRequest {

    @NotNull(message = "El monto de apertura es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto de apertura no puede ser negativo")
    private BigDecimal openingAmount;

    @NotNull(message = "El empleado que abre la caja es obligatorio")
    private Long openedBy;

    public BigDecimal getOpeningAmount() { return openingAmount; }
    public void setOpeningAmount(BigDecimal openingAmount) { this.openingAmount = openingAmount; }

    public Long getOpenedBy() { return openedBy; }
    public void setOpenedBy(Long openedBy) { this.openedBy = openedBy; }
}
