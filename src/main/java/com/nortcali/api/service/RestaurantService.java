package com.nortcali.api.service;

import com.nortcali.api.dto.request.RestaurantRequest;
import com.nortcali.api.dto.response.RestaurantResponse;

import java.util.List;

public interface RestaurantService {

    List<RestaurantResponse> getAll(Long cityId);

    RestaurantResponse getById(Long id);

    RestaurantResponse create(RestaurantRequest request);

    RestaurantResponse update(Long id, RestaurantRequest request);

    void deactivate(Long id);
}
