package com.nortcali.api.repository;

import com.nortcali.api.entity.DeliveryDriver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryDriverRepository extends JpaRepository<DeliveryDriver, Long> {

    List<DeliveryDriver> findByRestaurantIdAndIsActiveTrue(Long restaurantId);
}
