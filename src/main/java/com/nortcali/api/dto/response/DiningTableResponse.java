package com.nortcali.api.dto.response;

public record DiningTableResponse(
        Long id,
        Long restaurantId,
        String name,
        Integer capacity,
        String status,
        boolean isActive
) {}
