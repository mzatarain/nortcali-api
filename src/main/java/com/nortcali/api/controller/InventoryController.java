package com.nortcali.api.controller;

import com.nortcali.api.dto.request.InventoryMovementRequest;
import com.nortcali.api.dto.request.SupplyRequest;
import com.nortcali.api.dto.response.InventoryMovementResponse;
import com.nortcali.api.dto.response.SupplyResponse;
import com.nortcali.api.service.InventoryMovementService;
import com.nortcali.api.service.SupplyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class InventoryController {

    private final SupplyService supplyService;
    private final InventoryMovementService movementService;

    public InventoryController(SupplyService supplyService,
                               InventoryMovementService movementService) {
        this.supplyService = supplyService;
        this.movementService = movementService;
    }

    /* =====================
     * SUPPLIES
     * ===================== */

    @GetMapping("/restaurants/{restaurantId}/supplies")
    public ResponseEntity<List<SupplyResponse>> getSupplies(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(supplyService.getByRestaurant(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/supplies/low-stock")
    public ResponseEntity<List<SupplyResponse>> getLowStock(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(supplyService.getLowStock(restaurantId));
    }

    @GetMapping("/supplies/{id}")
    public ResponseEntity<SupplyResponse> getSupplyById(@PathVariable Long id) {
        return ResponseEntity.ok(supplyService.getById(id));
    }

    @PostMapping("/restaurants/{restaurantId}/supplies")
    public ResponseEntity<SupplyResponse> createSupply(@PathVariable Long restaurantId,
                                                       @Valid @RequestBody SupplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplyService.create(restaurantId, request));
    }

    @PutMapping("/supplies/{id}")
    public ResponseEntity<SupplyResponse> updateSupply(@PathVariable Long id,
                                                       @Valid @RequestBody SupplyRequest request) {
        return ResponseEntity.ok(supplyService.update(id, request));
    }

    @DeleteMapping("/supplies/{id}")
    public ResponseEntity<Void> deactivateSupply(@PathVariable Long id) {
        supplyService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /* =====================
     * MOVEMENTS
     * ===================== */

    @GetMapping("/supplies/{supplyId}/movements")
    public ResponseEntity<List<InventoryMovementResponse>> getMovements(@PathVariable Long supplyId) {
        return ResponseEntity.ok(movementService.getBySupply(supplyId));
    }

    @PostMapping("/supplies/{supplyId}/movements")
    public ResponseEntity<InventoryMovementResponse> registerMovement(@PathVariable Long supplyId,
                                                                       @Valid @RequestBody InventoryMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movementService.register(supplyId, request));
    }
}
