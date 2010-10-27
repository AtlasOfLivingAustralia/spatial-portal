<%-- 
    Document   : citation
    Created on : Oct 27, 2010, 2:00:08 PM
    Author     : gav
--%>

<%@page contentType="text/plain" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

Resource name\tCitation\tRights\tMore information\n
<c:forEach items="${layerList}" var="layer" varStatus="status">
    <c:choose>
        <c:when test="${layer != null}">
${layer.description}\t
Accessed via the ALA website.\t
${layer.license_notes}\t
For more information: http://spatial.ala.org.au/alaspatial/layers/{layer.uid}\n
        </c:when>
        <c:otherwise>
            Layer citation data is not available
        </c:otherwise>
    </c:choose>
</c:forEach>
