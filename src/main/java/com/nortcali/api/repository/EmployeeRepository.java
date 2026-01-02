package com.nortcali.api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nortcali.api.entity.Employees;

@Repository
public interface EmployeeRepository extends JpaRepository<Employees,Long> {
	Optional<Employees> findByEmail(String email);
}
