package com.nortcali.api.service;

import com.nortcali.api.dto.request.MenuItemVariantRequest;
import com.nortcali.api.dto.response.MenuItemVariantResponse;

import java.util.List;

public interface MenuItemVariantService {

    List<MenuItemVariantResponse> getByMenuItem(Long restaurantId, Long menuItemId);

    MenuItemVariantResponse getById(Long restaurantId, Long id);

    MenuItemVariantResponse create(Long restaurantId, Long menuItemId, MenuItemVariantRequest request);

    MenuItemVariantResponse update(Long restaurantId, Long id, MenuItemVariantRequest request);

    void deactivate(Long restaurantId, Long id);
}
