package com.nortcali.api.controller;

import com.nortcali.api.dto.request.DeliveryDriverRequest;
import com.nortcali.api.dto.response.DeliveryDriverResponse;
import com.nortcali.api.service.DeliveryDriverService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/drivers")
@Slf4j
public class DeliveryDriverController {

    private final DeliveryDriverService driverService;

    public DeliveryDriverController(DeliveryDriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public ResponseEntity<List<DeliveryDriverResponse>> getAll(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(driverService.getByRestaurant(restaurantId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeliveryDriverResponse>> getAvailable(@PathVariable Long restaurantId) {
        // Todos los activos del restaurante se consideran disponibles
        return ResponseEntity.ok(driverService.getByRestaurant(restaurantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryDriverResponse> getById(@PathVariable Long restaurantId,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(driverService.getById(id));
    }

    @PostMapping
    public ResponseEntity<DeliveryDriverResponse> create(@PathVariable Long restaurantId,
                                                         @Valid @RequestBody DeliveryDriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(driverService.create(restaurantId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryDriverResponse> update(@PathVariable Long restaurantId,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody DeliveryDriverRequest request) {
        return ResponseEntity.ok(driverService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long restaurantId,
                                           @PathVariable Long id) {
        driverService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
