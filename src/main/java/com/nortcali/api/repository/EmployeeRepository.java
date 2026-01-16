package com.nortcali.api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nortcali.api.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee,Long> {
	Optional<Employee> findByUsername(String username);
}
