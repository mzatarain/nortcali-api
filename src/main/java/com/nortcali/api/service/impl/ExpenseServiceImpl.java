package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.ExpenseCategoryRequest;
import com.nortcali.api.dto.request.ExpenseRequest;
import com.nortcali.api.dto.response.ExpenseCategoryResponse;
import com.nortcali.api.dto.response.ExpenseResponse;
import com.nortcali.api.entity.Expense;
import com.nortcali.api.entity.ExpenseCategory;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.ExpenseMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.service.ExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepo;
    private final ExpenseCategoryRepository categoryRepo;
    private final RestaurantRepository restaurantRepo;
    private final EmployeeRepository employeeRepo;
    private final ExpenseMapper mapper;

    public ExpenseServiceImpl(ExpenseRepository expenseRepo,
                              ExpenseCategoryRepository categoryRepo,
                              RestaurantRepository restaurantRepo,
                              EmployeeRepository employeeRepo,
                              ExpenseMapper mapper) {
        this.expenseRepo = expenseRepo;
        this.categoryRepo = categoryRepo;
        this.restaurantRepo = restaurantRepo;
        this.employeeRepo = employeeRepo;
        this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> getCategoriesByRestaurant(Long restaurantId) {
        return categoryRepo.findByRestaurantId(restaurantId).stream().map(mapper::toCategoryResponse).toList();
    }

    @Override
    public ExpenseCategoryResponse createCategory(Long restaurantId, ExpenseCategoryRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        ExpenseCategory entity = mapper.toCategoryEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        return mapper.toCategoryResponse(categoryRepo.save(entity));
    }

    @Override
    public ExpenseCategoryResponse updateCategory(Long id, ExpenseCategoryRequest request) {
        ExpenseCategory entity = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", id));
        mapper.updateCategory(request, entity);
        return mapper.toCategoryResponse(categoryRepo.save(entity));
    }

    @Override
    public void deleteCategory(Long id) {
        if (!categoryRepo.existsById(id)) throw new ResourceNotFoundException("ExpenseCategory", id);
        categoryRepo.deleteById(id);
    }

    @Override @Transactional(readOnly = true)
    public Page<ExpenseResponse> getByRestaurant(Long restaurantId, Pageable pageable) {
        return expenseRepo.findByRestaurantIdAndIsActiveTrueOrderByExpenseDateDesc(restaurantId, pageable)
                .map(mapper::toResponse);
    }

    @Override @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public ExpenseResponse create(Long restaurantId, ExpenseRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        var category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", request.getCategoryId()));
        var employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

        Expense entity = mapper.toEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        entity.setCategory(category);
        entity.setEmployee(employee);
        return mapper.toResponse(expenseRepo.save(entity));
    }

    @Override
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense entity = findOrThrow(id);
        var category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", request.getCategoryId()));
        var employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));
        mapper.updateEntity(request, entity);
        entity.setCategory(category);
        entity.setEmployee(employee);
        return mapper.toResponse(expenseRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        Expense entity = findOrThrow(id);
        entity.setActive(false);
        expenseRepo.save(entity);
    }

    private Expense findOrThrow(Long id) {
        return expenseRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }
}
