package com.nortcali.api.service;

import com.nortcali.api.dto.request.FinancialPeriodRequest;
import com.nortcali.api.dto.response.FinancialPeriodResponse;
import com.nortcali.api.dto.response.FinancialSummaryResponse;

import java.util.List;

public interface FinancialService {

    FinancialSummaryResponse getSummary(Long restaurantId, String period, String dateParam);

    List<FinancialPeriodResponse> getPeriods(Long restaurantId);

    FinancialPeriodResponse createPeriod(Long restaurantId, FinancialPeriodRequest request);

    FinancialPeriodResponse closePeriod(Long restaurantId, Long periodId);
}
