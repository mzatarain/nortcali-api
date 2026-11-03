package com.nortcali.api.repository;

import com.nortcali.api.entity.LegalAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptance, Long> {

    List<LegalAcceptance> findByTenantIdOrderByAcceptedAtDesc(Long tenantId);
}
