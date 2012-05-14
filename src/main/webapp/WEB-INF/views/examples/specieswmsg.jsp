<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="/tld/ala.tld" prefix="ala" %>
<%@include file="../common/top.jsp" %>
<header id="page-header">
    <div class="inner">
        <nav id="breadcrumb"><ol><li><a href="http://www.ala.org.au">Home</a></li> <li><a href="http://spatial.ala.org.au">Mapping &#038; analysis</a></li> <li><a href="http://spatial.ala.org.au/ws/">Spatial Web Services</a></li><li class="last">ALA Spatial demos</li></ol></nav>
        <section id="content-search">
            <h1>ALA Spatial Demos</h1>
        </section>
    </div><!--inner-->

</header>
<div class="inner">
    <div class="col-wide last" style="width:100%">
        <section class="results">
            <div>
                <p>
                    This is a demo of the Atlas of Living Australia's species
                    occurrences using Google API. Follow the steps bellow to view the
                    distribution and generate a base WMS URL for inclusion
                    in your own WMS client such as <a href="http://udig.refractions.net/" target="_blank">uDig</a> or <a href="http://www.esri.com/software/arcgis/index.html" target="_blank">ESRI ArcGIS</a>.
                </p>
                <p>
                    For a full version of ALA Spatial services, please head
                    over to the <a href="http://spatial.ala.org.au/">ALA Spatial Portal</a>.
                    For more information on how to include this demo in your
                    own code, <a href="view-source:" target="_blank">view the source</a> or read the <a href="http://code.google.com/p/alageospatialportal/wiki/SimpleWMSExample" target="_blank">wiki</a>.
                </p>
            </div>
            <ol>
                <li>
                    Start by entering a scientific name or a common name.
                </li>
                <li>
                    Select from the list available
                </li>
                <li>
                    Species occurrence is loaded on map and a base WMS url is generated
                </li>
            </ol>
            <div id="specieswrapper" class="ui-widget">
                <label for="species">Species: </label>
                <input id="species" placeholder="Search..." />
                <input type="checkbox" id="geoOnly" checked="checked" />
                <label for="geoOnly">only with locality information </label>
            </div>
            <div id="layerwrapper" class="ui-widget">
                <label for="species">Layer: </label>
                <input id="layer" placeholder="Search..." />
            </div>
            <div id="maploading">Loading...</div>
            <div id="map"></div>
        </section>
    </div><!--col-wide-->
</div><!--inner-->

<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.17/themes/base/jquery-ui.css" type="text/css" media="all" />
<link rel="stylesheet" href="http://dev.openlayers.org/releases/OpenLayers-2.11/theme/default/style.css" type="text/css" />
<style>
    #species {
        width: 250px;
        margin-bottom: 20px;
    }
    .ui-autocomplete-loading {
        background: white url('http://www.ala.org.au/wp-content/themes/ala/css/images/indicator.gif') right center no-repeat;
    }
    ul.ui-autocomplete {
        width: 250px;
        z-index: 12345 !important;
    }
    .ui-autocomplete {
        max-height: 200px;
        overflow-y: auto;
        /* prevent horizontal scrollbar */
        overflow-x: hidden;
        /* add padding to account for vertical scrollbar */
        padding-right: 20px;
    }
    /* IE 6 doesn't support max-height
     * we use height instead, but this forces the menu to always be this tall
    */
    * html .ui-autocomplete {
        height: 200px;
    }
    .ui-widget {
        font-size: 12px;
    }
    li.ui-menu-item a {
        font-size: 1em;
        float: left;
    }

    #map {
        width:100%;
        height:600px;
    }
    .olLayerGooglePoweredBy {display: none;}

</style>

<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" type="text/javascript"></script>
<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.17/jquery-ui.min.js" type="text/javascript"></script>

