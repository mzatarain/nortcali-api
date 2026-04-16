package com.nortcali.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SaleResponse(
        Long id,
        Long restaurantId,
        Long sourceId,
        String sourceName,
        String folio,
        BigDecimal total,
        BigDecimal commission,
        LocalDate saleDate,
        Long employeeId,
        boolean isActive,
        List<SaleItemResponse> items
) {}
