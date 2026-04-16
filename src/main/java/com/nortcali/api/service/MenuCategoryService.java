package com.nortcali.api.service;

import com.nortcali.api.dto.request.MenuCategoryRequest;
import com.nortcali.api.dto.response.MenuCategoryResponse;

import java.util.List;

public interface MenuCategoryService {

    List<MenuCategoryResponse> getByRestaurant(Long restaurantId);

    MenuCategoryResponse getById(Long id);

    MenuCategoryResponse create(Long restaurantId, MenuCategoryRequest request);

    MenuCategoryResponse update(Long id, MenuCategoryRequest request);

    void deactivate(Long id);
}
