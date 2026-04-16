package com.nortcali.api.controller;

import com.nortcali.api.dto.request.FinancialPeriodRequest;
import com.nortcali.api.dto.response.FinancialPeriodResponse;
import com.nortcali.api.dto.response.FinancialSummaryResponse;
import com.nortcali.api.service.FinancialService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/financial")
@Slf4j
public class FinancialController {

    private final FinancialService financialService;

    public FinancialController(FinancialService financialService) {
        this.financialService = financialService;
    }

    /**
     * GET /api/v1/restaurants/{id}/financial/summary?period=daily&date=2026-04-14
     * GET /api/v1/restaurants/{id}/financial/summary?period=monthly&month=2026-04
     */
    @GetMapping("/summary")
    public ResponseEntity<FinancialSummaryResponse> getSummary(
            @PathVariable Long restaurantId,
            @RequestParam String period,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month) {

        String dateParam = "daily".equalsIgnoreCase(period) ? date : month;
        return ResponseEntity.ok(financialService.getSummary(restaurantId, period, dateParam));
    }

    @GetMapping("/periods")
    public ResponseEntity<List<FinancialPeriodResponse>> getPeriods(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(financialService.getPeriods(restaurantId));
    }

    @PostMapping("/periods")
    public ResponseEntity<FinancialPeriodResponse> createPeriod(@PathVariable Long restaurantId,
                                                                 @Valid @RequestBody FinancialPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financialService.createPeriod(restaurantId, request));
    }

    @PostMapping("/periods/{periodId}/close")
    public ResponseEntity<FinancialPeriodResponse> closePeriod(@PathVariable Long restaurantId,
                                                                @PathVariable Long periodId) {
        return ResponseEntity.ok(financialService.closePeriod(periodId));
    }
}
