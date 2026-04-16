package com.nortcali.api.controller;

import com.nortcali.api.dto.request.CloseCashSessionRequest;
import com.nortcali.api.dto.request.OpenCashSessionRequest;
import com.nortcali.api.dto.response.CashSessionResponse;
import com.nortcali.api.service.CashSessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class CashSessionController {

    private final CashSessionService cashSessionService;

    public CashSessionController(CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
    }

    @PostMapping("/restaurants/{restaurantId}/cash-sessions/open")
    public ResponseEntity<CashSessionResponse> open(@PathVariable Long restaurantId,
                                                     @Valid @RequestBody OpenCashSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cashSessionService.open(restaurantId, request));
    }

    @PostMapping("/cash-sessions/{id}/close")
    public ResponseEntity<CashSessionResponse> close(@PathVariable Long id,
                                                      @Valid @RequestBody CloseCashSessionRequest request) {
        return ResponseEntity.ok(cashSessionService.close(id, request));
    }

    @GetMapping("/restaurants/{restaurantId}/cash-sessions/current")
    public ResponseEntity<CashSessionResponse> getCurrent(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(cashSessionService.getCurrent(restaurantId));
    }
}
