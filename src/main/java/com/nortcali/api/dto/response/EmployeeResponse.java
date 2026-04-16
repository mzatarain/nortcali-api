package com.nortcali.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String username,
        String phone,
        String email,
        String role,
        String status,
        boolean locked,
        LocalDate hireDate,
        LocalDateTime lastLogin
) {}
