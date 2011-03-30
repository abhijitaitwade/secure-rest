package com.earldouglas.securerest.web;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

public class EmployeeControllerTest extends TestCase {

	@Test
	public void testHtml() throws Exception {
		String responseBody = get("jmcdoe", "jmcdoe", "text/html");

		// Make sure some HTML came back.
		assertTrue(responseBody.trim().startsWith("<table"));

		// Make sure the right attributes came back.
		assertTrue(responseBody.contains(">Id:<"));
		assertTrue(responseBody.contains(">1<"));
		assertTrue(responseBody.contains(">Name:<"));
		assertTrue(responseBody.contains(">Max Power<"));
		assertTrue(responseBody.contains(">Title:<"));
		assertTrue(responseBody.contains(">The Leader<"));
		assertTrue(responseBody.contains(">Salary:<"));
		assertFalse(responseBody.contains(">640000<"));

		responseBody = get("ntwo", "ntwo", "text/html");

		// Make sure some HTML came back.
		assertTrue(responseBody.trim().startsWith("<table"));

		// Make sure the right attributes came back.
		assertTrue(responseBody.contains(">Id:<"));
		assertTrue(responseBody.contains(">1<"));
		assertTrue(responseBody.contains(">Name:<"));
		assertTrue(responseBody.contains(">Max Power<"));
		assertTrue(responseBody.contains(">Title:<"));
		assertTrue(responseBody.contains(">The Leader<"));
		assertTrue(responseBody.contains(">Salary:<"));
		assertTrue(responseBody.contains(">640000<"));
	}

	@Test
	public void testXml() throws Exception {
		String responseBody = get("jmcdoe", "jmcdoe", "application/xml");

		// Make sure some XML came back.
		assertTrue(responseBody.startsWith("<?xml"));

		// Make sure the right attributes came back.
		assertTrue(responseBody.contains("<id>1</id>"));
		assertTrue(responseBody.contains("<name>Max Power</name>"));
		assertTrue(responseBody.contains("<title>The Leader</title>"));
		assertFalse(responseBody.contains("<salary>640000</salary>"));

		responseBody = get("ntwo", "ntwo", "application/xml");
System.out.println(responseBody);
		// Make sure some XML came back.
		assertTrue(responseBody.startsWith("<?xml"));

		// Make sure the right attributes came back.
		assertTrue(responseBody.contains("<id>1</id>"));
		assertTrue(responseBody.contains("<name>Max Power</name>"));
		assertTrue(responseBody.contains("<title>The Leader</title>"));
		assertTrue(responseBody.contains("<salary>640000</salary>"));
	}

	private String get(String username, String password, String acceptHeader)
			throws Exception {
		HttpClient httpClient = new HttpClient();

		Credentials defaultcreds = new UsernamePasswordCredentials(username,
				password);
		httpClient.getState().setCredentials(AuthScope.ANY, defaultcreds);

		HttpMethod httpMethod = new GetMethod(
				"http://localhost:8080/secure-rest/directory/employee/1");
		httpMethod.setRequestHeader("Accept", acceptHeader);
		httpClient.executeMethod(httpMethod);
		String responseBody = new String(httpMethod.getResponseBody());
		httpMethod.releaseConnection();

		return responseBody;
	}
}
