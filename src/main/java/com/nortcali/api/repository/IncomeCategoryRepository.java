package com.nortcali.api.repository;

import com.nortcali.api.entity.IncomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Long> {

    List<IncomeCategory> findByRestaurantIdAndIsActiveTrue(Long restaurantId);
}
