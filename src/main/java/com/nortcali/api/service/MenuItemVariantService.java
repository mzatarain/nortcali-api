package com.nortcali.api.service;

import com.nortcali.api.dto.request.MenuItemVariantRequest;
import com.nortcali.api.dto.response.MenuItemVariantResponse;

import java.util.List;

public interface MenuItemVariantService {

    List<MenuItemVariantResponse> getByMenuItem(Long menuItemId);

    MenuItemVariantResponse getById(Long id);

    MenuItemVariantResponse create(Long menuItemId, MenuItemVariantRequest request);

    MenuItemVariantResponse update(Long id, MenuItemVariantRequest request);

    void deactivate(Long id);
}
