# A Secure RESTful Web Service

_27 Sep 2009_

REST-style architecture lends a comfortable aspect of familiarity to web services by enforcing a somewhat strict architectural style with which we have become accustomed to in our daily use of the web. It eliminates the unpredictable and sometimes obtuse web services definitions created in analogy to arbitrary verbs. It limits the types of actions taken by a web service to those of CRUD, and the resources on which to perform such actions to those identifiable by URLs.

Role-based security in web services is often overlooked as an architectural consideration, but with a REST-style architecture it follows as logically as in any web application. A web service consumer need not be a person at a terminal, but is any program, server, system, or other entity which interacts with the web service. These entities may be assigned roles analogous to those of web application users, which can be used to grant or limit access to capabilities of web services.

This example will build a relatively simple contract-first web service in a REST-style architecture, and implement role-based access control within both the application tier and the presentation tier, uniting them to secure access to the web service.

## Procedure

* Loosely define an example message from the web service
* Loosely define a data contract from the example message
* Tweak and finalize the data contract
* Generate server-side domain objects based on the data contract
* Build application-layer logic for retrieving data based on role access
* Build the web-layer controller
* Define the web-layer view resolution behavior
* Build the web-layer views
* Test the web service

## Web application file structure

```
WEB-INF/
  directory-data.xml
  directory-security.xml
  directory-servlet.xml
  web.xml

WEB-INF/jsp/
  employee.jsp

WEB-INF/lib/
  aopalliance.jar
  aspectjrt.jar
  cglib-nodep-2.1_3.jar
  commons-codec.jar
  commons-logging.jar
  hsqldb.jar
  log4j-1.2.15.jar
  org.springframework.aop-3.0.0.M4.jar
  org.springframework.asm-3.0.0.M4.jar
  org.springframework.beans-3.0.0.M4.jar
  org.springframework.context-3.0.0.M4.jar
  org.springframework.core-3.0.0.M4.jar
  org.springframework.expression-3.0.0.M4.jar
  org.springframework.oxm-3.0.0.M4.jar
  org.springframework.transaction-3.0.0.M4.jar
  org.springframework.web-3.0.0.M4.jar
  org.springframework.web.servlet-3.0.0.M4.jar
  spring-security-core-2.0.5.RELEASE.jar
  spring-security-core-tiger-2.0.5.RELEASE.jar
  spring-security-taglibs-2.0.5.RELEASE.jar
  standard.jar
```

## Source code file structure

```
src/com/earldouglas/directory/
  Employee.java
  ObjectFactory.java
  package-info.java

src/com/earldouglas/directory/service/
  EmployeeService.java

src/com/earldouglas/securerest/web/
  EmployeeController.java

test/
  log4j.properties

test/com/earldouglas/securerest/web/
  EmployeeControllerTest.java
```

## Step 1) Loosely define an example message from the web service.

As discussed in [A Contract-First Web Service with Spring WS](https://github.com/JamesEarlDouglas/contract-first-spring-ws), to get started with a contract-first web service, the data contract must be defined. This can be done by first writing a sample message in the format desired from the web service.

_employee.xml:_

```xml
<employee xmlns="http://earldouglas.com/schema/directory">
  <id>3</id>
  <name>Johnny McDoe</name>
  <title>Work Man</title>
  <salary>1234.56</salary>
</employee>
```

## Step 2) Loosely define a data contract from the example message.

The data contract is referse-engineered from the sample message using a utility such as Trang:

```bash
java -jar trang.jar employee.xml employee.xsd
```

## Step 3) Tweak and finalize the data contract.

The resulting generated schema isn't exactly as desired, and requires a bit of by-hand tweaking before it is finalized.

_employee.xsd:_

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
  elementFormDefault="qualified" targetNamespace="http://earldouglas.com/schema/directory"
  xmlns:directory="http://earldouglas.com/schema/directory">
  <xs:element name="employee">
    <xs:complexType>
      <xs:sequence minOccurs="1" maxOccurs="1">
        <xs:element name="id" type="xs:integer" />
        <xs:element name="name" type="xs:string" />
        <xs:element name="title" type="xs:string" />
        <xs:element name="salary" type="xs:decimal" />
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>
```

## Step 4) Generate server-side domain objects based on the data contract.

Server-side domain objects are generated using JAXB 2.0 to reverse engineer Java code from the XML schema.

```bash
xjc.sh -p com.earldouglas.directory directory.xsd 
```

Use of the `-p` option specifies that the reverse-engineered code shall be placed in the specified package.

The result is the `Employee` class, with supporting JAXB 2.0 infrastructure.

```
src/com/earldouglas/directory/
  Employee.java
  ObjectFactory.java
  package-info.java
```

The `Employee` class is a value object implementing the following methods:

```java
public BigInteger getId();
public void setId(BigInteger value);

public String getName();
public void setName(String value);

public String getTitle();
public void setTitle(String value);

public BigDecimal getSalary();
public void setSalary(BigDecimal value);
```

## Step 5) Build application-layer logic for retrieving data based on role access.

`EmployeeService` represents a simple data access object which in realistic use would integrate with a database. It implements basic role-based access control on `Employee` instances, limiting access to them based on two defined roles: `EMPLOYEE` and `HR`. Entities with the `EMPLOYEE` role are permitted to access an `Employee`'s id, name, and title data, while entities with the HR are also permitted to access an `Employee`'s salary data.

```java
@Component
public class EmployeeService {

  @Resource(name = "employees")
  private Map<String, Employee> employees;

  public void setEmployees(Map<String, Employee> employees) {
    this.employees = employees;
  }

  public Employee get(String id, String role) {
    Employee employee = new Employee();

    if ("EMPLOYEE".equals(role) || "HR".equals(role)) {
      employee.setId(employees.get(id).getId());
      employee.setName(employees.get(id).getName());
      employee.setTitle(employees.get(id).getTitle());
    }

    if ("HR".equals(role)) {
      employee.setSalary(employees.get(id).getSalary());
    }

    return employee;
  }
}
```

## Step 6) Build the web-layer controller.

A single controller handles web service requests, determining the consumer's role as provided by Spring Security's `SavedRequestAwareWrapper` and delegating to the `EmployeeService` object.

```java
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
    } else if (savedRequestAwareWrapper.isUserInRole("ROLE_EMPLOYEE")) {
      role = "EMPLOYEE";
    }

    return new ModelAndView("employee").addObject("employee",
        employeeService.get(id, role));
  }
}
```

## Step 7) Define the web-layer view resolution behavior.

A useful capability of Spring MVC is to resolve views based on the `Accept` header within the client's request. With a web service, it is most common that a consumer would expect to receive XML in the response, however this is not always the case. If the web service is consumed within the context of an outside HTML view, or if the consumer is simply accessing the web service from a web browser, it is more appropriate to return HTML in the response.

Using Spring's `ContentNegotiatingViewResolver`, multiple view resolvers can be defined, each able to generate views of different content types.

```xml
<!--
  Select an appropriate View to handle the request by comparing the
  request media type(s) with the media type supported by the View.
