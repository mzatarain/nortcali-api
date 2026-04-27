package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ModifierGroupRequest {

    @NotBlank(message = "El nombre del grupo es obligatorio")
    @Size(max = 100)
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
