package com.nortcali.api.repository;

import com.nortcali.api.entity.Order;
import com.nortcali.api.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);

    Page<Order> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, OrderStatus status, Pageable pageable);

    Page<Order> findByRestaurantIdAndStatusInOrderByCreatedAtDesc(Long restaurantId, List<OrderStatus> statuses, Pageable pageable);

    List<Order> findByRestaurantIdAndStatus(Long restaurantId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :r AND o.createdAt >= :start AND o.createdAt < :end ORDER BY o.createdAt DESC")
    Page<Order> findByRestaurantAndDate(@Param("r") Long restaurantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :r AND o.status = :status AND o.createdAt >= :start AND o.createdAt < :end ORDER BY o.createdAt DESC")
    Page<Order> findByRestaurantAndStatusAndDate(@Param("r") Long restaurantId, @Param("status") OrderStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :r AND o.status IN :statuses AND o.createdAt >= :start AND o.createdAt < :end ORDER BY o.createdAt DESC")
    Page<Order> findByRestaurantAndStatusInAndDate(@Param("r") Long restaurantId, @Param("statuses") List<OrderStatus> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    // MAX sobre el número de secuencia del folio para que hard-deletes no rompan la secuencia
    @Query(value = "SELECT MAX(CAST(SUBSTRING_INDEX(folio, '-', -1) AS UNSIGNED)) " +
                   "FROM orders WHERE restaurant_id = :restaurantId AND folio LIKE :folioPrefix",
           nativeQuery = true)
    Integer findMaxSequenceByFolioPrefix(@Param("restaurantId") Long restaurantId, @Param("folioPrefix") String folioPrefix);

    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :r AND o.status IN :statuses AND o.createdAt >= :start AND o.createdAt < :end")
    List<Order> findActiveOrdersForDay(@Param("r") Long restaurantId,
                                       @Param("statuses") List<OrderStatus> statuses,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
