package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.RegisterRequest;
import com.nortcali.api.entity.City;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.MenuCategory;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.SalesSource;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.Unit;
import com.nortcali.api.entity.enums.TenantStatus;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.repository.CityRepository;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.MenuCategoryRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.repository.SalesSourceRepository;
import com.nortcali.api.repository.TenantRepository;
import com.nortcali.api.repository.UnitRepository;
import com.nortcali.api.service.RegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fase 4 (plan SaaS): onboarding automatizado. Construye las entidades directamente en vez de
 * reusar {@code RestaurantServiceImpl.create()}/{@code EmployeeServiceImpl.create()} — esos
 * métodos derivan el tenant del empleado ya autenticado (`SecurityContextHolder`), que no
 * existe todavía durante el registro. La lógica de ubicación (`setLocation`) y las reglas de
 * negocio (password BCrypt, duplicado de email) se replican aquí para mantener el mismo
 * comportamiento que el alta manual.
 */
@Service
@Transactional
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    /** Duración del trial inicial — Fase 7 (aún sin implementar) decidirá si requiere tarjeta. */
    private static final long TRIAL_DAYS = 14;

    private static final List<String> STARTER_MENU_CATEGORIES = List.of(
            "Entradas", "Platos fuertes", "Bebidas", "Postres");

    private record StarterUnit(String name, String abbreviation) {}
    private static final List<StarterUnit> STARTER_UNITS = List.of(
            new StarterUnit("Kilogramo", "kg"),
            new StarterUnit("Gramo", "g"),
            new StarterUnit("Litro", "L"),
            new StarterUnit("Pieza", "pza"));

    // Mismos nombres que el mapeo OrderSource → SalesSource ya establecido (ver CLAUDE.md de
    // nortcali-management) — sin "Mostrador" sembrado, la primera orden entregada del tenant
    // nuevo lanzaría BusinessRuleException al no encontrar ninguna fuente de venta activa.
    private static final List<String> STARTER_SALES_SOURCES = List.of(
            "Mostrador", "Teléfono", "WhatsApp", "Rappi", "Uber Eats", "Didi Food");

    private final TenantRepository tenantRepo;
    private final RestaurantRepository restaurantRepo;
    private final CityRepository cityRepo;
    private final EmployeeRepository employeeRepo;
    private final MenuCategoryRepository menuCategoryRepo;
    private final UnitRepository unitRepo;
    private final SalesSourceRepository salesSourceRepo;
    private final PasswordEncoder passwordEncoder;

    public RegistrationServiceImpl(TenantRepository tenantRepo,
                                   RestaurantRepository restaurantRepo,
                                   CityRepository cityRepo,
                                   EmployeeRepository employeeRepo,
                                   MenuCategoryRepository menuCategoryRepo,
                                   UnitRepository unitRepo,
                                   SalesSourceRepository salesSourceRepo,
                                   PasswordEncoder passwordEncoder) {
        this.tenantRepo = tenantRepo;
        this.restaurantRepo = restaurantRepo;
        this.cityRepo = cityRepo;
        this.employeeRepo = employeeRepo;
        this.menuCategoryRepo = menuCategoryRepo;
        this.unitRepo = unitRepo;
        this.salesSourceRepo = salesSourceRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Employee register(RegisterRequest request) {
        if (employeeRepo.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new DuplicateResourceException(
                    "Ya existe una cuenta con el correo '" + request.getAdminEmail() + "'");
        }
        City city = cityRepo.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", request.getCityId()));

        Tenant tenant = new Tenant();
        tenant.setName(request.getBusinessName());
        tenant.setStatus(TenantStatus.TRIAL);
        tenant.setTrialEndsAt(LocalDateTime.now().plusDays(TRIAL_DAYS));
        tenant = tenantRepo.save(tenant);

        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getRestaurantName());
        restaurant.setPhone(request.getRestaurantPhone());
        restaurant.setActive(true);
        setLocation(restaurant, city);
        restaurant.setTenant(tenant);
        restaurant = restaurantRepo.save(restaurant);

        Employee employee = new Employee();
        employee.setFirstName(request.getAdminFirstName());
        employee.setLastName(request.getAdminLastName());
        employee.setUsername(generateUsername(request.getAdminEmail()));
        employee.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        employee.setPhone(request.getAdminPhone());
        employee.setEmail(request.getAdminEmail());
        employee.setRole("TENANT_ADMIN");
        employee.setStatus("ACTIVE");
        employee.setHireDate(LocalDate.now());
        Set<Restaurant> restaurants = new HashSet<>();
        restaurants.add(restaurant);
        employee.setRestaurant(restaurants);
        employee.setRestaurantId(restaurant.getId());
        employee = employeeRepo.save(employee);

        seedCatalogs(tenant, restaurant);

        log.info("Nuevo tenant '{}' registrado (id={}, restaurante={}, admin={})",
                tenant.getName(), tenant.getId(), restaurant.getId(), employee.getEmail());
        return employee;
    }

    private void seedCatalogs(Tenant tenant, Restaurant restaurant) {
        int order = 0;
        for (String name : STARTER_MENU_CATEGORIES) {
            MenuCategory category = new MenuCategory();
            category.setName(name);
            category.setDisplayOrder(order++);
            category.setActive(true);
            category.setRestaurant(restaurant);
            menuCategoryRepo.save(category);
        }

        for (StarterUnit starter : STARTER_UNITS) {
            Unit unit = new Unit();
            unit.setName(starter.name());
            unit.setAbbreviation(starter.abbreviation());
            unit.setTenant(tenant);
            unitRepo.save(unit);
        }

        for (String name : STARTER_SALES_SOURCES) {
            SalesSource source = new SalesSource();
            source.setName(name);
            source.setCommissionPct(BigDecimal.ZERO);
            source.setActive(true);
            source.setTenant(tenant);
            salesSourceRepo.save(source);
        }
    }

    /**
     * Mismo fix que {@code RestaurantServiceImpl.setLocation} — `state_id`/`country_id` son
     * `NOT NULL` en la BD pero no se exponen en el request; se derivan de la ciudad elegida.
     */
    private void setLocation(Restaurant entity, City city) {
        entity.setCity(city);
        entity.setState(city.getState());
        entity.setCountry(city.getState().getCountry());
    }

    /**
     * `employees.username` sigue siendo `NOT NULL UNIQUE` aunque ya no se usa para login (Fase
     * 2) — se deriva del correo y se desambigua con un sufijo numérico si ya existe.
     */
    private String generateUsername(String email) {
        String base = email.substring(0, email.indexOf('@'))
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        int suffix = 1;
        while (employeeRepo.findByUsername(candidate).isPresent()) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
