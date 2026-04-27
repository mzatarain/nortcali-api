package com.nortcali.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class SaleRequest {

    @NotNull(message = "La fuente de venta es obligatoria")
    private Long sourceId;

    @NotNull(message = "La fecha de venta es obligatoria")
    private LocalDate saleDate;

    @NotNull(message = "El empleado es obligatorio")
    private Long employeeId;

    private String paymentMethod;

    private String notes;

    @NotEmpty(message = "La venta debe tener al menos un ítem")
    @Valid
    private List<SaleItemRequest> items;

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<SaleItemRequest> getItems() { return items; }
    public void setItems(List<SaleItemRequest> items) { this.items = items; }
}
