package com.nortcali.api.service;

import com.nortcali.api.dto.request.RecipeRequest;
import com.nortcali.api.dto.response.RecipeResponse;

public interface RecipeService {

    RecipeResponse getByMenuItem(Long menuItemId);

    RecipeResponse createOrUpdate(Long menuItemId, RecipeRequest request);
}
