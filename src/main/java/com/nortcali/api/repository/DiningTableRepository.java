package com.nortcali.api.repository;

import com.nortcali.api.entity.DiningTable;
import com.nortcali.api.entity.enums.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findByRestaurantIdAndIsActiveTrue(Long restaurantId);

    List<DiningTable> findByRestaurantIdAndIsActiveTrueAndStatus(Long restaurantId, TableStatus status);
}
