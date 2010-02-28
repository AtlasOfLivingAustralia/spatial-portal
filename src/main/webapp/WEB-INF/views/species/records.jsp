<%@page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@page trimDirectiveWhitespaces="false"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<c:choose>
<c:when test="${fn:length(spList) > 0}">
            <c:forEach items="${spList}" var="sp" varStatus="status">
                <li>${sp.id} - ${sp.scientificname} at ${sp.longitude}, ${sp.latitude}</li>
            </c:forEach>
</c:when>
    <c:otherwise>
        <li>No species records available</li>
    </c:otherwise>
</c:choose>