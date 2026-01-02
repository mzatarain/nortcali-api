package com.nortcali.api.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nortcali.api.logindto.LoginDto;
import com.nortcali.api.entity.Employees;
import com.nortcali.api.repository.EmployeeRepository;

@Service
public class EmployeeService {
	@Autowired
	EmployeeRepository employeeRepository;

	public Boolean loginEmployee(LoginDto loginDto) {
		Optional<Employees> employee=employeeRepository.findByEmail(loginDto.getEmail());
		if(employee==null) {
			return false;
		}
		Employees employee1=employee.get();
		if(!employee1.getPassword().equals(loginDto.getPassword())) {
			return false;
		}
		return true;
	}
	
	public Employees addEmployee(Employees employees) {
		return employeeRepository.save(employees);
	}
}
