package com.nortcali.api.dto.response;

import java.math.BigDecimal;

/**
 * Fase 9 (plan SaaS): métricas de negocio del panel de superadmin — se derivan enteramente de
 * {@code tenants} (estado, plan) y de {@code employees.last_login}, sin llamadas a Stripe.
 * {@code mrr}: suma del precio de plan de los tenants {@code ACTIVE} (los únicos que se
 * consideran ingreso recurrente confirmado; {@code PAST_DUE} se excluye por estar en riesgo).
 * {@code churnRatePercent}: {@code cancelledTenants / (activeTenants + pastDueTenants + cancelledTenants)}
 * — una tasa acumulada de todo el histórico, no mensual (no existe timestamp de cambio de estado
 * previo a esta fase para calcular ventanas de tiempo).
 */
public record PlatformMetricsResponse(
        long totalTenants,
        long trialTenants,
        long activeTenants,
        long pastDueTenants,
        long suspendedTenants,
        long cancelledTenants,
        BigDecimal mrr,
        BigDecimal churnRatePercent,
        long inactiveTenants
) {}
