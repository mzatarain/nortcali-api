package com.nortcali.api.config;

import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.enums.TenantStatus;
import com.nortcali.api.exception.ForbiddenException;
import com.nortcali.api.exception.PaymentRequiredException;
import com.nortcali.api.repository.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Verifica que el empleado autenticado pertenezca al restaurantId de la URL antes de que
 * la request llegue al controller. Sin esto, cualquier empleado autenticado podía operar
 * sobre cualquier restaurante con solo cambiar el {restaurantId} del path — el control de
 * rol (SecurityConfig) y el de pertenencia de recurso (resource.restaurant.id == restaurantId
 * en los services) nunca validaban que el EMPLEADO tuviera acceso a ESE restaurante.
 *
 * Solo actúa quando el path resuelve un @PathVariable literal "restaurantId" — el resto de
 * las requests (auth, catálogos globales, endpoints sin restaurantId en el path) pasan de
 * largo sin verificación aquí.
 *
 * Fase 1 (plan SaaS): además de la pertenencia empleado↔restaurante, verifica que el tenant
 * del restaurante coincida con el {@code tenantId} que JwtAuthFilter dejó en la request
 * (leído del JWT — evita una consulta extra). Es defensa en profundidad: si algún día
 * `employee_restaurants` quedara con una fila inconsistente con el tenant del empleado, o el
 * JWT quedara desactualizado tras un cambio de tenant, este check lo bloquea igual. Tokens
 * emitidos antes de este cambio no traen tenantId — quedan rechazados hasta que el empleado
 * vuelva a iniciar sesión.
 */
@Component
public class RestaurantAccessInterceptor implements HandlerInterceptor {

    private final EmployeeRepository employeeRepo;

    public RestaurantAccessInterceptor(EmployeeRepository employeeRepo) {
        this.employeeRepo = employeeRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long restaurantId = extractRestaurantId(request);
        if (restaurantId == null) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            // Spring Security ya rechaza esto con 401 antes de llegar aquí para rutas
            // protegidas; dejamos pasar por si acaso para no interferir con permitAll.
            return true;
        }

        // findByEmailWithRestaurants (no findByUsername): preHandle corre antes de que Spring
        // abra el EntityManager de open-in-view — el orden entre interceptors no está
        // garantizado — así que `employee.getRestaurant()` debe venir ya inicializado. Por
        // email (Fase 2): auth.getName() es el principal de EmployeeDetailsService, que desde
        // el login por email ya no es Employee.username.
        Employee employee = employeeRepo.findByEmailWithRestaurants(auth.getName()).orElse(null);
        if (employee == null) {
            throw new ForbiddenException("No tienes acceso a este restaurante");
        }

        Restaurant restaurant = employee.getRestaurant().stream()
                .filter(r -> r.getId().equals(restaurantId))
                .findFirst()
                .orElse(null);

        if (restaurant == null) {
            throw new ForbiddenException("No tienes acceso a este restaurante");
        }

        Object tenantIdAttr = request.getAttribute("tenantId");
        Tenant tenant = restaurant.getTenant();
        Long restaurantTenantId = tenant == null ? null : tenant.getId();
        if (tenantIdAttr == null || !tenantIdAttr.equals(restaurantTenantId)) {
            throw new ForbiddenException("No tienes acceso a este restaurante");
        }

        checkTenantStatus(tenant, request.getMethod());

        return true;
    }

    /**
     * Fase 3 (plan SaaS): decisión de negocio confirmada con el usuario (2026-09-06) —
     * {@code past_due} degrada a solo lectura (GET sigue funcionando, el resto de métodos
     * queda bloqueado con 402 hasta regularizar el pago); {@code suspended}/{@code cancelled}
     * bloquean todo, incluido GET.
     *
     * Fase 7 (plan SaaS, 2026-09-07): con tarjeta obligatoria desde el registro, un tenant en
     * {@code TRIAL} que todavía no completó el Checkout de Stripe (sin
     * {@code stripe_subscription_id}) recibe el mismo trato de solo lectura que
     * {@code past_due} — el registro ya creó la cuenta y el JWT, pero el trial no cuenta como
     * "activo" hasta que la tarjeta quede registrada. Los endpoints de billing
     * (`/api/v1/billing/**`) no llevan `restaurantId` en el path, así que este interceptor no
     * los toca — el tenant siempre puede completar el checkout desde ahí para desbloquearse.
     */
    private void checkTenantStatus(Tenant tenant, String httpMethod) {
        if (tenant == null) {
            return;
        }
        TenantStatus status = tenant.getStatus();
        if (status == TenantStatus.SUSPENDED || status == TenantStatus.CANCELLED) {
            throw new ForbiddenException(
                    "La suscripción de este negocio está " + status.name().toLowerCase()
                            + ". Contacta a soporte para reactivarla.");
        }
        // Fase 13 (go-live): un tenant exento de cobro (NortCali) nunca queda en solo lectura
        // por pago. La suspensión/cancelación manual de arriba sí le aplica (interruptor del superadmin).
        if (tenant.isBillingExempt()) {
            return;
        }
        boolean readOnly = status == TenantStatus.PAST_DUE
                || (status == TenantStatus.TRIAL && tenant.getStripeSubscriptionId() == null);
        if (readOnly && !"GET".equalsIgnoreCase(httpMethod)) {
            throw new PaymentRequiredException(
                    status == TenantStatus.TRIAL
                            ? "Completa la configuración de tu método de pago para activar tu prueba gratuita."
                            : "El pago de la suscripción está pendiente. Solo se permite consulta hasta regularizar el pago.");
        }
    }

    @SuppressWarnings("unchecked")
    private Long extractRestaurantId(HttpServletRequest request) {
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attr instanceof Map<?, ?> vars)) {
            return null;
        }
        Object raw = ((Map<String, String>) vars).get("restaurantId");
        if (raw == null) {
            return null;
        }
        try {
            return Long.valueOf(raw.toString());
        } catch (NumberFormatException e) {
            // Formato inválido — dejar que la validación normal del controller lo maneje
            return null;
        }
    }
}
