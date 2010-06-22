<%-- 
    Document   : add
    Created on : Jun 17, 2010, 3:54:57 PM
    Author     : ajay
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=MacRoman">
        <title>Add new layer</title>
        <link rel="stylesheet" href="/alaspatial/styles/style.css" type="text/css" media="all" />
    </head>
    <body>
        <h1>Add new layer</h1>

        <form action="add" method="post">
            <input type="hidden" id="id" name="id" value="" />
            
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
            <input type="text" id="path" name="path" value="/mnt/ala/data/" />
            <br />

            <label for="displaypath">WMS URL:</label> <br />
            <input type="text" id="displaypath" name="displaypath" value="" />
            <br />

            <label for="notes">Notes (keywords):</label> <br />
            <textarea></textarea>
            <br />

            <label for="enabled">Enabled</label>
            <input type="checkbox" id="enabled" name="enabled" checked="checked" />
            <br />
            
            <input type="submit" class="button" value="Submit" />
            <input type="button" class="button" value="Cancel" onclick="location.href='index';" />
        </form>

    </body>
</html>
