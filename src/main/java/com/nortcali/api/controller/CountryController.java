package com.nortcali.api.controller;

import com.nortcali.api.dto.request.CountryRequest;
import com.nortcali.api.dto.response.CountryResponse;
import com.nortcali.api.service.GeoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
@Slf4j
public class CountryController {

    private final GeoService geoService;

    public CountryController(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping
    public ResponseEntity<List<CountryResponse>> getAll() {
        return ResponseEntity.ok(geoService.getAllCountries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(geoService.getCountryById(id));
    }

    @PostMapping
    public ResponseEntity<CountryResponse> create(@Valid @RequestBody CountryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(geoService.createCountry(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody CountryRequest request) {
        return ResponseEntity.ok(geoService.updateCountry(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        geoService.deleteCountry(id);
        return ResponseEntity.noContent().build();
    }
}