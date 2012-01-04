<%-- 
    Document   : list
    Created on : Sep 26, 2011, 9:52:39 PM
    Author     : ajay
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
       <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">       
       <title>${title}</title>
    </head>
    <body>
       <basefont size="2" >
       <h3>${title}</h3><br>
            
       <table border='1' align="right">
           <c:forEach items="${grid}" var="row" varStatus="counter">
               <tr>
               <c:forEach items="${row}" var="cell" varStatus="rowcounter">
                   <c:if test="${rowcounter.index == 0 || counter.index == 0}">
                    <c:choose>
                       <c:when test="${rowcounter.index ==0 && counter.index == 0}">
                           <c:set var="rowClass" value="${tabulationDescription}"/>
                            <td><b><c:out value="${rowClass}"/></b></td>
                       </c:when>
                       <c:otherwise>
                           <c:set var="rowClass" value="${cell}"/>
                           <td><c:out value="${rowClass}"/></td>
                       </c:otherwise>
                   </c:choose>
                   </c:if>
                   <c:if test="${rowcounter.index !=0 && counter.index != 0}">
                       <c:choose>
                           <c:when test="${cell != null && id == 1}">
                            <c:set var="rowClass" value="${cell / 1000000.00}"/>
                            <td align="right"><fmt:formatNumber value="${rowClass}" pattern="#0"/></td>
                           </c:when>
                            <c:when test="${cell != null && id == 2}">
                            <c:set var="rowClass" value="${cell}"/>
                            <td align="right"><fmt:formatNumber value="${rowClass}" pattern="#0"/></td>
                           </c:when>
                           <c:when test="${cell != null && id >= 3}">
                            <c:set var="rowClass" value="${gridpercentage[counter.index-1][rowcounter.index-1]}"/>
                            <td align="right"><fmt:formatNumber value="${rowClass/100.0}" pattern="#0.0%"/></td>
                           </c:when>
                           <c:otherwise>
                               <td></td>
                           </c:otherwise>    
                       </c:choose>
                   </c:if>
               </c:forEach>
                   <c:choose>
                   <c:when test="${counter.index == 0}">
                       <td><b><c:out value="${tabulationRowMargin}"/></b></td>    
                   </c:when>
                   <c:when test="${counter.index != 0 && id<=2}">
                       <c:set var="element" value="${sumofcolumns[counter.index-1]}"/>
                       <td align="right"><fmt:formatNumber value="${element}" pattern="#0"/></td>
                   </c:when>
                   <c:when test="${counter.index != 0 && id >3}">
                       <c:set var="element" value="${sumofcolumnsgridpercentage[counter.index-1]}"/>
                       <td align="right"><fmt:formatNumber value="${element/100.0}" pattern="#0.0%"/></td>
                   </c:when>
                   <c:when test="${counter.index != 0 && id ==3}">
                       <c:set var="element" value="${AveragePercentageOverColumns[counter.index-1]}"/>
                       <td align="right"><fmt:formatNumber value="${element/100.0}" pattern="#0.0%"/></td>
                   </c:when>
                   <c:otherwise>
                       <td></td>
                   </c:otherwise>    
                   </c:choose>
               </tr>               
           </c:forEach>
           <tr>
               <td><b><c:out value="${tabulationColumnMargin}"/></b></td>
               <c:forEach items="${sumofrows}" var="element" varStatus="elementcounter">
                   <c:choose>
                   <c:when test="${id<=2}">
                       <c:set var="element" value="${sumofrows[elementcounter.index]}"/>
                       <td align="right"><fmt:formatNumber value="${element}" pattern="#0"/></td>
                   </c:when>
                   <c:when test="${id ==4}">
                       <c:set var="element" value="${AveragePercentageOverRows[elementcounter.index]}"/>
                       <td align="right"><fmt:formatNumber value="${element/100.0}" pattern="#0.0%"/></td>
                   </c:when>
                   <c:when test="${id ==3 || id==5}">
                       <c:set var="element" value="${sumofrowsgridpercentage[elementcounter.index]}"/>
                       <td align="right"><fmt:formatNumber value="${element/100.0}" pattern="#0.0%"/></td>
                   </c:when>    
                   <c:otherwise>
                       <td></td>
                   </c:otherwise>    
                   </c:choose>
                   
               </c:forEach>
               <c:choose>
               <c:when test="${id==1 || id==2}">   
                   <td align="right"><fmt:formatNumber value="${total}" pattern="#0"/></td>
               </c:when>
               <c:when test="${id==5}">   
                   <td align="right"><fmt:formatNumber value="${totalpercentage/100.0}" pattern="#0.0%"/></td>
               </c:when>
               <c:otherwise>
                   <td></td>
               </c:otherwise>        
               </c:choose>
                   
           </tr>
       </table>
       <p>Blanks = no intersection</p>
       <p>0 = no records in intersection</p>
       </body>
</html>
