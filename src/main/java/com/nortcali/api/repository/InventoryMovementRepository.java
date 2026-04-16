package com.nortcali.api.repository;

import com.nortcali.api.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findBySupplyIdOrderByCreatedAtDesc(Long supplyId);
}
