package com.nortcali.api.repository;

import com.nortcali.api.entity.SalesSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesSourceRepository extends JpaRepository<SalesSource, Long> {

    List<SalesSource> findByIsActiveTrue();

    Optional<SalesSource> findByNameIgnoreCase(String name);

    Optional<SalesSource> findByNameIgnoreCaseAndIsActiveTrue(String name);

    Optional<SalesSource> findFirstByIsActiveTrueOrderByIdAsc();
}
