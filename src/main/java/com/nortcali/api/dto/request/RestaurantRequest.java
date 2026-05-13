package com.nortcali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RestaurantRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 20)
    private String whatsapp;

    @Size(max = 255)
    private String addressLine;

    @NotNull(message = "La ciudad es obligatoria")
    private Long cityId;

    private boolean isActive = true;

    @Size(max = 50)
    private String timezone;

    @Size(max = 18)
    private String clabeAccount;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getClabeAccount() { return clabeAccount; }
    public void setClabeAccount(String clabeAccount) { this.clabeAccount = clabeAccount; }
}
