package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        Long restaurantId,
        Long categoryId,
        String categoryName,
        String concept,
        BigDecimal amount,
        LocalDate expenseDate,
        Long employeeId,
        boolean isActive
) {}
