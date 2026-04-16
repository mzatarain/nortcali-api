package com.nortcali.api.service;

import com.nortcali.api.dto.request.OrderRequest;
import com.nortcali.api.dto.request.OrderStatusUpdateRequest;
import com.nortcali.api.dto.request.PaymentRequest;
import com.nortcali.api.dto.response.OrderResponse;
import com.nortcali.api.dto.response.OrderStatusHistoryResponse;
import com.nortcali.api.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    Page<OrderResponse> getByRestaurant(Long restaurantId, String status, Pageable pageable);

    OrderResponse getById(Long id);

    OrderResponse create(Long restaurantId, OrderRequest request);

    OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request);

    List<OrderStatusHistoryResponse> getHistory(Long orderId);

    PaymentResponse addPayment(Long orderId, PaymentRequest request);
}
