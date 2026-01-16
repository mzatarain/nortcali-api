package com.nortcali.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nortcali.api.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>{

}