<script src="http://maps.google.com/maps/api/js?sensor=true" type="text/javascript"></script>
<script src="http://biocache.ala.org.au/static/js/map.js" type="text/javascript"></script>
<script src="http://biocache.ala.org.au/static/js/wms.js" type="text/javascript"></script>
<script type="text/javascript">
    var SERVER_BASE = ""; //"http://spatial.ala.org.au"
    var BIOCACHE_SERVICE_URL = "http://biocache.ala.org.au/ws"; 
    var OCC_WMS_BASE_URL = BIOCACHE_SERVICE_URL + '/occurrences/wms?';
    var OCC_SEARCH_URL = BIOCACHE_SERVICE_URL + '/occurrences/info';
    
    
    var options, map, layer, speciesLayer, overlayLayers = [];;
    $(function() {

        $("#species").autocomplete({
            source : function(request, response) {
                $.ajax({
                    url : "http://bie.ala.org.au/search/auto.json",
                    dataType : "jsonp",
                    data : {
                        geoOnly : ($('#geoOnly').attr('checked') == "checked") ? true : false,
                        q : request.term,
                        limit: 100
                    },
                    success : function(data) {
                        response($.map(data.autoCompleteList, function(item) {
                            return {
                                label : item.matchedNames[0],
                                value : item.matchedNames[0],
                                id : item.guid,
                                name : item.name,
                                common : item.commonName
                            }
                        }));
                    },
                    error : function(jqXHR, textStatus, errorThrown) {
                        alert("Unable to complete request.\n" + errorThrown);
                    }
                });
            },
            minLength : 3,
            html : true,
            select : function(event, ui) {
                loadSpeciesInfo(ui.item);
            }
        });
        
        
        
        $("#layer").autocomplete({
            source : function(request, response) {
                $.ajax({
                    url : SERVER_BASE+"/ws/layers/search",
                    dataType : "json",
                    data: {
                        q: request.term
                    },
                    success : function(data) {
                        response($.map(data, function(item) {
                            return {
                                label : item.displayname,
                                value : item.displayname,
                                id : item.uid,
                                name : item.name,
                                description : item.description,
                                licence : item.licence_notes,
                                classification1: item.classification1,
                                classification2: item.classification2
                            }
                        }));
                    },
                    error : function(jqXHR, textStatus, errorThrown) {
                        alert("Unable to complete request.\n" + errorThrown);
                    }
                });
            },
            minLength : 3,
            html : true,
            select : function(event, ui) {
                loadLayerInfo(ui.item);
            }
        });
        

        initialise();
    });
    
    
    
    function initialise() {
            
        var myLatlng = new google.maps.LatLng(-27, 133);
        var myOptions = {
            zoom: 4,
            scrollwheel: false, // Dave says: leave as false
            center: myLatlng,
            mapTypeId: google.maps.MapTypeId.ROADMAP,
            scaleControl: true, 
            streetViewControl: false,
            draggableCursor: 'pointer'
        }
        map = new google.maps.Map(document.getElementById("map"), myOptions);
        infomarker = new google.maps.Marker({
            position: myLatlng,
            visible: false,
            title: "Click location", 
            map: map
        });
        infowindow = new google.maps.InfoWindow({
            maxWidth: 600
        });

        map.setOptions({
            mapTypeControlOptions: {
                mapTypeIds: [
                    google.maps.MapTypeId.ROADMAP,
                    google.maps.MapTypeId.TERRAIN,
                    google.maps.MapTypeId.SATELLITE,
                    google.maps.MapTypeId.HYBRID
                ],
                style: google.maps.MapTypeControlStyle.DROPDOWN_MENU
            }
        });
    }
    
    function initialiseOverlays(lyrCount) {
        // clear any existing MySpecies layers only from the map
        if (overlayLayers.length > 0) {
            for (i=0;i<overlayLayers.length;i++){
                map.overlayMapTypes.removeAt(1);
            }
        }
        
        // clear our local array
        //map.overlayMapTypes.clear();
        overlayLayers = [];
        
        // add some placeholders
        if (lyrCount > 0) {
            for (i=0;i<lyrCount;i++){
                map.overlayMapTypes.push(null);
            }
        }
    }
    
    /**
     * Load occurrences wms with the selected params
     */
    function insertWMSOverlay(name, params) {
        var customParams = [
            "FORMAT=image/png8"
        ];
        
        if (arguments.length > 1) {
            for (var i = 0; i < arguments.length; i++) {
                customParams.push(arguments[i]);
            }
        }
        
        //Add query string params to custom params
        var searchParam = encodeURI(location.search); // NdR - fixes bug where terms have quotes around them
        var pairs = searchParam.substring(1).split('&');
        for (var j = 0; j < pairs.length; j++) {
            customParams.push(pairs[j]);
        }
        
        // helps if you want to load multiple species layers
        // just push into the "overlayLayers" array and it'll add it
        var wmstile = new WMSTileLayer("MySpecies - " + name, OCC_WMS_BASE_URL, customParams, wmsTileLoaded);
        if (name=='Other') {
            overlayLayers.splice(0,0,wmstile);
            overlayLayers.pop();
        } else {
            overlayLayers.push(wmstile);
        }
        
        // now iterate thru' the array and load the layers
        $.each(overlayLayers, function(_idx, overlayWMS) {
            map.overlayMapTypes.setAt(_idx+1, overlayWMS);
        });
    }
    
    function wmsTileLoaded(numtiles) {
        $('#maploading').fadeOut("slow");
    }
    
    function loadSpeciesInfo(item) {
        loadSpeciesLayer(item.name, item.id);
    }
    function loadSpeciesLayer(name, lsid) {
        // remove existing layer
        // comment the next line to enable adding without removing
        initialiseOverlays(1);
        
        // add the species layer
        insertWMSOverlay(name, "q=lsid:"+lsid+"&colourby=3368652&symsize=4");
    }
    
    function loadLayerInfo(item) {
        loadEnvironmentalLayer(item.name, item.description);
    }
    function loadEnvironmentalLayer(lyrname, lyrDisplayName) {
        map.overlayMapTypes.setAt(0, null);
        if (lyrname != "") {
            var overlayWMS = new WMSTileLayer(lyrDisplayName, "http://spatial.ala.org.au/geoserver/gwc/service/wms/reflect?", ["format=image/png","layers=ALA:"+lyrname], wmsTileLoaded);
            map.overlayMapTypes.setAt(0, overlayWMS);
        }
    }

</script>

<%@include file="../common/bottom.jsp" %>
