<%-- 
    Document   : index
    Created on : Aug 25, 2011, 10:33:50 AM
    Author     : ajay
--%>

<%@ page contentType="text/html" pageEncoding="UTF-8" %><%@
taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
       %><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!DOCTYPE html>
<html dir="ltr" lang="en-US">
    <head profile="http://gmpg.org/xfn/11">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

        <title>Spatial | Atlas of Living Australia</title>
        <link rel="stylesheet" href="http://www.ala.org.au/wp-content/themes/ala/style.css" type="text/css" media="screen" />
        <link rel="icon" type="image/x-icon" href="http://www.ala.org.au/wp-content/themes/ala/images/favicon.ico" />
        <link rel="shortcut icon" type="image/x-icon" href="http://www.ala.org.au/wp-content/themes/ala/images/favicon.ico" />

        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/sf.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/superfish.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/skin.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/jquery.autocomplete.css" />




        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/biocache.css" />


        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/form.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery-1.4.3.min.js"></script>

        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/hoverintent-min.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/superfish/superfish.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.autocomplete.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/uservoice.js"></script>
        <script type="text/javascript">

            //add the indexOf method for IE7
            if(!Array.indexOf){
                Array.prototype.indexOf = function(obj){
                    for(var i=0; i<this.length; i++){
                        if(this[i]===obj){
                            return i;
                        }
                    }
                    return -1;
                }
            }
            // initialise plugins
            jQuery(function(){
                jQuery('ul.sf').superfish( {
                    delay:500,
                    autoArrows:false,
                    dropShadows:false
                });

                // highlight explore menu tab
                jQuery("div#nav li.nav-explore").addClass("selected");
                // autocomplete for search input (Note: JQuery UI version)
                jQuery("form#search-form input#search").autocomplete({
                    source: function( request, response ) {
                        $.ajax({
                            url: "http://bie.ala.org.au/search/auto.json",
                            dataType: "jsonp",
                            data: {
                                limit: 10,
                                q: request.term
                            },
                            success: function( data ) {
                                response( $.map( data.autoCompleteList, function( item ) {
                                    return {
                                        label: item.matchedNames[0],
                                        value: item.matchedNames[0]
                                    }
                                }));
                            }
                        });
                    },
                    minLength: 3,
                    zIndex: 11

                });
            });

        </script>
        <style type="text/css">
            ul.ui-autocomplete {
                text-align: left;
                z-index: 11 !important;
            }
        </style>

        <meta name='robots' content='index,follow' />
        <link rel="alternate" type="application/rss+xml" title="Atlas Living Australia NG &raquo; Feed" href="http://www.ala.org.au/?feed=rss2" />
        <link rel="alternate" type="application/rss+xml" title="Atlas Living Australia NG &raquo; Comments Feed" href="http://www.ala.org.au/?feed=comments-rss2" />
        <link rel='stylesheet' id='external-links-css'  href='http://www.ala.org.au/wp-content/plugins/sem-external-links/sem-external-links.css?ver=20090903' type='text/css' media='' />
        <!--<link rel="EditURI" type="application/rsd+xml" title="RSD" href="http://www.ala.org.au/xmlrpc.php?rsd" />
        <link rel="wlwmanifest" type="application/wlwmanifest+xml" href="http://www.ala.org.au/wp-includes/wlwmanifest.xml" />
        <link rel='index' title='Atlas Living Australia NG' href='http://www.ala.org.au' />
        <meta name="generator" content="WordPress 2.9.2" />
        <link rel='canonical' href='http://www.ala.org.au' />-->

        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="pageName" content="home"/>

        <style type="text/css">
            .code { font-family: courier new;}
        </style>


        <!-- WP Menubar 4.7: start CSS -->
        <!-- WP Menubar 4.7: end CSS -->
    </head>
    <body class="two-column-right">
        <div id="wrapper">
            <div id='banner'><div id='logo'><a href='http://www.ala.org.au' title='Atlas of Living Australia home'><img src='http://www.ala.org.au/wp-content/themes/ala/images/ala_logo.png' width='215' height='80' alt='Atlas of Living Australia logo'/></a></div><!--close logo--><div id='nav'><!-- WP Menubar 4.7: start menu nav-site, template Superfish, CSS  --><ul class='sf'><li class='nav-home'><a href='http://www.ala.org.au/'><span>Home</span></a></li><li class='nav-explore'><a href='http://www.ala.org.au/explore/'><span>Explore</span></a><ul><li><a href='http://biocache.ala.org.au/explore/your-area'><span>Your Area</span></a></li><li><a href='http://bie.ala.org.au/regions/'><span>Regions</span></a></li><li><a href='http://www.ala.org.au/explore/species-maps/'><span>Species Maps</span></a></li><li><a href='http://collections.ala.org.au/public/map'><span>Natural History Collections</span></a></li><li><a href='http://www.ala.org.au/explore/themes/'><span>Themes &amp; Highlights </span></a></li></ul></li><li class='nav-tools'><a href='http://www.ala.org.au/tools-services/'><span>Tools</span></a><ul><li><a href='http://www.ala.org.au/tools-services/species-name-services/'><span>Taxon Web Services</span></a></li><li><a href='http://www.ala.org.au/tools-services/sds/'><span>Sensitive Data Service</span></a></li><li><a href='http://www.ala.org.au/tools-services/spatial-analysis/'><span>Spatial Analysis</span></a></li><li><a href='http://www.ala.org.au/tools-services/citizen-science/'><span>Citizen Science</span></a></li><li><a href='http://www.ala.org.au/tools-services/identification-tools/'><span>Identification Tools</span></a></li><li><a href='http://www.ala.org.au/tools-services/onlinedesktop-tools-review/'><span>Online &amp; Desktop Tools Review</span></a></li></ul></li><li class='nav-share'><a href='http://www.ala.org.au/share/' title='Share - links, images, images, literature, your time'><span>Share</span></a><ul><li><a href='http://www.ala.org.au/share/share-links/'><span>Share links, ideas, information</span></a></li><li><a href='http://www.ala.org.au/share/share-data/'><span>Share Datasets</span></a></li><li><a href='http://www.ala.org.au/share/about-sharing/'><span>About Sharing</span></a></li></ul></li><li class='nav-support'><a href='http://www.ala.org.au/support/'><span>Support</span></a><ul><li><a href='http://www.ala.org.au/support/contact-us/'><span>Contact Us</span></a></li><li><a href='http://www.ala.org.au/support/get-started/'><span>Get Started</span></a></li><li><a href='http://www.ala.org.au/support/user-feedback/'><span>User Feedback</span></a></li><li><a href='http://www.ala.org.au/support/faq/'><span>Frequently Asked Questions</span></a></li></ul></li><li class='nav-contact'><a href='http://www.ala.org.au/support/contact-us/'><span>Contact Us</span></a></li><li class='nav-about'><a href='http://www.ala.org.au/about/'><span>About the Atlas</span></a><ul><li><a href='http://www.ala.org.au/about/progress/'><span>A Work in Progress</span></a></li><li><a href='http://www.ala.org.au/about/atlas-partners/'><span>Atlas Partners</span></a></li><li><a href='http://www.ala.org.au/about/people/'><span>Working Together</span></a></li><li><a href='http://www.ala.org.au/about/contributors/'><span>Atlas Contributors</span></a></li><li><a href='http://www.ala.org.au/about/project-time-line/'><span>Project Time Line</span></a></li><li><a href='http://www.ala.org.au/about/program-of-projects/'><span>Atlas Projects</span></a></li><li><a href='http://www.ala.org.au/about/atlas-partners/'><span>Atlas Partners</span></a></li><li><a href='http://www.ala.org.au/about/international-collaborations/'><span>Associated Projects</span></a></li><li><a href='http://www.ala.org.au/about/communications-centre/'><span>Communications Centre</span></a></li><li><a href='http://www.ala.org.au/about/governance/'><span>Atlas Governance</span></a></li><li><a href='http://www.ala.org.au/about/terms-of-use/'><span>Terms of Use</span></a></li></ul></li><li class='nav-myprofile nav-right'><a href='https://auth.ala.org.au/cas/login?service=http://www.ala.org.au/wp-login.php?redirect_to=http://www.ala.org.au/my-profile/'><span>My Profile</span></a></li><li class='nav-login nav-right'><a href='https://auth.ala.org.au/cas/login?service=http://biocache.ala.org.au/biocache-service/'>Log in</a></li></ul><!-- WP Menubar 4.7: end menu nav-site, template Superfish, CSS  --></div><!--close nav--><div id='wrapper_search'><form id='search-form' action='http://bie.ala.org.au/search' method='get' name='search-form'><label for='search'>Search</label><input type='text' class='filled' id='search' name='q' value='Search the Atlas'/><span class='search-button-wrapper'><input type='submit' class='search-button' id='search-button' alt='Search' value='Search' /></span></form></div><!--close wrapper_search--></div><!--close banner-->

            <div id="wrapper_border"><!--main content area-->
                <div id="border">
                    <div id="content">

                        <div class="section">
                            <h1> Spatial Web Services </h1>

                            <h3>Layers Web Services</h3>

                            <ul>
                                <li>Layers<ul>
                                        <li><strong>Get a list of all layers:</strong> <a href="/layers-service/layers">/layers-service/layers</a></li>
                                        <li><strong>Get a list of all environmental/grided layers:</strong> <a href="/layers-service/layers/grids">/layers-service/layers/grids</a></li>
                                        <li><strong>Get a list of all contextual layers:</strong> <a href="/layers-service/layers/shapes">/layers-service/layers/shapes</a></li>
                                    </ul></li>

                                <li>Fields<ul>
                                        <li><strong>Get a list of all fields:</strong> <a href="/layers-service/fields">/layers-service/fields</a></li>
                                        <li><strong>Get information about a specific field, given a field id:</strong> /layers-service/field/{id} e.g. <a href="/layers-service/field/cl22">/layers-service/field/cl22</a></li>
                                    </ul></li>

                                <li>Objects<ul>
                                        <li><strong>Get a list of objects, given the field id:</strong> /layers-service/objects/{id} e.g. <a href="/layers-service/objects/cl22">/layers-service/objects/cl22</a></li>
                                        <li><strong>Get information about an object, given its pid</strong> /layers-service/object/{pid} e.g. <a href="/layers-service/object/3742602">/layers-service/object/3742602</a></li>
                                        <li><strong>Download a shape object as KML, given its pid:</strong> /layers-service/shape/kml/{pid} e.g. <a href="/layers-service/shape/kml/3742602">/layers-service/shape/kml/3742602</a></li>
                                        <li><strong>Download a shape object as WKT, given its pid:</strong> /layers-service/shape/wkt/{pid} <a href="/layers-service/shape/wkt/3742602">/layers-service/shape/wkt/3742602</a></li>
                                        <li><strong>Download a shape object as GeoJSON, given its pid:</strong> /layers-service/shape/geojson/{pid} <a href="/layers-service/shape/geojson/3742602">/layers-service/shape/geojson/3742602</a></li>
                                    </ul></li>

                                <li>Search<ul>
                                        <li><strong>Search for gazzetter localities:</strong> /search?q={free text} e.g. <a href="/layers-service/search?q=canberra">/layers-service/search?q=canberra</a></li>
                                    </ul></li>

                                <li>Intersect<ul>
                                        <li><strong>Intersect a layer(s) at a given set of coordinates. Multiple field ids or layer names can be specified separated by a comma (e.g. cl22,cl23):</strong> /layers-service/intersect/{id}/{latitude}/{longitude} e.g. <a href="/layers-service/intersect/cl22/-29.911/132.769">/layers-service/intersect/cl22/-29.911/132.769</a></li>
                                        <li><strong>Batch intersect a layer(s) at given coordinates. Multiple field ids or layer names can be specified separated by a comma (e.g. cl22,cl23):</strong> /layers-service/intersect/batch e.g. <a href="/layers-service/intersect/batch?fids=cl22&points=-29.911,132.769">/layers-service/intersect/batch?fids=cl22&points=-29.911,132.769</a></li>
                                        <li><strong>Check batch intersect status with a batchId:</strong> /layers-service/intersect/batch/{batchId} e.g. /layers-service/intersect/batch/1234</li>
                                        <li><strong>Download a finished batch intersect with a batchId as zipped file 'sample.csv':</strong> /layers-service/intersect/batch/download/{batchId} e.g. /layers-service/intersect/batch/download/1234</li>
                                    </ul></li>

                                <li>Distributions<ul>
                                        <li><strong>Get a list of all distributions:</strong> <a href="/layers-service/distributions">/layers-service/distributions</a></li>
                                        <li><strong>Get information about a specific distribution, given a spcode:</strong> /layers-service/distribution/{spcode} e.g. <a href="/layers-service/distribution/37031044">/layers-service/distribution/37031044</a></li>
                                    </ul></li>

                                <!--
                                    <li>Tabulation<ul>
                                        <li><strong>Get a list of tabulations:</strong> <a href="/layers-service/tabulations">/layers-service/tabulations</a></li>
                                        <li><strong>Get a list of tabulations as HTML:</strong> <a href="/layers-service/tabulations/html">/layers-service/tabulations/html</a></li>
                                        <li><strong>Get tabulation for a single layer as HTML:</strong> /layers-service/tabulation/cl22/html?wkt={valid wkt polygon geometry} e.g. <a href="/layers-service/tabulation/cl22/html.html?wkt=POLYGON((130%20-24,138%20-24,138%20-20,130%20-20,130%20-24))">/layers-service/tabulation/cl22/html.html?wkt=POLYGON((130 -24,138 -24,138 -20,130 -20,130 -24))</a></li>
                                        <li><strong>Get tabulation for 2 layers, given their id's:</strong> /layers-service/tabulation/{id}/{id} e.g. <a href="/layers-service/tabulation/cl22/cl23">/layers-service/tabulation/cl22/cl23</a></li>
                                        <li><strong>Get tabulation as CSV for 2 layers, given their id's:</strong> /layers-service/tabulation/{id}/{id}/csv e.g. <a href="/layers-service/tabulation/cl22/cl23/csv">/layers-service/tabulation/cl22/cl23/csv</a></li>
                                        <li><strong>Get tabulation as HTML for 2 layers, given their id's:</strong> /layers-service/tabulation/{id}/{id}/html e.g. <a href="/layers-service/tabulation/cl22/cl23/html">/layers-service/tabulation/cl22/cl23/html</a></li>
                                        <li><strong>Get tabulation within an area as HTML for 2 layers, given their id's:</strong> /layers-service/tabulation/{id}/{id}/html?wkt={valid wkt polygon geometry} e.g. <a href="/layers-service/tabulation/cl22/cl23/html.html?wkt=POLYGON((130%20-24,138%20-24,138%20-20,130%20-20,130%20-24))">/layers-service/tabulation/cl22/cl23/html.html?wkt=POLYGON((130 -24,138 -24,138 -20,130 -20,130 -24))</a></li>
                                    </ul></li>
                                -->
                            </ul>

                            <h3>Occurrences</h3>
                            <ul>
                                <li><strong>Static Species Density Heatmap </strong><a href="http://biocache.ala.org.au/density/map?q=*:*">http://biocache.ala.org.au/density/map?q=*:*</a></li> - returns heatmap image (optional param forceRefresh=true will regenerate the image)
                                <li><strong>Static Species Density Legend: </strong><a href="http://biocache.ala.org.au/density/legend?q=*:*">http://biocache.ala.org.au/legend/map?q=*:*</a></li> - returns associated legend image (optional param forceRefresh=true will regenerate the image)
                            </ul>
                            
                            <h3>Webportal Services</h3>
                            <p>These Webportal services are available at <a href="http://biocache.ala.org.au/ws">http://biocache.ala.org.au/ws</a> </p>
                            <ul>
                                These services will include all records that satisfy the q, fq and wkt parameters.
                                <ul>
                                    <li>q - the initial query</li>
                                    <li>fq - filters to be applied to the original query</li>
                                    <li>wkt - filter polygon area to be applied to the original query</li>
                                    <li>fl - a comma separated list of fields to include (contains a list of default)</li>
                                    <li>pageSize - download limit (may be overridden)</li>
                                </ul>

                                <li><strong>Short Query Parameters:</strong>
                                    <ul>
                                        <li><strong>Construction:</strong> /webportal/params <br>
                                            POST service.<br>
                                            Stores q and wkt parameters.<br>
                                            Returns a short <b>value</b> that can be used as the initial q value in other services for webportal. e.g. q=qid:<b>value</b>
                                        </li>
                                        <li><strong>Test: </strong> /webportal/params/<b>value</b>
                                            Test if a short query parameter is valid.<br>
                                            Returns true or false</li>
                                    </ul>
                                </li>
                                <li><strong>Occurrences Bounding Box:</strong> /webportal/bbox <br>
                                    Returns CSV of bounding box of occurrences eg. <a href="http://biocache.ala.org.au/ws/webportal/bbox?q=macropus">http://biocache.ala.org.au/ws/webportal/bbox?q=macropus</a></li>
                                <li><strong>Data Providers</strong> /webportal/dataProviders eg. <a href="http://biocache.ala.org.au/ws/webportal/dataProviders?q=macropus">http://biocache.ala.org.au/ws/webportal/dataProviders?q=macropus</a></li>
                                <li><strong>Species List:</strong>
                                    <ul>
                                        <li><strong>Get species list:</strong> /webportal/species eg. <a href="http://biocache.ala.org.au/ws/webportal/species?q=macropus&pageSize=100">http://biocache.ala.org.au/ws/webportal/species?q=macropus&pageSize=100</a></li>
                                        <li><strong>Get species list as CSV:</strong> /webportal/species.csv eg. <a href="http://biocache.ala.org.au/ws/webportal/species.csv?q=macropus&wkt=POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))&pageSize=100">http://biocache.ala.org.au/ws/webportal/species.csv?q=macropus&wkt=POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))&pageSize=100</a></li>
                                    </ul>
                                </li>
                                <li><strong>Occurrences:</strong>
                                    <ul>
                                        <li><strong>Get occurrences:</strong> /webportal/occurrences eg. <a href="http://biocache.ala.org.au/ws/webportal/occurrences?q=macropus">http://biocache.ala.org.au/ws/webportal/occurrences?q=macropus</a></li>
                                        <li><strong>Get occurrences as gzipped CSV:</strong> /webportal/occurrences.gz eg. <a href="http://biocache.ala.org.au/ws/webportal/occurrences.gz?q=macropus&fl=longitude,latitude">http://biocache.ala.org.au/ws/webportal/occurrences.gz?q=macropus&fl=longitude,latitude</a></li>
                                    </ul>
                                </li>
                            </ul>

                            <h3>Webportal WMS Service</h3>
                            <p>A worked example is located <a href="http://code.google.com/p/alageospatialportal/wiki/SimpleWMSExample">here.</a></p>
                            <ul>
                                <li><strong>Tile:</strong> /webportal/wms/reflect
                                    <ul>
                                        <li>BBOX - EPSG900913 bounding box. e.g. &BBOX=12523443.0512,-2504688.2032,15028131.5936,0.3392000021413</li>
                                        <li>WIDTH - width in pixels</li>
                                        <li>HEIGHT - height in pixels</li>
                                        <li>CQL_FILTER - query parameter</li>
                                        <li>ENV - additional parameters. e.g. ENV=color%3Acd3844%3Bsize%3A3%3Bopacity%3A0.8
                                            <ul>
                                                <li>color - hex RGB values. e.g. colour:cd3844</li>
                                                <li>size - radius of points in pixels</li>
                                                <li>opacity - opacity value 0 - 1</li>
                                                <li>sel - fq parameter applied to CQL_FILTER.  Matching occurrences will be highlighted on the map in a Red circle</li>
                                                <li>uncertainty - presence of the uncertainty parameter draws uncertainty circles to a fixed maximum of 30km</li>
                                                <li>colormode - facet colouring type.  <br>
                                                    <table>
                                                        <tr><td>colourmode</td><td>description</td></tr>
                                                        <tr><td>-1</td><td>(default) use color value</td></tr>
                                                        <tr><td>grid</td><td>map as density grid.  Grid cells drawn are not restricted to within any query WKT parameters.</td></tr>
                                                        <tr><td>facetname</td><td>colour as categories in a facet</td></tr>
                                                        <tr><td>facetname,cutpoints</td><td>colour as range in a facet using the supplied
                                                                comma separated cutpoints.  4 to 10 values are required.  Include minimum and maximum.
                                                                Minimum and maximum values do not need to be accurate.
                                                                e.g. colormode:year,1800,1900,1950,1970,1990,2010</td></tr>
                                                    </table>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                <li><strong>Legend:</strong> /webportal/legend <br>
                                    Get a CSV legend.<br>
                                    Parameters:
                                    <ul>
                                        <li>q - CQL_FILTER value</li>
                                        <li>cm - ENV colormode value</li>
                                    </ul>
                                    Contains columns:
                                    <ul>
                                        <li>name - legend item name</li>
                                        <li>red - 0-255</li>
                                        <li>green - 0-255</li>
                                        <li>blue - 0-255</li>
                                        <li>count - number of occurrences for this legend category in the q parameter</li>
                                    </ul>
                                </li>
                            </ul>

                            <h3>Analysis Web Services</h3>
                            <p>There are three stages in using analysis web services; Start analysis, Monitor analysis, Retrieve output.</p>
                            <ul>
                                <li><strong>MaxEnt Prediction</strong>
                                    <p>Start Maxent /ws/maxent/procesgeoq, POST request.  E.g. http://spatial.ala.org.au/alaspatial/ws/maxent/processgeoq</p>
                                    Parameters:
                                    <ul>
                                        <li>taxonid - this will be the name of maxent model.  E.g. “Macropus Rufus”.</li>
                                        <li>taxonlsid – Life Science Identifier that is required but not currently used.  E.g. “urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537”.</li>
                                        <li>species – A csv file with a header record and containing all species points.  Column order is species name, longitude (decimal degrees), latitude (decimal degrees). E.g.
                                                    “Species,longitude,latitude
                                                    Macropus Rufus,122,-20
                                                    Macropus Rufus,123,-18”.
                                        </li>
                                        <li>area - bounding area in Well Known Text (WKT) format.  E.g.  “POLYGON((118 -30,146 -30,146 -11,118 -11,118 -30))”.</li>
                                        <li>envlist – a list of environmental and contextual layers as colon separated short names.  E.g. “bioclim_bio1:bioclim_bio12:bioclim_bio2:landuse”.
                                            <ul>
                                                <li>List of analysis valid environmental layer short names <a href="http://spatial.ala.org.au/alaspatial/ws/spatial/settings/layers/environmental/string">here</a>. These are a subset of all layers <a href="http://spatial.ala.org.au/layers.">here</a></li>
                                                <li>List of analysis valid contextual layers; landcover, landuse, vast, native_veg, present_veg </li>
                                            </ul>
                                        </li>
                                        <li>txtTestPercentage - optional percentage of records dedicated to testing.  E.g. “23”.</li>
                                        <li>chkJackKnife - optional parameter to enable/disable Jacknifing.  E.g. “Y”.</li>
                                        <li>chkResponseCurves – optional parameter to enable/disable plots of response curves.  E.g. “Y”.</li>
                                    </ul>
                                    <br>
                                    <p>Returns: analysis id.  E.g. “123”.</p>
                                </li>
                                <li><strong>Classification (ALOC)</strong>
                                    <p>Start ALOC /ws/aloc/processgeoq, POST request.  E.g. http://spatial.ala.org.au/alaspatial/ws/aloc/processgeoq</p>
                                    Parameters:
                                    <ul>
                                        <li>gc - number of groups to try and produce. No guarantee that convergence to the exact number will occur. If not, it will generate as close a number of groups as possible.  E.g. “20”.</li>
                                        <li>area - bounding area in Well Known Text (WKT) format.  E.g.  “POLYGON((118 -30,146 -30,146 -11,118 -11,118 -30))”.</li>
                                        <li>envlist – a list of environmental layers as colon separated short names.  E.g. “bioclim_bio1:bioclim_bio12:bioclim_bio2”.
                                            <ul>
                                                <li>List of analysis valid environmental layer short names <a href="http://spatial.ala.org.au/alaspatial/ws/spatial/settings/layers/environmental/string">here</a>. These are a subset of all layers <a href="http://spatial.ala.org.au/layers.">here</a></li>
                                                <li>List of analysis valid contextual layers; landcover, landuse, vast, native_veg, present_veg </li>
                                            </ul>
                                        </li>
                                    </ul>
                                    <br>
                                    <p>Returns: analysis id.  E.g. “123”.</p>
                                </li>
                                <li><strong>Monitor Analysis</strong>
                                    <ul>
                                        <li>/ws/jobs/state?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/state?pid=123
                                                <br>returns one of "WAITING", "RUNNING", "SUCCESSFUL", "FAILED", "CANCELLED", </li>
                                        <li>/ws/jobs/message?pid=&lt;analysis id>&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/message?pid=123
                                                <br>returns any associated message or "job does not exist".</li>
                                        <li>/ws/jobs/status?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/status?pid=123
                                                <br>returns status text that may contain an estimate of time remaining or "job does not exist".</li>
                                        <li>/ws/jobs/progress?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/progress?pid=123
                                                <br>returns analysis job progress as a number between 0 and 1 or "job does not exist".</li>
                                        <li>/ws/jobs/log?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/log?pid=123
                                                <br>returns analysis job log or "job does not exist".</li>
                                        <li>/ws/jobs/cancel?pid=&lt;analysis id&gt;.  E.g. http://spatial.ala.org.au/alaspatial/ws/jobs/cancel?pid=123
                                                <br>returns nothing if successful or "job does not exist"</li>
                                    </ul>
                                </li>
                                <br>
                                <li><strong>Retrieving Results</strong>
                                    <ul>
                                        <li>/ws/download/&lt;analysis id&gt;.  E.g. .  E.g. http://spatial.ala.org.au/alaspatial/ws/download/123
                                            <br>downloads the zipped output of "SUCCESSFUL" analysis.</li>
                                        <li>ALOC WMS service for the layer is /geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:aloc_&lt;analysis id&gt;&styles=alastyles&FORMAT=image%2Fpng.
                                            <br> E.g. http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:aloc_123&height=200&width=200
                                            <li>Maxent WMS service for the layer is /geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_&lt;analysis id&gt;&styles=alastyles&FORMAT=image%2Fpng.  
                                                <br>E.g. http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:species_123&height=200&width=200
                                             </li>
                                    </ul>
                                </li>
                            </ul>
 
                        </div>

                    </div><!--close content-->
                </div><!--close border-->
            </div><!--close wrapper_border-->
            <div id="footer">
                <div id='footer-nav'><ul id='menu-footer-site'><li id='menu-item-5064' class='menu-item menu-item-type-post_type menu-item-5064'><a href='http://www.ala.org.au'>Home</a></li><li id='menu-item-8093' class='menu-item menu-item-type-post_type current-menu-item page_item page-item-883 current_page_item menu-item-8093'><a href='http://www.ala.org.au/explore/'>Explore</a></li><li id='menu-item-5065' class='menu-item menu-item-type-post_type menu-item-5065'><a href='http://www.ala.org.au/tools-services/'>Tools</a></li><li id='menu-item-8092' class='menu-item menu-item-type-post_type menu-item-8092'><a href='http://www.ala.org.au/share/'>Share</a></li><li id='menu-item-1066' class='menu-item menu-item-type-post_type menu-item-1066'><a href='http://www.ala.org.au/support/'>Support</a></li><li id='menu-item-1067' class='menu-item menu-item-type-post_type menu-item-1067'><a href='http://www.ala.org.au/support/contact-us/'>Contact Us</a></li><li id='menu-item-5068' class='menu-item menu-item-type-post_type menu-item-5068'><a href='http://www.ala.org.au/about/'>About the Atlas</a></li><li id='menu-item-10433' class='last menu-item menu-item-type-post_type menu-item-10433'><a href='http://www.ala.org.au/my-profile/'>My Profile</a></li></ul><ul id='menu-footer-legal'><li id='menu-item-1045' class='menu-item menu-item-type-post_type menu-item-1045'><a href='http://www.ala.org.au/about/terms-of-use/'>Terms of Use</a></li><li id='menu-item-1042' class='menu-item menu-item-type-post_type menu-item-1042'><a href='http://www.ala.org.au/about/terms-of-use/citing-the-atlas/'>Citing the Atlas</a></li><li id='menu-item-12256' class='menu-item menu-item-type-post_type menu-item-12256'><a href='http://www.ala.org.au/about/privacy-policy'>Privacy Policy</a></li><li id='menu-item-3090' class='last menu-item menu-item-type-post_type menu-item-3090'><a href='http://www.ala.org.au/site-map/'>Site Map</a></li></ul></div><div class='copyright'><p><a href='http://creativecommons.org/licenses/by/3.0/au/' title='External link to Creative Commons' class='left no-pipe'><img src='http://www.ala.org.au/wp-content/themes/ala/images/creativecommons.png' width='88' height='31' alt=''></a>This site is licensed under a <a href='http://creativecommons.org/licenses/by/3.0/au/' title='External link to Creative Commons'>Creative Commons Attribution 3.0 Australia License</a></p><p>Provider content may be covered by other <span class='asterisk-container'><a href='http://www.ala.org.au/about/terms-of-use/' title='Terms of Use'>Terms of Use</a>.</span></p></div><script type='text/javascript'> var gaJsHost = (('https:' == document.location.protocol) ? 'https://ssl.' : 'http://www.');document.write(unescape('%3Cscript src="' + gaJsHost + 'google-analytics.com/ga.js" type="text/javascript"%3E%3C/script%3E'));</script> <script type='text/javascript'> try{var pageTracker = _gat._getTracker('UA-4355440-1');pageTracker._initData();pageTracker._trackPageview();} catch(err) {}</script>

            </div><!--close footer-->
        </div><!--close wrapper-->
    </body>
</html>