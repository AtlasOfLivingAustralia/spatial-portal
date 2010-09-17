<%-- 
    Document   : index
    Created on : 22/02/2010, 12:38:56 PM
    Author     : ajayr
--%>

<%@page contentType="text/html" pageEncoding="windows-1252"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
        <title>JSP Page</title>
        <style type="text/css">
            body {

                margin: 0;
                padding: 0;
                font-family: Verdana;
                font-size: small;
            }
            #header {
                margin: 10px;
}
            #display {
                width: 100%;
                height: 800px;
            }
            #navlist
            {
                border-bottom: 1px solid #ccc;
                margin: 0;
                padding-bottom: 19px;
                padding-left: 10px;
            }

            #navlist ul, #navlist li
            {
                display: inline;
                list-style-type: none;
                margin: 0;
                padding: 0;
            }

            #navlist a:link, #navlist a:visited
            {
                background: #E8EBF0;
                border: 1px solid #ccc;
                color: #666;
                float: left;
                font-size: small;
                font-weight: normal;
                line-height: 14px;
                margin-right: 8px;
                padding: 2px 10px 2px 10px;
                text-decoration: none;
            }

            #navlist a:link.current, #navlist a:visited.current
            {
                background: #fff;
                border-bottom: 1px solid #fff;
                color: #000;
            }

            #navlist a:hover { color: #f00; }

            #navlist ul a:hover { color: #f00 !important; }

            #contents
            {
                background: #fff;
                border: 1px solid #ccc;
                border-top: none;
                clear: both;
                margin: 0px;
                padding: 15px;
            } 
        </style>
        <script type="text/javascript">
            //var methodList = new Array("mfilter", "msample", "mmaxent");

            var mfilterUrl = "speciesList.zul";
            var msampleUrl = "sampling.zul";
            var malocUrl = "ALOC.zul";
            var mjobsUrl = "jobs.zul";
            var mmaxentUrl = "maxent2";

            function menuClick(method) {
                var murl = "";

                if (method == 'mfilter') murl = mfilterUrl;
                else if (method == 'msample') murl = msampleUrl;
                else if (method == 'maloc') murl = malocUrl;
                else if (method == 'mjobs') murl = mjobsUrl;
                else if (method == 'mmaxent') murl = mmaxentUrl;
                else alert("Please select a method from the list");

                document.getElementById('display').src = murl;
            }
        </script>
    </head>
    <body>
        <div id="header">
            <h1>ALA Spatial Analysis Toolkit</h1>
            <p>Please select a method to start.</p>

        </div>
        <div id="navcontainer">
            <ul id="navlist">
                <!-- <li id="active"><a href="#" id="current">Item one</a></li> -->
                <li><a id="mfilter" href="#" onclick="menuClick(this.id);return false;">Filtering</a></li>
                <li><a id="msample" href="#" onclick="menuClick(this.id);return false;">Sampling</a></li>
                <li><a id="mmaxent" href="#" onclick="menuClick(this.id);return false;">Maxent</a></li>
                <li><a id="maloc" href="#" onclick="menuClick(this.id);return false;">ALOC</a></li>
                <li><a id="mjobs" href="#" onclick="menuClick(this.id);return false;">Admin</a></li>
            </ul>
        </div>

        <div id="contents">
            <iframe id="display" width="100%" height="100%" frameborder="0" scrolling="no" />
        </div>

        <!--
        <p><a href="species">Species</a></p>
        <p><a href="maxent">Maxent</a></p>
        <p><a href="tabulation.zul">Sampling</a></p>
        <p><a href="filtering_cl.html">Filtering (A)</a></p>
        <p><a href="filtering_svr.zul">Filtering (B)</a></p>
        -->



    </body>
</html>
