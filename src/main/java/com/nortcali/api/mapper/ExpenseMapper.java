package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.ExpenseCategoryRequest;
import com.nortcali.api.dto.request.ExpenseRequest;
import com.nortcali.api.dto.response.ExpenseCategoryResponse;
import com.nortcali.api.dto.response.ExpenseResponse;
import com.nortcali.api.entity.Expense;
import com.nortcali.api.entity.ExpenseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    ExpenseCategoryResponse toCategoryResponse(ExpenseCategory entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    ExpenseCategory toCategoryEntity(ExpenseCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateCategory(ExpenseCategoryRequest request, @MappingTarget ExpenseCategory entity);

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "active", target = "isActive")
    ExpenseResponse toResponse(Expense entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "employee", ignore = true)
    Expense toEntity(ExpenseRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "employee", ignore = true)
    void updateEntity(ExpenseRequest request, @MappingTarget Expense entity);
}
