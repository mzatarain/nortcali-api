package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.RestaurantRequest;
import com.nortcali.api.dto.response.RestaurantResponse;
import com.nortcali.api.entity.City;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.RestaurantMapper;
import com.nortcali.api.repository.CityRepository;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.RestaurantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepo;
    private final CityRepository cityRepo;
    private final EmployeeRepository employeeRepo;
    private final RestaurantMapper mapper;

    public RestaurantServiceImpl(RestaurantRepository restaurantRepo,
                                 CityRepository cityRepo,
                                 EmployeeRepository employeeRepo,
                                 RestaurantMapper mapper) {
        this.restaurantRepo = restaurantRepo;
        this.cityRepo = cityRepo;
        this.employeeRepo = employeeRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAll(Long cityId) {
        Long tenantId = resolveAuthenticatedTenant().getId();
        List<Restaurant> list = (cityId == null)
                ? restaurantRepo.findByTenantId(tenantId)
                : restaurantRepo.findByTenantIdAndCityId(tenantId, cityId);
        return list.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public RestaurantResponse create(RestaurantRequest request) {
        var city = cityRepo.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", request.getCityId()));

        Restaurant entity = mapper.toEntity(request);
        setLocation(entity, city);
        entity.setTenant(resolveAuthenticatedTenant());
        return mapper.toResponse(restaurantRepo.save(entity));
    }

    /**
     * `restaurants.state_id` y `restaurants.country_id` son columnas `NOT NULL` en la BD sin
     * mapeo en `RestaurantRequest`/`RestaurantResponse` (solo se expone `cityId`) — se derivan
     * de la ciudad seleccionada. Sin esto, cualquier alta o edición de restaurante falla en el
     * INSERT/UPDATE con `Field 'country_id' doesn't have a default value`.
     */
    private void setLocation(Restaurant entity, City city) {
        entity.setCity(city);
        entity.setState(city.getState());
        entity.setCountry(city.getState().getCountry());
    }

    /**
     * Multi-sucursal desde el día 1 (Fase 1 del plan SaaS): el tenant del nuevo restaurante es
     * el del empleado autenticado (`ADMIN`/`MANAGER` que ya pertenece a un restaurante del
     * tenant), no un valor adivinado — soporta múltiples tenants en el sistema.
     */
    private Tenant resolveAuthenticatedTenant() {
        Employee employee = resolveAuthenticatedEmployee();
        return employee.getRestaurant().stream()
                .findFirst()
                .map(Restaurant::getTenant)
                .orElseThrow(() -> new BusinessRuleException(
                        "El empleado autenticado no tiene ningún restaurante asignado — no se puede determinar su tenant"));
    }

    private Employee resolveAuthenticatedEmployee() {
        // Fase 2: el principal es el email (login por email), no Employee.username.
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado autenticado no encontrado: " + email));
    }

    @Override
    public RestaurantResponse update(Long id, RestaurantRequest request) {
        Restaurant entity = findOrThrow(id);
        var city = cityRepo.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", request.getCityId()));

        mapper.updateEntity(request, entity);
        setLocation(entity, city);
        return mapper.toResponse(restaurantRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        Restaurant entity = findOrThrow(id);
        entity.setActive(false);
        restaurantRepo.save(entity);
        log.info("Restaurante {} desactivado", id);
    }

    private Restaurant findOrThrow(Long id) {
        return restaurantRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
    }
}
