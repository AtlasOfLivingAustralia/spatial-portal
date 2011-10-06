<%-- 
    Document   : list
    Created on : Sep 26, 2011, 9:52:39 PM
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
        <title>Available tabulation layers : ALA</title>
    </head>
    <body>
        <h1>Available tabulation layers</h1>
        <table border="1">
            <tr>
                <th>Field 1</th>
                <th>Field 2</th>
                <th>Link</th>
            </tr>
        <c:forEach items="${tabulations}" var="layer" varStatus="status">
            <tr>
                <td>${layer.name1}</td>
                <td>${layer.name2}</td>
                <td><a href="../tabulation/${layer.fid1}/${layer.fid2}/html">table</a></td>
            </tr>
        </c:forEach>
        </table>
    </body>
</html>
