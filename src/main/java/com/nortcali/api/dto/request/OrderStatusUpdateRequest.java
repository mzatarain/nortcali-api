package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderStatusUpdateRequest {

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String toStatus;

    @NotNull(message = "El empleado que realiza el cambio es obligatorio")
    private Long employeeId;

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
}
