package com.nortcali.api.config;

import com.nortcali.api.entity.Employee;
import com.nortcali.api.exception.ForbiddenException;
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

        Employee employee = employeeRepo.findByUsername(auth.getName()).orElse(null);
        if (employee == null) {
            throw new ForbiddenException("No tienes acceso a este restaurante");
        }

        boolean hasAccess = employee.getRestaurant().stream()
                .anyMatch(r -> r.getId().equals(restaurantId));

        if (!hasAccess) {
            throw new ForbiddenException("No tienes acceso a este restaurante");
        }

        return true;
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
