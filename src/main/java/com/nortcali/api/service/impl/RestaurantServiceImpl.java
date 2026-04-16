package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.RestaurantRequest;
import com.nortcali.api.dto.response.RestaurantResponse;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.RestaurantMapper;
import com.nortcali.api.repository.CityRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.RestaurantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepo;
    private final CityRepository cityRepo;
    private final RestaurantMapper mapper;

    public RestaurantServiceImpl(RestaurantRepository restaurantRepo,
                                 CityRepository cityRepo,
                                 RestaurantMapper mapper) {
        this.restaurantRepo = restaurantRepo;
        this.cityRepo = cityRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAll(Long cityId) {
        List<Restaurant> list = (cityId == null)
                ? restaurantRepo.findAll()
                : restaurantRepo.findByCityId(cityId);
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
        entity.setCity(city);
        return mapper.toResponse(restaurantRepo.save(entity));
    }

    @Override
    public RestaurantResponse update(Long id, RestaurantRequest request) {
        Restaurant entity = findOrThrow(id);
        var city = cityRepo.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", request.getCityId()));

        mapper.updateEntity(request, entity);
        entity.setCity(city);
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
