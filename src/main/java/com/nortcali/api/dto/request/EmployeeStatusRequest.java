package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class EmployeeStatusRequest {

    @NotBlank(message = "El estado es obligatorio")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
