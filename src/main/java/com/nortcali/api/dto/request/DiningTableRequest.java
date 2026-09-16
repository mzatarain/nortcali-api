package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DiningTableRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 40, message = "El nombre no puede superar los 40 caracteres")
    private String name;

    private Integer capacity;

    private boolean isActive = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}
