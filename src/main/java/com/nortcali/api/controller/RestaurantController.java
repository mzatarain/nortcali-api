package com.nortcali.api.controller;

import com.nortcali.api.dto.request.RestaurantRequest;
import com.nortcali.api.dto.response.RestaurantResponse;
import com.nortcali.api.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@Slf4j
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAll(@RequestParam(required = false) Long cityId) {
        return ResponseEntity.ok(restaurantService.getAll(cityId));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantResponse> getById(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.getById(restaurantId));
    }

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantService.create(request));
    }

    @PutMapping("/{restaurantId}")
    public ResponseEntity<RestaurantResponse> update(@PathVariable Long restaurantId,
                                                     @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.ok(restaurantService.update(restaurantId, request));
    }

    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<Void> deactivate(@PathVariable Long restaurantId) {
        restaurantService.deactivate(restaurantId);
        return ResponseEntity.noContent().build();
    }
}
