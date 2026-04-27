package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ModifierRequest {

    @NotBlank(message = "El nombre del modificador es obligatorio")
    @Size(max = 100)
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
