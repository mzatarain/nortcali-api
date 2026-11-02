package com.nortcali.api.dto.response;

import com.nortcali.api.entity.enums.TenantStatus;

import java.time.LocalDateTime;

/** Fase 8 (plan SaaS): fila del listado de tenants del panel de superadmin. */
public record TenantSummaryResponse(
        Long id,
        String name,
        TenantStatus status,
        String plan,
        LocalDateTime createdAt,
        LocalDateTime trialEndsAt,
        int restaurantCount,
        long employeeCount,
        LocalDateTime lastConnection,
        /** Fase 13 (go-live): exento de cobro — se muestra como "Cortesía" y no cuenta en el MRR. */
        boolean billingExempt
) {}
