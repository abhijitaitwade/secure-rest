package com.earldouglas.directory.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.earldouglas.directory.Employee;

@Component
public class DefaultEmployeeService implements EmployeeService {

	private Map<String, Employee> employees;

	public void setEmployees(Map<String, Employee> employees) {
		this.employees = employees;
	}

	public Employee get(String id, String role) {
		Employee employee = employees.get(id);
		Employee returnEmployee = new Employee();

		if ("EMPLOYEE".equals(role) || "HR".equals(role)) {
			returnEmployee.setId(employee.getId());
			returnEmployee.setName(employee.getName());
			returnEmployee.setTitle(employee.getTitle());
		}

		if ("HR".equals(role)) {
			returnEmployee.setSalary(employee.getSalary());
		}

		return returnEmployee;
	}
}
