package com.nortcali.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AddOrderItemsRequest {

    @NotEmpty(message = "Debe incluir al menos un ítem para agregar")
    @Valid
    private List<OrderItemRequest> items;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
