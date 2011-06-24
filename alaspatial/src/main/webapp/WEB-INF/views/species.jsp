<%-- 
    Document   : species
    Created on : 22/02/2010, 1:23:39 PM
    Author     : ajayr
--%>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
        <title>Species</title>

        <link rel="stylesheet" href="styles/jquery.flexbox.css" type="text/css" media="all" />

        <script type="text/javascript" src="scripts/libs/jquery/jquery-1.3.2.js"></script>
        <script type="text/javascript" src="scripts/libs/jquery/jquery.flexbox.min.js"></script>

        <script type="text/javascript" src="scripts/main.js"></script>
    </head>
    <body>
        <h1>Hello World!</h1>
        <p>
            ${message}
        </p>

        <div id="splist"></div>
        <input type="text" class="ac_sp_data" id="spdata" />
        <input type="button" id="getrecs" name="getrecs" />

        <ul id="records">
            <c:forEach items="${spList}" var="sp" varStatus="status">
                <li>${sp.scientificname}</li>
            </c:forEach>
        </ul>

    </body>
</html>
