package com.nortcali.api.dto;

public class CityResponseDto {

    private Long id;
    private String name;
    private Long stateId;
    private String stateName;

    public CityResponseDto(Long id, String name, Long stateId, String stateName) {
        this.id = id;
        this.name = name;
        this.stateId = stateId;
        this.stateName = stateName;
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
    
    public String getStateName() {
    	return stateName;
    }
}