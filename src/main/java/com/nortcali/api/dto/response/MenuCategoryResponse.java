package com.nortcali.api.dto.response;

public record MenuCategoryResponse(
        Long id,
        Long restaurantId,
        String name,
        Integer displayOrder,
        boolean isActive
) {}
