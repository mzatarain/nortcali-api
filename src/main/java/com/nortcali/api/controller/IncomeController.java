package com.nortcali.api.controller;

import com.nortcali.api.dto.request.IncomeCategoryRequest;
import com.nortcali.api.dto.request.IncomeRequest;
import com.nortcali.api.dto.response.IncomeCategoryResponse;
import com.nortcali.api.dto.response.IncomeResponse;
import com.nortcali.api.service.IncomeService;
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
@RequestMapping("/api/v1/restaurants/{restaurantId}")
@Slf4j
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    /* ===== CATEGORIES ===== */
    @GetMapping("/income-categories")
    public ResponseEntity<List<IncomeCategoryResponse>> getCategories(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(incomeService.getCategoriesByRestaurant(restaurantId));
    }

    @PostMapping("/income-categories")
    public ResponseEntity<IncomeCategoryResponse> createCategory(@PathVariable Long restaurantId,
                                                                  @Valid @RequestBody IncomeCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incomeService.createCategory(restaurantId, request));
    }

    @PutMapping("/income-categories/{id}")
    public ResponseEntity<IncomeCategoryResponse> updateCategory(@PathVariable Long restaurantId,
                                                                  @PathVariable Long id,
                                                                  @Valid @RequestBody IncomeCategoryRequest request) {
        return ResponseEntity.ok(incomeService.updateCategory(id, request));
    }

    @DeleteMapping("/income-categories/{id}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long restaurantId, @PathVariable Long id) {
        incomeService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }

    /* ===== INCOMES ===== */
    @GetMapping("/incomes")
    public ResponseEntity<Page<IncomeResponse>> getIncomes(
            @PathVariable Long restaurantId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(incomeService.getByRestaurant(restaurantId, pageable));
    }

    @GetMapping("/incomes/{id}")
    public ResponseEntity<IncomeResponse> getById(@PathVariable Long restaurantId, @PathVariable Long id) {
        return ResponseEntity.ok(incomeService.getById(id));
    }

    @PostMapping("/incomes")
    public ResponseEntity<IncomeResponse> create(@PathVariable Long restaurantId,
                                                 @Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incomeService.create(restaurantId, request));
    }

    @PutMapping("/incomes/{id}")
    public ResponseEntity<IncomeResponse> update(@PathVariable Long restaurantId,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.ok(incomeService.update(id, request));
    }

    @DeleteMapping("/incomes/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long restaurantId, @PathVariable Long id) {
        incomeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
