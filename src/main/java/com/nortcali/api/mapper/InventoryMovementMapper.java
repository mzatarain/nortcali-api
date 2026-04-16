package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.InventoryMovementResponse;
import com.nortcali.api.entity.InventoryMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMovementMapper {

    @Mapping(source = "supply.id", target = "supplyId")
    @Mapping(source = "supply.name", target = "supplyName")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.username", target = "employeeUsername")
    @Mapping(expression = "java(entity.getMovementType().name().toLowerCase())", target = "movementType")
    InventoryMovementResponse toResponse(InventoryMovement entity);
}
