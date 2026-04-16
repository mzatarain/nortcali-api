package com.nortcali.api.service;

import com.nortcali.api.dto.request.ExpenseCategoryRequest;
import com.nortcali.api.dto.request.ExpenseRequest;
import com.nortcali.api.dto.response.ExpenseCategoryResponse;
import com.nortcali.api.dto.response.ExpenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExpenseService {

    List<ExpenseCategoryResponse> getCategoriesByRestaurant(Long restaurantId);
    ExpenseCategoryResponse createCategory(Long restaurantId, ExpenseCategoryRequest request);
    ExpenseCategoryResponse updateCategory(Long id, ExpenseCategoryRequest request);
    void deleteCategory(Long id);

    Page<ExpenseResponse> getByRestaurant(Long restaurantId, Pageable pageable);
    ExpenseResponse getById(Long id);
    ExpenseResponse create(Long restaurantId, ExpenseRequest request);
    ExpenseResponse update(Long id, ExpenseRequest request);
    void deactivate(Long id);
}
