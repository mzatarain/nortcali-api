package com.nortcali.api.dto.response;

public record MenuItemResponse(
        Long id,
        Long restaurantId,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        boolean isActive
) {}
