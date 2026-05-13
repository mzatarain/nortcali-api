package com.nortcali.api.dto.response;

public record RestaurantResponse(
        Long id,
        String name,
        String phone,
        String whatsapp,
        String addressLine,
        boolean isActive,
        String timezone,
        String clabeAccount,
        Long cityId,
        String cityName
) {}
