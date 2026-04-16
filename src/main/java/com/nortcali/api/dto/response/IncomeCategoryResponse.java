package com.nortcali.api.dto.response;

public record IncomeCategoryResponse(Long id, Long restaurantId, String name, String description, boolean isActive) {}
