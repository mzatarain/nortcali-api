package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StateRequest {

    @NotBlank(message = "El nombre del estado es obligatorio")
    private String name;

    @Size(max = 10, message = "El código no puede superar los 10 caracteres")
    private String code;

    @NotNull(message = "El país es obligatorio")
    private Long countryId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getCountryId() { return countryId; }
    public void setCountryId(Long countryId) { this.countryId = countryId; }
}
