package com.nortcali.api.repository;

import com.nortcali.api.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantIdAndIsActiveTrue(Long restaurantId);

    List<MenuItem> findByCategoryIdAndIsActiveTrue(Long categoryId);
}
