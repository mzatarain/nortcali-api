package com.nortcali.api.service;

import com.nortcali.api.dto.request.ComboRequest;
import com.nortcali.api.dto.response.ComboResponse;

import java.util.List;

public interface ComboService {
    List<ComboResponse> getByRestaurant(Long restaurantId);
    ComboResponse create(Long restaurantId, ComboRequest request);
    ComboResponse update(Long restaurantId, Long id, ComboRequest request);
    void delete(Long restaurantId, Long id);
}
