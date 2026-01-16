package com.nortcali.api.dto;

public class CityResponseDto {

    private Long id;
    private String name;
    private Long stateId;

    public CityResponseDto(Long id, String name, Long stateId) {
        this.id = id;
        this.name = name;
        this.stateId = stateId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getStateId() {
        return stateId;
    }
}