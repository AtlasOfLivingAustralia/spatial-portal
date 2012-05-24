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
<jsp:include page="../common/top.jsp?fluid=true" />

<header id="page-header">
    <div class="inner">
        <nav id="breadcrumb"><ol><li><a href="http://www.ala.org.au">Home</a></li> 
                <li><a href="http://spatial.ala.org.au">Mapping &#038; analysis</a></li>
                <li><a href="../../sxs">Sites by Species list</a></li>
                <li class="last">Sites by species tabulated by ${sxs.tablename}</li></ol></nav>
        <section id="content-search">
            <h1>Spatial Web Services</h1>
        </section>
    </div><!--inner-->

</header>
<div class="inner">
    <div class="col-wide last" style="width:100%">

        <script type="text/javascript" src="/layers-service/javascript/SortingTable.js"></script>

        <div class="col-wide last" style="width:100%">
            <span class="alignleft">
                <table class="table-borders">
                    <thead>
                    <tr>
                        <th>Tables available</th>
                    </tr>
                    </thead>
                    <c:forEach items="${sxs.tables}" var="t" >
                        <c:if test="${t == sxs.tablename}" >
                        <tr><td><b>${t}</b></td>
                                <td><b>SELECTED</b></td>
                        </c:if>
                        </tr>
                        <c:if test="${t != sxs.tablename}" >
                            <tr><td><a href="../${sxs.id}/${t}">${t}</a></td>
                        </tr>
                        </c:if>
                    </c:forEach>
                </table>
            </span>
            <span class="alignright">
                <a class="button" href="${sxs.csvurl}" >Download as CSV</a>
                <a class="button" href="${sxs.jsonurl}" >Download as JSON</a>
            </span>
        </div>
        

       <table class="sortable table-borders" style="width:100%">
           <thead>
               <tr>
            <th></th>
           <c:forEach items="${sxs.table.columns}" var="cell" varStatus="rowcounter">
               <th>${cell} Species</th>
           </c:forEach>

           </tr>
           </thead>
           <c:forEach items="${sxs.table.rows}" var="row" varStatus="counter">
               <tr>
                   <td>${row.name}</td>
               <c:forEach items="${row.row}" var="cell" varStatus="rowcounter">
                   <td>${cell}</td>
               </c:forEach>
               </tr>               
           </c:forEach>
       </table>
    </div>
</div><!--inner-->

<%@include file="../common/bottom.jsp" %>
