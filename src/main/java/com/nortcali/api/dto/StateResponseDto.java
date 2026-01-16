package com.nortcali.api.dto;

public class StateResponseDto {

    private Long id;
    private String name;
    private String code;
    private Long countryId;

    public StateResponseDto(Long id, String name, String code, Long countryId) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.countryId = countryId;
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
}