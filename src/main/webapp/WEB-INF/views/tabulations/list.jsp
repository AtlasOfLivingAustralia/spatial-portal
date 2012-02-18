<%-- 
    Document   : list
    Created on : Sep 26, 2011, 9:52:39 PM
    Author     : ajay
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" %><%@
taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><%@
taglib uri="/tld/ala.tld" prefix="ala" %>
<%@include file="../common/top.jsp" %>
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
            <tr>
                <th style="-moz-user-select: none;" class="sortable-keep fd-column-0"><a title="Sort on Field" href="#">Contextual layer 1</th>
                <th style="-moz-user-select: none;" class="sortable-numeric fd-column-1"><a title="Sort on Field" href="#">Contextual layer 2</th>
                <th>Area (sqkm) </th>
                <th>Row %</th>
                <th>Column %</th>
                <!--<th>Link</th>-->
            </tr>
        <c:forEach items="${tabulations}" var="layer" varStatus="status">
            <tr>
                <td>${layer.name1}</td>
                <td>${layer.name2}</td>
                <td>
                    <a href='tabulation/area/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/area/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/area/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/area/row/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/area/row/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/area/row/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/area/column/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/area/column/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/area/column/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <!--<td><a href="../tabulation/${layer.fid1}/${layer.fid2}/html">table</a></td>-->
            </tr>
        </c:forEach>
        </table>

        <script type="text/javascript" src="/layers-service/javascript/SortingTable.js"></script>


    </div>
</div><!--inner-->

<%@include file="../common/bottom.jsp" %>
