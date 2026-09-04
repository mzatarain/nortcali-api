package com.nortcali.api.controller;

import com.nortcali.api.dto.request.VariantModifierRequest;
import com.nortcali.api.dto.response.VariantModifierResponse;
import com.nortcali.api.service.VariantModifierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menu-item-variants/{variantId}/modifiers")
public class VariantModifierController {

    private final VariantModifierService variantModifierService;

    public VariantModifierController(VariantModifierService variantModifierService) {
        this.variantModifierService = variantModifierService;
    }

    @GetMapping
    public ResponseEntity<List<VariantModifierResponse>> getAll(@PathVariable Long restaurantId,
                                                                  @PathVariable Long variantId) {
        return ResponseEntity.ok(variantModifierService.getByVariant(restaurantId, variantId));
    }

    @PostMapping
    public ResponseEntity<VariantModifierResponse> add(@PathVariable Long restaurantId,
                                                       @PathVariable Long variantId,
                                                       @Valid @RequestBody VariantModifierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(variantModifierService.add(restaurantId, variantId, request));
    }

    @DeleteMapping("/{modifierId}")
    public ResponseEntity<Void> remove(@PathVariable Long restaurantId,
                                       @PathVariable Long variantId,
                                       @PathVariable Long modifierId) {
        variantModifierService.remove(restaurantId, variantId, modifierId);
        return ResponseEntity.noContent().build();
    }
}
