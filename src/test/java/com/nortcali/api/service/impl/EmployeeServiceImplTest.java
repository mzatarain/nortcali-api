package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.EmployeeRequest;
import com.nortcali.api.dto.request.EmployeeUpdateRequest;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.mapper.EmployeeMapper;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Fase 11 (plan SaaS): límite de empleados por plan en {@code create()}. No cubre el resto de
 * {@code EmployeeServiceImpl} — solo el enforcement nuevo.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock EmployeeRepository employeeRepo;
    @Mock RestaurantRepository restaurantRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmployeeMapper mapper;

    @InjectMocks
    EmployeeServiceImpl service;

    @BeforeEach
    void mockAuthenticatedCaller() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@nortcali.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Restaurant restaurantWithPlan(Long tenantId, String plan) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setPlan(plan);
        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setTenant(tenant);
        return restaurant;
    }

    private EmployeeRequest request() {
        EmployeeRequest req = new EmployeeRequest();
        req.setFirstName("Juan");
        req.setLastName("Perez");
        req.setUsername("juanp");
        req.setPassword("Clave123!456");
        req.setEmail("juan@example.com");
        req.setRole("CASHIER");
        req.setStatus("ACTIVE");
        req.setHireDate(LocalDate.now());
        return req;
    }

    @Test
    void create_lanzaBusinessRuleException_cuandoPlanBasicoYaTiene5EmpleadosActivos() {
        Long restaurantId = 1L;
        Restaurant restaurant = restaurantWithPlan(10L, "basico");

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(employeeRepo.countDistinctActiveByRestaurantsTenantId(10L)).thenReturn(5L);

        assertThatThrownBy(() -> service.create(restaurantId, request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Básico")
                .hasMessageContaining("5 empleados");

        verify(employeeRepo, never()).save(any());
    }

    @Test
    void create_permiteCrear_cuandoPlanBasicoTieneMenosDe5EmpleadosActivos() {
        Long restaurantId = 1L;
        Restaurant restaurant = restaurantWithPlan(10L, "basico");

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(employeeRepo.countDistinctActiveByRestaurantsTenantId(10L)).thenReturn(4L);
        when(employeeRepo.findByTenantIdAndUsername(anyLong(), anyString())).thenReturn(Optional.empty());
        when(employeeRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(employeeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(restaurantId, request());

        verify(employeeRepo).save(any());
    }

    @Test
    void create_planEnterprise_nuncaBloquea() {
        Long restaurantId = 1L;
        Restaurant restaurant = restaurantWithPlan(10L, "enterprise");

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(employeeRepo.countDistinctActiveByRestaurantsTenantId(10L)).thenReturn(500L);
        when(employeeRepo.findByTenantIdAndUsername(anyLong(), anyString())).thenReturn(Optional.empty());
        when(employeeRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(employeeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(restaurantId, request());

        verify(employeeRepo).save(any());
    }

    private Employee existingEmployee(Long restaurantId) {
        Employee employee = new Employee();
        employee.setFirstName("Juan");
        employee.setLastName("Perez");
        employee.setUsername("juanp");
        employee.setPassword("hash-original");
        employee.setEmail("juan@example.com");
        employee.setRole("CASHIER");
        employee.setStatus("ACTIVE");
        employee.setHireDate(LocalDate.now());
        employee.setRestaurantId(restaurantId);
        return employee;
    }

    private EmployeeUpdateRequest updateRequest(String password) {
        EmployeeUpdateRequest req = new EmployeeUpdateRequest();
        req.setFirstName("Juan");
        req.setLastName("Perez");
        req.setPassword(password);
        req.setEmail("juan@example.com");
        req.setRole("CASHIER");
        req.setStatus("ACTIVE");
        req.setHireDate(LocalDate.now());
        return req;
    }

    /**
     * Fase 12: la contraseña es opcional al editar — {@code EmployeeUpdateRequest} ya no lleva
     * {@code @NotBlank} en el campo, y el service no debe re-encodear ni tocar el hash existente
     * cuando llega vacía o ausente.
     */
    @Test
    void update_conPasswordVacio_conservaElHashActual() {
        Long restaurantId = 1L;
        Employee employee = existingEmployee(restaurantId);

        when(employeeRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(employeeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(restaurantId, 1L, updateRequest(null));

        assertThat(employee.getPassword()).isEqualTo("hash-original");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void update_conPasswordNuevo_reencodeaYGuarda() {
        Long restaurantId = 1L;
        Employee employee = existingEmployee(restaurantId);

        when(employeeRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Clave123!456")).thenReturn("hash-nuevo");
        when(employeeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(restaurantId, 1L, updateRequest("Clave123!456"));

        assertThat(employee.getPassword()).isEqualTo("hash-nuevo");
    }

    /**
     * Fase 12: el username es inmutable en edición — {@code EmployeeUpdateRequest} ni siquiera
     * expone el campo, así que no hay forma de que el service lo modifique.
     */
    @Test
    void update_nuncaModificaElUsername() {
        Long restaurantId = 1L;
        Employee employee = existingEmployee(restaurantId);

        when(employeeRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(employeeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(restaurantId, 1L, updateRequest(null));

        assertThat(employee.getUsername()).isEqualTo("juanp");
    }
}
