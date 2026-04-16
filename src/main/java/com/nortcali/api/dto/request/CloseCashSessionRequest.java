package com.nortcali.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CloseCashSessionRequest {

    @NotNull(message = "El empleado que cierra la caja es obligatorio")
    private Long closedBy;

    @NotEmpty(message = "Debe proveer el conteo por método de pago")
    @Valid
    private List<CashSessionItemCountRequest> countedAmounts;

    public Long getClosedBy() { return closedBy; }
    public void setClosedBy(Long closedBy) { this.closedBy = closedBy; }

    public List<CashSessionItemCountRequest> getCountedAmounts() { return countedAmounts; }
    public void setCountedAmounts(List<CashSessionItemCountRequest> countedAmounts) { this.countedAmounts = countedAmounts; }
}
