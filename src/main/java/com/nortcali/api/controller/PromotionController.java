package com.nortcali.api.controller;

import com.nortcali.api.dto.request.PromotionRequest;
import com.nortcali.api.dto.response.PromotionResponse;
import com.nortcali.api.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAll(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(promotionService.getByRestaurant(restaurantId));
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> create(@PathVariable Long restaurantId,
                                                    @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotionService.create(restaurantId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> update(@PathVariable Long restaurantId,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionService.update(restaurantId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long restaurantId,
                                       @PathVariable Long id) {
        promotionService.delete(restaurantId, id);
        return ResponseEntity.noContent().build();
    }
}
