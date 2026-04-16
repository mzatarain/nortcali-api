package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryMovementResponse(
        Long id,
        Long supplyId,
        String supplyName,
        String movementType,
        BigDecimal quantity,
        Long employeeId,
        String employeeUsername,
        LocalDateTime createdAt
) {}
