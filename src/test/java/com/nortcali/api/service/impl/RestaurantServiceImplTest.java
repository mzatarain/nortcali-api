package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.RestaurantRequest;
import com.nortcali.api.entity.City;
import com.nortcali.api.entity.Country;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.State;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.mapper.RestaurantMapper;
import com.nortcali.api.repository.CityRepository;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.RestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Fase 11 (plan SaaS): límite de restaurantes por plan en {@code create()}. No cubre el resto
 * de {@code RestaurantServiceImpl} — solo el enforcement nuevo.
 */
@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock RestaurantRepository restaurantRepo;
    @Mock CityRepository cityRepo;
    @Mock EmployeeRepository employeeRepo;
    @Mock RestaurantMapper mapper;

    @InjectMocks
    RestaurantServiceImpl service;

    private static final Long TENANT_ID = 10L;
    private static final Long CITY_ID = 1L;

    @BeforeEach
    void mockAuthenticatedTenant() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@nortcali.com", null, Collections.emptyList());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedEmployee(String plan) {
        mockAuthenticatedEmployee(plan, false);
    }

    private void mockAuthenticatedEmployee(String plan, boolean billingExempt) {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setPlan(plan);
        tenant.setBillingExempt(billingExempt);
        Restaurant employeeRestaurant = new Restaurant();
        employeeRestaurant.setTenant(tenant);
        Employee employee = new Employee();
        employee.setRestaurant(Set.of(employeeRestaurant));
        lenient().when(employeeRepo.findByEmail("admin@nortcali.com")).thenReturn(Optional.of(employee));
    }

    private City city() {
        Country country = mock(Country.class);
        State state = mock(State.class);
        lenient().when(state.getCountry()).thenReturn(country);
        City city = mock(City.class);
        lenient().when(city.getState()).thenReturn(state);
        return city;
    }

    private RestaurantRequest request() {
        RestaurantRequest req = new RestaurantRequest();
        req.setName("Sucursal nueva");
        req.setCityId(CITY_ID);
        return req;
    }

    @Test
    void create_lanzaBusinessRuleException_cuandoPlanBasicoYaTiene1RestauranteActivo() {
        mockAuthenticatedEmployee("basico");
        City city = city();
        when(cityRepo.findById(CITY_ID)).thenReturn(Optional.of(city));
        when(restaurantRepo.countByTenantIdAndIsActiveTrue(TENANT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Básico")
                .hasMessageContaining("1 restaurante");

        verify(restaurantRepo, never()).save(any());
    }

    @Test
    void create_permiteCrear_cuandoPlanProTieneMenosDe3RestaurantesActivos() {
        mockAuthenticatedEmployee("pro");
        City city = city();
        when(cityRepo.findById(CITY_ID)).thenReturn(Optional.of(city));
        when(restaurantRepo.countByTenantIdAndIsActiveTrue(TENANT_ID)).thenReturn(2L);
        when(mapper.toEntity(any())).thenReturn(new Restaurant());
        when(restaurantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(request());

        verify(restaurantRepo).save(any());
    }

    @Test
    void create_planEnterprise_nuncaBloquea() {
        mockAuthenticatedEmployee("enterprise");
        City city = city();
        when(cityRepo.findById(CITY_ID)).thenReturn(Optional.of(city));
        when(restaurantRepo.countByTenantIdAndIsActiveTrue(TENANT_ID)).thenReturn(50L);
        when(mapper.toEntity(any())).thenReturn(new Restaurant());
        when(restaurantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(request());

        verify(restaurantRepo).save(any());
    }

    /** Fase 13 (go-live): NortCali (exento de cobro) no tiene límite de sucursales aunque su plan guardado sea Básico/Pro. */
    @Test
    void create_tenantExento_nuncaBloqueaAunqueElPlanSeaBasico() {
        mockAuthenticatedEmployee("basico", true);
        City city = city();
        when(cityRepo.findById(CITY_ID)).thenReturn(Optional.of(city));
        when(restaurantRepo.countByTenantIdAndIsActiveTrue(TENANT_ID)).thenReturn(10L);
        when(mapper.toEntity(any())).thenReturn(new Restaurant());
        when(restaurantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(request());

        verify(restaurantRepo).save(any());
    }
}
