package com.nortcali.api.repository;

import com.nortcali.api.entity.Order;
import com.nortcali.api.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);

    Page<Order> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, OrderStatus status, Pageable pageable);

    List<Order> findByRestaurantIdAndStatus(Long restaurantId, OrderStatus status);

    @Query("SELECT COALESCE(COUNT(o), 0) FROM Order o WHERE o.restaurant.id = :restaurantId AND CAST(o.createdAt AS date) = :date")
    long countByRestaurantAndDate(@Param("restaurantId") Long restaurantId, @Param("date") LocalDate date);
}
