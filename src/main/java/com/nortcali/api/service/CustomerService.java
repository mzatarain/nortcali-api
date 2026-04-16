package com.nortcali.api.service;

import com.nortcali.api.dto.request.CustomerRequest;
import com.nortcali.api.dto.response.CustomerResponse;

import java.util.List;

public interface CustomerService {

    List<CustomerResponse> getByRestaurant(Long restaurantId);

    CustomerResponse getById(Long id);

    CustomerResponse create(Long restaurantId, CustomerRequest request);

    CustomerResponse update(Long id, CustomerRequest request);

    void deactivate(Long id);
}
