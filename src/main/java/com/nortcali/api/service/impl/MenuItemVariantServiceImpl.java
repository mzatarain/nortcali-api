package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.MenuItemVariantRequest;
import com.nortcali.api.dto.response.MenuItemVariantResponse;
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
    public List<MenuItemVariantResponse> getByMenuItem(Long menuItemId) {
        return variantRepo.findByMenuItemIdAndIsActiveTrue(menuItemId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemVariantResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public MenuItemVariantResponse create(Long menuItemId, MenuItemVariantRequest request) {
        var menuItem = itemRepo.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", menuItemId));

        MenuItemVariant entity = mapper.toEntity(request);
        entity.setMenuItem(menuItem);
        log.info("Creando variante '{}' para platillo {}", request.getName(), menuItemId);
        return mapper.toResponse(variantRepo.save(entity));
    }

    @Override
    public MenuItemVariantResponse update(Long id, MenuItemVariantRequest request) {
        MenuItemVariant entity = findOrThrow(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(variantRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        MenuItemVariant entity = findOrThrow(id);
        entity.setActive(false);
        variantRepo.save(entity);
        log.info("Variante {} desactivada", id);
    }

    private MenuItemVariant findOrThrow(Long id) {
        return variantRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", id));
    }
}
