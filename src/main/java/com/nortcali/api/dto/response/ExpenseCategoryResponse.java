package com.nortcali.api.dto.response;

public record ExpenseCategoryResponse(Long id, Long restaurantId, String name, String type, boolean isActive) {}
