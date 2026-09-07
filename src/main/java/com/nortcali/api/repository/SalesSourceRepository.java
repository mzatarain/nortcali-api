package com.nortcali.api.repository;

import com.nortcali.api.entity.SalesSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesSourceRepository extends JpaRepository<SalesSource, Long> {

    List<SalesSource> findByIsActiveTrue();

    List<SalesSource> findByTenantIdAndIsActiveTrue(Long tenantId);

    Optional<SalesSource> findByNameIgnoreCase(String name);

    Optional<SalesSource> findByNameIgnoreCaseAndTenantId(String name, Long tenantId);

    Optional<SalesSource> findByNameIgnoreCaseAndIsActiveTrue(String name);

    Optional<SalesSource> findByNameIgnoreCaseAndIsActiveTrueAndTenantId(String name, Long tenantId);

    Optional<SalesSource> findFirstByIsActiveTrueOrderByIdAsc();

    Optional<SalesSource> findFirstByTenantIdAndIsActiveTrueOrderByIdAsc(Long tenantId);
}
