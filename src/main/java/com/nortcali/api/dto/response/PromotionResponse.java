package com.nortcali.api.dto.response;

import java.util.List;

public record PromotionResponse(
        Long id,
        Long restaurantId,
        String name,
        String description,
        String type,
        String discountValue,
        String startDate,
        String endDate,
        boolean isActive,
        List<PromotionItemResponse> items
) {}
