package com.nortcali.api.controller;

import com.nortcali.api.dto.request.OrderRequest;
import com.nortcali.api.dto.request.OrderStatusUpdateRequest;
import com.nortcali.api.dto.request.PaymentRequest;
import com.nortcali.api.dto.response.CloseDayResponse;
import com.nortcali.api.dto.response.OrderResponse;
import com.nortcali.api.dto.response.OrderStatusHistoryResponse;
import com.nortcali.api.dto.response.PaymentResponse;
import com.nortcali.api.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.getByRestaurant(restaurantId, status, date, pageable));
    }

    @GetMapping("/restaurants/{restaurantId}/orders/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long restaurantId,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PostMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<OrderResponse> create(@PathVariable Long restaurantId,
                                                @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(restaurantId, request));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info(">>> updateOrderStatus llamado. orderId={}, toStatus={}", id, request.getToStatus());
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    @GetMapping("/orders/{id}/history")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getHistory(id));
    }

    @PostMapping("/restaurants/{restaurantId}/orders/close-day")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CloseDayResponse> closeDay(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.closeDay(restaurantId));
    }

    @DeleteMapping("/restaurants/{restaurantId}/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long restaurantId,
                                       @PathVariable Long orderId) {
        orderService.delete(restaurantId, orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{id}/payments")
    public ResponseEntity<PaymentResponse> addPayment(@PathVariable Long id,
                                                      @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.addPayment(id, request));
    }
}
