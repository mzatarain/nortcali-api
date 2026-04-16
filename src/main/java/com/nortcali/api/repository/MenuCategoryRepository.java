package com.nortcali.api.repository;

import com.nortcali.api.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    @Query("SELECT mc FROM MenuCategory mc WHERE mc.restaurant.id = :restaurantId AND mc.isActive = true ORDER BY mc.displayOrder")
    List<MenuCategory> findActiveByRestaurant(@Param("restaurantId") Long restaurantId);

    boolean existsByRestaurantIdAndNameAndIsActiveTrue(Long restaurantId, String name);
}
