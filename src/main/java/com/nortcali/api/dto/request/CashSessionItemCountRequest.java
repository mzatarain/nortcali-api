package com.nortcali.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CashSessionItemCountRequest {

    @NotBlank(message = "El método de pago es obligatorio")
    private String method;

    @NotNull(message = "El monto contado es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto contado no puede ser negativo")
    private BigDecimal countedAmount;

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public BigDecimal getCountedAmount() { return countedAmount; }
    public void setCountedAmount(BigDecimal countedAmount) { this.countedAmount = countedAmount; }
}
