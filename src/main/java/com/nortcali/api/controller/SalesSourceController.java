package com.nortcali.api.controller;

import com.nortcali.api.dto.request.SalesSourceRequest;
import com.nortcali.api.dto.response.SalesSourceResponse;
import com.nortcali.api.service.SalesSourceService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales-sources")
@Slf4j
public class SalesSourceController {

    private final SalesSourceService salesSourceService;

    public SalesSourceController(SalesSourceService salesSourceService) {
        this.salesSourceService = salesSourceService;
    }

    @GetMapping
    public ResponseEntity<List<SalesSourceResponse>> getAll() {
        return ResponseEntity.ok(salesSourceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesSourceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salesSourceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SalesSourceResponse> create(@Valid @RequestBody SalesSourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesSourceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesSourceResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody SalesSourceRequest request) {
        return ResponseEntity.ok(salesSourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        salesSourceService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
