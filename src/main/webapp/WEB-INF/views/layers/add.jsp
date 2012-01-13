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
        <title>Add new layer metadata</title>
        <link rel="stylesheet" href="/alaspatial/styles/style.css" type="text/css" media="all" />
    </head>
    <body>
        <h1>Add new layer metadata</h1>

        <form action="add" method="post">
            <input type="hidden" id="id" name="id" value="" />
            <input type="hidden" id="uid" name="uid" value="" />

            <label for="name">Name:</label> <br />
            <input type="text" id="name" name="name" value="" />
            <br />

            <label for="displayname">Display name:</label> <br />
            <input type="text" id="displayname" name="displayname" value="" />
            <br />

            <label for="description">Description:</label> <br />
            <input type="text" id="description" name="description" value="" />
            <br />

            <label for="type">Type:</label> <br />
            <select id="type" name="type">
                <option value="environmental">Environmental</option>
                <option value="contextual">Contextual</option>
            </select>
            <br />

            <label for="source">Source:</label> <br />
            <select id="source" name="source">
                <option value="local">Local</option>
                <option value="remote">Remote (WMS)</option>
            </select>
            <br />

            <label for="path">Path:</label> <br />
            <input type="text" id="path" name="path" value="" />
            <br />

            <label for="displaypath">WMS URL:</label> <br />
            <input type="text" id="displaypath" name="displaypath" value="" />
            <br />

            <label for="scale">Scale:</label> <br />
            <input type="text" id="scale" name="scale" value="" />
            <br />

            <label for="extent">Extent</label> <br />
            <input type="text" id="extent" name="extent" value="" />
            <br />

            <label for="minlatitude">Min Latitude</label> <br />
            <input type="text" id="minlatitude" name="minlatitude" value="" />
            <br />

            <label for="minlongitude">Min Longitude</label> <br />
            <input type="text" id="minlongitude" name="minlongitude" value="" />
            <br />

            <label for="maxlatitude">Min Latitude</label> <br />
            <input type="text" id="maxlatitude" name="maxlatitude" value="" />
            <br />

            <label for="maxlongitude">Min Longitude</label> <br />
            <input type="text" id="maxlongitude" name="maxlongitude" value="" />
            <br />

            <label for="environmentalvaluemin">Min Environmental value</label> <br />
            <input type="text" id="environmentalvaluemin" name="environmentalvaluemin" value="" />
            <br />

            <label for="environmentalvaluemax">Max Environmental value</label> <br />
            <input type="text" id="environmentalvaluemax" name="environmentalvaluemax" value="" />
            <br />

            <label for="environmentalvalueunits">Environmental value units</label> <br />
            <input type="text" id="environmentalvalueunits" name="environmentalvalueunits" value="" />
            <br />

            <label for="lookuptablepath">Lookup table path</label> <br />
            <input type="text" id="lookuptablepath" name="lookuptablepath" value="" />
            <br />

            <label for="metadatapath">Metadata path</label> <br />
            <input type="text" id="metadatapath" name="metadatapath" value="" />
            <br />

            <label for="classification1">Classification 1</label> <br />
            <input type="text" id="classification1" name="classification1" value="" />
            <br />

            <label for="classification2">Classification 2</label> <br />
            <input type="text" id="classification2" name="classification2" value="" />
            <br />

            <label for="mddatest">mddatest</label> <br />
            <input type="text" id="mddatest" name="mddatest" value="" />
            <br />

            <label for="citationdate">Citation date</label> <br />
            <input type="text" id="citationdate" name="citationdate" value="" />
            <br />

            <label for="datalang">Data language</label> <br />
            <input type="text" id="datalang" name="datalang" value="" />
            <br />

            <label for="mdhrlv">mdhrlv</label> <br />
            <input type="text" id="mdhrlv" name="mdhrlv" value="" />
            <br />

            <label for="resppartyrole">resppartyrole</label> <br />
            <input type="text" id="resppartyrole" name="resppartyrole" value="" />
            <br />

            <label for="licencelevel">Licence level</label> <br />
            <input type="text" id="licencelevel" name="licencelevel" value="" />
            <br />

            <label for="licence_notes">Licence notes</label> <br />
            <input type="text" id="licence_notes" name="licence_notes" value="" />
            <br />

            <label for="sourcelink">Source link</label> <br />
            <input type="text" id="sourcelink" name="sourcelink" value="" />
            <br />

            <label for="keywords">Keywords</label> <br />
            <input type="text" id="keywords" name="keywords" value="" />
            <br />

            <label for="notes">Notes:</label> <br />
            <textarea></textarea>
            <br />

            <label for="enabled">Enabled</label>
            <input type="checkbox" id="enabled" name="enabled" checked="checked" />
            <br />

            <input type="submit" class="button" value="Add" />
            <input type="button" class="button" value="Cancel" onclick="location.href='index';" />
        </form>

    </body>
</html>
