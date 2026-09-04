package com.nortcali.api.service;

import com.nortcali.api.dto.request.RecipeRequest;
import com.nortcali.api.dto.response.RecipeResponse;

import java.util.List;

public interface RecipeService {

    RecipeResponse getByMenuItem(Long restaurantId, Long menuItemId);

    List<RecipeResponse> getAllByMenuItem(Long restaurantId, Long menuItemId);

    RecipeResponse createOrUpdate(Long restaurantId, Long menuItemId, RecipeRequest request);
}
