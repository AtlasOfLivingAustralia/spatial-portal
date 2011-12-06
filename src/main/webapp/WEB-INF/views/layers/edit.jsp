<%-- 
    Document   : add
    Created on : Jun 17, 2010, 3:54:57 PM
    Author     : ajay
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib prefix="spring" uri="http://www.springframework.org/tags" %><%@
taglib prefix="form" uri="http://www.springframework.org/tags/form" %><%@
taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=MacRoman">
        <title>Edit layer metadata</title>
        <link rel="stylesheet" href="/alaspatial/styles/style.css" type="text/css" media="all" />
    </head>
    <body>
        <c:choose>
            <c:when test="${empty layer}">
                <h1>Editing layer metadata</h1>
                <p>
                    Unable to find the layer. Please try again.
                </p>
            </c:when>
            <c:otherwise>

        <h1>Editing layer metadata for ${layer.displayname}</h1>

        <form action="edit" method="post">
            <input type="hidden" id="id" name="id" value="${layer.id}" />
            <input type="hidden" id="uid" name="uid" value="${layer.uid}" />

            <label for="name">Name:</label> <br />
            <input type="text" id="name" name="name" value="${layer.name}" />
            <br />

            <label for="displayname">Display name:</label> <br />
            <input type="text" id="displayname" name="displayname" value="${layer.displayname}" />
            <br />

            <label for="description">Description:</label> <br />
            <textarea id="description" name="description">${layer.description}</textarea>
            <br />

            <label for="pid">PID:</label> <br />
            <input type="text" id="pid" name="pid" value="${layer.pid}" />
            <br />

            <label for="type">Type:</label> <br />
            <input type="text" id="type" name="type" value="${layer.type}" />
            <!--
            <select id="type" name="type">
                <option value="environmental" <c:if test="${layer.type eq 'environmental'}">selected="selected"</c:if>>Environmental</option>
                <option value="contextual" <c:if test="${layer.type eq 'contextual'}">selected="selected"</c:if>>Contextual</option>
            </select>
            -->
            <br />

            <label for="env_category">env category:</label> <br />
            <input type="text" id="env_category" name="env_category" value="${layer.env_category}" />
            <br />

            <label for="source">Source:</label> <br />
            <input type="text" id="source" name="source" value="${layer.source}" />
            <!--
            <select id="source" name="source">
                <option value="local" <c:if test="${layer.source eq 'local'}">selected="selected"</c:if>>Local</option>
                <option value="remote" <c:if test="${layer.source eq 'remote'}">selected="selected"</c:if>>Remote (WMS)</option>
            </select>
            -->
            <br />

            <label for="path">Path:</label> <br />
            <input type="text" id="path" name="path" value="${layer.path}" />
            <br />

            <label for="path_1km">Path 1km:</label> <br />
            <input type="text" id="path_1km" name="path_1km" value="${layer.path_1km}" />
            <br />

            <label for="path_250m">Path 250m:</label> <br />
            <input type="text" id="path_250m" name="path_250m" value="${layer.path_250m}" />
            <br />

            <label for="path_orig">path orig:</label> <br />
            <input type="text" id="path_orig" name="path_orig" value="${layer.path_orig}" />
            <br />

            <label for="displaypath">WMS URL:</label> <br />
            <input type="text" id="displaypath" name="displaypath" value="${layer.displaypath}" />
            <br />

            <label for="scale">Scale:</label> <br />
            <input type="text" id="scale" name="scale" value="${layer.scale}" />
            <br />

            <label for="extent">Extent</label> <br />
            <input type="text" id="extent" name="extent" value="${layer.extent}" />
            <br />

            <label for="minlatitude">Min Latitude</label> <br />
            <input type="text" id="minlatitude" name="minlatitude" value="${layer.minlatitude}" />
            <br />

            <label for="minlongitude">Min Longitude</label> <br />
            <input type="text" id="minlongitude" name="minlongitude" value="${layer.minlongitude}" />
            <br />

            <label for="maxlatitude">Max Latitude</label> <br />
            <input type="text" id="maxlatitude" name="maxlatitude" value="${layer.maxlatitude}" />
            <br />

            <label for="maxlongitude">Max Longitude</label> <br />
            <input type="text" id="maxlongitude" name="maxlongitude" value="${layer.maxlongitude}" />
            <br />

            <label for="environmentalvaluemin">Min Environmental value</label> <br />
            <input type="text" id="environmentalvaluemin" name="environmentalvaluemin" value="${layer.environmentalvaluemin}" />
            <br />

            <label for="environmentalvaluemax">Max Environmental value</label> <br />
            <input type="text" id="environmentalvaluemax" name="environmentalvaluemax" value="${layer.environmentalvaluemax}" />
            <br />

            <label for="environmentalvalueunits">Environmental value units</label> <br />
            <input type="text" id="environmentalvalueunits" name="environmentalvalueunits" value="${layer.environmentalvalueunits}" />
            <br />

            <label for="lookuptablepath">Lookup table path</label> <br />
            <input type="text" id="lookuptablepath" name="lookuptablepath" value="${layer.lookuptablepath}" />
            <br />

            <label for="metadatapath">Metadata path</label> <br />
            <input type="text" id="metadatapath" name="metadatapath" value="${layer.metadatapath}" />
            <br />

            <label for="classification1">Classification 1</label> <br />
            <input type="text" id="classification1" name="classification1" value="${layer.classification1}" />
            <br />

            <label for="classification2">Classification 2</label> <br />
            <input type="text" id="classification2" name="classification2" value="${layer.classification2}" />
            <br />

            <label for="mddatest">mddatest</label> <br />
            <input type="text" id="mddatest" name="mddatest" value="${layer.mddatest}" />
            <br />

            <label for="citation_date">Citation date</label> <br />
            <input type="text" id="citation_date" name="citation_date" value="${layer.citation_date}" />
            <br />

            <label for="datalang">Data language</label> <br />
            <input type="text" id="datalang" name="datalang" value="${layer.datalang}" />
            <br />

            <label for="mdhrlv">mdhrlv</label> <br />
            <input type="text" id="mdhrlv" name="mdhrlv" value="${layer.mdhrlv}" />
            <br />

            <label for="respparty_role">respparty role</label> <br />
            <input type="text" id="respparty_role" name="respparty_role" value="${layer.respparty_role}" />
            <br />

            <label for="licence_level">Licence level</label> <br />
            <input type="text" id="licence_level" name="licence_level" value="${layer.licence_level}" />
            <br />

            <label for="licence_link">Licence link</label> <br />
            <input type="text" id="licence_link" name="licence_link" value="${layer.licence_link}" />
            <br />

            <label for="licence_notes">Licence notes</label> <br />
            <input type="text" id="licence_notes" name="licence_notes" value="${layer.licence_notes}" />
            <br />

            <label for="source_link">Source link</label> <br />
            <input type="text" id="source_link" name="source_link" value="${layer.source_link}" />
            <br />

            <label for="keywords">Keywords</label> <br />
            <input type="text" id="keywords" name="keywords" value="${layer.keywords}" />
            <br />

            <label for="notes">Notes:</label> <br />
            <textarea id="notes" name="notes">${layer.notes}</textarea>
            <br />

            <label for="enabled">Enabled</label>
            <input type="checkbox" id="enabled" name="enabled" <c:if test="${layer.enabled}">checked="checked"</c:if> />
            <br />

            <input type="submit" class="button" value="Save changes" />
            <input type="button" class="button" value="Cancel" onclick="location.href='index';" />
        </form>
            </c:otherwise>
        </c:choose>

    </body>
</html>
