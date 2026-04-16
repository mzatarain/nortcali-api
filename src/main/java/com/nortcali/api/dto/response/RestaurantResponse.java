package com.nortcali.api.dto.response;

public record RestaurantResponse(
        Long id,
        String name,
        String phone,
        String whatsapp,
        String addressLine,
        boolean isActive,
        Long cityId,
        String cityName
) {}
