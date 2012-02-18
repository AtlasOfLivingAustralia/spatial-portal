<%-- 
    Document   : list
    Created on : Sep 26, 2011, 9:52:39 PM
    Author     : ajay
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" %><%@
taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><%@
taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%><%@
taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt" %><%@
taglib uri="/tld/ala.tld" prefix="ala" %>
<%-- <%@include file="../common/top.jsp" %> --%>
<jsp:include page="../common/top.jsp?fluid=true" />
<header id="page-header">
    <div class="inner">
        <nav id="breadcrumb"><ol><li><a href="http://www.ala.org.au">Home</a></li> <li><a href="http://spatial.ala.org.au">Mapping &#038; analysis</a></li> <li class="last">Spatial Web Services</li></ol></nav>
        <section id="content-search">
            <h1>Spatial Web Services</h1>
            <p>${title} for ${tabulationDescription}</p>
        </section>
    </div><!--inner-->

</header>
<div class="inner">
    <div class="col-wide last" style="width:100%">

        <script type="text/javascript" src="/layers-service/javascript/SortingTable.js"></script>
            
       <table class="table-borders" style="width:100%">
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
       <p>
           Blanks = no intersection
           <br />
           0 = no records in intersection
       </p>

    </div>
</div><!--inner-->

<%@include file="../common/bottom.jsp" %>
