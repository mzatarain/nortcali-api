package com.nortcali.api.controller;

import com.nortcali.api.dto.request.SaleRequest;
import com.nortcali.api.dto.response.SaleResponse;
import com.nortcali.api.dto.response.SalesBySourceResponse;
import com.nortcali.api.service.SaleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/sales")
@Slf4j
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public ResponseEntity<Page<SaleResponse>> getAll(@PathVariable Long restaurantId,
                                                     @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(saleService.getByRestaurant(restaurantId, pageable));
    }

    @GetMapping("/by-source")
    public ResponseEntity<List<SalesBySourceResponse>> getBySource(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(saleService.getSalesBySource(restaurantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getById(@PathVariable Long restaurantId, @PathVariable Long id) {
        return ResponseEntity.ok(saleService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SaleResponse> create(@PathVariable Long restaurantId,
                                               @Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.create(restaurantId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long restaurantId, @PathVariable Long id) {
        saleService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
