package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeResponse(
        Long id,
        Long restaurantId,
        Long categoryId,
        String categoryName,
        String concept,
        BigDecimal amount,
        LocalDate incomeDate,
        String paymentMethod,
        Long employeeId,
        boolean isActive
) {}
