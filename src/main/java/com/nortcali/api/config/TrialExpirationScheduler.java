package com.nortcali.api.config;

import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.enums.TenantStatus;
import com.nortcali.api.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fase 7 (plan SaaS, 2026-09-07): red de seguridad, no el mecanismo principal. Con tarjeta
 * obligatoria desde el registro, el ciclo normal de un tenant lo maneja Stripe directamente —
 * intenta el cobro real al terminar el trial y dispara {@code invoice.payment_succeeded}
 * ({@code BillingServiceImpl} → {@code ACTIVE}) o {@code invoice.payment_failed}
 * (→ {@code PAST_DUE}, solo lectura). Este job cubre el caso en que ningún webhook llegó a
 * tiempo (entrega fallida de Stripe, tenant que nunca completó el checkout tras registrarse) —
 * revisa diariamente los tenants que siguen en {@code TRIAL} con {@code trial_ends_at} vencido
 * hace más de un día (margen de gracia para que el webhook normal alcance a llegar) y los
 * suspende. Mismo patrón que {@code EndOfDayScheduler} (@EnableScheduling ya activo en
 * {@code NortcaliApiApplication}).
 */
@Component
@Slf4j
public class TrialExpirationScheduler {

    private static final int GRACE_DAYS = 1;

    private final TenantRepository tenantRepo;

    public TrialExpirationScheduler(TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void suspendExpiredTrials() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(GRACE_DAYS);
        List<Tenant> expired = tenantRepo.findByStatusAndTrialEndsAtBefore(TenantStatus.TRIAL, cutoff);

        for (Tenant tenant : expired) {
            tenant.setStatus(TenantStatus.SUSPENDED);
            tenantRepo.save(tenant);
            log.info("[TrialExpiration] Tenant {} ('{}') suspendido — trial venció el {} sin conversión",
                    tenant.getId(), tenant.getName(), tenant.getTrialEndsAt());
        }

        if (!expired.isEmpty()) {
            log.info("[TrialExpiration] {} tenant(s) suspendido(s) por trial vencido sin conversión.", expired.size());
        }
    }
}
