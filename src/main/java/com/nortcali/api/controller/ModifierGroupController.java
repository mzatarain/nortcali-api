package com.nortcali.api.controller;

import com.nortcali.api.dto.request.ModifierGroupRequest;
import com.nortcali.api.dto.request.ModifierRequest;
import com.nortcali.api.dto.response.ModifierGroupResponse;
import com.nortcali.api.dto.response.ModifierResponse;
import com.nortcali.api.service.ModifierGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/modifier-groups")
public class ModifierGroupController {

    private final ModifierGroupService modifierGroupService;

    public ModifierGroupController(ModifierGroupService modifierGroupService) {
        this.modifierGroupService = modifierGroupService;
    }

    @GetMapping
    public ResponseEntity<List<ModifierGroupResponse>> getAll(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(modifierGroupService.getByRestaurant(restaurantId));
    }

    @PostMapping
    public ResponseEntity<ModifierGroupResponse> create(@PathVariable Long restaurantId,
                                                        @Valid @RequestBody ModifierGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modifierGroupService.create(restaurantId, request));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<ModifierGroupResponse> update(@PathVariable Long restaurantId,
                                                        @PathVariable Long groupId,
                                                        @Valid @RequestBody ModifierGroupRequest request) {
        return ResponseEntity.ok(modifierGroupService.update(groupId, request));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(@PathVariable Long restaurantId,
                                       @PathVariable Long groupId) {
        modifierGroupService.delete(groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/modifiers")
    public ResponseEntity<List<ModifierResponse>> getModifiers(@PathVariable Long restaurantId,
                                                               @PathVariable Long groupId) {
        return ResponseEntity.ok(modifierGroupService.getModifiers(groupId));
    }

    @PostMapping("/{groupId}/modifiers")
    public ResponseEntity<ModifierResponse> addModifier(@PathVariable Long restaurantId,
                                                        @PathVariable Long groupId,
                                                        @Valid @RequestBody ModifierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modifierGroupService.addModifier(groupId, request));
    }

    @PutMapping("/{groupId}/modifiers/{modifierId}")
    public ResponseEntity<ModifierResponse> updateModifier(@PathVariable Long restaurantId,
                                                           @PathVariable Long groupId,
                                                           @PathVariable Long modifierId,
                                                           @Valid @RequestBody ModifierRequest request) {
        return ResponseEntity.ok(modifierGroupService.updateModifier(modifierId, request));
    }

    @DeleteMapping("/{groupId}/modifiers/{modifierId}")
    public ResponseEntity<Void> deleteModifier(@PathVariable Long restaurantId,
                                               @PathVariable Long groupId,
                                               @PathVariable Long modifierId) {
        modifierGroupService.deleteModifier(modifierId);
        return ResponseEntity.noContent().build();
    }
}
