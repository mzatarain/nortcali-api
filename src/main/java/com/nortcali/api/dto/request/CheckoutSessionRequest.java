package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CheckoutSessionRequest {

    @NotBlank(message = "El plan es obligatorio")
    @Pattern(regexp = "basico|pro|enterprise", message = "Plan inválido — valores permitidos: basico, pro, enterprise")
    private String plan;

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
}
