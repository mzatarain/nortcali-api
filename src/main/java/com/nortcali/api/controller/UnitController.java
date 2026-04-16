package com.nortcali.api.controller;

import com.nortcali.api.dto.request.UnitRequest;
import com.nortcali.api.dto.response.UnitResponse;
import com.nortcali.api.service.UnitService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/units")
@Slf4j
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    public ResponseEntity<List<UnitResponse>> getAll() {
        return ResponseEntity.ok(unitService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(unitService.getById(id));
    }

    @PostMapping
    public ResponseEntity<UnitResponse> create(@Valid @RequestBody UnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unitService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnitResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody UnitRequest request) {
        return ResponseEntity.ok(unitService.update(id, request));
    }
}
