package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.MenuItemVariantRequest;
import com.nortcali.api.dto.response.MenuItemVariantResponse;
import com.nortcali.api.entity.MenuItem;
import com.nortcali.api.entity.MenuItemVariant;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.MenuItemVariantMapper;
import com.nortcali.api.repository.MenuItemRepository;
import com.nortcali.api.repository.MenuItemVariantRepository;
import com.nortcali.api.service.MenuItemVariantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class MenuItemVariantServiceImpl implements MenuItemVariantService {

    private final MenuItemVariantRepository variantRepo;
    private final MenuItemRepository itemRepo;
    private final MenuItemVariantMapper mapper;

    public MenuItemVariantServiceImpl(MenuItemVariantRepository variantRepo,
                                      MenuItemRepository itemRepo,
                                      MenuItemVariantMapper mapper) {
        this.variantRepo = variantRepo;
        this.itemRepo = itemRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemVariantResponse> getByMenuItem(Long restaurantId, Long menuItemId) {
        requireMenuItemInRestaurant(restaurantId, menuItemId);
        return variantRepo.findByMenuItemIdAndIsActiveTrue(menuItemId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemVariantResponse getById(Long restaurantId, Long id) {
        return mapper.toResponse(findOrThrow(restaurantId, id));
    }

    @Override
    public MenuItemVariantResponse create(Long restaurantId, Long menuItemId, MenuItemVariantRequest request) {
        var menuItem = requireMenuItemInRestaurant(restaurantId, menuItemId);

        MenuItemVariant entity = mapper.toEntity(request);
        entity.setMenuItem(menuItem);
        log.info("Creando variante '{}' para platillo {}", request.getName(), menuItemId);
        return mapper.toResponse(variantRepo.save(entity));
    }

    @Override
    public MenuItemVariantResponse update(Long restaurantId, Long id, MenuItemVariantRequest request) {
        MenuItemVariant entity = findOrThrow(restaurantId, id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(variantRepo.save(entity));
    }

    @Override
    public void deactivate(Long restaurantId, Long id) {
        MenuItemVariant entity = findOrThrow(restaurantId, id);
        entity.setActive(false);
        variantRepo.save(entity);
        log.info("Variante {} desactivada", id);
    }

    private MenuItemVariant findOrThrow(Long restaurantId, Long id) {
        MenuItemVariant entity = variantRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", id));
        if (!entity.getMenuItem().getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("MenuItemVariant", id);
        }
        return entity;
    }

    private MenuItem requireMenuItemInRestaurant(Long restaurantId, Long menuItemId) {
        var menuItem = itemRepo.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", menuItemId));
        if (!menuItem.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("MenuItem", menuItemId);
        }
        return menuItem;
    }
}
