package com.nortcali.api.service;

import com.nortcali.api.entity.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Fase 13 (go-live): un tenant exento de cobro (NortCali) no tiene límites de plan. */
class PlanLimitsTest {

    @Test
    void forTenant_exento_devuelveEnterpriseSinImportarElPlan() {
        Tenant tenant = new Tenant();
        tenant.setPlan("basico");
        tenant.setBillingExempt(true);

        PlanLimits limits = PlanLimits.forTenant(tenant);

        assertThat(limits).isEqualTo(PlanLimits.ENTERPRISE);
        assertThat(limits.getMaxRestaurants()).isEqualTo(Integer.MAX_VALUE);
        assertThat(limits.getMaxEmployees()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void forTenant_noExento_usaSuPlan() {
        Tenant tenant = new Tenant();
        tenant.setPlan("pro");

        assertThat(PlanLimits.forTenant(tenant)).isEqualTo(PlanLimits.PRO);
    }
}
