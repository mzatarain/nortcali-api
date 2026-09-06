package com.nortcali.api.repository;

import com.nortcali.api.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /** Fase 3 (plan SaaS): resuelve el tenant a partir del customer id que Stripe manda en cada webhook. */
    Optional<Tenant> findByStripeCustomerId(String stripeCustomerId);
}
