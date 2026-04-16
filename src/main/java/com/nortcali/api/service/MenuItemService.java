package com.nortcali.api.service;

import com.nortcali.api.dto.request.MenuItemRequest;
import com.nortcali.api.dto.response.MenuItemResponse;

import java.util.List;

public interface MenuItemService {

    List<MenuItemResponse> getByRestaurant(Long restaurantId);

    MenuItemResponse getById(Long id);

    MenuItemResponse create(Long restaurantId, MenuItemRequest request);

    MenuItemResponse update(Long id, MenuItemRequest request);

    void deactivate(Long id);
}
