package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.SupplyRequest;
import com.nortcali.api.dto.response.SupplyResponse;
import com.nortcali.api.entity.Supply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SupplyMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "unit.id", target = "unitId")
    @Mapping(source = "unit.name", target = "unitName")
    @Mapping(source = "unit.abbreviation", target = "unitAbbreviation")
    @Mapping(source = "active", target = "isActive")
    @Mapping(expression = "java(entity.getCurrentStock().compareTo(entity.getMinimumStock()) < 0)", target = "isBelowMinimum")
    SupplyResponse toResponse(Supply entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "unit", ignore = true)
    Supply toEntity(SupplyRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "unit", ignore = true)
    void updateEntity(SupplyRequest request, @MappingTarget Supply entity);
}
