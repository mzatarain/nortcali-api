package com.nortcali.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class SalesSourceRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String name;

    @NotNull(message = "El porcentaje de comisión es obligatorio")
    @DecimalMin(value = "0.0", message = "El porcentaje de comisión no puede ser negativo")
    private BigDecimal commissionPct;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }
}
