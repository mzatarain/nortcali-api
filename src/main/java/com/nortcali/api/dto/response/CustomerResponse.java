package com.nortcali.api.dto.response;

public record CustomerResponse(
        Long id,
        Long restaurantId,
        String firstName,
        String phone,
        String address,
        Integer totalOrders,
        boolean isActive
) {}
