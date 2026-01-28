package com.nortcali.api.controller;

import com.nortcali.api.dto.StateRequestDto;
import com.nortcali.api.dto.StateResponseDto;
import com.nortcali.api.entity.Country;
import com.nortcali.api.entity.State;
import com.nortcali.api.repository.CountryRepository;
import com.nortcali.api.repository.StateRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/states")
public class StateController {
	private final StateRepository stateRepository;
	private final CountryRepository countryRepository;
	
	public StateController(StateRepository stateRepository, CountryRepository countryRepository) {
        this.stateRepository = stateRepository;
        this.countryRepository = countryRepository;
    }
	
	/* =============================
	 * GET ALL
	 */	
	@GetMapping
    public List<StateResponseDto> getAll() {
        return stateRepository.findAll()
        		.stream()
        		.map(this::toDto)
        		.collect(Collectors.toList());
    }
	
	/*=====================
	 * GET BY ID
	 */
	@GetMapping("/{id}")
	public ResponseEntity<StateResponseDto> getById(@PathVariable Long id){
		return stateRepository.findById(id)
				.map(state -> ResponseEntity.ok(toDto(state)))
				.orElse(ResponseEntity.notFound().build());
	}

	/* =================
	 * CREATE
	 */
    @PostMapping
    public ResponseEntity<StateResponseDto> create(@RequestBody StateRequestDto dto) {
        Country country = countryRepository.findById(dto.getCountryId())
        		.orElseThrow(() -> new RuntimeException("Country not found."));
        State state = new State();
        state.setName(dto.getName());
        state.setCode(dto.getCode());
        state.setCountry(country);
    	
    	State saved = stateRepository.save(state);
    	return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }
    
    /* =========================
     * UPDATE
     */
    @PutMapping("/{id}")
    public ResponseEntity<StateResponseDto> update(@PathVariable Long id, @RequestBody StateRequestDto dto){
    	return stateRepository.findById(id)
    			.map(state -> {
    				Country country = countryRepository.findById(dto.getCountryId())
    						.orElseThrow(() -> new RuntimeException("Country not found"));
    				state.setName(dto.getName());
    				state.setCode(dto.getCode());
    				state.setCountry(country);
    				
    				State updated = stateRepository.save(state);
    				return ResponseEntity.ok(toDto(updated));
    			})
    			.orElse(ResponseEntity.notFound().build());
    }
    
    /* =========================
     * DELETE
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
    	if (!stateRepository.existsById(id)) {
    		return ResponseEntity.notFound().build();
    	}
    	stateRepository.deleteById(id);
    	return ResponseEntity.noContent().build();
    }
    
    
    /*==================
     * MAPPER
     */
    private StateResponseDto toDto(State state) {
    	return new StateResponseDto(
    			state.getId(),
    			state.getName(),
    			state.getCode(),
    			state.getCountry().getId(),
    			state.getCountry().getName()
    			);
    }
}
