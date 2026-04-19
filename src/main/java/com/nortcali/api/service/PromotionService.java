package com.nortcali.api.service;

import com.nortcali.api.dto.request.PromotionRequest;
import com.nortcali.api.dto.response.PromotionResponse;

import java.util.List;

public interface PromotionService {
    List<PromotionResponse> getByRestaurant(Long restaurantId);
    PromotionResponse create(Long restaurantId, PromotionRequest request);
    PromotionResponse update(Long restaurantId, Long id, PromotionRequest request);
    void delete(Long restaurantId, Long id);
}