-->
<bean
  class="org.springframework.web.servlet.view.ContentNegotiatingViewResolver">
  <property name="viewResolvers">
    <list>
      <bean class="org.springframework.web.servlet.view.BeanNameViewResolver" />
      <bean
        class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/jsp/" />
        <property name="suffix" value=".jsp" />
      </bean>
    </list>
  </property>
</bean>

<!-- Configure a org.springframework.oxm.jaxb.Jaxb2Marshaller. -->
<oxm:jaxb2-marshaller id="marshaller"
  contextPath="com.earldouglas.directory" />

<!-- Provide the employee XML view. -->
<bean name="employee"
  class="org.springframework.web.servlet.view.xml.MarshallingView">
  <constructor-arg ref="marshaller" />
</bean>
```

This instance of `ContentNegotiatingViewResolver` will use JAXB 2.0 to marshall `Employee` objects as XML for web service requests that accept `application/xml`, and will use a JSP file to render views for web service requests that accept `text/html`.

## Step 8) Build the web-layer views.

JAXB 2.0's marshaller handles XML views of web service responses, and a simple JSP file handles HTML views of web service responses.

_employee.jsp:_

```xml
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<table>
  <tr>
    <th align="right">Id:</th>
    <td><c:out value="${employee.id}" /></td>
  </tr>
  <tr>
    <th align="right">Name:</th>
    <td><c:out value="${employee.name}" /></td>
  </tr>
  <tr>
    <th align="right">Title:</th>
    <td><c:out value="${employee.title}" /></td>
  </tr>
  <tr>
    <th align="right">Salary:</th>
    <td><c:out value="${employee.salary}" /></td>
  </tr>
</table>
```

## Step 9) Test the web service.

The web service is expected to generate HTML output such as the following:

```xml
<table>
  <tr>
    <th align="right">Id:</th>
    <td>1</td>
  </tr>
  <tr>
    <th align="right">Name:</th>
    <td>Max Power</td>
  </tr>
  <tr>
    <th align="right">Title:</th>
    <td>The Leader</td>
  </tr>
  <tr>
    <th align="right">Salary:</th>
    <td>640000</td>
  </tr>
</table>
```

The web service is expected to generate XML output such as the following:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<employee xmlns="http://earldouglas.com/schema/directory">
  <id>1</id>
  <name>Max Power</name>
  <title>The Leader</title>
  <salary>640000</salary>
</employee>
```

Testing the web service requires asserting the following:

* The web service returns HTML for requests that accept `text/html`
* The web service returns XML for requests that accept `application/xml`
* The web service returns data appropriate for each role

These are all captured within a single JUnit test case.

```java
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
        "http://localhost:8080/securerest/directory/employee/1");
    httpMethod.setRequestHeader("Accept", acceptHeader);
    httpClient.executeMethod(httpMethod);
    String responseBody = new String(httpMethod.getResponseBody());
    httpMethod.releaseConnection();

    return responseBody;
  }
}
```

