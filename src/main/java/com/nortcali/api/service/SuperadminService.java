package com.nortcali.api.service;

import com.nortcali.api.dto.request.SuperadminLoginRequest;
import com.nortcali.api.dto.response.LoginResponse;
import com.nortcali.api.dto.response.PlatformMetricsResponse;
import com.nortcali.api.dto.response.SuperadminEmployeeResponse;
import com.nortcali.api.dto.response.SuperadminLoginResponse;
import com.nortcali.api.dto.response.TenantSummaryResponse;

import java.util.List;

public interface SuperadminService {

    SuperadminLoginResponse login(SuperadminLoginRequest request);

    List<TenantSummaryResponse> listTenants();

    /** Fase 9 (plan SaaS): MRR, churn y conteos por estado — ver {@link PlatformMetricsResponse}. */
    PlatformMetricsResponse getMetrics();

    void suspendTenant(Long tenantId);

    void activateTenant(Long tenantId);

    List<SuperadminEmployeeResponse> listTenantEmployees(Long tenantId);

    /**
     * Fase 8 (plan SaaS): emite una sesión válida como el empleado dado, para que el superadmin
     * pueda navegar la app con ese rol sin conocer su contraseña. Queda auditado en
     * {@code impersonation_audit} y la sesión resultante no puede renovarse vía
     * {@code /auth/refresh} — expira sola.
     */
    LoginResponse impersonate(Long employeeId, String superadminEmail, String ipAddress);
}
