package com.nortcali.api.service;

import com.nortcali.api.dto.request.EmployeeRoleRequest;
import com.nortcali.api.dto.response.EmployeeRoleResponse;

import java.util.List;

public interface EmployeeRoleService {

    List<EmployeeRoleResponse> getAll();

    EmployeeRoleResponse getById(Long id);

    EmployeeRoleResponse create(EmployeeRoleRequest request);

    EmployeeRoleResponse update(Long id, EmployeeRoleRequest request);

    void deactivate(Long id);
}
