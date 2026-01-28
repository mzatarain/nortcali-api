package com.nortcali.api.dto;

public class RestaurantResponseDto {

    private Long id;
    private String name;
    private String phone;
    private String whatsapp;
    private String addressLine;
    private boolean active;
    private Long cityId;
    private String cityName;
    
    public RestaurantResponseDto(
            Long id,
            String name,
            String phone,
            String whatsapp,
            String addressLine,
            boolean active,
            Long cityId,
            String cityName
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.whatsapp = whatsapp;
        this.addressLine = addressLine;
        this.active = active;
        this.cityId = cityId;
        this.cityName = cityName;
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
        return active;
    }


    public Long getCityId() {
        return cityId;
    }
    
    public String getCityName() {
    	return cityName;
    }
}