package com.nortcali.api.controller;

import com.nortcali.api.dto.request.StateRequest;
import com.nortcali.api.dto.response.StateResponse;
import com.nortcali.api.service.GeoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/states")
@Slf4j
public class StateController {

    private final GeoService geoService;

    public StateController(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping
    public ResponseEntity<List<StateResponse>> getAll(@RequestParam(required = false) Long countryId) {
        if (countryId != null) {
            return ResponseEntity.ok(geoService.getStatesByCountry(countryId));
        }
        return ResponseEntity.ok(geoService.getAllStates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(geoService.getStateById(id));
    }

    @PostMapping
    public ResponseEntity<StateResponse> create(@Valid @RequestBody StateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(geoService.createState(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StateResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody StateRequest request) {
        return ResponseEntity.ok(geoService.updateState(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        geoService.deleteState(id);
        return ResponseEntity.noContent().build();
    }
}
