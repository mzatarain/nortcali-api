package com.nortcali.api.repository;

import com.nortcali.api.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, Long> {
    List<ModifierGroup> findByRestaurantId(Long restaurantId);
}
