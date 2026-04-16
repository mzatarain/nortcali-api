package com.nortcali.api.repository;

import com.nortcali.api.entity.CashSession;
import com.nortcali.api.entity.enums.CashSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

    Optional<CashSession> findByRestaurantIdAndStatus(Long restaurantId, CashSessionStatus status);

    boolean existsByRestaurantIdAndStatus(Long restaurantId, CashSessionStatus status);
}
