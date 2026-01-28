package com.nortcali.api.controller;

import com.nortcali.api.dto.RestaurantRequestDto;
import com.nortcali.api.dto.RestaurantResponseDto;
import com.nortcali.api.entity.City;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.repository.CityRepository;
import com.nortcali.api.repository.RestaurantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {
	private final RestaurantRepository restaurantRepository;
	private final CityRepository cityRepository;
	
	public RestaurantController(RestaurantRepository restaurantRepository, CityRepository cityRepository) {
		this.restaurantRepository = restaurantRepository;
		this.cityRepository = cityRepository;
	}
	
	/* =======================
	 * GET ALL
	 */
	@GetMapping
	public List<RestaurantResponseDto> getAll(@RequestParam(required = false) Long cityId){
		List<Restaurant> restaurants = (cityId == null)
				? restaurantRepository.findAll()
				: restaurantRepository.findByCityId(cityId);
		return restaurants.stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}
	
	/* ============================
	 * GET BY ID
	 */
	@GetMapping("/{id}")
	public ResponseEntity<RestaurantResponseDto> getById(@PathVariable Long id){
		return restaurantRepository.findById(id)
				.map(restaurant -> ResponseEntity.ok(toDto(restaurant)))
				.orElse(ResponseEntity.notFound().build());
	}
	
	/* =========================
	 * CREATE
	 */	
	@PostMapping
	public ResponseEntity<RestaurantResponseDto> create(@RequestBody RestaurantRequestDto dto) {
		City city = cityRepository.findById(dto.getCityId())
				.orElseThrow(() -> new RuntimeException("City not found."));
		Restaurant restaurant = new Restaurant();
		restaurant.setName(dto.getName());
		restaurant.setPhone(dto.getPhone());
		restaurant.setWhatsapp(dto.getWhatsapp());
		restaurant.setAddressLine(dto.getAddressLine());
		restaurant.setActive(dto.isActive());
		restaurant.setCity(city);
		
		Restaurant saved = restaurantRepository.save(restaurant);
		return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
	}
	
	/* ================
	 * UPDATE
	 */
	@PutMapping("/{id}")
	public ResponseEntity<RestaurantResponseDto> update(@PathVariable Long id, @RequestBody RestaurantRequestDto dto){
		return restaurantRepository.findById(id)
				.map(restaurant -> {
					City city = cityRepository.findById(dto.getCityId())
							.orElseThrow(() -> new RuntimeException("City not found."));
					restaurant.setName(dto.getName());
					restaurant.setPhone(dto.getPhone());
					restaurant.setWhatsapp(dto.getWhatsapp());
					restaurant.setAddressLine(dto.getAddressLine());
					restaurant.setActive(dto.isActive());
					restaurant.setCity(city);
					
					Restaurant updated = restaurantRepository.save(restaurant);
					return ResponseEntity.ok(toDto(updated));
				})
				.orElse(ResponseEntity.notFound().build());
	}
	
	/* ===============
	 * DELETE
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id){
		if (!restaurantRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		restaurantRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
	
	
	/* =========
	 * MAPPER
	 */
	private RestaurantResponseDto toDto(Restaurant restaurant) {
		return new RestaurantResponseDto(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getPhone(),
				restaurant.getWhatsapp(),
				restaurant.getAddressLine(),
				restaurant.isActive(),
				restaurant.getCity().getId(),
				restaurant.getCity().getName()
				);
	}
}
