package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.SuperadminLoginRequest;
import com.nortcali.api.dto.response.LoginResponse;
import com.nortcali.api.dto.response.PlatformMetricsResponse;
import com.nortcali.api.dto.response.SuperadminEmployeeResponse;
import com.nortcali.api.dto.response.SuperadminLoginResponse;
import com.nortcali.api.dto.response.TenantSummaryResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.ImpersonationAudit;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.Session;
import com.nortcali.api.entity.Superadmin;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.enums.TenantStatus;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.ImpersonationAuditRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.repository.SessionRepository;
import com.nortcali.api.repository.SuperadminRepository;
import com.nortcali.api.repository.TenantRepository;
import com.nortcali.api.security.JwtUtil;
import com.nortcali.api.service.SuperadminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
@Slf4j
public class SuperadminServiceImpl implements SuperadminService {

    private final SuperadminRepository superadminRepo;
    private final TenantRepository tenantRepo;
    private final RestaurantRepository restaurantRepo;
    private final EmployeeRepository employeeRepo;
    private final SessionRepository sessionRepo;
    private final ImpersonationAuditRepository auditRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /** Fase 9 (plan SaaS): precios de plan en MXN — mismos valores que la tabla de Fase 3, para MRR sin llamar a Stripe. */
    @Value("${billing.plan-amounts.basico}")
    private BigDecimal planAmountBasico;

    @Value("${billing.plan-amounts.pro}")
    private BigDecimal planAmountPro;

    @Value("${billing.plan-amounts.enterprise}")
    private BigDecimal planAmountEnterprise;

    @Value("${app.tenant-inactivity-threshold-days:14}")
    private int inactivityThresholdDays;

