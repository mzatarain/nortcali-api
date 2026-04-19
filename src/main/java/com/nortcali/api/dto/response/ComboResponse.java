package com.nortcali.api.dto.response;

import java.util.List;

public record ComboResponse(
        Long id,
        Long restaurantId,
        String name,
        String description,
        String salePrice,
        boolean isActive,
        List<ComboItemResponse> items
) {}
