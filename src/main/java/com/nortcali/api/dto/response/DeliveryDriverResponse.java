package com.nortcali.api.dto.response;

public record DeliveryDriverResponse(
        Long id,
        Long restaurantId,
        String firstName,
        String phone,
        String vehicle,
        boolean isActive
) {}
