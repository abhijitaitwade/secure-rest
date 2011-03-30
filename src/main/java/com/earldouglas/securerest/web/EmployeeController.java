package com.earldouglas.securerest.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.wrapper.SavedRequestAwareWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.earldouglas.directory.service.EmployeeService;

@Controller
public class EmployeeController {

	@Autowired
	private EmployeeService employeeService;

	@RequestMapping(value = "/employee/{id}", method = RequestMethod.GET)
	public ModelAndView getHrEmployee(@PathVariable String id,
			SavedRequestAwareWrapper savedRequestAwareWrapper) {

		String role = null;

		if (savedRequestAwareWrapper.isUserInRole("ROLE_HR")) {
			role = "HR";
		}
		else if (savedRequestAwareWrapper.isUserInRole("ROLE_EMPLOYEE")) {
			role = "EMPLOYEE";
		}

		return new ModelAndView("employee").addObject("employee",
				employeeService.get(id, role));
	}
}
