package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CashSessionResponse(
        Long id,
        Long restaurantId,
        Long openedBy,
        Long closedBy,
        BigDecimal openingAmount,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal difference,
        BigDecimal totalSales,
        BigDecimal totalExpenses,
        BigDecimal totalIncomes,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        List<CashSessionItemResponse> items
) {}
