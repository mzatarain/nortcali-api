package com.nortcali.api.repository;

import com.nortcali.api.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByNameIgnoreCase(String name);

    List<Unit> findByTenantId(Long tenantId);

    Optional<Unit> findByNameIgnoreCaseAndTenantId(String name, Long tenantId);
}
