package com.earldouglas.directory.service;

import org.springframework.stereotype.Component;

import com.earldouglas.directory.Employee;

@Component
public interface EmployeeService {

	public Employee get(String id, String role);
}
