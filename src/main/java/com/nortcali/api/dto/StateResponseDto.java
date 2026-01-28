package com.nortcali.api.dto;

public class StateResponseDto {

    private Long id;
    private String name;
    private String code;
    private Long countryId;
    private String countryName;

    public StateResponseDto(Long id, String name, String code, Long countryId, String countryName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.countryId = countryId;
        this.countryName = countryName;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Long getCountryId() {
        return countryId;
    }
    
    public String getCountryName() {
    	return countryName;
    }
}