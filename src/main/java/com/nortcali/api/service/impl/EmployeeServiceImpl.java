package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.EmployeeRequest;
import com.nortcali.api.dto.request.EmployeeStatusRequest;
import com.nortcali.api.dto.response.EmployeeResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.EmployeeMapper;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.EmployeeService;
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
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        if (employeeRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Ya existe un empleado con el username '" + request.getUsername() + "'");
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

        log.info("Creando empleado '{}' para restaurante {}", request.getUsername(), restaurantId);
        return mapper.toResponse(employeeRepo.save(employee));
    }

    @Override
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findOrThrow(id);
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
    public EmployeeResponse updateStatus(Long id, EmployeeStatusRequest request) {
        Employee employee = findOrThrow(id);
        employee.setStatus(request.getStatus());
        log.info("Empleado {} cambió status a {}", id, request.getStatus());
        return mapper.toResponse(employeeRepo.save(employee));
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
