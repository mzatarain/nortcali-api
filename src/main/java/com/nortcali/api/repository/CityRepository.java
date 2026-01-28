package com.nortcali.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.nortcali.api.entity.City;

public interface CityRepository extends JpaRepository<City, Long>{
	List<City> findByStateId(Long stateId);
}
