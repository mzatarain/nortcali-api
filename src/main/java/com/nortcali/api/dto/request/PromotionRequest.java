package com.nortcali.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PromotionRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    private String name;

    @Size(max = 400)
    private String description;

    @NotBlank(message = "El tipo es obligatorio")
    private String type;

    private BigDecimal discountValue;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    private boolean isActive = true;

    @NotNull(message = "Los items son obligatorios")
    @Size(min = 1, message = "La promoción debe tener al menos 1 item")
    @Valid
    private List<PromotionItemRequest> items;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public List<PromotionItemRequest> getItems() { return items; }
    public void setItems(List<PromotionItemRequest> items) { this.items = items; }
}
