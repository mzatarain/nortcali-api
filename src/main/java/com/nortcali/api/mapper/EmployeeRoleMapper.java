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

    EmployeeRole toEntity(EmployeeRoleRequest request);

    void updateEntity(EmployeeRoleRequest request, @MappingTarget EmployeeRole entity);
}
