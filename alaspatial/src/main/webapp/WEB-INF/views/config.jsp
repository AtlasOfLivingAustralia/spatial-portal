<%
/*
 * Dynamic properties file for ALA Spatial Portal
 */

 /**
 *
 * Modelling options
 *
 */

 String worldClimPresentVars = "/Users/ajay/projects/data/modelling/WorldClimCurrent/"; // E:\\projects\\data\\env\\au\\bioclim\\
 String maxentCmdPath = "java -mx900m -jar /Users/ajay/projects/modelling/maxent/maxent.jar "; // E:\\projects\\modelling\\avid\\

 session.setAttribute("worldClimPresentVars",worldClimPresentVars);
 session.setAttribute("maxentCmdPath",maxentCmdPath);

 /**
 *
 * GeoServer settings
 *
 */

 String geoserver_url = "http://localhost:8080/geoserver"; // make sure there isn't a ending forward slash
 String geoserver_username = "admin";
 String geoserver_password = "geoserver";

 session.setAttribute("geoserver_url",geoserver_url);
 session.setAttribute("geoserver_username",geoserver_username);
 session.setAttribute("geoserver_password",geoserver_password);


/*
 // general/global options
 String sessionPath = "c:\\projects\\runtime\\sessiondata\\"; // E:\\projects\\sessiondata\\
 String bioClimVars = "C:\\projects\\data\\modelling\\env\\au\\bioclim\\"; // E:\\projects\\data\\env\\au\\bioclim\\ c:\\projects\\biomaps\\modelling\\avid\\
 String worldClimPresentVars = "C:\\projects\\data\\modelling\\WorldClimCurrent\\"; // E:\\projects\\data\\env\\au\\bioclim\\
 String worldClimFutureVars = "C:\\projects\\data\\modelling\\WorldClimFuture\\"; // E:\\projects\\data\\env\\au\\bioclim\\
 String worldGri = "C:\\projects\\data\\modelling\\env\\world\\";

 // avid options
 String avidCmdPath = "c:\\projects\\biomaps\\modelling\\avid\\"; // E:\\projects\\modelling\\avid\\

 session.setAttribute("sessionPath", sessionPath);
 session.setAttribute("bioClimVars",bioClimVars);
 session.setAttribute("worldClimPresentVars",worldClimPresentVars);
 session.setAttribute("worldClimFutureVars",worldClimFutureVars);
 session.setAttribute("worldGri",worldGri);
 session.setAttribute("avidCmdPath",avidCmdPath);
 //session.setAttribute("",);
 //session.setAttribute("",);

*/
%>