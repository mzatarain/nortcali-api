package com.nortcali.api.controller;

import com.nortcali.api.dto.request.CityRequest;
import com.nortcali.api.dto.response.CityResponse;
import com.nortcali.api.service.GeoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
@Slf4j
public class CityController {

    private final GeoService geoService;

    public CityController(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping
    public ResponseEntity<List<CityResponse>> getAll(@RequestParam(required = false) Long stateId) {
        if (stateId != null) {
            return ResponseEntity.ok(geoService.getCitiesByState(stateId));
        }
        return ResponseEntity.ok(geoService.getAllCities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(geoService.getCityById(id));
    }

    @PostMapping
    public ResponseEntity<CityResponse> create(@Valid @RequestBody CityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(geoService.createCity(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CityResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody CityRequest request) {
        return ResponseEntity.ok(geoService.updateCity(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        geoService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }
}
