package com.nortcali.api.service;

import com.nortcali.api.dto.request.DiningTableRequest;
import com.nortcali.api.dto.response.DiningTableResponse;
import com.nortcali.api.entity.DiningTable;

import java.util.List;

public interface DiningTableService {

    List<DiningTableResponse> getByRestaurant(Long restaurantId);

    List<DiningTableResponse> getAvailable(Long restaurantId);

    DiningTableResponse getById(Long id);

    DiningTableResponse create(Long restaurantId, DiningTableRequest request);

    DiningTableResponse update(Long id, DiningTableRequest request);

    void deactivate(Long id);

    DiningTable occupy(Long tableId, Long restaurantId);

    void release(Long tableId);
}