    public SuperadminServiceImpl(SuperadminRepository superadminRepo,
                                  TenantRepository tenantRepo,
                                  RestaurantRepository restaurantRepo,
                                  EmployeeRepository employeeRepo,
                                  SessionRepository sessionRepo,
                                  ImpersonationAuditRepository auditRepo,
                                  JwtUtil jwtUtil,
                                  PasswordEncoder passwordEncoder) {
        this.superadminRepo = superadminRepo;
        this.tenantRepo = tenantRepo;
        this.restaurantRepo = restaurantRepo;
        this.employeeRepo = employeeRepo;
        this.sessionRepo = sessionRepo;
        this.auditRepo = auditRepo;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SuperadminLoginResponse login(SuperadminLoginRequest request) {
        Superadmin superadmin = superadminRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), superadmin.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtUtil.generateSuperadminToken(superadmin.getEmail());
        log.info("Login de superadmin exitoso para '{}'", superadmin.getEmail());
        return new SuperadminLoginResponse(token, superadmin.getName(), superadmin.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantSummaryResponse> listTenants() {
        return tenantRepo.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformMetricsResponse getMetrics() {
        List<Tenant> all = tenantRepo.findAll();

        long trial = 0, active = 0, pastDue = 0, suspended = 0, cancelled = 0, inactive = 0, exempt = 0;
        BigDecimal mrr = BigDecimal.ZERO;

        for (Tenant tenant : all) {
            if (tenant.isBillingExempt()) {
                exempt++;
            }
            switch (tenant.getStatus()) {
                case TRIAL -> trial++;
                case ACTIVE -> {
                    active++;
                    // Fase 13 (go-live): un tenant exento (NortCali) no es ingreso recurrente
                    if (!tenant.isBillingExempt()) {
                        mrr = mrr.add(planAmount(tenant.getPlan()));
                    }
                    if (isInactive(tenant)) {
                        inactive++;
                    }
                }
                case PAST_DUE -> pastDue++;
                case SUSPENDED -> suspended++;
                case CANCELLED -> cancelled++;
            }
        }

        long everPaidOrChurned = active + pastDue + cancelled;
        BigDecimal churnRate = everPaidOrChurned == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(cancelled)
                        .divide(BigDecimal.valueOf(everPaidOrChurned), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        return new PlatformMetricsResponse(
                all.size(), trial, active, pastDue, suspended, cancelled, mrr, churnRate, inactive, exempt);
    }

    private BigDecimal planAmount(String plan) {
        if (plan == null) {
            return BigDecimal.ZERO;
        }
        return switch (plan) {
            case "basico" -> planAmountBasico;
            case "pro" -> planAmountPro;
            case "enterprise" -> planAmountEnterprise;
            default -> BigDecimal.ZERO;
        };
    }

    /** Fase 9: mismo criterio que {@code TenantInactivityScheduler} — última conexión de cualquier empleado del tenant, o su alta si nunca se conectó nadie. */
    private boolean isInactive(Tenant tenant) {
        LocalDateTime lastLogin = employeeRepo.findMaxLastLoginByRestaurantsTenantId(tenant.getId());
        LocalDateTime baseline = lastLogin != null ? lastLogin : tenant.getCreatedAt();
        return baseline != null && ChronoUnit.DAYS.between(baseline, LocalDateTime.now()) >= inactivityThresholdDays;
    }

    @Override
    public void suspendTenant(Long tenantId) {
        Tenant tenant = findTenant(tenantId);
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepo.save(tenant);
        log.info("Tenant {} suspendido desde el panel de superadmin", tenantId);
    }

    @Override
    public void activateTenant(Long tenantId) {
        Tenant tenant = findTenant(tenantId);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepo.save(tenant);
        log.info("Tenant {} activado desde el panel de superadmin", tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuperadminEmployeeResponse> listTenantEmployees(Long tenantId) {
        findTenant(tenantId);
        return employeeRepo.findByRestaurantsTenantId(tenantId).stream()
                .map(e -> new SuperadminEmployeeResponse(
                        e.getId(),
                        e.getFirstName(),
                        e.getLastName(),
                        e.getEmail(),
                        e.getRole(),
                        e.getStatus(),
                        e.getRestaurant().stream().map(Restaurant::getName).toList()))
                .toList();
    }

    @Override
    public LoginResponse impersonate(Long employeeId, String superadminEmail, String ipAddress) {
        Employee employee = employeeRepo.findByIdWithRestaurants(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + employeeId));

        Long tenantId = employee.getRestaurant().stream()
                .findFirst()
                .map(r -> r.getTenant().getId())
                .orElseThrow(() -> new BusinessRuleException("Este empleado no tiene ningún restaurante asignado"));

        Superadmin superadmin = superadminRepo.findByEmail(superadminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Superadmin " + superadminEmail));

        String token = jwtUtil.generateImpersonationToken(employee.getEmail(), tenantId, superadminEmail);

        Session session = new Session();
        session.setToken(sha256Hex(token));
        session.setEmployee(employee);
        session.setIpAddress(ipAddress);
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getExpiration() / 1000));
        sessionRepo.save(session);

        ImpersonationAudit audit = new ImpersonationAudit();
        audit.setSuperadminId(superadmin.getId());
        audit.setEmployeeId(employee.getId());
        audit.setTenantId(tenantId);
        audit.setIpAddress(ipAddress);
        auditRepo.save(audit);

        log.info("Superadmin '{}' impersonó al empleado '{}' (tenant {})", superadminEmail, employee.getEmail(), tenantId);
        return new LoginResponse(token, employee.getUsername(), employee.getRole());
    }

    private Tenant findTenant(Long tenantId) {
        return tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant " + tenantId));
    }

    private TenantSummaryResponse toSummary(Tenant tenant) {
        int restaurantCount = restaurantRepo.findByTenantId(tenant.getId()).size();
        long employeeCount = employeeRepo.countDistinctByRestaurantsTenantId(tenant.getId());
        LocalDateTime lastConnection = employeeRepo.findMaxLastLoginByRestaurantsTenantId(tenant.getId());
        return new TenantSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getPlan(),
                tenant.getCreatedAt(),
                tenant.getTrialEndsAt(),
                restaurantCount,
                employeeCount,
                lastConnection,
                tenant.isBillingExempt());
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
