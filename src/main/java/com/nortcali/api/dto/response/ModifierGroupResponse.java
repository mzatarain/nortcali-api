package com.nortcali.api.dto.response;

public record ModifierGroupResponse(
        Long id,
        Long restaurantId,
        String name,
        boolean isActive
) {}
