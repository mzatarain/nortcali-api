package com.nortcali.api.controller;

import com.nortcali.api.dto.request.ExpenseCategoryRequest;
import com.nortcali.api.dto.request.ExpensePaidRequest;
import com.nortcali.api.dto.request.ExpenseRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import com.nortcali.api.dto.response.ExpenseCategoryResponse;
import com.nortcali.api.dto.response.ExpenseResponse;
import com.nortcali.api.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}")
@Slf4j
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /* ===== CATEGORIES ===== */
    @GetMapping("/expense-categories")
    public ResponseEntity<List<ExpenseCategoryResponse>> getCategories(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(expenseService.getCategoriesByRestaurant(restaurantId));
    }

    @PostMapping("/expense-categories")
    public ResponseEntity<ExpenseCategoryResponse> createCategory(@PathVariable Long restaurantId,
                                                                   @Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createCategory(restaurantId, request));
    }

    @PutMapping("/expense-categories/{id}")
    public ResponseEntity<ExpenseCategoryResponse> updateCategory(@PathVariable Long restaurantId,
                                                                   @PathVariable Long id,
                                                                   @Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(expenseService.updateCategory(id, request));
    }

    @DeleteMapping("/expense-categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long restaurantId, @PathVariable Long id) {
        expenseService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    /* ===== EXPENSES ===== */
    @GetMapping("/expenses")
    public ResponseEntity<Page<ExpenseResponse>> getExpenses(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getByRestaurant(restaurantId, startDate, endDate, pageable));
    }

    @GetMapping("/expenses/{id}")
    public ResponseEntity<ExpenseResponse> getById(@PathVariable Long restaurantId, @PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getById(id));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> create(@PathVariable Long restaurantId,
                                                  @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(restaurantId, request));
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<ExpenseResponse> update(@PathVariable Long restaurantId,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/expenses/{id}/paid")
    public ResponseEntity<ExpenseResponse> updatePaidStatus(@PathVariable Long restaurantId,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody ExpensePaidRequest request) {
        return ResponseEntity.ok(expenseService.updatePaidStatus(restaurantId, id, request));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long restaurantId, @PathVariable Long id) {
        expenseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
