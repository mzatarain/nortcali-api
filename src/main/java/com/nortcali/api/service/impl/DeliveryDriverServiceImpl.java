package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.DeliveryDriverRequest;
import com.nortcali.api.dto.response.DeliveryDriverResponse;
import com.nortcali.api.entity.DeliveryDriver;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.DeliveryDriverMapper;
import com.nortcali.api.repository.DeliveryDriverRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.DeliveryDriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class DeliveryDriverServiceImpl implements DeliveryDriverService {

    private final DeliveryDriverRepository driverRepo;
    private final RestaurantRepository restaurantRepo;
    private final DeliveryDriverMapper mapper;

    public DeliveryDriverServiceImpl(DeliveryDriverRepository driverRepo,
                                     RestaurantRepository restaurantRepo,
                                     DeliveryDriverMapper mapper) {
        this.driverRepo = driverRepo;
        this.restaurantRepo = restaurantRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDriverResponse> getByRestaurant(Long restaurantId) {
        return driverRepo.findByRestaurantIdAndIsActiveTrue(restaurantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDriverResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public DeliveryDriverResponse create(Long restaurantId, DeliveryDriverRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        DeliveryDriver entity = mapper.toEntity(request);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        return mapper.toResponse(driverRepo.save(entity));
    }

    @Override
    public DeliveryDriverResponse update(Long id, DeliveryDriverRequest request) {
        DeliveryDriver entity = findOrThrow(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(driverRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        DeliveryDriver entity = findOrThrow(id);
        entity.setActive(false);
        driverRepo.save(entity);
        log.info("Repartidor {} desactivado", id);
    }

    private DeliveryDriver findOrThrow(Long id) {
        return driverRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryDriver", id));
    }
}
