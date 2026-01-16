package com.nortcali.api.dto;

public class RestaurantResponseDto {

    private Long id;
    private String name;
    private String phone;
    private String whatsapp;
    private String addressLine;
    private boolean isActive;

    private Long countryId;
    private Long stateId;
    private Long cityId;

    public RestaurantResponseDto(
            Long id,
            String name,
            String phone,
            String whatsapp,
            String addressLine,
            boolean isActive,
            Long countryId,
            Long stateId,
            Long cityId
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.whatsapp = whatsapp;
        this.addressLine = addressLine;
        this.isActive = isActive;
        this.countryId = countryId;
        this.stateId = stateId;
        this.cityId = cityId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public boolean isActive() {
        return isActive;
    }

    public Long getCountryId() {
        return countryId;
    }

    public Long getStateId() {
        return stateId;
    }

    public Long getCityId() {
        return cityId;
    }
}