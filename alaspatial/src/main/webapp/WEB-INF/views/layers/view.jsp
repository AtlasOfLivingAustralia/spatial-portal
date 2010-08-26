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
        <meta http-equiv="Content-Type" content="text/html; charset=MacRoman">
        <title>Layer information</title>
        <link rel="stylesheet" href="/alaspatial/styles/style.css" type="text/css" media="all" />
    </head>
    <body>
        <c:choose>
            <c:when test="${layer != null}">

                <h1>${layer.displayname}</h1>

                <div class="msg">
                    ${message}
                </div>

                <!--
                <p>
                    <span class="title">Name:</span> <br />
                    ${layer.displayname}
                </p>
                -->

                <p>
                    <span class="title">Description:</span> <br />
                    ${layer.description}
                </p>

                <p>
                    <span class="title">Source:</span> <br />
                    ${layer.source}
                </p>

                <p>
                    <span class="title">Type:</span> <br />
                    ${layer.type}
                </p>

                <p>
                    <span class="title">Classification:</span> <br />
                    ${layer.classification1}
                    <c:if test="${fn:length(layer.classification2) > 0}">
                        &rArr; ${layer.classification2}
                    </c:if>
                </p>

                <p>
                    <span class="title">Metadata:</span> <br />
                    ${layer.metadatapath}
                </p>

                <p>
                    <span class="title">Extent:</span> <br />
                    2. ${layer.minlongitude} ${layer.minlatitude}, ${layer.maxlongitude}, ${layer.maxlatitude}
                </p>

                <p>
                    <span class="title">Display path:</span> <br />
                    ${layer.displaypath}
                </p>
            </c:when>
            <c:otherwise>
                <div class="notavailable">Layer info not available</div>
            </c:otherwise>
        </c:choose>
    </body>
</html>
