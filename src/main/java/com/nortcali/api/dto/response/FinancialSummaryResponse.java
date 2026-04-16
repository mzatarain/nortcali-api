package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        String period,
        BigDecimal grossIncome,
        BigDecimal totalCommissions,
        BigDecimal totalExpenses,
        BigDecimal totalIncomes,
        BigDecimal netProfit
) {}
