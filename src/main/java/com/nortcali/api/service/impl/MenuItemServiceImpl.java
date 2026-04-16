package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.MenuItemRequest;
import com.nortcali.api.dto.response.MenuItemResponse;
import com.nortcali.api.entity.MenuItem;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.MenuItemMapper;
import com.nortcali.api.repository.MenuCategoryRepository;
import com.nortcali.api.repository.MenuItemRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.MenuItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository itemRepo;
    private final RestaurantRepository restaurantRepo;
    private final MenuCategoryRepository categoryRepo;
    private final MenuItemMapper mapper;

    public MenuItemServiceImpl(MenuItemRepository itemRepo,
                               RestaurantRepository restaurantRepo,
                               MenuCategoryRepository categoryRepo,
                               MenuItemMapper mapper) {
        this.itemRepo = itemRepo;
        this.restaurantRepo = restaurantRepo;
        this.categoryRepo = categoryRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getByRestaurant(Long restaurantId) {
        return itemRepo.findByRestaurantIdAndIsActiveTrue(restaurantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public MenuItemResponse create(Long restaurantId, MenuItemRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        var category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", request.getCategoryId()));

        // Verificar que la categoría pertenece al mismo restaurante
        if (!category.getRestaurant().getId().equals(restaurantId)) {
            throw new BusinessRuleException("La categoría no pertenece al restaurante indicado");
        }

        MenuItem entity = mapper.toEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        entity.setCategory(category);
        log.info("Creando platillo '{}' para restaurante {}", request.getName(), restaurantId);
        return mapper.toResponse(itemRepo.save(entity));
    }

    @Override
    public MenuItemResponse update(Long id, MenuItemRequest request) {
        MenuItem entity = findOrThrow(id);

        var category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", request.getCategoryId()));

        mapper.updateEntity(request, entity);
        entity.setCategory(category);
        return mapper.toResponse(itemRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        MenuItem entity = findOrThrow(id);
        entity.setActive(false);
        itemRepo.save(entity);
        log.info("Platillo {} desactivado", id);
    }

    private MenuItem findOrThrow(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
    }
}
