package com.nortcali.api.controller;

import com.nortcali.api.dto.request.EmployeeRoleRequest;
import com.nortcali.api.dto.response.EmployeeRoleResponse;
import com.nortcali.api.service.EmployeeRoleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-roles")
@Slf4j
public class EmployeeRoleController {

    private final EmployeeRoleService employeeRoleService;

    public EmployeeRoleController(EmployeeRoleService employeeRoleService) {
        this.employeeRoleService = employeeRoleService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeRoleResponse>> getAll() {
        return ResponseEntity.ok(employeeRoleService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeRoleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeRoleService.getById(id));
    }

    @PostMapping
    public ResponseEntity<EmployeeRoleResponse> create(@Valid @RequestBody EmployeeRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeRoleService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeRoleResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody EmployeeRoleRequest request) {
        return ResponseEntity.ok(employeeRoleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        employeeRoleService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
