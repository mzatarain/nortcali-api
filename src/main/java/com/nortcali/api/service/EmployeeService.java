package com.nortcali.api.service;

import com.nortcali.api.dto.request.EmployeeRequest;
import com.nortcali.api.dto.request.EmployeeStatusRequest;
import com.nortcali.api.dto.response.EmployeeResponse;

import java.util.List;

public interface EmployeeService {

    List<EmployeeResponse> getByRestaurant(Long restaurantId);

    EmployeeResponse getById(Long id);

    EmployeeResponse create(Long restaurantId, EmployeeRequest request);

    EmployeeResponse update(Long id, EmployeeRequest request);

    EmployeeResponse updateStatus(Long id, EmployeeStatusRequest request);
}
