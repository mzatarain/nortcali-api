package com.nortcali.api.dto;

public class CountryResponseDto {

    private Long id;
    private String name;
    private String isoCode;

    public CountryResponseDto(Long id, String name, String isoCode) {
        this.id = id;
        this.name = name;
        this.isoCode = isoCode;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIsoCode() {
        return isoCode;
    }
}