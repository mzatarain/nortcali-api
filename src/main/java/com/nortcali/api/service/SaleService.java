package com.nortcali.api.service;

import com.nortcali.api.dto.request.SaleRequest;
import com.nortcali.api.dto.response.SaleResponse;
import com.nortcali.api.dto.response.SalesBySourceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SaleService {

    Page<SaleResponse> getByRestaurant(Long restaurantId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    SaleResponse getById(Long id);

    SaleResponse create(Long restaurantId, SaleRequest request);

    void deactivate(Long id);

    List<SalesBySourceResponse> getSalesBySource(Long restaurantId, LocalDate startDate, LocalDate endDate);

    void createFromOrder(Long orderId, Long employeeId);
}
