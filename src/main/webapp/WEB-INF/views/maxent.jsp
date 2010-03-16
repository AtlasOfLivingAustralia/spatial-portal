<%-- 
    Document   : maxent
    Created on : 23/02/2010, 6:23:46 PM
    Author     : ajayr
--%>

<%@page contentType="text/html" pageEncoding="windows-1252"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">
<%@include file="config.jsp" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
        <title>Maxent</title>

        <link rel="stylesheet" href="styles/jquery.flexbox.css" type="text/css" media="all" />

        <script type="text/javascript" src="scripts/libs/jquery/jquery-1.3.2.js"></script>
        <script type="text/javascript" src="scripts/libs/jquery/jquery.flexbox.min.js"></script>
        <script type="text/javascript" src="scripts/libs/jquery/jquery.form.js"></script>
        <script type="text/javascript" src="scripts/libs/jquery/jquery.validate.min.js"></script>
        <script type="text/javascript" src="scripts/libs/jquery/jquery.form.wizard-2.0.0-RC3-min.js"></script>

        <script type="text/javascript" src="scripts/main.js"></script>
    </head>
    <body>
        <h1>Maxent</h1>
        <div style="width: 550px; margin: 0 auto;">
            <div id="addprocessform" class="form1" style="border: 1px solid #eee;">
                <form id="theProcessForm" method="post" action="maxent/process">
                    <div id="firstProcessStep" class="step">
                        <h1>Step 1 - Biodiversity data</h1>
                        <p>Select your biodiversity data from OZCAM to analyse.</p>
                        <p>Start by adding a first few characters and select your preferred name from the options available.</p>

                        <div id="splist"></div>
                        <input type="hidden" name="spdata" class="required ac_sp_data" />

                        <br /><br /><br /><br />
                        <br /><br /><br /><br />

                    </div>
                    <div id="secondProcessStep" class="step">
                        <h1>Step 2 - Environmental variables</h1>
                        <p>Select your WorldCLIM environmental variables.</p>
                        <p>
                            Selecting appropriate variables gives you the best case analysis output.
                            Please view the Help page to get more information about the variables.
                        </p>

                        <label for="txtDSourceData">Environmental values: </label>
                        <!-- <ul id="evarsList2" class="evarsList checklist"></ul> -->
                        <%@include file="includes/envlayers.jsp" %>

                        <br /> <br /> <br /> <br />

                    </div>
                    <div id="lastProcessStep" class="step"> <!-- thirdProcessStep -->
                        <h1>Step 3 - Method parameters</h1>
                        <p>
                            Select the optional parameters for Maxent.
                            More information about the parameters is available under Help.
                        </p>

                        <label for="">
                            <input type="checkbox" name="chkJackknife" id="chkJackknife" />
                            Do jackknife to measure variable importance
                        </label>

                        <br /> <br />

                        <label for="">
                            <input type="checkbox" name="chkResponseCurves" id="chkResponseCurves" />
                            Create response curves
                        </label>

                        <br /> <br />

                        <label for="">Random test percentage (0 - 100):
                            <input type="text" name="txtTestPercentage" id="txtTestPercentage" value="0" />

                        </label>
                    </div>
                    <!--
                    <div id="lastProcessStep" class="step">
                        <h1>Step 4 - Name your process</h1>
                        <p>
                            <span>Taxon: </span>
                            <span id="confirmTaxonName"></span>
                            <span id="confirmTaxonLevel"></span>
                        </p>
                        <p>
                            <span>Env Vars: </span>
                            <span id="confirmEnvVars"></span>
                        </p>
                        <p>
                            <span>Env Params: </span>
                            <span id="confirmEnvParams"></span>
                        </p>
                    </div>
                    -->
                    <div style="text-align: right">
                        <input type="reset" value="Reset" />
                        <input type="submit" value="Submit" id="prSubmit" />
                    </div>
                </form>

            </div>
                        
            <div id="resultUrl" style="border: 1px solid #eee;"></div>
            <div id="map"></div>
        </div>
    </body>
</html>
