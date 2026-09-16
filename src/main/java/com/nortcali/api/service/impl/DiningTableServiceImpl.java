package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.DiningTableRequest;
import com.nortcali.api.dto.response.DiningTableResponse;
import com.nortcali.api.entity.DiningTable;
import com.nortcali.api.entity.enums.TableStatus;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.DiningTableMapper;
import com.nortcali.api.repository.DiningTableRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.DiningTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class DiningTableServiceImpl implements DiningTableService {

    private final DiningTableRepository tableRepo;
    private final RestaurantRepository restaurantRepo;
    private final DiningTableMapper mapper;

    public DiningTableServiceImpl(DiningTableRepository tableRepo,
                                   RestaurantRepository restaurantRepo,
                                   DiningTableMapper mapper) {
        this.tableRepo = tableRepo;
        this.restaurantRepo = restaurantRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiningTableResponse> getByRestaurant(Long restaurantId) {
        return tableRepo.findByRestaurantIdAndIsActiveTrue(restaurantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiningTableResponse> getAvailable(Long restaurantId) {
        return tableRepo.findByRestaurantIdAndIsActiveTrueAndStatus(restaurantId, TableStatus.AVAILABLE)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DiningTableResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public DiningTableResponse create(Long restaurantId, DiningTableRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        DiningTable entity = mapper.toEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        entity.setStatus(TableStatus.AVAILABLE);
        return mapper.toResponse(tableRepo.save(entity));
    }

    @Override
    public DiningTableResponse update(Long id, DiningTableRequest request) {
        DiningTable entity = findOrThrow(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(tableRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        DiningTable entity = findOrThrow(id);
        if (entity.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessRuleException("No se puede desactivar una mesa ocupada");
        }
        entity.setActive(false);
        tableRepo.save(entity);
        log.info("Mesa {} desactivada", id);
    }

    @Override
    public DiningTable occupy(Long tableId, Long restaurantId) {
        DiningTable table = findOrThrow(tableId);
        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new BusinessRuleException("La mesa no pertenece a este restaurante");
        }
        if (table.getStatus() != TableStatus.AVAILABLE) {
            throw new BusinessRuleException("La mesa no está disponible");
        }
        table.setStatus(TableStatus.OCCUPIED);
        return tableRepo.save(table);
    }

    @Override
    public void release(Long tableId) {
        DiningTable table = findOrThrow(tableId);
        table.setStatus(TableStatus.AVAILABLE);
        tableRepo.save(table);
    }

    private DiningTable findOrThrow(Long id) {
        return tableRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DiningTable", id));
    }
}
