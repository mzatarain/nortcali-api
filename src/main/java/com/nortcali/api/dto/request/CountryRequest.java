package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CountryRequest {

    @NotBlank(message = "El nombre del país es obligatorio")
    private String name;

    @Size(max = 3, message = "El código ISO no puede superar los 3 caracteres")
    private String isoCode;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
}
