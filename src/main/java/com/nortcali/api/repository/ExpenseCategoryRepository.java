package com.nortcali.api.repository;

import com.nortcali.api.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findByRestaurantId(Long restaurantId);
}
