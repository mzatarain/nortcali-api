package com.nortcali.api.repository;

import com.nortcali.api.entity.FinancialPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, Long> {

    List<FinancialPeriod> findByRestaurantIdOrderByStartDateDesc(Long restaurantId);
}
