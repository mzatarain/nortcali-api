package com.nortcali.api.controller;

import com.nortcali.api.dto.CountryRequestDto;
import com.nortcali.api.dto.CountryResponseDto;
import com.nortcali.api.entity.Country;
import com.nortcali.api.repository.CountryRepository;

//import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryRepository countryRepository;

    public CountryController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    
    /* =========================
    GET ALL
    ========================= */
    @GetMapping
    public List<CountryResponseDto> getAll() {
    	return countryRepository.findAll()
    			.stream()
    			.map(this::toDto)
    			.collect(Collectors.toList());
    }

    /* =========================
    GET BY ID
    ========================= */
    @GetMapping("/{id}")
    public ResponseEntity<CountryResponseDto> getById(@PathVariable Long id) {
    	return countryRepository.findById(id)
    			.map(country -> ResponseEntity.ok(toDto(country)))
    			.orElse(ResponseEntity.notFound().build());
    }

    /* =========================
    CREATE
    ========================= */

    @PostMapping
    public Country create(@RequestBody Country c) {
    	return countryRepository.save(c);
    }

    /* =========================
    UPDATE
    ========================= */
    @PutMapping("/{id}")
    public ResponseEntity<CountryResponseDto> update(
    		@PathVariable Long id,
    		@RequestBody CountryRequestDto dto
    		) {
    	return countryRepository.findById(id)
    			.map(country -> {
    				country.setName(dto.getName());
    				country.setIsoCode(dto.getIsoCode());
    				Country updated = countryRepository.save(country);
    				return ResponseEntity.ok(toDto(updated));
    			})
    			.orElse(ResponseEntity.notFound().build());
    }

    /* =========================
    DELETE
    ========================= */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
    	if (!countryRepository.existsById(id)) {
    		return ResponseEntity.notFound().build();
    	}
    	countryRepository.deleteById(id);
    	return ResponseEntity.noContent().build();
    }

    /* =========================
    MAPPER
    ========================= */
    private CountryResponseDto toDto(Country country) {
    	return new CountryResponseDto(
    			country.getId(),
    			country.getName(),
    			country.getIsoCode()
    			);
    }
}