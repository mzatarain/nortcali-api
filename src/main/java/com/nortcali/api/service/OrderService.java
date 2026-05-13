package com.nortcali.api.service;

import com.nortcali.api.dto.request.OrderRequest;
import com.nortcali.api.dto.request.OrderStatusUpdateRequest;
import com.nortcali.api.dto.request.PaymentRequest;
import com.nortcali.api.dto.response.CloseDayResponse;
import com.nortcali.api.dto.response.OrderResponse;
import com.nortcali.api.dto.response.OrderStatusHistoryResponse;
import com.nortcali.api.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    Page<OrderResponse> getByRestaurant(Long restaurantId, List<String> statuses, LocalDate date, Pageable pageable);

    OrderResponse getById(Long id);

    OrderResponse create(Long restaurantId, OrderRequest request);

    OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request);

    List<OrderStatusHistoryResponse> getHistory(Long orderId);

    PaymentResponse addPayment(Long orderId, PaymentRequest request);

    void delete(Long restaurantId, Long orderId);

    CloseDayResponse closeDay(Long restaurantId);
}
