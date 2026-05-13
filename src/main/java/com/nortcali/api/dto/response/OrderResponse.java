package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long restaurantId,
        String folio,
        String orderType,
        String source,
        String status,
        BigDecimal total,
        String paymentMethod,
        Long customerId,
        String customerFirstName,
        Long employeeId,
        String employeeUsername,
        Long driverId,
        String driverFirstName,
        LocalDateTime createdAt,
        LocalDateTime preparingAt,
        LocalDateTime readyAt,
        Integer preparationTimeSeconds,
        List<OrderItemResponse> items,
        Long saleId,
        String notes
) {}
