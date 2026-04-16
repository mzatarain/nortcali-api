package com.nortcali.api.dto.response;

public record EmployeeRoleResponse(
        Long id,
        String name,
        String description,
        boolean isActive
) {}
