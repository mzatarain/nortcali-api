package com.nortcali.api.controller;

import com.nortcali.api.dto.request.EmployeeRequest;
import com.nortcali.api.dto.request.EmployeeStatusRequest;
import com.nortcali.api.dto.response.EmployeeResponse;
import com.nortcali.api.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/restaurants/{restaurantId}/employees")
    public ResponseEntity<List<EmployeeResponse>> getByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(employeeService.getByRestaurant(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/employees/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long restaurantId,
                                                    @PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(restaurantId, id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/restaurants/{restaurantId}/employees")
    public ResponseEntity<EmployeeResponse> create(@PathVariable Long restaurantId,
                                                   @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.create(restaurantId, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/employees/{id}/status")
    public ResponseEntity<EmployeeResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody EmployeeStatusRequest request) {
        return ResponseEntity.ok(employeeService.updateStatus(id, request));
    }
}
