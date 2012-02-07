<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
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
						This is a demo of the Atlas of Living Australia's spatial
                        layers. The GetCapabilities document is available 
                        <a href="http://spatial.ala.org.au/geoserver/wms?request=getCapabilities">here</a>
                        for inclusion in your own WMS client such as
                        <a href="http://udig.refractions.net/" target="_blank">uDig</a>
                        or <a href="http://www.esri.com/software/arcgis/index.html" target="_blank">ESRI ArcGIS</a>.
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
						Start by entering a layer name or id.
                </li>
                <li>
						Select from the list available
                </li>
                <li>
						Layer is loaded via WMS service 
                </li>
            </ol>
            <div id="layerwrapper" class="ui-widget">
                <label for="species">Layer: </label>
                <input id="layer" placeholder="Search..." />
            </div>
            <div id="speciesinfo">
                <table border="1">
                    <tr>
                        <td width="120px"><b>Layer id:</b></td>
                        <td><span id="lyrid"></span></td>
                    </tr>
                    <tr>
                        <td width="120px"><b>Layer name:</b></td>
                        <td><span id="lyrname"></span></td>
                    </tr>
                    <tr>
                        <td><b>Layer description: </b></td>
                        <td><span id="lyrdesc"></span></td>
                    </tr>
                    <tr>
                        <td><b>Layer licence: </b></td>
                        <td><span id="lyrlic"></span></td>
                    </tr>
                    <tr>
                        <td><b>Layer classificaton: </b></td>
                        <td><span id="lyrcls"></span></td>
                    </tr>
                    <tr>
                        <td><b>WMS GetCapabilities for WMS Clients: </b></td>
                        <td><span id="lyrwmsgm"></span></td>
                    </tr>
                    <tr>
                        <td><b>WMS Legend: </b></td>
                        <td><span id="lyrleg"></span></td>
                    </tr>
                </table>
            </div>
            <div id="map"></div>
        </section>
    </div><!--col-wide-->
</div><!--inner-->

<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.17/themes/base/jquery-ui.css" type="text/css" media="all" />
<link rel="stylesheet" href="http://dev.openlayers.org/releases/OpenLayers-2.11/theme/default/style.css" type="text/css" />
<style>
    #layer {
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

</style>

<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" type="text/javascript"></script>
<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.17/jquery-ui.min.js" type="text/javascript"></script>

<script src="http://maps.google.com/maps/api/js?sensor=true" type="text/javascript"></script>
<script src="http://spatial.ala.org.au/scripts/OpenLayers-2.11.js" type="text/javascript"></script>
<script type="text/javascript">
    var options, map, layer, wmsLayer, ejq;
    $(function() {

        $("#layer").autocomplete({
            source : function(request, response) {
                $.ajax({
                    url : "http://spatial-dev.ala.org.au/ws/layers/search",
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

        init();
    });
    function init() {
        options = {
            projection : "EPSG:900913",
            units : "m",
            maxExtent : new OpenLayers.Bounds(-20037508, -20037508, 20037508, 20037508)
        };
        map = new OpenLayers.Map('map', options, {
            numZoomLevels : 12
        });
        layer = new OpenLayers.Layer.Google("Google Physical", {
            type : google.maps.MapTypeId.TERRAIN
        });
        map.addLayer(layer);
        //map.setCenter(new OpenLayers.LonLat(lon, lat), zoom);
        map.addControl(new OpenLayers.Control.LayerSwitcher());

        var proj = new OpenLayers.Projection("EPSG:4326");
        var point = new OpenLayers.LonLat(133, -28);
        point.transform(proj, map.getProjectionObject());
        map.setCenter(point, 4);

        //loadSpeciesLayer("urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537");
    }

    function loadLayerInfo(item) {
        loadWMSLayer(item.name);
        $("#lyrid").text(item.id);
        $("#lyrname").text(item.name);
        $("#lyrdesc").text(item.description);
        $("#lyrlic").text(item.licence);
        //$("#lyrcls").text(item.classification1 + (item.classification2)?(" > " + item.classification2):"");
        $("#lyrcls").text(item.classification1 + " > " + item.classification2);
        $("#lyrwmsgm").text(wmsLayer.getFullRequestString(""));
        $("#lyrleg").text("http://spatial-dev.ala.org.au/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=ALA:"+item.name);

    }

    function loadWMSLayer(name) {
        if(wmsLayer != null) {
            wmsLayer.mergeNewParams({
                layers : 'ALA:'+name
            });
        } else {
            wmsLayer = new OpenLayers.Layer.WMS("Spatial layer", "http://spatial-dev.ala.org.au/geoserver/gwc/service/wms/reflect", {
                layers : 'ALA:'+name,
                srs : 'EPSG:900913',
                format : 'image/png',
                transparent : true
            });
            map.addLayer(wmsLayer);

        }
    }

</script>

<%@include file="../common/bottom.jsp" %>
