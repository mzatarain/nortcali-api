package com.nortcali.api.repository;

import com.nortcali.api.entity.Supply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplyRepository extends JpaRepository<Supply, Long> {

    List<Supply> findByRestaurantIdAndIsActiveTrue(Long restaurantId);

    @Query("SELECT s FROM Supply s WHERE s.restaurant.id = :restaurantId AND s.isActive = true AND s.currentStock < s.minimumStock")
    List<Supply> findLowStock(@Param("restaurantId") Long restaurantId);
}
