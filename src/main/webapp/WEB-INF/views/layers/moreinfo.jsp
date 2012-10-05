<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="/tld/ala.tld" prefix="ala" %>
<%@include file="../common/top.jsp" %>
<header id="page-header">
    <div class="inner">
        <nav id="breadcrumb">
            <ol>
                <li><a href="http://www.ala.org.au">Home</a></li> 
                <li><a href="/">Mapping &#038; analysis</a></li> 
                <li><a href="../../layers/">Spatial Layers</a></li> 
                <li class="last">Layer information</li>
            </ol>
        </nav>
        <section id="content-search">
            <h1>${layer.displayname}</h1>
        </section>
    </div><!--inner-->

</header>
<div class="inner">

    <div class="col-wide last" style="width:100%">
        <c:choose>
            <c:when test="${layer != null}">

                <table class="table-borders" style="width:100%">
                    <tr>
                        <td class="title">Description:</td>
                        <td>${layer.description}</td>
                    </tr>
                    <tr>
                        <td class="title">Short Name:</td>
                        <td>${layer.name}</td>
                    </tr>
                    <tr>
                        <td class="title">Metadata contact organization:</td>
                        <td><a href="${layer.source_link}" target="_blank">${layer.source}</a></td>
                    </tr>
                    <tr>
                        <td class="title">Organisation role: </td>
                        <td>${layer.respparty_role}</td>
                    </tr>
                    <tr>
                        <td class="title">Metadata date:</td>
                        <td>${layer.mddatest}</td>
                    </tr>
                    <tr>
                        <td class="title">Reference date:</td>
                        <td>${layer.citation_date}</td>
                    </tr>
                    <tr>
                        <td class="title">Resource constraints:</td>
                        <td>
                            <ul>
                                <li>Licence level: ${layer.licence_level} </li>
                                <li>Licence info: <a href="${layer.licence_link}" target="_blank">${layer.licence_link}</a> </li>
                            </ul>
                        </td>
                    </tr>
                    <tr>
                        <td class="title">Licence notes:</td>
                        <td>${layer.licence_notes}</td>
                    </tr>
                    <tr>
                        <td class="title">Type:</td>
                        <td>
                            <c:choose>
                                <c:when test="${layer.type eq 'Environmental'}">
                                    Environmental (gridded) ${layer.scale}
                                </c:when>
                                <c:when test="${layer.type eq 'Contextual'}">
                                    Contextual (polygonal) ${layer.scale}
                                </c:when>
                                <c:otherwise>
                                    ${layer.type} ${layer.scale}
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="title">Classification:</td>
                        <td>
                            ${layer.classification1}
                            <c:if test="${fn:length(layer.classification2) > 0}">
                                &rArr; ${layer.classification2}
                            </c:if>
                        </td>
                    </tr>
                    <tr>
                        <td class="title">Units:</td>
                        <td>${layer.environmentalvalueunits}</td>
                    </tr>
                    <tr>
                        <td class="title">Data language:</td>
                        <td>${layer.datalang}</td>
                    </tr>
                    <tr>
                        <td class="title">Scope:</td>
                        <td>${layer.mdhrlv}</td>
                    </tr>
                    <tr>
                        <td class="title">Notes:</td>
                        <td>${layer.notes}</td>
                    </tr>
                    <tr>
                        <td class="title">Keywords:</td>
                        <td>${layer.keywords}</td>
                    </tr>
                    <tr>
                        <td class="title">More information:</td>
                        <td>
                            <c:forEach var="u" items="${fn:split(layer.metadatapath, '|')}">
                                <a href="${u}">${u}</a><br />
                            </c:forEach>
                        </td>
                    </tr>
                    
                    <tr>
                        <td class="title">View in spatial portal :</td>
                        <td>
                           <a href="http://spatial.ala.org.au/?layers=${layer.name}">Click to view this layer</a>
                        </td>
                    </tr>                    
                    
                </table>



            </c:when>
            <c:otherwise>
                <h1>Layer information not available</h1>
            </c:otherwise>
        </c:choose>
    </div>
</div><!--inner-->

<%@include file="../common/bottom.jsp" %>

