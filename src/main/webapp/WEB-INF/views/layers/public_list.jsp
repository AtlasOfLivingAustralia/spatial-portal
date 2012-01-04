<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!DOCTYPE html>
<html dir="ltr" lang="en-US">

    <head profile="http://gmpg.org/xfn/11">
        <meta name="google-site-verification" content="MdnA79C1YfZ6Yx2qYOXWi_TYFfUvEJOQAmHNaeEWIts" />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="description" content="The Atlas of Living Australia provides tools to enable users of biodiversity information to find, access, combine and visualise data on Australian plants and animals"/>
        <title>Layer list |  Atlas of Living Australia</title>

        <link rel="stylesheet" href="http://www.ala.org.au/wp-content/themes/ala/style.css" type="text/css" media="screen" />
        <link rel="icon" type="image/x-icon" href="http://www.ala.org.au/wp-content/themes/ala/images/favicon.ico" />
        <link rel="shortcut icon" type="image/x-icon" href="http://www.ala.org.au/wp-content/themes/ala/images/favicon.ico" />

        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/sf.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/highlights.css" />

        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/jquery.autocomplete.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/search.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/skin.css" />
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/iframe.js"></script>
        <link rel="alternate" type="application/rss+xml" title="Atlas of Living Australia &raquo; Feed" href="http://www.ala.org.au/feed/" />
        <link rel="alternate" type="application/rss+xml" title="Atlas of Living Australia &raquo; Comments Feed" href="http://www.ala.org.au/comments/feed/" />
        <link rel='stylesheet' id='contact-form-7-css'  href='http://www.ala.org.au/wp-content/plugins/contact-form-7/styles.css?ver=2.3.1' type='text/css' media='all' />
        <script type='text/javascript' src='http://www.ala.org.au/wp-includes/js/jquery/jquery.js?ver=1.4.2'></script>
        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/menubar-templates/Superfish/superfish.js?ver=3.0.1'></script>

        <link rel="EditURI" type="application/rsd+xml" title="RSD" href="http://www.ala.org.au/xmlrpc.php?rsd" />
        <link rel="wlwmanifest" type="application/wlwmanifest+xml" href="http://www.ala.org.au/wp-includes/wlwmanifest.xml" />
        <link rel='index' title='Atlas of Living Australia' href='http://www.ala.org.au/' />
        <link rel='up' title='Explore' href='http://www.ala.org.au/explore/' />
        <link rel='prev' title='Share Photos' href='http://www.ala.org.au/share-images/' />
        <link rel='next' title='Species Maps Help' href='http://www.ala.org.au/explore/species-maps/species-maps-help/' />
        <meta name="generator" content="ALA Spatial Portal" />
        <link rel='canonical' href='http://www.ala.org.au/explore/species-maps/' />
        <script type="text/javascript">
            //<![CDATA[
            var _wpcf7 = { cached: 1 };
            //]]>
        </script>

        <!-- WP Menubar 4.8: start CSS -->
        <!-- WP Menubar 4.8: end CSS -->
        <style type="text/css">.broken_link, a.broken_link {
                text-decoration: line-through;
            }</style>	
        <!-- This style is used to (force) wrap a word correctly in a table -->
        <style type="text/css">.wrapword{
                white-space: -moz-pre-wrap !important;  /* Mozilla, since 1999 */
                white-space: -pre-wrap;      /* Opera 4-6 */
                white-space: -o-pre-wrap;    /* Opera 7 */
                white-space: pre-wrap;       /* css-3 */
                word-wrap: break-word;       /* Internet Explorer 5.5+ */
            }</style>

        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.dimensions.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.mousewheel.min.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/hoverintent-min.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/superfish/superfish.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.autocomplete.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/uservoice.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.jcarousel.min.js"></script>

        <link rel="stylesheet" type="text/css" href="http://www.ala.org.au/wp-content/themes/ala/scripts/fancybox/jquery.fancybox-1.3.1.css" media="screen" />
        <script type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/fancybox/jquery.fancybox-1.3.1.pack.js"></script>

        <script type="text/javascript">
            jQuery(document).ready(function($){
                $("a#asterisk").fancybox({
                    'hideOnContentClick' : false,
                    'titleShow' : false,
                    'autoDimensions' : false,
                    'width' : 600,
                    'height' : 350
                });
                $("a.pop-up").fancybox({
                    'hideOnContentClick' : false,
                    'titleShow' : false,
                    'autoDimensions' : false,
                    'width' : 550,
                    'height' : 100
                });
            });
        </script>
        <script type="text/javascript">

            // initialise plugins
            jQuery(function(){
                jQuery('ul.sf').superfish( {
                    delay:500,
                    autoArrows:false,
                    dropShadows:false
                });
                /**
                 * We use the initCallback callback
                 * to assign functionality to the controls
                 */
                function mycarousel_initCallback(carousel) {
                    jQuery('.jcarousel-control a').bind('click', function() {
                        carousel.scroll(jQuery.jcarousel.intval(jQuery(this).text()));
                        return false;
                    });

                    jQuery('#mycarousel-next').bind('click', function() {
                        carousel.next();
                        return false;
                    });

                    jQuery('#mycarousel-prev').bind('click', function() {
                        carousel.prev();
                        return false;
                    });
                };

                // Ride the carousel...
                jQuery(document).ready(function() {
                    jQuery("#mycarousel").jcarousel({
                        scroll: 6,
                        initCallback: mycarousel_initCallback
                        // This tells jCarousel NOT to autobuild prev/next buttons
                        //buttonNextHTML: null,
                        //buttonPrevHTML: null
                    });
                });

                jQuery("form#search-form input#search").autocomplete('http://bie.ala.org.au/search/auto.jsonp', {
                    extraParams: {limit: 100},
                    dataType: 'jsonp',
                    parse: function(data) {
                        var rows = new Array();
                        data = data.autoCompleteList;
                        for(var i=0; i<data.length; i++){
                            rows[i] = {
                                data:data[i],
                                value: data[i].matchedNames[0],
                                result: data[i].matchedNames[0]
                            };
                        }
                        return rows;
                    },
                    matchSubset: false,
                    formatItem: function(row, i, n) {
                        return row.matchedNames[0];
                    },
                    cacheLength: 10,
                    minChars: 3,
                    scroll: false,
                    max: 10,
                    selectFirst: false
                });
                jQuery("form#search-inpage input#search").autocomplete('http://bie.ala.org.au/search/auto.jsonp', {
                    extraParams: {limit: 100},
                    dataType: 'jsonp',
                    parse: function(data) {
                        var rows = new Array();
                        data = data.autoCompleteList;
                        for(var i=0; i<data.length; i++){
                            rows[i] = {
                                data:data[i],
                                value: data[i].matchedNames[0],
                                result: data[i].matchedNames[0]
                            };
                        }
                        return rows;
                    },
                    matchSubset: false,
                    formatItem: function(row, i, n) {
                        return row.matchedNames[0];
                    },
                    cacheLength: 10,
                    minChars: 3,
                    scroll: false,
                    max: 10,
                    selectFirst: false
                });
            });
        </script>
    <body id="page-layerlist" class="">
        <div id="wrapper">
            <div id="banner">
                <div id="logo">

                    <a href="http://www.ala.org.au" title="Atlas of Living Australia home"><img src="http://www.ala.org.au/wp-content/themes/ala/images/ala_logo.png" width="215" height="80" alt="Atlas of Living Ausralia logo" /></a>
                </div><!--close logo-->
                <div id="nav">
                    <!-- WP Menubar 4.8: start menu nav-site-loggedout, template Superfish, CSS  -->


                    <ul class="sf"><li class="nav-home"><a href="http://www.ala.org.au/" ><span>Home</span></a></li><li class="nav-explore selected"><a href="http://www.ala.org.au/explore/" ><span>Explore</span></a><ul><li><a href="http://biocache.ala.org.au/explore/your-area" ><span>Your Area</span></a></li><li><a href="http://bie.ala.org.au/regions/" ><span>Regions</span></a></li><li><a href="http://www.ala.org.au/explore/species-maps/" ><span>Species Maps</span></a></li><li><a href="http://collections.ala.org.au/public/map" ><span>Natural History Collections</span></a></li><li><a href="http://www.ala.org.au/explore/themes/" ><span>Themes & Case Studies</span></a></li></ul></li><li class="nav-tools"><a href="http://www.ala.org.au/tools-services/" ><span>Tools</span></a><ul><li><a href="http://www.ala.org.au/tools-services/citizen-science/" ><span>Citizen Science</span></a></li><li><a href="http://www.ala.org.au/tools-services/identification-tools/" ><span>Identification Tools</span></a></li><li><a href="http://www.ala.org.au/tools-services/sds/" ><span>Sensitive Data Service</span></a></li><li><a href="http://www.ala.org.au/tools-services/spatial-analysis/" ><span>Spatial Analysis</span></a></li><li><a href="http://www.ala.org.au/tools-services/species-name-services/" ><span>Taxon Web Services</span></a></li><li><a href="http://www.ala.org.au/tools-services/images/" ><span>Images</span></a></li><li><a href="http://www.ala.org.au/tools-services/onlinedesktop-tools-review/" ><span>Online & Desktop Tools Review </span></a></li></ul></li><li class="nav-share"><a href="http://www.ala.org.au/share/" title="Share - links, images, images, literature, your time"><span>Share</span></a><ul><li><a href="http://www.ala.org.au/share/share-links/" ><span>Share links, ideas, information</span></a></li><li><a href="http://www.ala.org.au/share/share-data/" ><span>Share Datasets</span></a></li><li><a href="http://www.ala.org.au/share/about-sharing/" ><span>About Sharing</span></a></li></ul></li><li class="nav-support"><a href="http://www.ala.org.au/support/" ><span>Support</span></a><ul><li><a href="http://www.ala.org.au/support/contact-us/" ><span>Contact Us</span></a></li><li><a href="http://www.ala.org.au/support/get-started/" ><span>Get Started</span></a></li><li><a href="http://www.ala.org.au/support/user-feedback/" ><span>User Feedback</span></a></li><li><a href="http://www.ala.org.au/support/faq/" ><span>Frequently Asked Questions</span></a></li></ul></li><li class="nav-contact"><a href="http://www.ala.org.au/support/contact-us/" ><span>Contact Us</span></a></li><li class="nav-about"><a href="http://www.ala.org.au/about/" ><span>About the Atlas</span></a><ul><li><a href="http://www.ala.org.au/about/progress/" ><span>A Work In Progress</span></a></li><li><a href="http://www.ala.org.au/about/people/" ><span>Working Together</span></a></li><li><a href="http://www.ala.org.au/about/contributors/" ><span>Atlas Contributors</span></a></li><li><a href="http://www.ala.org.au/about/progress/" ><span>Project Time Line</span></a></li><li><a href="http://www.ala.org.au/about/program-of-projects/" ><span>Atlas Projects</span></a></li><li><a href="http://www.ala.org.au/about/international-collaborations/" ><span>Associated Projects</span></a></li><li><a href="http://www.ala.org.au/about/communications-centre/" ><span>Communications Centre</span></a></li><li><a href="http://www.ala.org.au/about/governance/" ><span>Atlas Governance</span></a></li><li><a href="http://www.ala.org.au/about/terms-of-use/" ><span>Terms of Use</span></a></li></ul></li><li class="nav-myprofile nav-right"><a href="https://auth.ala.org.au/cas/login?service=http://www.ala.org.au/wp-login.php?redirect_to=http://www.ala.org.au/my-profile/" ><span>My Profile</span></a></li><li class="nav-login nav-right"><a href="https://auth.ala.org.au/cas/login?service=http://www.ala.org.au/wp-login.php?redirect_to=http://www.ala.org.au/" ><span>Log in</span></a></li></ul>

                    <!-- WP Menubar 4.8: end menu nav-site-loggedout, template Superfish, CSS  -->
                </div><!--close nav-->
                <div id="wrapper_search">
                    <form id="search-form" action="http://bie.ala.org.au/search" method="get" name="search-form">
                        <label for="search">Search</label>
                        <input type="text" class="filled" id="search" name="q" title="Search the Atlas" placeholder="Search the Atlas" />
                        <span class="search-button-wrapper"><input type="submit" class="search-button" id="search-button" alt="Search" value="Search" /></span>
                    </form>

                </div><!--close wrapper_search-->
            </div><!--close banner-->
            <div id="loginId" class="hide"></div>
            <div style="display:none; text-align: left;">
                <div id="search_record" style="text-align: left;">
                    <h3>Enter the species' scientific or common name</h3>
                    <div id="inpage_search">
                        <form id="search-inpage" action="http://bie.ala.org.au/search" method="get" name="search-form">

                            <label for="search">Search</label>
                            <input type="text" class="filled" id="search" name="q" placeholder="e.g. Ornithorhynchus anatinus" />
                            <span class="search-button-wrapper"><input type="submit" class="search-button" alt="Search" value="Search" /></span>
                        </form>
                    </div><!--close wrapper_search-->
                </div>
            </div><!--close lightbox-->
            <div id="content">

                <div id="header">
                    <div id="breadcrumb"><a href="http://www.ala.org.au">Home</a> <a href="http://www.ala.org.au/explore/species-maps/">Spatial Portal</a> Layer list</div>
                    <h1>Layer list</h1>
                </div><!--close header-->

                <c:choose>
                    <c:when test="${empty param.q}">
                        <c:set var="searchquery" value="" scope="request" />
                    </c:when>
                    <c:otherwise>
                        <c:set var="searchquery" value="?q=${param.q}" scope="request" />
                    </c:otherwise>
                </c:choose>


                <div class="section">

                    <div>
                        <form action="" method="get">
                            <label for="q">Search layers:</label>
                            <input type="text" id="q" name="q" value="${param.q}" />
                            <input type="submit" class="button" value="GO" />
                            <c:if test="${!empty param.q}">
                                <input type="button" class="button" onclick="location.href='/layers'" value="Display all" />
                            </c:if>
                        </form>
                    </div>

                    <c:choose>
                        <c:when test="${fn:length(layerList) > 0}">
                            <ul>
                                <li>Click on layer name to link to the metadata summary and links to full metadata record.</li>
                                <li>Download as <a href="/layers.csv${searchquery}">CSV</a> | <a href="/layers.json${searchquery}">JSON</a></li>
                                <!--  | <a href="/layers.xml${searchquery}">XML</a> -->
                                <!--<li><a href="/layers.csv">Download as CSV</a></li>-->
                            </ul>
                            <table border="1">
                                <tr style="height:220px">
                                    <th>Classification 1</th>
                                    <th>Classification 2</th>
                                    <th>Display name</th>
                                    <th>Short name</th>
                                    <th>Description</th>
                                    <th>Type</th>
                                    <th>Metadata contact organization</th>
                                    <th>Keywords</th>
                                    <th>Preview</th>
                                    <!-- <th>Reference date</th> -->
                                </tr>
                                <c:forEach items="${layerList}" var="layer" varStatus="status">
                                    <tr>
                                        <td>${layer.classification1}</td>
                                        <td>${layer.classification2}</td>
                                        <td><a href="/layers/more/${layer.name}">${layer.displayname}</a></td>
                                        <td style="max-width:80px" class="wrapword">${layer.name}</td>
                                        <td>${layer.description}</td>
                                        <c:choose>
                                            <c:when test="${layer.type eq 'Environmental'}">
                                                <td>Environmental (gridded) ${layer.scale}</td>
                                            </c:when>
                                            <c:when test="${layer.type eq 'Contextual'}">
                                                <td>Contextual (polygonal) ${layer.scale}</td>
                                            </c:when>
                                            <c:otherwise>
                                                <td>${layer.type} ${layer.scale}</td>
                                            </c:otherwise>
                                        </c:choose>
                                        <td>${layer.source}</td>
                                        <td>${layer.keywords}</td>
                                        <td>
                                            <!--<img src="/output/layerthumbs/ALA:${layer.name}.jpeg" />-->
                                            <img src="/geoserver/wms/reflect?layers=ALA:${layer.name}&width=200&height=200" />
                                            <br />
                                            <!--<a href="/geoserver/wms/kml?layers=ALA:${layer.name}">KML</a>-->
                                        </td>
                                        <!-- <td>${layer.citationdate}</td> -->
                                    </tr>
                                </c:forEach>
                            </table>
                            Download as <a href="/layers.csv${searchquery}">CSV</a> | <a href="/layers.json${searchquery}">JSON</a>
                            <!--  | <a href="/layers.xml${searchquery}">XML</a> -->
                        </c:when>
                        <c:otherwise>
                            <ul><li>No layers available</li></ul>
                        </c:otherwise>
                    </c:choose>
                </div>


            </div><!--close content-->
            <div id="footer">
                <div id="footer-nav">
                    <ul id="menu-footer-site"><li id="menu-item-1046" class="menu-item menu-item-type-post_type menu-item-1046"><a href="http://www.ala.org.au/">Home</a></li>
                        <li id="menu-item-8090" class="menu-item menu-item-type-post_type menu-item-8090"><a href="http://www.ala.org.au/explore/">Explore</a></li>
                        <li id="menu-item-1051" class="menu-item menu-item-type-post_type menu-item-1051"><a href="http://www.ala.org.au/tools-services/">Tools</a></li>

                        <li id="menu-item-8091" class="menu-item menu-item-type-post_type menu-item-8091"><a href="http://www.ala.org.au/share/">Share</a></li>
                        <li id="menu-item-1050" class="menu-item menu-item-type-post_type current-page-ancestor menu-item-1050"><a href="http://www.ala.org.au/support/">Support</a></li>
                        <li id="menu-item-1048" class="menu-item menu-item-type-post_type menu-item-1048"><a href="http://www.ala.org.au/support/contact-us/">Contact Us</a></li>
                        <li id="menu-item-1047" class="menu-item menu-item-type-post_type menu-item-1047"><a href="http://www.ala.org.au/about/">About the Atlas</a></li>
                        <li id="menu-item-1052" class="last menu-item menu-item-type-custom menu-item-1052"><a href="http://www.ala.org.au/wp-login.php">Log in</a></li>
                    </ul>		<ul id="menu-footer-legal"><li id="menu-item-1045" class="menu-item menu-item-type-post_type menu-item-1045"><a href="http://www.ala.org.au/about/terms-of-use/">Terms of Use</a></li>
                        <li id="menu-item-1042" class="menu-item menu-item-type-post_type menu-item-1042"><a href="http://www.ala.org.au/about/terms-of-use/citing-the-atlas/">Citing the Atlas</a></li>
                        <li id="menu-item-12256" class="menu-item menu-item-type-custom menu-item-12256"><a href="http://www.ala.org.au/about/terms-of-use/privacy-policy/">Privacy Policy</a></li>

                        <li id="menu-item-3090" class="last menu-item menu-item-type-post_type menu-item-3090"><a href="http://www.ala.org.au/site-map/">Site Map</a></li>
                    </ul>		</div>
                <div class="copyright"><p><a href="http://creativecommons.org/licenses/by/3.0/au/" title="External link to Creative Commons" class="left no-pipe"><img src="http://www.ala.org.au/wp-content/themes/ala/images/creativecommons.png" width="88" height="31" alt="" /></a>This site is licensed under a <a href="http://creativecommons.org/licenses/by/3.0/au/" title="External link to Creative Commons">Creative Commons Attribution 3.0 Australia License</a></p><p>Provider content may be covered by other <span class="asterisk-container"><a href="http://www.ala.org.au/about/terms-of-use/" title="Terms of Use">Terms of Use</a>.</span></div>
            </div><!--close footer-->
        </div><!--close wrapper-->
        <script type='text/javascript' src='http://www.ala.org.au/wp-includes/js/jquery/jquery.form.js?ver=2.02m'></script>

        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/contact-form-7/scripts.js?ver=2.3.1'></script>
    </body>
</html>
