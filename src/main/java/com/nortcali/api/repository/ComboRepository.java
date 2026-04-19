package com.nortcali.api.repository;

import com.nortcali.api.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByRestaurantId(Long restaurantId);
}
