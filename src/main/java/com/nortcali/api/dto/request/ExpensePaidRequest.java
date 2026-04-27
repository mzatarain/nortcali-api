package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotNull;

public class ExpensePaidRequest {

    @NotNull(message = "isPaid es obligatorio")
    private Boolean isPaid;

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }
}
