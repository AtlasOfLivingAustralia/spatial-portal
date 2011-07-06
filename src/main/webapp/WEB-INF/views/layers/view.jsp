<%-- 
    Document   : list
    Created on : Jun 17, 2010, 3:54:08 PM
    Author     : ajay
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Layer information</title>
        <link rel="stylesheet" href="/alaspatial/styles/style.css" type="text/css" media="all" />
    </head>
    <body>
        <c:choose>
            <c:when test="${layer != null}">

                <h1 class="md_heading">${layer.displayname}</h1>

                <div class="msg">
                    ${message}
                </div>

                <table class="md_table">

                    <tbody>
                        <tr class="md_grey-bg">
                            <th class="md_th">Description</th>
                            <td class="md_spacer"/>
                            <td class="md_value">
                                ${layer.description}
                            </td>
                        </tr>
                        
                        <tr>
                            <th class="md_th">Short name</th>
                            <td class="md_spacer"/>
                            <td class="md_value">${layer.name}</td>
                        </tr>

                        <tr>
                            <th class="md_th">Metadata contact organisation</th>
                            <td class="md_spacer"/>
                            <td class="md_value"><a class="md_a" href="${layer.sourcelink}" target="_blank">${layer.source}</a></td>
                        </tr>

                        <tr class="md_grey-bg">
                            <th class="md_th">Metadata date</th>
                            <td class="md_spacer"/>
                            <td class="md_value">${layer.mddatest}</td>
                        </tr>


                        <tr>
                            <th class="md_th">Reference date</th>
                            <td class="md_spacer"/>
                            <td class="md_value">${layer.citationdate}</td>
                        </tr>

                        <tr class="md_grey-bg">
                            <th class="md_th">Resource constraints</th>
                            <td class="md_spacer"/>
                            <td class="md_value">${layer.licencelevel}: <a class="md_a" href="${layer.licence_link}" target="_blank">Resource constraints</a></td>
                        </tr>


                        <tr>
                            <th class="md_th">Licence notes</th>
                            <td class="md_spacer"/>
                            <td class="md_value">${layer.licence_notes}</td>
                        </tr>

                        <tr class="md_grey-bg">
                            <th class="md_th">Type</th>
                            <td class="md_spacer"/>
                            <c:choose>
                                <c:when test="${layer.type eq 'Environmental'}">
                                    <td class="md_value">Environmental (gridded) ${layer.scale}</td>
                                </c:when>
                                <c:when test="${layer.type eq 'Contextual'}">
                                    <td class="md_value">Contextual (polygonal) ${layer.scale}</td>
                                </c:when>
                                <c:otherwise>
                                    <td class="md_value">${layer.type} ${layer.scale}</td>
                                </c:otherwise>
                            </c:choose>
                        </tr>


                        <tr>
                            <th class="md_th">Classification</th>
                            <td class="md_spacer"/>
                            <td class="md_value">${layer.classification1}
                                <c:if test="${fn:length(layer.classification2) > 0}">
                                    &rArr; ${layer.classification2}
                                </c:if>
                            </td>
                        </tr>

                        <tr class="md_grey-bg">
                            <th class="md_th">Notes (used for search index)</th>
                            <td class="md_spacer"/>
                            <td class="md_value">${layer.notes}</td>
                        </tr>

                        <tr>
                            <th class="md_th">More information</th>
                            <td class="md_spacer"/>
                            <td class="md_value">
                                <c:forEach var="u" items="${fn:split(layer.metadatapath, '|')}">
                                    <a class="md_a" href="${u}">${u}</a><br />
                                </c:forEach>
                            </td>
                        </tr>

                    </tbody>
                </table>

            </c:when>
            <c:otherwise>
                <div class="notavailable">Layer information not available</div>
            </c:otherwise>
        </c:choose>
    </body>
</html>
