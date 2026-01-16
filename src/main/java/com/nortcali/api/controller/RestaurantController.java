package com.nortcali.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.repository.RestaurantRepository;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {
	private final RestaurantRepository repo;
	
	public RestaurantController(RestaurantRepository repo) {
		this.repo = repo;
	}
	
	@GetMapping
	public List<Restaurant> all(){
		return repo.findAll();
	}
	
	@PostMapping
	public Restaurant create(@RequestBody Restaurant r) {
		return repo.save(r);
	}
}
