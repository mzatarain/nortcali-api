package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CityRequest {

    @NotBlank(message = "El nombre de la ciudad es obligatorio")
    private String name;

    @NotNull(message = "El estado es obligatorio")
    private Long stateId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getStateId() { return stateId; }
    public void setStateId(Long stateId) { this.stateId = stateId; }
}
