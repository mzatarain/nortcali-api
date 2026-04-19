package com.nortcali.api.repository;

import com.nortcali.api.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByRestaurantId(Long restaurantId);
}
