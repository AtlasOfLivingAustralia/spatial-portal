<%-- 
    Document   : citation
    Created on : Oct 27, 2010, 2:00:08 PM
    Author     : gav
--%>

<%@page contentType="text/plain" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<% out.print("Name\tDescription\tDource\tSource information\tLicence\tLicence information\n"); %>

<c:forEach items="${layerList}" var="layer" varStatus="status">
    <c:choose>
        <c:when test="${layer != null}">
${layer.name}<%out.print("\t");%>${layer.name}<%out.print("\t");%>${layer.description}<%out.print("\t");%>${source}<%out.print("\t");%>${source_link}<%out.print("\t");%>${license_notes}<%out.print("\t");%>${license_link}<%out.print("\n");%>
        </c:when>
        <c:otherwise>
            Layer citation data is not available
        </c:otherwise>
    </c:choose>
</c:forEach>
