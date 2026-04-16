package com.nortcali.api.repository;

import com.nortcali.api.entity.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, Long> {

    List<EmployeeRole> findByIsActiveTrueOrderByNameAsc();

    Optional<EmployeeRole> findByNameIgnoreCase(String name);
}
