package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.SupplyRequest;
import com.nortcali.api.dto.response.SupplyResponse;
import com.nortcali.api.entity.Supply;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.SupplyMapper;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.repository.SupplyRepository;
import com.nortcali.api.repository.UnitRepository;
import com.nortcali.api.service.SupplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class SupplyServiceImpl implements SupplyService {

    private final SupplyRepository supplyRepo;
    private final RestaurantRepository restaurantRepo;
    private final UnitRepository unitRepo;
    private final SupplyMapper mapper;

    public SupplyServiceImpl(SupplyRepository supplyRepo,
                             RestaurantRepository restaurantRepo,
                             UnitRepository unitRepo,
                             SupplyMapper mapper) {
        this.supplyRepo = supplyRepo;
        this.restaurantRepo = restaurantRepo;
        this.unitRepo = unitRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplyResponse> getByRestaurant(Long restaurantId) {
        return supplyRepo.findByRestaurantIdAndIsActiveTrue(restaurantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplyResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplyResponse> getLowStock(Long restaurantId) {
        return supplyRepo.findLowStock(restaurantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public SupplyResponse create(Long restaurantId, SupplyRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        var unit = unitRepo.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit", request.getUnitId()));

        Supply entity = mapper.toEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        entity.setUnit(unit);
        log.info("Creando insumo '{}' para restaurante {}", request.getName(), restaurantId);
        return mapper.toResponse(supplyRepo.save(entity));
    }

    @Override
    public SupplyResponse update(Long id, SupplyRequest request) {
        Supply entity = findOrThrow(id);
        var unit = unitRepo.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit", request.getUnitId()));
        mapper.updateEntity(request, entity);
        entity.setUnit(unit);
        return mapper.toResponse(supplyRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        Supply entity = findOrThrow(id);
        entity.setActive(false);
        supplyRepo.save(entity);
        log.info("Insumo {} desactivado", id);
    }

    private Supply findOrThrow(Long id) {
        return supplyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supply", id));
    }
}
