package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.IncomeCategoryRequest;
import com.nortcali.api.dto.request.IncomeRequest;
import com.nortcali.api.dto.response.IncomeCategoryResponse;
import com.nortcali.api.dto.response.IncomeResponse;
import com.nortcali.api.entity.Income;
import com.nortcali.api.entity.IncomeCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface IncomeMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "active", target = "isActive")
    IncomeCategoryResponse toCategoryResponse(IncomeCategory entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    IncomeCategory toCategoryEntity(IncomeCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateCategory(IncomeCategoryRequest request, @MappingTarget IncomeCategory entity);

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "active", target = "isActive")
    @Mapping(expression = "java(entity.getPaymentMethod() != null ? entity.getPaymentMethod().name().toLowerCase() : null)", target = "paymentMethod")
    IncomeResponse toResponse(Income entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    Income toEntity(IncomeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    void updateEntity(IncomeRequest request, @MappingTarget Income entity);
}
