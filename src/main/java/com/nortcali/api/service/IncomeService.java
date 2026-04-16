package com.nortcali.api.service;

import com.nortcali.api.dto.request.IncomeCategoryRequest;
import com.nortcali.api.dto.request.IncomeRequest;
import com.nortcali.api.dto.response.IncomeCategoryResponse;
import com.nortcali.api.dto.response.IncomeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IncomeService {

    List<IncomeCategoryResponse> getCategoriesByRestaurant(Long restaurantId);
    IncomeCategoryResponse createCategory(Long restaurantId, IncomeCategoryRequest request);
    IncomeCategoryResponse updateCategory(Long id, IncomeCategoryRequest request);
    void deactivateCategory(Long id);

    Page<IncomeResponse> getByRestaurant(Long restaurantId, Pageable pageable);
    IncomeResponse getById(Long id);
    IncomeResponse create(Long restaurantId, IncomeRequest request);
    IncomeResponse update(Long id, IncomeRequest request);
    void deactivate(Long id);
}
