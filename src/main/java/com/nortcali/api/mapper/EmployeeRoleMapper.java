package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.EmployeeRoleRequest;
import com.nortcali.api.dto.response.EmployeeRoleResponse;
import com.nortcali.api.entity.EmployeeRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeRoleMapper {

    @Mapping(source = "active", target = "isActive")
    EmployeeRoleResponse toResponse(EmployeeRole entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    EmployeeRole toEntity(EmployeeRoleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(EmployeeRoleRequest request, @MappingTarget EmployeeRole entity);
}
