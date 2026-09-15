package com.nortcali.api.config;

import com.nortcali.api.entity.Superadmin;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.enums.TenantStatus;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.SuperadminRepository;
import com.nortcali.api.repository.TenantRepository;
import com.nortcali.api.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Fase 9 (plan SaaS): tenants {@code ACTIVE} sin ninguna conexión de sus empleados en
 * {@code app.tenant-inactivity-threshold-days} días — señal de riesgo de cancelación (churn).
 * Notifica por email a todos los superadmins una sola vez por episodio de inactividad
 * ({@code Tenant.inactivityAlertedAt} evita reenviar el mismo aviso cada día); si el tenant
 * vuelve a tener actividad reciente, se limpia el flag para que un futuro episodio de
 * inactividad pueda alertar de nuevo. Mismo patrón que {@code TrialExpirationScheduler}.
 */
@Component
@Slf4j
public class TenantInactivityScheduler {

    private final TenantRepository tenantRepo;
    private final EmployeeRepository employeeRepo;
    private final SuperadminRepository superadminRepo;
    private final EmailService emailService;

    @Value("${app.tenant-inactivity-threshold-days:14}")
    private int thresholdDays;

    public TenantInactivityScheduler(TenantRepository tenantRepo,
                                      EmployeeRepository employeeRepo,
                                      SuperadminRepository superadminRepo,
                                      EmailService emailService) {
        this.tenantRepo = tenantRepo;
        this.employeeRepo = employeeRepo;
        this.superadminRepo = superadminRepo;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void checkInactiveTenants() {
        List<Tenant> activeTenants = tenantRepo.findByStatus(TenantStatus.ACTIVE);
        List<String> superadminEmails = superadminRepo.findAll().stream()
                .map(Superadmin::getEmail)
                .toList();

        int newlyAlerted = 0;
        for (Tenant tenant : activeTenants) {
            LocalDateTime lastLogin = employeeRepo.findMaxLastLoginByRestaurantsTenantId(tenant.getId());
            LocalDateTime baseline = lastLogin != null ? lastLogin : tenant.getCreatedAt();
            if (baseline == null) {
                continue;
            }
            long daysInactive = ChronoUnit.DAYS.between(baseline, LocalDateTime.now());

            if (daysInactive >= thresholdDays) {
                if (tenant.getInactivityAlertedAt() == null) {
                    for (String email : superadminEmails) {
                        emailService.sendInactivityAlert(email, tenant.getName(), daysInactive);
                    }
                    tenant.setInactivityAlertedAt(LocalDateTime.now());
                    tenantRepo.save(tenant);
                    newlyAlerted++;
                    log.info("[TenantInactivity] Tenant {} ('{}') inactivo hace {} día(s) — alerta enviada a {} superadmin(s)",
                            tenant.getId(), tenant.getName(), daysInactive, superadminEmails.size());
                }
            } else if (tenant.getInactivityAlertedAt() != null) {
                tenant.setInactivityAlertedAt(null);
                tenantRepo.save(tenant);
                log.info("[TenantInactivity] Tenant {} ('{}') volvió a tener actividad — alerta reiniciada",
                        tenant.getId(), tenant.getName());
            }
        }

        if (newlyAlerted > 0) {
            log.info("[TenantInactivity] {} tenant(s) marcado(s) como inactivo(s) en esta corrida.", newlyAlerted);
        }
    }
}
