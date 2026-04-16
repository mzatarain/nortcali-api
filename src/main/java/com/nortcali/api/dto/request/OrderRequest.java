package com.nortcali.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderRequest {

    @NotBlank(message = "El tipo de orden es obligatorio (dine_in, takeout, delivery)")
    private String orderType;

    @NotBlank(message = "La fuente es obligatoria (pos, whatsapp, phone, rappi, uber_eats, web)")
    private String source;

    private Long customerId;

    private Long driverId;

    @NotNull(message = "El empleado es obligatorio")
    private Long employeeId;

    private String paymentMethod;

    @NotEmpty(message = "La orden debe tener al menos un ítem")
    @Valid
    private List<OrderItemRequest> items;

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
