package com.nortcali.api.repository;

import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.enums.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /** Fase 3 (plan SaaS): resuelve el tenant a partir del customer id que Stripe manda en cada webhook. */
    Optional<Tenant> findByStripeCustomerId(String stripeCustomerId);

    /**
     * Fase 7 (plan SaaS): red de seguridad de {@code TrialExpirationScheduler} — tenants cuyo
     * trial venció sin que ningún webhook de Stripe los haya movido de {@code TRIAL} todavía.
     */
    List<Tenant> findByStatusAndTrialEndsAtBefore(TenantStatus status, LocalDateTime cutoff);
}
