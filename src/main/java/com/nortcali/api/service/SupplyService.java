package com.nortcali.api.service;

import com.nortcali.api.dto.request.SupplyRequest;
import com.nortcali.api.dto.response.SupplyResponse;

import java.util.List;

public interface SupplyService {

    List<SupplyResponse> getByRestaurant(Long restaurantId);

    SupplyResponse getById(Long id);

    List<SupplyResponse> getLowStock(Long restaurantId);

    SupplyResponse create(Long restaurantId, SupplyRequest request);

    SupplyResponse update(Long id, SupplyRequest request);

    void deactivate(Long id);
}
