package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.SalesSourceRequest;
import com.nortcali.api.dto.response.SalesSourceResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.SalesSource;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.SalesSourceMapper;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.SalesSourceRepository;
import com.nortcali.api.service.SalesSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class SalesSourceServiceImpl implements SalesSourceService {

    private final SalesSourceRepository repo;
    private final EmployeeRepository employeeRepo;
    private final SalesSourceMapper mapper;

    public SalesSourceServiceImpl(SalesSourceRepository repo, EmployeeRepository employeeRepo, SalesSourceMapper mapper) {
        this.repo = repo;
        this.employeeRepo = employeeRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesSourceResponse> getAll() {
        Long tenantId = resolveAuthenticatedTenant().getId();
        return repo.findByTenantIdAndIsActiveTrue(tenantId).stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesSourceResponse getById(Long id) {
        return mapper.toResponse(findOwnedOrThrow(id));
    }

    @Override
    public SalesSourceResponse create(SalesSourceRequest request) {
        Tenant tenant = resolveAuthenticatedTenant();
        // Verificar nombre duplicado (ignorando case) dentro del mismo tenant
        repo.findByNameIgnoreCaseAndTenantId(request.getName(), tenant.getId()).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Ya existe una fuente de venta con el nombre '" + request.getName() + "'");
        });

        SalesSource entity = mapper.toEntity(request);
        entity.setActive(true);
        entity.setTenant(tenant);
        log.info("Creando fuente de venta '{}' para tenant {}", request.getName(), tenant.getId());
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    public SalesSourceResponse update(Long id, SalesSourceRequest request) {
        SalesSource entity = findOwnedOrThrow(id);

        // Verificar nombre duplicado solo si cambió, dentro del mismo tenant
        if (!entity.getName().equalsIgnoreCase(request.getName())) {
            repo.findByNameIgnoreCaseAndTenantId(request.getName(), entity.getTenant().getId()).ifPresent(existing -> {
                throw new DuplicateResourceException(
                        "Ya existe una fuente de venta con el nombre '" + request.getName() + "'");
            });
        }

        mapper.updateEntity(request, entity);
        log.info("Actualizando fuente de venta id={}", id);
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        SalesSource entity = findOwnedOrThrow(id);
        entity.setActive(false);
        repo.save(entity);
        log.info("Desactivando fuente de venta id={}", id);
    }

    /**
     * Fase 4 (plan SaaS): `sales_sources` dejó de ser un catálogo global (V23) — sin esta
     * verificación, un tenant podría leer/editar/desactivar fuentes de venta (y su
     * commission_pct) de otro tenant adivinando el id.
     */
    private SalesSource findOwnedOrThrow(Long id) {
        SalesSource entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesSource", id));
        if (!entity.getTenant().getId().equals(resolveAuthenticatedTenant().getId())) {
            throw new ResourceNotFoundException("SalesSource", id);
        }
        return entity;
    }

    private Tenant resolveAuthenticatedTenant() {
        Employee employee = resolveAuthenticatedEmployee();
        return employee.getRestaurant().stream()
                .findFirst()
                .map(r -> r.getTenant())
                .orElseThrow(() -> new BusinessRuleException(
                        "El empleado autenticado no tiene ningún restaurante asignado — no se puede determinar su tenant"));
    }

    private Employee resolveAuthenticatedEmployee() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado autenticado no encontrado: " + email));
    }
}
