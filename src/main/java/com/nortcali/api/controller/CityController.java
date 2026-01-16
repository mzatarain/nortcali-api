package com.nortcali.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nortcali.api.entity.City;
import com.nortcali.api.repository.CityRepository;

@RestController
@RequestMapping("/api/cities")
public class CityController {
	private final CityRepository repo;
	
	public CityController(CityRepository repo) {
		this.repo = repo;
	}
	
	@GetMapping
	public List<City> all(){
		return repo.findAll();
	}
	
	@PostMapping
	public City create(@RequestBody City c) {
		return repo.save(c);
	}
}
