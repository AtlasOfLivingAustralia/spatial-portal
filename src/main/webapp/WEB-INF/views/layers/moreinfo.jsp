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
        <link rel='up' title='Support' href='http://www.ala.org.au/support/' />
        <link rel='prev' title='Support' href='http://www.ala.org.au/support/' />
        <link rel='next' title='Frequently Asked Questions' href='http://www.ala.org.au/support/faq/' />
        <meta name="generator" content="WordPress 3.0.1" />
        <link rel='canonical' href='http://www.ala.org.au/support/user-feedback/' />
        <script type="text/javascript">
            //<![CDATA[
            var _wpcf7 = { cached: 1 };
            //]]>
        </script>

        <!-- WP Menubar 4.8: start CSS -->
        <!-- WP Menubar 4.8: end CSS -->
        <style type="text/css">.broken_link, a.broken_link {
                text-decoration: line-through;
            }</style>	<script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.dimensions.js"></script>

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
        <style type="text/css">
            .title {font-weight: bold}
        </style>
    </head>
    <body id="page-layerinfo" class="">
        <div id="wrapper">
            <div id="banner">
                <div id="logo">

                    <a href="http://www.ala.org.au" title="Atlas of Living Australia home"><img src="http://www.ala.org.au/wp-content/themes/ala/images/ala_logo.png" width="215" height="80" alt="Atlas of Living Ausralia logo" /></a>
                </div><!--close logo-->
                <div id="nav">
                    <!-- WP Menubar 4.8: start menu nav-site-loggedout, template Superfish, CSS  -->


                    <ul class="sf"><li class="nav-home"><a href="http://www.ala.org.au/" ><span>Home</span></a></li><li class="nav-explore"><a href="http://www.ala.org.au/explore/" ><span>Explore</span></a><ul><li><a href="http://biocache.ala.org.au/explore/your-area" ><span>Your Area</span></a></li><li><a href="http://bie.ala.org.au/regions/" ><span>Regions</span></a></li><li><a href="http://www.ala.org.au/explore/species-maps/" ><span>Species Maps</span></a></li><li><a href="http://collections.ala.org.au/public/map" ><span>Natural History Collections</span></a></li><li><a href="http://www.ala.org.au/explore/themes/" ><span>Themes & Case Studies</span></a></li></ul></li><li class="nav-tools"><a href="http://www.ala.org.au/tools-services/" ><span>Tools</span></a><ul><li><a href="http://www.ala.org.au/tools-services/citizen-science/" ><span>Citizen Science</span></a></li><li><a href="http://www.ala.org.au/tools-services/identification-tools/" ><span>Identification Tools</span></a></li><li><a href="http://www.ala.org.au/tools-services/sds/" ><span>Sensitive Data Service</span></a></li><li><a href="http://www.ala.org.au/tools-services/spatial-analysis/" ><span>Spatial Analysis</span></a></li><li><a href="http://www.ala.org.au/tools-services/species-name-services/" ><span>Taxon Web Services</span></a></li><li><a href="http://www.ala.org.au/tools-services/images/" ><span>Images</span></a></li><li><a href="http://www.ala.org.au/tools-services/onlinedesktop-tools-review/" ><span>Online & Desktop Tools Review </span></a></li></ul></li><li class="nav-share"><a href="http://www.ala.org.au/share/" title="Share - links, images, images, literature, your time"><span>Share</span></a><ul><li><a href="http://www.ala.org.au/share/share-links/" ><span>Share links, ideas, information</span></a></li><li><a href="http://www.ala.org.au/share/share-data/" ><span>Share Datasets</span></a></li><li><a href="http://www.ala.org.au/share/about-sharing/" ><span>About Sharing</span></a></li></ul></li><li class="nav-support"><a href="http://www.ala.org.au/support/" ><span>Support</span></a><ul><li><a href="http://www.ala.org.au/support/contact-us/" ><span>Contact Us</span></a></li><li><a href="http://www.ala.org.au/support/get-started/" ><span>Get Started</span></a></li><li><a href="http://www.ala.org.au/support/user-feedback/" ><span>User Feedback</span></a></li><li><a href="http://www.ala.org.au/support/faq/" ><span>Frequently Asked Questions</span></a></li></ul></li><li class="nav-contact"><a href="http://www.ala.org.au/support/contact-us/" ><span>Contact Us</span></a></li><li class="nav-about"><a href="http://www.ala.org.au/about/" ><span>About the Atlas</span></a><ul><li><a href="http://www.ala.org.au/about/progress/" ><span>A Work In Progress</span></a></li><li><a href="http://www.ala.org.au/about/people/" ><span>Working Together</span></a></li><li><a href="http://www.ala.org.au/about/contributors/" ><span>Atlas Contributors</span></a></li><li><a href="http://www.ala.org.au/about/project-time-line/" ><span>Project Time Line</span></a></li><li><a href="http://www.ala.org.au/about/program-of-projects/" ><span>Atlas Projects</span></a></li><li><a href="http://www.ala.org.au/about/international-collaborations/" ><span>Associated Projects</span></a></li><li><a href="http://www.ala.org.au/about/communications-centre/" ><span>Communications Centre</span></a></li><li><a href="http://www.ala.org.au/about/governance/" ><span>Atlas Governance</span></a></li><li><a href="http://www.ala.org.au/about/terms-of-use/" ><span>Terms of Use</span></a></li></ul></li><li class="nav-myprofile nav-right"><a href="https://auth.ala.org.au/cas/login?service=http://www.ala.org.au/wp-login.php?redirect_to=http://www.ala.org.au/my-profile/" ><span>My Profile</span></a></li><li class="nav-login nav-right"><a href="https://auth.ala.org.au/cas/login?service=http://www.ala.org.au/wp-login.php?redirect_to=http://www.ala.org.au/" ><span>Log in</span></a></li></ul>

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


                <div class="section">
                    <c:choose>
                        <c:when test="${layer != null}">

                            <div id="header">
                                <div id="breadcrumb"><a href="http://www.ala.org.au">Home</a> <a href="http://www.ala.org.au/explore/species-maps/">Spatial Portal</a> <a href="http://spatial.ala.org.au/alaspatial/layers/">Layer list</a> ${layer.displayname}</div>
                                <h1>${layer.displayname}</h1>
                            </div><!--close header-->


                            <p>
                                <span class="title">Description:</span> <br />
                                ${layer.description}
                            </p>
                            <p>
                                <span class="title">Short Name:</span> <br />
                                ${layer.name}
                            </p>
                            <p>
                                <span class="title">Metadata contact organization:</span> <br />
                                <a href="${layer.sourcelink}" target="_blank">${layer.source}</a>
                            </p>

                            <p>
                                <span class="title">Organisation role: </span> <br />
                                ${layer.resppartyrole}
                            </p>

                            <p>
                                <span class="title">Metadata date:</span> <br />
                                ${layer.mddatest}
                            </p>

                            <p>
                                <span class="title">Reference date:</span> <br />
                                ${layer.citationdate}
                            </p>

                            <p>
                                <span class="title">Resource constraints:</span> 
                            </p>
                            <ul>
                                <li>Licence level: ${layer.licencelevel} </li>
                                <li>Licence info: <a href="${layer.licence_link}" target="_blank">${layer.licence_link}</a> </li>
                            </ul>

                            <p>
                                <span class="title">Licence notes:</span> <br />
                                ${layer.licence_notes}
                            </p>

                            <p>
                                <span class="title">Type:</span> <br />
                                <c:choose>
                                    <c:when test="${layer.type eq 'Environmental'}">
									Environmental (gridded) ${layer.scale}
                                    </c:when>
                                    <c:when test="${layer.type eq 'Contextual'}">
									Contextual (polygonal) ${layer.scale}
                                    </c:when>
                                    <c:otherwise>
                                        ${layer.type} ${layer.scale}
                                    </c:otherwise>
                                </c:choose>
                            </p>

                            <p>
                                <span class="title">Classification:</span> <br />
                                ${layer.classification1}
                                <c:if test="${fn:length(layer.classification2) > 0}">
                                    &rArr; ${layer.classification2}
                                </c:if>
                            </p>

                            <p>
                                <span class="title">Units: </span> <br />
                                ${layer.environmentalvalueunits}
                            </p>

                            <p>
                                <span class="title">Data language: </span> <br />
                                ${layer.datalang}
                            </p>

                            <p>
                                <span class="title">Scope: </span> <br />
                                ${layer.mdhrlv}
                            </p>

                            <p>
                                <span class="title">Notes:</span> <br />
                                ${layer.notes}
                            </p>

                            <p>
                                <span class="title">Keywords:</span> <br />
                                ${layer.keywords}
                            </p>

                            <p>
                                <span class="title">More information:</span> <br />
                                <c:forEach var="u" items="${fn:split(layer.metadatapath, '|')}">
                                    <a href="${u}">${u}</a><br />
                                </c:forEach>
                            </p>

                        </c:when>
                        <c:otherwise>
                            <div id="header">
                                <div id="breadcrumb"><a href="http://www.ala.org.au">Home</a> <a href="http://www.ala.org.au/explore/species-maps/">Spatial Portal</a> <a href="http://spatial.ala.org.au/alaspatial/layers/">Layer list</a> Layer</div>
                                <h1>Layer information not available</h1>
                            </div><!--close header-->
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
                        <li id="menu-item-12256" class="menu-item menu-item-type-custom menu-item-12256"><a href="/about/privacy-policy">Privacy Policy</a></li>

                        <li id="menu-item-3090" class="last menu-item menu-item-type-post_type menu-item-3090"><a href="http://www.ala.org.au/site-map/">Site Map</a></li>
                    </ul>		</div>
                <div class="copyright"><p><a href="http://creativecommons.org/licenses/by/3.0/au/" title="External link to Creative Commons" class="left no-pipe"><img src="http://www.ala.org.au/wp-content/themes/ala/images/creativecommons.png" width="88" height="31" alt="" /></a>This site is licensed under a <a href="http://creativecommons.org/licenses/by/3.0/au/" title="External link to Creative Commons">Creative Commons Attribution 3.0 Australia License</a></p><p>Provider content may be covered by other <span class="asterisk-container"><a href="http://www.ala.org.au/about/terms-of-use/" title="Terms of Use">Terms of Use</a>.</span></div>
            </div><!--close footer-->
        </div><!--close wrapper-->
        <script type='text/javascript' src='http://www.ala.org.au/wp-includes/js/jquery/jquery.form.js?ver=2.02m'></script>

        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/contact-form-7/scripts.js?ver=2.3.1'></script>
    </body>
</html>
