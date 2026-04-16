package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialPeriodResponse(
        Long id,
        Long restaurantId,
        String periodType,
        String periodLabel,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal grossIncome,
        BigDecimal totalCommissions,
        BigDecimal totalExpenses,
        BigDecimal netProfit,
        String paymentBreakdown,
        String status
) {}
