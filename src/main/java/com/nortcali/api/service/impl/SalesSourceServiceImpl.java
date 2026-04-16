package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.SalesSourceRequest;
import com.nortcali.api.dto.response.SalesSourceResponse;
import com.nortcali.api.entity.SalesSource;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.SalesSourceMapper;
import com.nortcali.api.repository.SalesSourceRepository;
import com.nortcali.api.service.SalesSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class SalesSourceServiceImpl implements SalesSourceService {

    private final SalesSourceRepository repo;
    private final SalesSourceMapper mapper;

    public SalesSourceServiceImpl(SalesSourceRepository repo, SalesSourceMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesSourceResponse> getAll() {
        return repo.findByIsActiveTrue().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesSourceResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public SalesSourceResponse create(SalesSourceRequest request) {
        // Verificar nombre duplicado (ignorando case)
        repo.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Ya existe una fuente de venta con el nombre '" + request.getName() + "'");
        });

        SalesSource entity = mapper.toEntity(request);
        entity.setActive(true);
        log.info("Creando fuente de venta '{}'", request.getName());
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    public SalesSourceResponse update(Long id, SalesSourceRequest request) {
        SalesSource entity = findOrThrow(id);

        // Verificar nombre duplicado solo si cambió
        if (!entity.getName().equalsIgnoreCase(request.getName())) {
            repo.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
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
        SalesSource entity = findOrThrow(id);
        entity.setActive(false);
        repo.save(entity);
        log.info("Desactivando fuente de venta id={}", id);
    }

    private SalesSource findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesSource", id));
    }
}
