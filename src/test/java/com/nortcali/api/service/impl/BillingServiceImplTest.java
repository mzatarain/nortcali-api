package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.CheckoutSessionRequest;
import com.nortcali.api.dto.response.SubscriptionStatusResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.enums.TenantStatus;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.repository.TenantRepository;
import com.nortcali.api.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fase 13 (go-live): un tenant exento de cobro (NortCali) no puede iniciar checkout, abrir el
 * portal ni cancelar — se rechaza antes de cualquier llamada a Stripe. No cubre el resto de
 * {@code BillingServiceImpl} (las rutas felices llaman a la API real de Stripe).
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    @Mock TenantRepository tenantRepo;
    @Mock RestaurantRepository restaurantRepo;
    @Mock EmployeeRepository employeeRepo;
    @Mock EmailService emailService;

    @InjectMocks
    BillingServiceImpl service;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@nortcali.com", null, List.of()));
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("NortCali");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setPlan("pro");
        tenant.setBillingExempt(true);
        Restaurant restaurant = new Restaurant();
        restaurant.setTenant(tenant);
        Employee employee = new Employee();
        employee.setRestaurant(Set.of(restaurant));
        when(employeeRepo.findByEmailWithRestaurants("admin@nortcali.com")).thenReturn(Optional.of(employee));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCheckoutSession_tenantExento_lanzaBusinessRuleException() {
        CheckoutSessionRequest request = new CheckoutSessionRequest();
        request.setPlan("pro");

        assertThatThrownBy(() -> service.createCheckoutSession(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cortesía");
        verify(tenantRepo, never()).save(any());
    }

    @Test
    void createPortalSession_tenantExento_lanzaBusinessRuleException() {
        tenant.setStripeCustomerId("cus_x");

        assertThatThrownBy(() -> service.createPortalSession())
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cortesía");
    }

    @Test
    void cancelSubscription_tenantExento_lanzaBusinessRuleException() {
        tenant.setStripeSubscriptionId("sub_x");

        assertThatThrownBy(() -> service.cancelSubscription())
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cortesía");
        verify(tenantRepo, never()).save(any());
    }

    @Test
    void getSubscriptionStatus_tenantExento_reportaBillingExempt() {
        SubscriptionStatusResponse status = service.getSubscriptionStatus();

        assertThat(status.billingExempt()).isTrue();
        assertThat(status.status()).isEqualTo("active");
    }
}
