<%-- 
    Document   : top
    Created on : Dec 7, 2011, 10:33:50 AM
    Author     : ajay
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" %><%@
taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><%@
taglib uri="/tld/ala.tld" prefix="ala" %>
<!DOCTYPE html>
<html dir="ltr" lang="en-US">

    <head profile="http://gmpg.org/xfn/11">
        <meta name="google-site-verification" content="MdnA79C1YfZ6Yx2qYOXWi_TYFfUvEJOQAmHNaeEWIts" />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="description" content="The Atlas of Living Australia provides tools to enable users of biodiversity information to find, access, combine and visualise data on Australian plants and animals" />
        <title>Spatial | Atlas of Living Australia</title>

        <link rel="stylesheet" href="http://www.ala.org.au/wp-content/themes/ala2011/style.css" type="text/css" media="screen" />
        <link rel="stylesheet" href="http://www.ala.org.au/wp-content/themes/ala2011/css/wp-styles.css" type="text/css" media="screen" />
        <link rel="stylesheet" href="http://www.ala.org.au/wp-content/themes/ala2011/css/buttons.css" type="text/css" media="screen" />
        <link rel="icon" type="image/x-icon" href="http://www.ala.org.au/wp-content/themes/ala2011/images/favicon.ico" />
        <link rel="shortcut icon" type="image/x-icon" href="http://www.ala.org.au/wp-content/themes/ala2011/images/favicon.ico" />

        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala2011/css/jquery.autocomplete.css" />

        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala2011/css/search.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala2011/css/skin.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="http://www.ala.org.au/wp-content/themes/ala2011/css/sf.css" />

        <link rel="stylesheet" type="text/css" media="screen" href="/layers-service/includes/css/sf.css" />
        <link rel="stylesheet" type="text/css" media="screen" href="/actions/static/styles/demo_table.css" />
        <style type="text/css">
            .dataTables_info {
                width: 60%;
                float: left;
            }
        </style>

        <script type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala2011/scripts/html5.js"></script>
        <link rel="alternate" type="application/rss+xml" title="Atlas of Living Australia &raquo; Feed" href="http://www.ala.org.au/feed/" />
        <link rel="alternate" type="application/rss+xml" title="Atlas of Living Australia &raquo; Comments Feed" href="http://www.ala.org.au/comments/feed/" />
        <link rel="alternate" type="application/rss+xml" title="Atlas of Living Australia &raquo; Australia&#8217;s species Comments Feed" href="http://www.ala.org.au/australias-species/feed/" />
        <link rel='stylesheet' id='commentvalidation-css'  href='http://www.ala.org.au/wp-content/plugins/comment-validation/comment-validation.css?ver=3.3.1' type='text/css' media='all' />
        <link rel='stylesheet' id='jquery.lightbox.min.css-css'  href='http://www.ala.org.au/wp-content/plugins/wp-jquery-lightbox/lightbox.min.css?ver=1.2' type='text/css' media='all' />

        <script type='text/javascript' src='http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js?ver=3.3.1'></script>
        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/comment-validation/jquery.validate.pack.js?ver=3.3.1'></script>
        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/comment-validation/comment-validation.js?ver=3.3.1'></script>
        <script type='text/javascript' src='http://www.ala.org.au/wp-content/plugins/menubar-templates/Superfish/superfish.js?ver=3.3.1'></script>
        <link rel="EditURI" type="application/rsd+xml" title="RSD" href="http://www.ala.org.au/xmlrpc.php?rsd" />
        <link rel="wlwmanifest" type="application/wlwmanifest+xml" href="http://www.ala.org.au/wp-includes/wlwmanifest.xml" />
        <link rel='index' title='Atlas of Living Australia' href='http://www.ala.org.au/' />
        <link rel='up' title='Mapping &amp; analysis' href='http://www.ala.org.au/mapping-analysis/' />
        <link rel='prev' title='bkThe Collection Manager Story' href='http://www.ala.org.au/about-home/digitisation-guidance/the-collection-manager-story/' />
        <link rel='next' title='Department of Sustainability, Environment, Water, Population and Communities' href='http://www.ala.org.au/natural-history-collections/department-of-sustainability-environment-water-population-and-communities/' />
        <meta name="generator" content="WordPress 3.2.1" />
        <link rel='canonical' href='http://www.ala.org.au/mapping-analysis/layer-list/' />

        <!-- WP Menubar 4.10: start CSS -->
        <!-- WP Menubar 4.10: end CSS -->
        <style type="text/css">.broken_link, a.broken_link {
                text-decoration: line-through;
            }</style>	<script src="http://cdn.jquerytools.org/1.2.6/full/jquery.tools.min.js"></script>

        <script type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala2011/scripts/jquery.dimensions.js"></script>
        <script type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala2011/scripts/jquery.mousewheel.min.js"></script>
        <script type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala2011/scripts/hoverintent-min.js"></script>
        <script type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala2011/scripts/superfish/superfish.js"></script>
        <script type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala2011/scripts/jquery.autocomplete.js"></script>
        <script type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala2011/scripts/uservoice.js"></script>

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
                jQuery("ul.tabs").tabs("div.tabs-panes-noborder > section"), { history: true,effect: 'fade' };
            });
        </script>

    </head>
    <!-- <body id="page-24214" class="page page-id-24214 page-template page-template-landing_w_search-php"> -->
    <c:choose>
        <c:when test="${param.fluid}">
            <body id="page-spatial" class="fluid">
        </c:when>
        <c:otherwise>
            <body id="page-spatial" class="">
        </c:otherwise>
    </c:choose>
    <c:set var="returnUrlPath" value="${initParam.serverName}${pageContext.request.requestURI}${not empty pageContext.request.queryString ? '?' : ''}${pageContext.request.queryString}"/>
    <ala:banner returnUrlPath="${returnUrlPath}" />
    <ala:menu />
