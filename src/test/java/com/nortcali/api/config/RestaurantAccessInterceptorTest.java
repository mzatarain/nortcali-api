package com.nortcali.api.config;

import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.exception.ForbiddenException;
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
        withEmployeeRestaurants("cajero1", Set.of(restaurantWithId(OWN_RESTAURANT_ID)));

        MockHttpServletRequest request = requestWithRestaurantId(OTHER_RESTAURANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void restaurantePropio_dejaPasar() {
        authenticateAs("cajero1");
        withEmployeeRestaurants("cajero1", Set.of(restaurantWithId(OWN_RESTAURANT_ID)));

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void empleadoConVariosRestaurantes_permiteCualquieraDeLosSuyos() {
        authenticateAs("gerente1");
        withEmployeeRestaurants("gerente1", Set.of(restaurantWithId(OWN_RESTAURANT_ID), restaurantWithId(OTHER_RESTAURANT_ID)));

        MockHttpServletRequest request = requestWithRestaurantId(OTHER_RESTAURANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void empleadoNoEncontrado_lanzaForbiddenException() {
        authenticateAs("fantasma");
        when(employeeRepo.findByUsername("fantasma")).thenReturn(Optional.empty());

        MockHttpServletRequest request = requestWithRestaurantId(OWN_RESTAURANT_ID);
        HttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    private void authenticateAs(String username) {
        // 3 argumentos (con authorities, aunque vacías) marca el token como autenticado —
        // igual que JwtAuthFilter.doFilterInternal al construir el authToken real.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, java.util.List.of()));
    }

    private void withEmployeeRestaurants(String username, Set<Restaurant> restaurants) {
        Employee employee = new Employee();
        employee.setRestaurant(restaurants);
        when(employeeRepo.findByUsername(username)).thenReturn(Optional.of(employee));
    }

    private Restaurant restaurantWithId(Long id) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        return restaurant;
    }

    private MockHttpServletRequest requestWithRestaurantId(Long restaurantId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Map.of("restaurantId", String.valueOf(restaurantId)));
        return request;
    }
}
