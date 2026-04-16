package com.nortcali.api.service;

import com.nortcali.api.dto.request.InventoryMovementRequest;
import com.nortcali.api.dto.response.InventoryMovementResponse;

import java.util.List;

public interface InventoryMovementService {

    List<InventoryMovementResponse> getBySupply(Long supplyId);

    InventoryMovementResponse register(Long supplyId, InventoryMovementRequest request);
}
