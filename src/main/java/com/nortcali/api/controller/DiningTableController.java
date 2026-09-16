package com.nortcali.api.controller;

import com.nortcali.api.dto.request.DiningTableRequest;
import com.nortcali.api.dto.response.DiningTableResponse;
import com.nortcali.api.service.DiningTableService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/tables")
@Slf4j
public class DiningTableController {

    private final DiningTableService tableService;

    public DiningTableController(DiningTableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    public ResponseEntity<List<DiningTableResponse>> getAll(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(tableService.getByRestaurant(restaurantId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<DiningTableResponse>> getAvailable(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(tableService.getAvailable(restaurantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiningTableResponse> getById(@PathVariable Long restaurantId,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(tableService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TENANT_ADMIN')")
    public ResponseEntity<DiningTableResponse> create(@PathVariable Long restaurantId,
                                                       @Valid @RequestBody DiningTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tableService.create(restaurantId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TENANT_ADMIN')")
    public ResponseEntity<DiningTableResponse> update(@PathVariable Long restaurantId,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody DiningTableRequest request) {
        return ResponseEntity.ok(tableService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TENANT_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long restaurantId,
                                            @PathVariable Long id) {
        tableService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
