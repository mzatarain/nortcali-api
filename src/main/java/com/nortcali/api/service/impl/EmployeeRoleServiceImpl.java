package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.EmployeeRoleRequest;
import com.nortcali.api.dto.response.EmployeeRoleResponse;
import com.nortcali.api.entity.EmployeeRole;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.EmployeeRoleMapper;
import com.nortcali.api.repository.EmployeeRoleRepository;
import com.nortcali.api.service.EmployeeRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class EmployeeRoleServiceImpl implements EmployeeRoleService {

    private final EmployeeRoleRepository repo;
    private final EmployeeRoleMapper mapper;

    public EmployeeRoleServiceImpl(EmployeeRoleRepository repo, EmployeeRoleMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeRoleResponse> getAll() {
        return repo.findByIsActiveTrueOrderByNameAsc().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeRoleResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public EmployeeRoleResponse create(EmployeeRoleRequest request) {
        repo.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Ya existe un rol con el nombre '" + request.getName() + "'");
        });

        EmployeeRole entity = mapper.toEntity(request);
        entity.setActive(true);
        log.info("Creando rol de empleado '{}'", request.getName());
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    public EmployeeRoleResponse update(Long id, EmployeeRoleRequest request) {
        EmployeeRole entity = findOrThrow(id);

        if (!entity.getName().equalsIgnoreCase(request.getName())) {
            repo.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
                throw new DuplicateResourceException(
                        "Ya existe un rol con el nombre '" + request.getName() + "'");
            });
        }

        mapper.updateEntity(request, entity);
        log.info("Actualizando rol de empleado id={}", id);
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        EmployeeRole entity = findOrThrow(id);
        entity.setActive(false);
        repo.save(entity);
        log.info("Desactivando rol de empleado id={}", id);
    }

    private EmployeeRole findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeRole", id));
    }
}
