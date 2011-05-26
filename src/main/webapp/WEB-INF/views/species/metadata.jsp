<%-- 
    Document   : metadata
    Created on : Apr 20, 2011, 7:41:49 PM
    Author     : ajay
--%>

<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@page trimDirectiveWhitespaces="false"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!DOCTYPE html>
<html dir="ltr" lang="en-US">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Species information for ${speciesname}</title>
        <link rel="stylesheet" href="/alaspatial/styles/style.css" type="text/css" media="all" />
    </head>
    <body>
        <!--<h1 class="md_heading">Species information for ${speciesname}</h1>-->

        <!-- metadatatype: ${metadatatype} -->

        <c:choose>
            <c:when test='${metadatatype == "activearea"}'>
                <table class="md_table">
                    <tbody>
                        <tr class="md_grey-bg">
                            <td class="md_th">Number of Occurrences</td>
                            <td class="md_spacer"/>
                            <td class="md_value">${geoOccCount}</td>
                        </tr>
                        <tr>
                            <td class="md_th">Number of Species</td>
                            <td class="md_spacer"/>
                            <td class="md_value">${geoSpCount}</td>
                        </tr>
                        <tr class="md_grey-bg">
                            <td class="md_th">Data resources</td>
                            <td class="md_spacer"/>
                            <td class="md_value">
                                <c:forEach var="i" items="${dataresources}" varStatus="curr">
                                    ${i.key}: ${i.value} records <br />
                                </c:forEach>
                            </td>
                        </tr>
                        <!--
                        <tr>
                            <td class="md_value" colspan="3">Download as <a href="/output/sampling/${lsid}_0" target="_blank">GeoJSON</a></td>
                        </tr>
                        -->
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                <table class="md_table">
                    <tbody>
                        <tr class="md_grey-bg">
                            <td class="md_th">Classification</td>
                            <td class="md_spacer"/>
                            <td class="md_value">
                                <%--
                                <c:forEach var="c" items="${classification}" varStatus="curr">
                                    <a href="http://bie.ala.org.au/species/${c.value}">${c.key}</a> ${!curr.last ? "&gt;" : ""} <br />
                                </c:forEach>
                                --%>
                                <c:forEach var="cl" items="${cList}" varStatus="curr">
                                    <c:set var="c" value="${fn:split(cl, ';')}" />
                                    <a href="http://bie.ala.org.au/species/${c[2]}" target="_blank">${c[1]}</a>${!curr.last ? " :" : ""}
                                </c:forEach>
                            </td>
                        </tr>
                        <tr>
                            <td class="md_th">Number of Species</td>
                            <td class="md_spacer"/>
                            <td class="md_value">${geoSpCount}</td>
                        </tr>
                        <tr class="md_grey-bg">
                            <td class="md_th">Number of Occurrences</td>
                            <td class="md_spacer"/>
                            <td class="md_value">${geoOccCount}</td>
                        </tr>
                        <tr>
                            <td class="md_th">Data resources</td>
                            <td class="md_spacer"/>
                            <td class="md_value">
                                <c:forEach var="i" items="${dataresources}" varStatus="curr">
                                    ${i.key}: ${i.value} records <br />
                                </c:forEach>
                            </td>
                        </tr>
                        <c:if test="${dparams != null}">
                            <c:forEach var="cl" items="${dparams}" varStatus="curr">
                                <c:set var="c" value="${fn:split(cl, ':')}" />
                                <tr class="md_grey-bg">
                                    <td class="md_th">${c[0]}</td>
                                    <td class="md_spacer"/>
                                    <td class="md_value">${c[1]}</td>
                                </tr>
                            </c:forEach>
                        </c:if>
                        <tr>
                            <td class="md_value" colspan="3">More information for <a href="http://bie.ala.org.au/species/${lsid}" target="_blank">${speciesname}</a></td>
                        </tr>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </body>
</html>
