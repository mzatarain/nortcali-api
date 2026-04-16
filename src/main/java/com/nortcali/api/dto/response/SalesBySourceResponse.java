package com.nortcali.api.dto.response;

import java.math.BigDecimal;

public record SalesBySourceResponse(String sourceName, Long saleCount, BigDecimal totalAmount) {}
