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
        <title>Layer list</title>
        <link rel="stylesheet" href="/alaspatial/styles/style.css" type="text/css" media="all" />
    </head>
    <body>
        <h1>Layer list</h1>
        
        <p>
            <a href="add">Add new layer</a>
        </p>

        <form action="index" method="post">
            <label for="q">Search:</label>
            <input type="text" id="q" name="q" />
            <input type="submit" class="button" value="GO" />
        </form>

        <div>
            ${message}
            <c:if test="${mode} == 'search'">
                <a href="index">Display all.</a>
            </c:if>
        </div>

        <c:choose>
            <c:when test="${fn:length(layerList) > 0}">
                <table border="1">
                    <tr>
                        <th>Name</th>
                        <th>Display name</th>
                        <th>Type</th>
                        <th>Actions</th>
                    </tr>
                    <c:forEach items="${layerList}" var="layer" varStatus="status">
                        <tr>
                            <td>${layer.name}</td>
                            <td>${layer.displayname}</td>
                            <td>${layer.type}</td>
                            <td>
                                <a href="edit/${layer.id}">edit</a>
                                | <a href="delete/${layer.id}">delete</a>
                            </td>
                        </tr>
                    </c:forEach>
                </table>
            </c:when>
            <c:otherwise>
                <ul><li>No layers available</li></ul>
            </c:otherwise>
        </c:choose>
    </body>
</html>
