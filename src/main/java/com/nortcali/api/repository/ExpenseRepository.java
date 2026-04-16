package com.nortcali.api.repository;

import com.nortcali.api.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByRestaurantIdAndIsActiveTrue(Long restaurantId);

    Page<Expense> findByRestaurantIdAndIsActiveTrueOrderByExpenseDateDesc(Long restaurantId, Pageable pageable);

    List<Expense> findByRestaurantIdAndExpenseDateBetweenAndIsActiveTrue(
            Long restaurantId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.restaurant.id = :rid AND e.expenseDate BETWEEN :from AND :to AND e.isActive = true")
    BigDecimal sumByRestaurantAndPeriod(@Param("rid") Long restaurantId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);
}
