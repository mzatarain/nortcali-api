package com.nortcali.api.controller;

import com.nortcali.api.dto.request.ComboRequest;
import com.nortcali.api.dto.response.ComboResponse;
import com.nortcali.api.service.ComboService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/combos")
public class ComboController {

    private final ComboService comboService;

    public ComboController(ComboService comboService) {
        this.comboService = comboService;
    }

    @GetMapping
    public ResponseEntity<List<ComboResponse>> getAll(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(comboService.getByRestaurant(restaurantId));
    }

    @PostMapping
    public ResponseEntity<ComboResponse> create(@PathVariable Long restaurantId,
                                                @Valid @RequestBody ComboRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comboService.create(restaurantId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComboResponse> update(@PathVariable Long restaurantId,
                                                @PathVariable Long id,
                                                @Valid @RequestBody ComboRequest request) {
        return ResponseEntity.ok(comboService.update(restaurantId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long restaurantId,
                                       @PathVariable Long id) {
        comboService.delete(restaurantId, id);
        return ResponseEntity.noContent().build();
    }
}
