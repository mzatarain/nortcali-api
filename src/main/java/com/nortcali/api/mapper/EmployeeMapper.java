package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.EmployeeResponse;
import com.nortcali.api.entity.Employee;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    // password_hash nunca se expone en el response
    EmployeeResponse toResponse(Employee entity);
}
