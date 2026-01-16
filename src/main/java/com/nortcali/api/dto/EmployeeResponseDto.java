package com.nortcali.api.dto;

public class EmployeeResponseDto {

    private Long id;
    private String username;
    private String role;
    private String status;
    private boolean isLocked;
    private Long restaurantId;

    public EmployeeResponseDto(
            Long id,
            String username,
            String role,
            String status,
            boolean isLocked,
            Long restaurantId
    ) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
        this.isLocked = isLocked;
        this.restaurantId = restaurantId;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }
}