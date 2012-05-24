<%-- 
    Document   : list
    Created on : Sep 26, 2011, 9:52:39 PM
    Author     : ajay
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" %><%@
taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><%@
taglib uri="/tld/ala.tld" prefix="ala" %>
<%-- <%@include file="../common/top.jsp" %> --%>
<jsp:include page="../common/top.jsp?fluid=true" />
<header id="page-header">
    <div class="inner">
        <nav id="breadcrumb"><ol><li><a href="http://www.ala.org.au">Home</a></li> <li><a href="http://spatial.ala.org.au">Mapping &#038; analysis</a></li> <li class="last">Spatial Web Services</li></ol></nav>
        <section id="content-search">
            <h1>Spatial Web Services</h1>
            <p>Following are the available tabulation layers</p>            
        </section>
    </div><!--inner-->

</header>
<div class="inner">
    <div class="col-wide last" style="width:100%">

        <table class="sortable table-borders" style="width:100%">
            <thead>
            <tr><th>Add another sites by species tabulated</th></tr></thead>
            <tr><td>
            <form nmethod="get" action="sxs/add" target="self">
                input species <input name="speciesquery" value="tasmanian%20devil"/>
                &nbsp;layers <input name="layers" value="aus1" />
                &nbsp;grid size <input name="gridsize" value="0.01" />
                <input name="bs" value="http%3A%2F%2Fbiocache.ala.org.au%2Fws" style="display:none"/>
                <input type="submit" value="Add" />
                <br>
            </form>
                </td></tr>
        </table>

        <table class="sortable table-borders" style="width:100%">
            <thead>
            <tr>
                <th style="-moz-user-select: none;" class="sortable-keep fd-column-0"><a title="Sort on Field" href="#">Query</a></th>
                <th style="-moz-user-select: none;" class="sortable-numeric fd-column-1"><a title="Sort on Field" href="#">Analysis ID</a></th>
                <th style="-moz-user-select: none;" class="sortable-numeric fd-column-1"><a title="Sort on Field" href="#">Status</a></th>
            </tr>
            </thead>
            <tbody>
        <c:forEach items="${sxs}" var="layer">
            <tr>
                <td>${layer.value}</td>
                <c:if test="${layer.status == 'SUCCESSFUL'}" >
                    <td><a href="sxs/${layer.analysisId}">${layer.analysisId}</a></td>
                </c:if>
                <c:if test="${layer.status != 'SUCCESSFUL'}" >
                    <td><a href="ws/job?pid=${layer.analysisId}">${layer.analysisId}</a></td>
                </c:if>
                <td>${layer.status}</td>
            </tr>
        </c:forEach>
            </tbody>
        </table>

        <script type="text/javascript" src="/layers-service/javascript/SortingTable.js"></script>


    </div>
</div><!--inner-->

<%@include file="../common/bottom.jsp" %>
