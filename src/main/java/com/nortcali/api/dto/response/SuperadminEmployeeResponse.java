package com.nortcali.api.dto.response;

import java.util.List;

/** Fase 8 (plan SaaS): fila del picker de empleados a impersonar, dentro de un tenant. */
public record SuperadminEmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        String status,
        List<String> restaurantNames
) {}
