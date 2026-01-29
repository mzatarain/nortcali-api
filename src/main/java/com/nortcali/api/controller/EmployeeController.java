package com.nortcali.api.controller;

import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.RestaurantRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
	private final EmployeeRepository employeeRepository;
	private final RestaurantRepository restaurantRepository;
	private final PasswordEncoder passwordEncoder;
	
	public EmployeeController(EmployeeRepository employeeRepository, RestaurantRepository restaurantRepository, PasswordEncoder passwordEncoder) {
		this.employeeRepository = employeeRepository;
		this.restaurantRepository = restaurantRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	@GetMapping
	public List<Employee> getAll() {
		return employeeRepository.findAll();
	}
	
	@PostMapping
	public Employee create(@RequestBody Employee employee) {
		employee.setPassword(passwordEncoder.encode(employee.getPassword()));
		return employeeRepository.save(employee);
	}
	
	@PutMapping("/{id}/restaurants")
	public ResponseEntity<?> assignRestaurants(@PathVariable Long id, @RequestBody List<Long> restaurantIds){
		Employee employee = employeeRepository.findById(id)
				.orElseThrow();
		Set<Restaurant> restaurants = new HashSet<>(restaurantRepository.findAllById(restaurantIds));
		
		employee.setRestaurant(restaurants);
		employeeRepository.save(employee);
		
		return ResponseEntity.ok().build();
	}
}