package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExpenseCategoryRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80)
    private String name;

    @Size(max = 40)
    private String type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
