package com.nortcali.api.dto.response;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponse(
        Long id,
        String fromStatus,
        String toStatus,
        Long employeeId,
        String employeeUsername,
        LocalDateTime changedAt
) {}
