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
            <tr>
                <th rowspan="2" style="-moz-user-select: none;" class="sortable-keep fd-column-0"><a title="Sort on Field" href="#">Contextual layer 1</a></th>
                <th rowspan="2" style="-moz-user-select: none;" class="sortable-numeric fd-column-1"><a title="Sort on Field" href="#">Contextual layer 2</a></th>
                <th colspan="4">Area (sqkm)</th>
                <th colspan="4">Species</th>
                <th colspan="4">Occurrences</th>
            </tr>
            <tr>
                <!-- area -->
                <th>Area</th>
                <th>Total %</th>
                <th>Row %</th>
                <th>Column %</th>
                <!-- species -->
                <th>Species</th>
                <th>Total %</th>
                <th>Row %</th>
                <th>Column %</th>
                <!-- occurrences -->
                <th>Occurrences</th>
                <th>Total %</th>
                <th>Row %</th>
                <th>Column %</th>
            </tr>
            </thead>
            <tbody>
        <c:forEach items="${tabulations}" var="layer" varStatus="status">
            <tr>
                <td>${layer.name1}</td>
                <td>${layer.name2}</td>
                <!-- area -->
                <td>
                    <a href='tabulation/area/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/area/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/area/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/area/total/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/area/total/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/area/total/${layer.fid1}/${layer.fid2}/json'>json</a>
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
                <!-- species -->
                <td>
                    <a href='tabulation/species/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/species/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/species/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/species/total/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/species/total/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/species/total/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/species/row/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/species/row/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/species/row/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/species/column/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/species/column/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/species/column/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <!-- occurrences -->
                <td>
                    <a href='tabulation/occurrences/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/occurrences/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/occurrences/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/occurrences/total/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/occurrences/total/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/occurrences/total/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/occurrences/row/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/occurrences/row/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/occurrences/row/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
                <td>
                    <a href='tabulation/occurrences/column/${layer.fid1}/${layer.fid2}/html'>html</a>
                    <a href='tabulation/occurrences/column/${layer.fid1}/${layer.fid2}/csv'>csv</a>
                    <a href='tabulation/occurrences/column/${layer.fid1}/${layer.fid2}/json'>json</a>
                </td>
            </tr>
        </c:forEach>
            </tbody>
        </table>

        <script type="text/javascript" src="/layers-service/javascript/SortingTable.js"></script>


    </div>
</div><!--inner-->

<%@include file="../common/bottom.jsp" %>
