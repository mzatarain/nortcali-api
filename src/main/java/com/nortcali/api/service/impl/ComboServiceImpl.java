package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.ComboItemRequest;
import com.nortcali.api.dto.request.ComboRequest;
import com.nortcali.api.dto.response.ComboResponse;
import com.nortcali.api.entity.Combo;
import com.nortcali.api.entity.ComboItem;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.ComboMapper;
import com.nortcali.api.repository.ComboRepository;
import com.nortcali.api.repository.MenuItemRepository;
import com.nortcali.api.repository.MenuItemVariantRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.ComboService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class ComboServiceImpl implements ComboService {

    private final ComboRepository comboRepo;
    private final RestaurantRepository restaurantRepo;
    private final MenuItemRepository menuItemRepo;
    private final MenuItemVariantRepository variantRepo;
    private final ComboMapper mapper;

    public ComboServiceImpl(ComboRepository comboRepo,
                            RestaurantRepository restaurantRepo,
                            MenuItemRepository menuItemRepo,
                            MenuItemVariantRepository variantRepo,
                            ComboMapper mapper) {
        this.comboRepo = comboRepo;
        this.restaurantRepo = restaurantRepo;
        this.menuItemRepo = menuItemRepo;
        this.variantRepo = variantRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComboResponse> getByRestaurant(Long restaurantId) {
        return comboRepo.findByRestaurantId(restaurantId).stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    public ComboResponse create(Long restaurantId, ComboRequest request) {
        var restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        Combo combo = new Combo();
        combo.setRestaurant(restaurant);
        applyFields(combo, request);
        addItems(combo, request);

        log.info("Creando combo '{}' para restaurante {}", request.getName(), restaurantId);
        return mapper.toResponse(comboRepo.save(combo));
    }

    @Override
    public ComboResponse update(Long restaurantId, Long id, ComboRequest request) {
        Combo combo = findOrThrow(id, restaurantId);
        applyFields(combo, request);
        combo.getItems().clear();
        addItems(combo, request);
        return mapper.toResponse(comboRepo.save(combo));
    }

    @Override
    public void delete(Long restaurantId, Long id) {
        Combo combo = findOrThrow(id, restaurantId);
        combo.setActive(false);
        comboRepo.save(combo);
    }

    private void applyFields(Combo combo, ComboRequest req) {
        combo.setName(req.getName());
        combo.setDescription(req.getDescription());
        combo.setSalePrice(req.getSalePrice());
        combo.setActive(req.isActive());
    }

    private void addItems(Combo combo, ComboRequest req) {
        for (ComboItemRequest itemReq : req.getItems()) {
            ComboItem item = new ComboItem();
            item.setCombo(combo);
            item.setQuantity(itemReq.getQuantity());
            item.setMenuItem(menuItemRepo.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemReq.getMenuItemId())));
            if (itemReq.getVariantId() != null) {
                item.setVariant(variantRepo.findById(itemReq.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", itemReq.getVariantId())));
            }
            combo.getItems().add(item);
        }
    }

    private Combo findOrThrow(Long id, Long restaurantId) {
        Combo combo = comboRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo", id));
        if (!combo.getRestaurant().getId().equals(restaurantId)) {
            throw new BusinessRuleException("El combo no pertenece al restaurante indicado");
        }
        return combo;
    }
}
