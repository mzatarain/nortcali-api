package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.IncomeCategoryRequest;
import com.nortcali.api.dto.request.IncomeRequest;
import com.nortcali.api.dto.response.IncomeCategoryResponse;
import com.nortcali.api.dto.response.IncomeResponse;
import com.nortcali.api.entity.Income;
import com.nortcali.api.entity.IncomeCategory;
import com.nortcali.api.entity.enums.PaymentMethod;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.IncomeMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.service.IncomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepo;
    private final IncomeCategoryRepository categoryRepo;
    private final RestaurantRepository restaurantRepo;
    private final EmployeeRepository employeeRepo;
    private final IncomeMapper mapper;

    public IncomeServiceImpl(IncomeRepository incomeRepo,
                             IncomeCategoryRepository categoryRepo,
                             RestaurantRepository restaurantRepo,
                             EmployeeRepository employeeRepo,
                             IncomeMapper mapper) {
        this.incomeRepo = incomeRepo;
        this.categoryRepo = categoryRepo;
        this.restaurantRepo = restaurantRepo;
        this.employeeRepo = employeeRepo;
        this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public List<IncomeCategoryResponse> getCategoriesByRestaurant(Long restaurantId) {
        return categoryRepo.findByRestaurantIdAndIsActiveTrue(restaurantId).stream().map(mapper::toCategoryResponse).toList();
    }

    @Override
    public IncomeCategoryResponse createCategory(Long restaurantId, IncomeCategoryRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        IncomeCategory entity = mapper.toCategoryEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        return mapper.toCategoryResponse(categoryRepo.save(entity));
    }

    @Override
    public IncomeCategoryResponse updateCategory(Long id, IncomeCategoryRequest request) {
        IncomeCategory entity = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncomeCategory", id));
        mapper.updateCategory(request, entity);
        return mapper.toCategoryResponse(categoryRepo.save(entity));
    }

    @Override
    public void deactivateCategory(Long id) {
        IncomeCategory entity = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncomeCategory", id));
        entity.setActive(false);
        categoryRepo.save(entity);
    }

    @Override @Transactional(readOnly = true)
    public Page<IncomeResponse> getByRestaurant(Long restaurantId, Pageable pageable) {
        return incomeRepo.findByRestaurantIdAndIsActiveTrueOrderByIncomeDateDesc(restaurantId, pageable)
                .map(mapper::toResponse);
    }

    @Override @Transactional(readOnly = true)
    public IncomeResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public IncomeResponse create(Long restaurantId, IncomeRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        var category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("IncomeCategory", request.getCategoryId()));
        var employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

        Income entity = mapper.toEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        entity.setCategory(category);
        entity.setEmployee(employee);
        if (request.getPaymentMethod() != null) {
            entity.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        }
        return mapper.toResponse(incomeRepo.save(entity));
    }

    @Override
    public IncomeResponse update(Long id, IncomeRequest request) {
        Income entity = findOrThrow(id);
        var category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("IncomeCategory", request.getCategoryId()));
        var employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));
        mapper.updateEntity(request, entity);
        entity.setCategory(category);
        entity.setEmployee(employee);
        if (request.getPaymentMethod() != null) {
            entity.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        }
        return mapper.toResponse(incomeRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        Income entity = findOrThrow(id);
        entity.setActive(false);
        incomeRepo.save(entity);
    }

    private Income findOrThrow(Long id) {
        return incomeRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Income", id));
    }
}
