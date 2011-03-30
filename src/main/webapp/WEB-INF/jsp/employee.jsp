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
