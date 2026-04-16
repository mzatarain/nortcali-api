package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.MenuCategoryRequest;
import com.nortcali.api.dto.response.MenuCategoryResponse;
import com.nortcali.api.entity.MenuCategory;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.MenuCategoryMapper;
import com.nortcali.api.repository.MenuCategoryRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.MenuCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class MenuCategoryServiceImpl implements MenuCategoryService {

    private final MenuCategoryRepository categoryRepo;
    private final RestaurantRepository restaurantRepo;
    private final MenuCategoryMapper mapper;

    public MenuCategoryServiceImpl(MenuCategoryRepository categoryRepo,
                                   RestaurantRepository restaurantRepo,
                                   MenuCategoryMapper mapper) {
        this.categoryRepo = categoryRepo;
        this.restaurantRepo = restaurantRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getByRestaurant(Long restaurantId) {
        return categoryRepo.findActiveByRestaurant(restaurantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuCategoryResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public MenuCategoryResponse create(Long restaurantId, MenuCategoryRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        if (categoryRepo.existsByRestaurantIdAndNameAndIsActiveTrue(restaurantId, request.getName())) {
            throw new DuplicateResourceException(
                    "Ya existe una categoría activa con el nombre '" + request.getName() + "' en este restaurante");
        }

        MenuCategory entity = mapper.toEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        log.info("Creando categoría '{}' para restaurante {}", request.getName(), restaurantId);
        return mapper.toResponse(categoryRepo.save(entity));
    }

    @Override
    public MenuCategoryResponse update(Long id, MenuCategoryRequest request) {
        MenuCategory entity = findOrThrow(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(categoryRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        MenuCategory entity = findOrThrow(id);
        entity.setActive(false);
        categoryRepo.save(entity);
        log.info("Categoría {} desactivada", id);
    }

    private MenuCategory findOrThrow(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", id));
    }
}
