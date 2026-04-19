package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.PromotionItemRequest;
import com.nortcali.api.dto.request.PromotionRequest;
import com.nortcali.api.dto.response.PromotionResponse;
import com.nortcali.api.entity.Promotion;
import com.nortcali.api.entity.PromotionItem;
import com.nortcali.api.entity.enums.PromotionType;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.PromotionMapper;
import com.nortcali.api.repository.MenuItemRepository;
import com.nortcali.api.repository.MenuItemVariantRepository;
import com.nortcali.api.repository.PromotionRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.PromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepo;
    private final RestaurantRepository restaurantRepo;
    private final MenuItemRepository menuItemRepo;
    private final MenuItemVariantRepository variantRepo;
    private final PromotionMapper mapper;

    public PromotionServiceImpl(PromotionRepository promotionRepo,
                                RestaurantRepository restaurantRepo,
                                MenuItemRepository menuItemRepo,
                                MenuItemVariantRepository variantRepo,
                                PromotionMapper mapper) {
        this.promotionRepo = promotionRepo;
        this.restaurantRepo = restaurantRepo;
        this.menuItemRepo = menuItemRepo;
        this.variantRepo = variantRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getByRestaurant(Long restaurantId) {
        return promotionRepo.findByRestaurantId(restaurantId).stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    public PromotionResponse create(Long restaurantId, PromotionRequest request) {
        var restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        Promotion promotion = new Promotion();
        promotion.setRestaurant(restaurant);
        applyFields(promotion, request);
        addItems(promotion, request);

        log.info("Creando promoción '{}' para restaurante {}", request.getName(), restaurantId);
        return mapper.toResponse(promotionRepo.save(promotion));
    }

    @Override
    public PromotionResponse update(Long restaurantId, Long id, PromotionRequest request) {
        Promotion promotion = findOrThrow(id, restaurantId);
        applyFields(promotion, request);
        promotion.getItems().clear();
        addItems(promotion, request);
        return mapper.toResponse(promotionRepo.save(promotion));
    }

    @Override
    public void delete(Long restaurantId, Long id) {
        Promotion promotion = findOrThrow(id, restaurantId);
        promotion.setActive(false);
        promotionRepo.save(promotion);
    }

    private void applyFields(Promotion promotion, PromotionRequest req) {
        promotion.setName(req.getName());
        promotion.setDescription(req.getDescription());
        promotion.setType(PromotionType.fromValue(req.getType()));
        promotion.setDiscountValue(req.getDiscountValue());
        promotion.setStartDate(req.getStartDate());
        promotion.setEndDate(req.getEndDate());
        promotion.setActive(req.isActive());
    }

    private void addItems(Promotion promotion, PromotionRequest req) {
        for (PromotionItemRequest itemReq : req.getItems()) {
            PromotionItem item = new PromotionItem();
            item.setPromotion(promotion);
            item.setMenuItem(menuItemRepo.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemReq.getMenuItemId())));
            if (itemReq.getVariantId() != null) {
                item.setVariant(variantRepo.findById(itemReq.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", itemReq.getVariantId())));
            }
            promotion.getItems().add(item);
        }
    }

    private Promotion findOrThrow(Long id, Long restaurantId) {
        Promotion promotion = promotionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
        if (!promotion.getRestaurant().getId().equals(restaurantId)) {
            throw new BusinessRuleException("La promoción no pertenece al restaurante indicado");
        }
        return promotion;
    }
}
