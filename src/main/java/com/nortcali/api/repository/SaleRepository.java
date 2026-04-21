package com.nortcali.api.repository;

import com.nortcali.api.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Page<Sale> findByRestaurantIdAndIsActiveTrueOrderBySaleDateDesc(Long restaurantId, Pageable pageable);

    @Query("SELECT ss.name, COUNT(s.id), SUM(s.total) FROM Sale s JOIN s.source ss WHERE s.restaurant.id = :rid AND s.isActive = true GROUP BY ss.name")
    List<Object[]> findSalesBySource(@Param("rid") Long restaurantId);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.restaurant.id = :rid AND s.saleDate BETWEEN :from AND :to AND s.isActive = true")
    BigDecimal sumByRestaurantAndPeriod(@Param("rid") Long restaurantId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.commission), 0) FROM Sale s WHERE s.restaurant.id = :rid AND s.saleDate BETWEEN :from AND :to AND s.isActive = true")
    BigDecimal sumCommissionByRestaurantAndPeriod(@Param("rid") Long restaurantId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.cashSession.id = :sessionId AND s.isActive = true")
    BigDecimal sumTotalByCashSessionId(@Param("sessionId") Long cashSessionId);
}
