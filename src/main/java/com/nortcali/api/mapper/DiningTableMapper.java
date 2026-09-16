package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.DiningTableRequest;
import com.nortcali.api.dto.response.DiningTableResponse;
import com.nortcali.api.entity.DiningTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DiningTableMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "active", target = "isActive")
    @Mapping(expression = "java(entity.getStatus().name().toLowerCase())", target = "status")
    DiningTableResponse toResponse(DiningTable entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "status", ignore = true)
    DiningTable toEntity(DiningTableRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(DiningTableRequest request, @MappingTarget DiningTable entity);
}
