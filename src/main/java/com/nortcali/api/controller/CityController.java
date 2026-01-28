package com.nortcali.api.controller;

import com.nortcali.api.dto.CityRequestDto;
import com.nortcali.api.dto.CityResponseDto;
import com.nortcali.api.entity.City;
import com.nortcali.api.entity.State;
import com.nortcali.api.repository.CityRepository;
import com.nortcali.api.repository.StateRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cities")
public class CityController {
	private final CityRepository cityRepository;
	private final StateRepository stateRepository;
		
	public CityController(CityRepository cityRepository, StateRepository stateRepository) {
		this.cityRepository = cityRepository;
		this.stateRepository = stateRepository;
	}
	
	/* ==========================
	 * GET ALL
	 */	
	@GetMapping
	public List<CityResponseDto> getAll(@RequestParam(required = false) Long stateId){
		List<City> cities = (stateId == null)
				? cityRepository.findAll()
				: cityRepository.findByStateId(stateId);
		return cities.stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}
	
	/* ===========
	 * GET BY ID
	 */
	@GetMapping("/{id}")
	public ResponseEntity<CityResponseDto> getById(@PathVariable Long id){
		return cityRepository.findById(id)
				.map(city -> ResponseEntity.ok(toDto(city)))
				.orElse(ResponseEntity.notFound().build());
	}
	/* ===================
	 * CREATE
	 */
	@PostMapping
	public ResponseEntity<CityResponseDto> create(@RequestBody CityRequestDto dto) {
		State state = stateRepository.findById(dto.getStateId())
				.orElseThrow(() -> new RuntimeException("State not found"));
		City city = new City();
		city.setName(dto.getName());
		city.setState(state);
		
		City saved = cityRepository.save(city);
		return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
	}
	
	/*===========================
	 * UPDATE
	 */
	@PutMapping("/{id}")
	public ResponseEntity<CityResponseDto> update(@PathVariable Long id, @RequestBody CityRequestDto dto){
		return cityRepository.findById(id)
				.map(city -> {
					State state = stateRepository.findById(dto.getStateId())
							.orElseThrow(() -> new RuntimeException("State not found"));
					city.setName(dto.getName());
					city.setState(state);
					
					City updated = cityRepository.save(city);
					return ResponseEntity.ok(toDto(updated));
				})
				.orElse(ResponseEntity.notFound().build());
	}
	
	/* ==============
	 * DELETE
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete (@PathVariable Long id){
		if (!cityRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		cityRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
	
	/* ============
	 * MAPPER
	 */	
	private CityResponseDto toDto(City city) {
		return new CityResponseDto(city.getId(), city.getName(), city.getState().getId(), city.getState().getName());
	}
}
