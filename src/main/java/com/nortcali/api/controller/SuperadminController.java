package com.nortcali.api.controller;

import com.nortcali.api.config.LoginRateLimiter;
import com.nortcali.api.dto.request.SuperadminLoginRequest;
import com.nortcali.api.dto.response.LoginResponse;
import com.nortcali.api.dto.response.SuperadminEmployeeResponse;
import com.nortcali.api.dto.response.SuperadminLoginResponse;
import com.nortcali.api.dto.response.TenantSummaryResponse;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.service.SuperadminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Fase 8 (plan SaaS): panel de administración interno — fuera del modelo multi-tenant de clientes. */
@RestController
@RequestMapping("/api/v1/superadmin")
public class SuperadminController {

    private final SuperadminService superadminService;
    private final LoginRateLimiter loginRateLimiter;

    public SuperadminController(SuperadminService superadminService, LoginRateLimiter loginRateLimiter) {
        this.superadminService = superadminService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<SuperadminLoginResponse> login(@Valid @RequestBody SuperadminLoginRequest request,
                                                          HttpServletRequest httpRequest) {
        if (!loginRateLimiter.tryConsume(httpRequest.getRemoteAddr())) {
            throw new BusinessRuleException("Demasiados intentos de inicio de sesión. Intenta de nuevo en un minuto.");
        }
        return ResponseEntity.ok(superadminService.login(request));
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantSummaryResponse>> listTenants() {
        return ResponseEntity.ok(superadminService.listTenants());
    }

    @PostMapping("/tenants/{tenantId}/suspend")
    public ResponseEntity<Void> suspendTenant(@PathVariable Long tenantId) {
        superadminService.suspendTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tenants/{tenantId}/activate")
    public ResponseEntity<Void> activateTenant(@PathVariable Long tenantId) {
        superadminService.activateTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenants/{tenantId}/employees")
    public ResponseEntity<List<SuperadminEmployeeResponse>> listTenantEmployees(@PathVariable Long tenantId) {
        return ResponseEntity.ok(superadminService.listTenantEmployees(tenantId));
    }

    @PostMapping("/employees/{employeeId}/impersonate")
    public ResponseEntity<LoginResponse> impersonate(@PathVariable Long employeeId, HttpServletRequest httpRequest) {
        String superadminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                superadminService.impersonate(employeeId, superadminEmail, httpRequest.getRemoteAddr()));
    }
}
