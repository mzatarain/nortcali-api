package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.UnitRequest;
import com.nortcali.api.dto.response.UnitResponse;
import com.nortcali.api.entity.Unit;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.UnitMapper;
import com.nortcali.api.repository.UnitRepository;
import com.nortcali.api.service.UnitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class UnitServiceImpl implements UnitService {

    private final UnitRepository repo;
    private final UnitMapper mapper;

    public UnitServiceImpl(UnitRepository repo, UnitMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> getAll() {
        return repo.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public UnitResponse create(UnitRequest request) {
        repo.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Ya existe una unidad de medida con el nombre '" + request.getName() + "'");
        });

        Unit entity = mapper.toEntity(request);
        log.info("Creando unidad de medida '{}'", request.getName());
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    public UnitResponse update(Long id, UnitRequest request) {
        Unit entity = findOrThrow(id);

        if (!entity.getName().equalsIgnoreCase(request.getName())) {
            repo.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
                throw new DuplicateResourceException(
                        "Ya existe una unidad de medida con el nombre '" + request.getName() + "'");
            });
        }

        mapper.updateEntity(request, entity);
        log.info("Actualizando unidad de medida id={}", id);
        return mapper.toResponse(repo.save(entity));
    }

    private Unit findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", id));
    }
}
