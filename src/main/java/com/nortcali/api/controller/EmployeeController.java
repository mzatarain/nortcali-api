package com.nortcali.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import com.nortcali.api.logindto.LoginDto;
import com.nortcali.api.entity.Employees;
import com.nortcali.api.service.EmployeeService;

@RestController
@CrossOrigin("http://localhost:3000/signup")
public class EmployeeController {
	@Autowired
	private EmployeeService employeeService;
	
	@PostMapping("/addEmployee")
	public Employees addEmployee(@RequestBody Employees employees) {
		return employeeService.addEmployee(employees);
	}
	
	@PostMapping("/loginEmployee")
	public Boolean loginEmployee(@RequestBody LoginDto loginDto) {
		return employeeService.loginEmployee(loginDto);
	}
	
}
