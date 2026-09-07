package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.UnitRequest;
import com.nortcali.api.dto.response.UnitResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.Unit;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.UnitMapper;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.UnitRepository;
import com.nortcali.api.service.UnitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class UnitServiceImpl implements UnitService {

    private final UnitRepository repo;
    private final EmployeeRepository employeeRepo;
    private final UnitMapper mapper;

    public UnitServiceImpl(UnitRepository repo, EmployeeRepository employeeRepo, UnitMapper mapper) {
        this.repo = repo;
        this.employeeRepo = employeeRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> getAll() {
        Long tenantId = resolveAuthenticatedTenant().getId();
        return repo.findByTenantId(tenantId).stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getById(Long id) {
        return mapper.toResponse(findOwnedOrThrow(id));
    }

    @Override
    public UnitResponse create(UnitRequest request) {
        Tenant tenant = resolveAuthenticatedTenant();
        repo.findByNameIgnoreCaseAndTenantId(request.getName(), tenant.getId()).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Ya existe una unidad de medida con el nombre '" + request.getName() + "'");
        });

        Unit entity = mapper.toEntity(request);
        entity.setTenant(tenant);
        log.info("Creando unidad de medida '{}' para tenant {}", request.getName(), tenant.getId());
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    public UnitResponse update(Long id, UnitRequest request) {
        Unit entity = findOwnedOrThrow(id);

        if (!entity.getName().equalsIgnoreCase(request.getName())) {
            repo.findByNameIgnoreCaseAndTenantId(request.getName(), entity.getTenant().getId()).ifPresent(existing -> {
                throw new DuplicateResourceException(
                        "Ya existe una unidad de medida con el nombre '" + request.getName() + "'");
            });
        }

        mapper.updateEntity(request, entity);
        log.info("Actualizando unidad de medida id={}", id);
        return mapper.toResponse(repo.save(entity));
    }

    /**
     * Fase 4 (plan SaaS): `units` dejó de ser un catálogo global (V23) — sin esta
     * verificación, un tenant podría leer/editar unidades de otro tenant adivinando el id.
     */
    private Unit findOwnedOrThrow(Long id) {
        Unit entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", id));
        if (!entity.getTenant().getId().equals(resolveAuthenticatedTenant().getId())) {
            throw new ResourceNotFoundException("Unit", id);
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
