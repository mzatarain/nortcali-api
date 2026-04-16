package com.nortcali.api.service;

import com.nortcali.api.dto.request.DeliveryDriverRequest;
import com.nortcali.api.dto.response.DeliveryDriverResponse;

import java.util.List;

public interface DeliveryDriverService {

    List<DeliveryDriverResponse> getByRestaurant(Long restaurantId);

    DeliveryDriverResponse getById(Long id);

    DeliveryDriverResponse create(Long restaurantId, DeliveryDriverRequest request);

    DeliveryDriverResponse update(Long id, DeliveryDriverRequest request);

    void deactivate(Long id);
}
