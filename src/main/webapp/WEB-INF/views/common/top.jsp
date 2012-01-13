<%-- 
    Document   : top
    Created on : Dec 7, 2011, 10:33:50 AM
    Author     : ajay
--%>
<!DOCTYPE html>
<html dir="ltr" lang="en-US">

    <head profile="http://gmpg.org/xfn/11">
        <meta name="google-site-verification" content="MdnA79C1YfZ6Yx2qYOXWi_TYFfUvEJOQAmHNaeEWIts" />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="description" content="The Atlas of Living Australia provides tools to enable users of biodiversity information to find, access, combine and visualise data on Australian plants and animals" />
        <title>Spatial | Atlas of Living Australia</title>

        <link rel="stylesheet" href="/layers-service/includes/style.css" type="text/css" media="screen" />
        <link rel="stylesheet" href="/layers-service/includes/css/wp-styles.css" type="text/css" media="screen" />
        <link rel="stylesheet" href="/layers-service/includes/css/buttons.css" type="text/css" media="screen" />
        <link rel="icon" type="image/x-icon" href="/layers-service/includes/images/favicon.ico" />
        <link rel="shortcut icon" type="image/x-icon" href="/layers-service/includes/images/favicon.ico" />

        <link rel="stylesheet" type="text/css" media="screen" href="/layers-service/includes/css/jquery.autocomplete.css" />

        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/search.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala/css/skin.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="/layers-service/includes/css/sf.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="/actions/static/styles/demo_table.css" />
        <style type="text/css">
            .dataTables_info {
                width: 60%;
                float: left;
            }
        </style>

        <script type="text/javascript" src="/layers-service/includes/scripts/html5.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/iframe.js"></script>
        <link rel="alternate" type="application/rss+xml" title="Atlas of Living Australia &raquo; Feed" href="http://www.ala.org.au/feed/" />
        <link rel="alternate" type="application/rss+xml" title="Atlas of Living Australia &raquo; Comments Feed" href="http://www.ala.org.au/comments/feed/" />
        <link rel="alternate" type="application/rss+xml" title="Atlas of Living Australia &raquo; Layer list Comments Feed" href="http://www.ala.org.au/mapping-analysis/layer-list/feed/" />
        <link rel='stylesheet' id='commentvalidation-css'  href='http://www.ala.org.au/wp-content/plugins/comment-validation/comment-validation.css?ver=3.2.1' type='text/css' media='all' />

        <link rel='stylesheet' id='contact-form-7-css'  href='http://www.ala.org.au/wp-content/plugins/contact-form-7/styles.css?ver=2.3.1' type='text/css' media='all' />
        <link rel='stylesheet' id='jquery.lightbox.min.css-css'  href='http://www.ala.org.au/wp-content/plugins/wp-jquery-lightbox/lightbox.min.css?ver=1.2' type='text/css' media='all' />
        <script type='text/javascript' src='http://www.ala.org.au/wp-includes/js/l10n.js?ver=20101110'></script>
        <script type='text/javascript' src='https://ajax.googleapis.com/ajax/libs/jquery/1.7.0/jquery.min.js'></script>
        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/comment-validation/jquery.validate.pack.js?ver=3.2.1'></script>
        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/comment-validation/comment-validation.js?ver=3.2.1'></script>
        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/menubar-templates/Superfish/superfish.js?ver=3.2.1'></script>
        <link rel="EditURI" type="application/rsd+xml" title="RSD" href="http://www.ala.org.au/xmlrpc.php?rsd" />
        <link rel="wlwmanifest" type="application/wlwmanifest+xml" href="http://www.ala.org.au/wp-includes/wlwmanifest.xml" />
        <link rel='index' title='Atlas of Living Australia' href='http://www.ala.org.au/' />
        <link rel='up' title='Mapping &amp; analysis' href='http://www.ala.org.au/mapping-analysis/' />
        <link rel='prev' title='bkThe Collection Manager Story' href='http://www.ala.org.au/about-home/digitisation-guidance/the-collection-manager-story/' />

        <link rel='next' title='Department of Sustainability, Environment, Water, Population and Communities' href='http://www.ala.org.au/natural-history-collections/department-of-sustainability-environment-water-population-and-communities/' />
        <meta name="generator" content="WordPress 3.2.1" />
        <link rel='canonical' href='http://www.ala.org.au/mapping-analysis/layer-list/' />

        <!-- WP Menubar 4.8: start CSS -->
        <!-- WP Menubar 4.8: end CSS -->
        <script src="http://cdn.jquerytools.org/1.2.6/full/jquery.tools.min.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.dimensions.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.mousewheel.min.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/hoverintent-min.js"></script>

        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/superfish/superfish.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery.autocomplete.js"></script>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/uservoice.js"></script>
        <script type="text/javascript">

            // initialise plugins

            jQuery(function(){
                jQuery('ul.sf').superfish( {
                    delay:500,
                    autoArrows:false,
                    dropShadows:false
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

                jQuery("ul.button-tabs").tabs("div.panes > ul"), { history: true,effect: 'fade' };
            });
        </script>

    </head>
    <!-- <body id="page-datasets" class="page-template-landing_wo_search-php"> -->
    <c:choose>
        <c:when test="${param.fluid}">
            <body id="page-spatial" class="fluid">
        </c:when>
        <c:otherwise>
            <body id="page-spatial" class="">
        </c:otherwise>
    </c:choose>
    <header id="site-header">
        <div class="inner">

            <h1 title="Atlas of Living Australia"><a href="http://www.ala.org.au" title="Atlas of Living Australia home"><img src="/layers-service/includes/images/logo.png" width="315" height="33" alt="" /></a></h1>
            <section id="nav-search">
                <section id="header-search">
                    <form id="search-form" action="http://bie.ala.org.au/search" method="get" name="search-form"><label for="search">Search</label>
                        <input id="search" class="filled" title="Search" type="text" name="q" placeholder="Search the Atlas" />
                        <span class="search-button-wrapper"><button id="search-button" class="search-button" value="Search" type="submit"><img src="/layers-service/includes/images/button_search-grey.png" alt="Search" width="12" height="12" /></button></span></form>
                </section>
                <nav>

                    <ol>
                        <li><a href="http://www.ala.org.au" title="Atlas of Living Australia home">Home</a></li>
                        <li class="last"><a href="https://auth.ala.org.au/cas/login?service=http://www.ala.org.au/wp-login.php?redirect_to=http://www.ala.org.au/my-profile/">Log in</a></li>
                    </ol>
                </nav>
            </section>
        </div>
    </header>

    <nav id="nav-site">
        <!-- WP Menubar 4.8: start menu nav-site, template Superfish, CSS  -->


        <ul class="sf">
            <li class="nav-species"><a href="http://test.ala.org.au/australias-species/" >Species</a></li>
            <li class="nav-locations"><a href="http://test.ala.org.au/species-by-location/" >Locations</a>
            </li><li class="nav-collections"><a href="http://test.ala.org.au/natural-history-collections/" >Collections</a></li>
            <li class="nav-mapping"><a href="http://spatial.ala.org.au" >Mapping & analysis</a></li>
            <li class="nav-datasets"><a href="http://test.ala.org.au/data-sets/" >Data sets</a></li>
            <li class="nav-blogs"><a href="http://test.ala.org.au/blogs-news/" >Blogs</a></li>
            <li class="nav-getinvolved"><a href="http://test.ala.org.au/get-involved/" >Get involved</a></li>
            <li class="nav-about"><a href="http://test.ala.org.au/about-home/" >About the Atlas</a>
                <ul>
                    <li><a href="http://test.ala.org.au/about-home/atlas-background/" >bkAtlas Background</a></li>
                    <li><a href="http://test.ala.org.au/about-home/our-data/" >bkOur Data</a></li>
                    <li><a href="http://test.ala.org.au/about-home/our-data-providers/" >bkOur Data Providers</a></li>
                    <li><a href="http://test.ala.org.au/about-home/how-we-integrate-data/" >bkHow we Integrate Data</a></li>
                    <li><a href="http://test.ala.org.au/about-home/downloadable-tools/" >bkDownloadable Tools</a></li>
                    <li><a href="http://test.ala.org.au/about-home/digitisation-guidance/" >bkDigitisation Guidance</a></li>
                    <li><a href="http://test.ala.org.au/about-home/communications-centre/" >bkCommunications Centre</a></li>
                    <li><a href="http://test.ala.org.au/about-home/terms-of-use/" >bkTerms of Use</a></li>
                    <li><a href="http://test.ala.org.au/about-home/contact-us/" >bkContact Us</a></li>
                </ul>
            </li>
        </ul>

        <!-- WP Menubar 4.8: end menu nav-site, template Superfish, CSS  -->
    </nav>
