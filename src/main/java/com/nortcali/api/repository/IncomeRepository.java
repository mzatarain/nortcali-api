package com.nortcali.api.repository;

import com.nortcali.api.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByRestaurantIdAndIsActiveTrue(Long restaurantId);

    Page<Income> findByRestaurantIdAndIsActiveTrueOrderByIncomeDateDesc(Long restaurantId, Pageable pageable);

    List<Income> findByRestaurantIdAndIncomeDateBetweenAndIsActiveTrue(
            Long restaurantId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.restaurant.id = :rid AND i.incomeDate BETWEEN :from AND :to AND i.isActive = true")
    BigDecimal sumByRestaurantAndPeriod(@Param("rid") Long restaurantId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);
}
