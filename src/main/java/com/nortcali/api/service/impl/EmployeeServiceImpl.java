package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.EmployeeRequest;
import com.nortcali.api.dto.request.EmployeeStatusRequest;
import com.nortcali.api.dto.request.EmployeeUpdateRequest;
import com.nortcali.api.dto.response.EmployeeResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.EmployeeMapper;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.EmployeeService;
import com.nortcali.api.service.PlanLimits;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final RestaurantRepository restaurantRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeMapper mapper;

    public EmployeeServiceImpl(EmployeeRepository employeeRepo,
                               RestaurantRepository restaurantRepo,
                               PasswordEncoder passwordEncoder,
                               EmployeeMapper mapper) {
        this.employeeRepo = employeeRepo;
        this.restaurantRepo = restaurantRepo;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getByRestaurant(Long restaurantId) {
        return employeeRepo.findByRestaurantsId(restaurantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long restaurantId, Long id) {
        Employee employee = findOrThrow(id);
        if (!employee.getRestaurantId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Employee", id);
        }
        return mapper.toResponse(employee);
    }

    @Override
    public EmployeeResponse create(Long restaurantId, EmployeeRequest request) {
        var targetRestaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        var tenant = targetRestaurant.getTenant();
        enforceEmployeeLimit(tenant);

        // Fase SaaS: username escopado por tenant (V29) — dos negocios distintos pueden tener
        // cada uno un empleado con el mismo username sin chocar.
        if (employeeRepo.findByTenantIdAndUsername(tenant.getId(), request.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Ya existe un empleado con el username '" + request.getUsername() + "'");
        }
        // Fase 2 (plan SaaS): email es el identificador de login — employees.email tiene
        // constraint UNIQUE en BD (V20), este check da un 409 legible en vez del error crudo
        // de la BD.
        if (employeeRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Ya existe un empleado con el email '" + request.getEmail() + "'");
        }

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setUsername(request.getUsername());
        // BCrypt: nunca guardar password en texto plano
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        enforceRoleCeiling(request.getRole());
        employee.setRole(request.getRole());
        employee.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        employee.setHireDate(request.getHireDate());

        // Asociar al restaurante (ManyToMany + columna legacy restaurant_id)
        var restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        Set<com.nortcali.api.entity.Restaurant> restaurants = new HashSet<>();
        restaurants.add(restaurant);
        employee.setRestaurant(restaurants);
        employee.setRestaurantId(restaurantId);
        employee.setTenantId(tenant.getId());

        log.info("Creando empleado '{}' para restaurante {}", request.getUsername(), restaurantId);
        return mapper.toResponse(employeeRepo.save(employee));
    }

    @Override
    public EmployeeResponse update(Long restaurantId, Long id, EmployeeUpdateRequest request) {
        Employee employee = findOrThrow(id);
        if (!employee.getRestaurantId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Employee", id);
        }
        employeeRepo.findByEmail(request.getEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Ya existe un empleado con el email '" + request.getEmail() + "'");
                });
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        enforceRoleCeiling(request.getRole());
        employee.setRole(request.getRole());
        employee.setStatus(request.getStatus());
        employee.setHireDate(request.getHireDate());
        // Solo re-encodear si se envía nueva contraseña
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return mapper.toResponse(employeeRepo.save(employee));
    }

    @Override
    public EmployeeResponse updateStatus(Long restaurantId, Long id, EmployeeStatusRequest request) {
        Employee employee = findOrThrow(id);
        if (!employee.getRestaurantId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Employee", id);
        }
        employee.setStatus(request.getStatus());
        log.info("Empleado {} cambió status a {}", id, request.getStatus());
        return mapper.toResponse(employeeRepo.save(employee));
    }

    /** Fase 11 (plan SaaS): límite de empleados por plan — cuenta por tenant, no por restaurante. */
    private void enforceEmployeeLimit(com.nortcali.api.entity.Tenant tenant) {
        PlanLimits limits = PlanLimits.forTenant(tenant);
        long current = employeeRepo.countDistinctActiveByRestaurantsTenantId(tenant.getId());
        if (current >= limits.getMaxEmployees()) {
            throw new BusinessRuleException("Tu plan " + limits.getDisplayName() + " permite hasta "
                    + limits.getMaxEmployees() + " empleados — actualiza de plan para agregar más.");
        }
    }

    private void enforceRoleCeiling(String requestedRole) {
        boolean callerIsAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if ("ADMIN".equals(requestedRole) && !callerIsAdmin) {
            throw new BusinessRuleException("Solo un ADMIN puede asignar el rol ADMIN");
        }
    }

    private Employee findOrThrow(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }
}
