package com.nortcali.api.config;

import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.enums.TenantStatus;
import com.nortcali.api.exception.ForbiddenException;
import com.nortcali.api.exception.PaymentRequiredException;
import com.nortcali.api.repository.EmployeeRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Test unitario (sin contexto Spring / BD) de RestaurantAccessInterceptor — verifica
 * que un empleado autenticado solo pase para restaurantId a los que pertenece.
 */
@ExtendWith(MockitoExtension.class)
class RestaurantAccessInterceptorTest {

    @Mock EmployeeRepository employeeRepo;
    @InjectMocks RestaurantAccessInterceptor interceptor;

    private static final Long OWN_RESTAURANT_ID = 1L;
    private static final Long OTHER_RESTAURANT_ID = 2L;
    private static final Long OWN_TENANT_ID = 10L;
    private static final Long OTHER_TENANT_ID = 20L;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinRestaurantIdEnElPath_dejaPasar() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void restauranteAjeno_lanzaForbiddenException() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1", Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID)));

        MockHttpServletRequest request = requestWithRestaurantId(OTHER_RESTAURANT_ID, OWN_TENANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void restaurantePropio_dejaPasar() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1", Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void empleadoConVariosRestaurantes_permiteCualquieraDeLosSuyos() {
        authenticateAs("gerente1");
        withEmployeeRestaurants("gerente1", Set.of(
                restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID),
                restaurantWithId(OTHER_RESTAURANT_ID, OWN_TENANT_ID)));

        MockHttpServletRequest request = requestWithRestaurantId(OTHER_RESTAURANT_ID, OWN_TENANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void empleadoNoEncontrado_lanzaForbiddenException() {
        authenticateAs("fantasma");
        when(employeeRepo.findByEmailWithRestaurants("fantasma")).thenReturn(Optional.empty());

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void tenantIdDelTokenNoCoincideConElDelRestaurante_lanzaForbiddenException() {
        // Pertenencia al restaurante ok, pero el tenantId que trae el JWT es de otro tenant —
        // caso defensivo (Fase 1): no debería pasar aunque employee_restaurants diga que sí.
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1", Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OTHER_TENANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void tokenSinTenantId_lanzaForbiddenException() {
        // Token emitido antes de la Fase 1 (o sin el claim por cualquier motivo) — sin
        // tenantId no hay forma de validar, se rechaza en vez de asumir acceso.
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1", Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, null);
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void tenantSuspendido_lanzaForbiddenExceptionInclusoEnGet() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1",
                Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.SUSPENDED)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("GET");
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void tenantCancelado_lanzaForbiddenException() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1",
                Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.CANCELLED)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void tenantPastDue_metodoGet_dejaPasar() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1",
                Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.PAST_DUE)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("GET");
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void tenantPastDue_metodoPost_lanzaPaymentRequiredException() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1",
                Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.PAST_DUE)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("POST");
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(PaymentRequiredException.class);
    }

    @Test
    void tenantActivo_metodoPost_dejaPasar() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1",
                Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.ACTIVE)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("POST");
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    /** Fase 7 (plan SaaS): tarjeta obligatoria — TRIAL sin `stripe_subscription_id` es solo lectura. */
    @Test
    void tenantTrialSinSuscripcion_metodoGet_dejaPasar() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1",
                Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.TRIAL, null)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("GET");
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void tenantTrialSinSuscripcion_metodoPost_lanzaPaymentRequiredException() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1",
                Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.TRIAL, null)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("POST");
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(PaymentRequiredException.class);
    }

    @Test
    void tenantTrialConSuscripcion_metodoPost_dejaPasar() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1",
                Set.of(restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.TRIAL, "sub_123")));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("POST");
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    /** Fase 13 (go-live): NortCali exento de cobro — nunca queda en solo lectura por pago. */
    @Test
    void tenantExentoPastDue_metodoPost_dejaPasar() {
        authenticateAs("cajero1");
        Restaurant restaurant = restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.PAST_DUE, null);
        restaurant.getTenant().setBillingExempt(true);
        withEmployeeRestaurants("cajero1", Set.of(restaurant));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("POST");
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void tenantExentoTrialSinSuscripcion_metodoPost_dejaPasar() {
        authenticateAs("cajero1");
        Restaurant restaurant = restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.TRIAL, null);
        restaurant.getTenant().setBillingExempt(true);
        withEmployeeRestaurants("cajero1", Set.of(restaurant));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("POST");
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    /** La suspensión manual del superadmin sigue aplicando aunque el tenant esté exento. */
    @Test
    void tenantExentoSuspendido_lanzaForbiddenException() {
        authenticateAs("cajero1");
        Restaurant restaurant = restaurantWithId(OWN_RESTAURANT_ID, OWN_TENANT_ID, TenantStatus.SUSPENDED, null);
        restaurant.getTenant().setBillingExempt(true);
        withEmployeeRestaurants("cajero1", Set.of(restaurant));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID, OWN_TENANT_ID);
        request.setMethod("GET");
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    private void authenticateAs(String email) {
        // 3 argumentos (con authorities, aunque vacías) marca el token como autenticado —
        // igual que JwtAuthFilter.doFilterInternal al construir el authToken real. El
        // principal es el email (Fase 2, login por email) — ver EmployeeDetailsService.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, java.util.List.of()));
    }

    private void withEmployeeRestaurants(String email, Set<Restaurant> restaurants) {
        Employee employee = new Employee();
        employee.setRestaurant(restaurants);
        when(employeeRepo.findByEmailWithRestaurants(email)).thenReturn(Optional.of(employee));
    }

    private Restaurant restaurantWithId(Long id, Long tenantId) {
        return restaurantWithId(id, tenantId, TenantStatus.ACTIVE);
    }

    private Restaurant restaurantWithId(Long id, Long tenantId, TenantStatus status) {
        return restaurantWithId(id, tenantId, status, "sub_default");
    }

    private Restaurant restaurantWithId(Long id, Long tenantId, TenantStatus status, String stripeSubscriptionId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setStatus(status);
        tenant.setStripeSubscriptionId(stripeSubscriptionId);
        restaurant.setTenant(tenant);
        return restaurant;
    }

    private MockHttpServletRequest requestWithRestaurantId(Long restaurantId, Long tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Map.of("restaurantId", String.valueOf(restaurantId)));
        request.setAttribute("tenantId", tenantId);
        return request;
    }
}
