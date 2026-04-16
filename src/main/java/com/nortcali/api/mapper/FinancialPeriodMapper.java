package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.FinancialPeriodResponse;
import com.nortcali.api.entity.FinancialPeriod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FinancialPeriodMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(expression = "java(entity.getPeriodType().name().toLowerCase())", target = "periodType")
    FinancialPeriodResponse toResponse(FinancialPeriod entity);
}
