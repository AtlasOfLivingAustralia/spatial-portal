/**
 * Instance of OpenLayers map
 */
var map;
var proxy_script="/webportal/RemoteRequest?url=";
var tmp_response;
var popup;
var selectControl;

var popupWidth = 435; //pixels
var popupHeight = 320; //pixels

var requestCount = 0; // getFeatureInfo request count
var queries = new Object(); // current getFeatureInfo requests
var queries_valid_content = false;
var timestamp; // timestamp for getFeatureInfo requests
var X,Y; // getfeatureInfo Click point

var clickEventHandler; // single clickhandler
var toolPanel; // container for OpenLayer controls
var pan; // OpenLayers.Control.Navigation
var zoom; // OpenLayers.Control.ZoomBox
var areaToolsButton// OpenLayers.Control.Button
//var selectableLayers = new Array();
/**
 * Associative array of all current active map layers except
 * for baselayers
 */
var mapLayers = new Object();

/**
 * Associative array of all available base layers
 */
var baseLayers = new Object();

/**
 * Key of the currently selected base layer in baseLayers
 */
var currentBaseLayer = null;

var selectionLayers = new Array();

var mapClickControl;
var polyControl = null;
var radiusControl = null;  //for deactivate after drawing
var boxControl = null;	//for deactivate after drawing
var areaSelectControl = null;
var filteringPolygon = null;	//temporary for destroy option after display
var alocPolygon = null;			//temporary for destroy option after display
var polygonLayer = null;
var boxLayer = null;
var radiusLayer = null;
var featureSelectLayer = null;
var featureSpeciesSelectLayer = null;
var areaSelectOn = false;
var layersLoading = 0;
var layername; // current layer name

var attempts = 0;
var libraryLoaded = false;
var checkLibraryLoadedTimeout = null;
var libraryCheckIntervalMs=100;
var secondsToWaitForLibrary=30;
var maxAttempts = (secondsToWaitForLibrary * 1000) / libraryCheckIntervalMs;

var selecteFeature;

// check if the Google Layer automatically got
// switched between normal and hybrid around zoom level 16
var autoBaseLayerSwitch = false;
// 0 = normal
// 1 = user-switch
// 2 = auto-switch 
var baseLayerSwitchStatus = 0;
var activeAreaPresent = false;


function stopCheckingLibraryLoaded() {
    clearInterval(checkLibraryLoadedTimeout);
}

function registerLayer(layer) {
    layer.events.register('loadstart', this, loadStart);
    layer.events.register('loadend', this, loadEnd);
}

function loadStart() {
    if (layersLoading == 0) {
        toggleLoadingImage("block");
    }
    layersLoading++;
}

function loadEnd() {
    layersLoading--;
    if (layersLoading == 0) {
        toggleLoadingImage("none");
    }
//signal webportal

}

function toggleLoadingImage(display) {
    var div = document.getElementById("loader");
    if (div != null) {
        if (display == "none") {
            jQuery("#loader").hide(2000);
        }
        else {
            setTimeout(function(){
                if (layersLoading > 0) {
                    div.style.display=display;
                }
            }, 2000);
        }
    }
}

function checkLibraryLoaded() {
    if (typeof OpenLayers == 'undefined') {
        if ((attempts < maxAttempts) && (typeof OpenLayers == 'undefined')) {
            attempts++;
        }
        else if (attempts == maxAttempts) {
            // give up loading - too many attempts
            stopCheckingLibraryLoaded();
            alert(
                "Map not loaded after waiting " + secondsToWaitForLibrary + " seconds.  " +
                "Please wait a moment and then reload the page.  If this does not fix your " +
                "problem, please contact ALA for assistance"
                );
        }
    }
    else {
        // library loaded OK stop delay timer
        stopCheckingLibraryLoaded();
        libraryLoaded = true;

        parent.updateSafeToLoadMap(libraryLoaded);

        // ok now init the map...
        parent.onIframeMapFullyLoaded();
    }

}

var bLayer,bLayer2,bLayer3,bLayer4;
function loadBaseMap() {

    goToLocation(134, -25, 4);

// Google.v3 uses EPSG:900913 as projection, so we have to
// transform our coordinates
//    map.setCenter(
//        new OpenLayers.LonLat(134, -25).transform(
//            new OpenLayers.Projection("EPSG:4326"),
//            map.getProjectionObject()),
//        4);
}
function goToLocation(lon, lat, zoom) {
    // Google.v3 uses EPSG:900913 as projection, so we have to
    // transform our coordinates
    map.setCenter(
        new OpenLayers.LonLat(lon, lat).transform(
            new OpenLayers.Projection("EPSG:4326"),
            map.getProjectionObject()),
        zoom);
}

function changeBaseLayer(type) {
    if (type == 'normal') {
        map.setBaseLayer(bLayer2);
    } else if (type == 'hybrid') {
        map.setBaseLayer(bLayer);
    } else if (type == 'minimal') {
        map.setBaseLayer(bLayer3);
    } else if (type == 'outline') {
        map.setBaseLayer(bLayer4);
    }
}

function buildMap() {
    checkLibraryLoadedTimeout = setInterval('checkLibraryLoaded()', libraryCheckIntervalMs);
}

function buildMapReal() {

    // fix IE7 errors due to being in an iframe
    document.getElementById('mapdiv').style.width = '100%';
    document.getElementById('mapdiv').style.height = '100%';

    // highlight for the scale and lonlat info
    function onOver(){
        jQuery("div#mapinfo ").css("opacity","0.9");
    }
    function onOut(){
        jQuery("div#mapinfo ").css("opacity","0.5");
    }

    jQuery("div#mapinfo ").css("opacity","0.5");
    jQuery("div#mapinfo").hover(onOver,onOut);

    // proxy.cgi script provided by OpenLayers written in Python, must be on the same domain
    OpenLayers.ProxyHost = proxy_script;


    // ---------- map setup --------------- //

    var mapControls = [
    new OpenLayers.Control.PanZoomBar({
        div: document.getElementById('controlPanZoom')
    }),
    new OpenLayers.Control.LayerSwitcher(),
    new OpenLayers.Control.ScaleLine({
        div: document.getElementById('mapscale'),
        geodesic: true
    }),

    new OpenLayers.Control.Attribution(),
    new OpenLayers.Control.MousePosition({
        div: document.getElementById('mapcoords'),
        prefix: '<b>Lon:</b> ',
        separator: ' <BR><b>Lat:</b> '
    }),
    new OpenLayers.Control.Navigation()
    ];
    var mapOptions = {
        projection: new OpenLayers.Projection("EPSG:900913"),
        displayProjection: new OpenLayers.Projection("EPSG:4326"),
        units: "m",
        numZoomLevels: 18,
        maxResolution: 156543.0339,
        maxExtent: new OpenLayers.Bounds(-20037508, -20037508,
            20037508, 20037508.34),
        controls: mapControls
    };
    map = new OpenLayers.Map('mapdiv', mapOptions);


    // make OL compute scale according to WMS spec
    OpenLayers.DOTS_PER_INCH = 25.4 / 0.28;
    // Stop the pink tiles appearing on error
    OpenLayers.Util.onImageLoadError = function() {
        this.style.display = "";
        this.src="img/blank.png";
    }

    bLayer = new OpenLayers.Layer.Google("Google Hybrid",
    {
        type: google.maps.MapTypeId.HYBRID,
        wrapDateLine: false,
        'sphericalMercator': true
    });
    bLayer2 = new OpenLayers.Layer.Google("Google Streets",
    {
        wrapDateLine: false,
        'sphericalMercator': true
    });
    bLayer3 = new OpenLayers.Layer.OSM();
    
    bLayer4 = new OpenLayers.Layer.WMS("Outline",parent.jq('$geoserver_url')[0].innerHTML + "/gwc/service/wms/reflect",{
        layers:"ALA:world"},
	{isBaseLayer: true,'wrapDateLine': true,
        projection: new OpenLayers.Projection("EPSG:900913"),
        'sphericalMercator': true}
    );
    map.addLayers([bLayer2,bLayer,bLayer3,bLayer4]);
    parent.bLayer = bLayer;
    parent.bLayer2 = bLayer2;
    parent.bLayer3 = bLayer3;
    parent.bLayer4 = bLayer4;

    loadBaseMap();

    // create a new event handler for single click query
    clickEventHandler = new OpenLayers.Handler.Click({
        'map': map
    }, {
        'click': function(e) {
            envLayerInspection(e);
        }
    });
    clickEventHandler.activate();
    clickEventHandler.fallThrough = true;
        
    // cursor mods
    map.div.style.cursor="pointer";
    jQuery("#navtoolbar div.olControlZoomBoxItemInactive ").click(function(){
        map.div.style.cursor="crosshair";
        clickEventHandler.deactivate();
    });
    jQuery("#navtoolbar div.olControlNavigationItemActive ").click(function(){
        map.div.style.cursor="pointer";
        clickEventHandler.activate();
        setVectorLayersSelectable();
    });

    map.events.register("moveend" , map, function (e) {
        parent.setExtent();
        Event.stop(e);
    });

    map.events.register("zoomend" , map, function (e) {
        Event.stop(e);

        if (map.zoom > 15) {
            autoBaseLayerSwitch = true;
            if (baseLayerSwitchStatus != 1) {
                baseLayerSwitchStatus = 2; 
            }
            changeBaseLayer('hybrid');
        }
        else {
            //if (autoBaseLayerSwitch) {
            //    changeBaseLayer('normal');
            //    autoBaseLayerSwitch = false;
            //}
            if (baseLayerSwitchStatus == 2) {
                changeBaseLayer('normal');
                baseLayerSwitchStatus = 0;
            }
        }
    });

    registerSpeciesClick();
}

var query_
var query_size
function iterateSpeciesInfoQuery(curr) {
    var pos = 0;
    var curpos = curr;
    while(curpos >= query_size[pos]) {
        curpos -= query_size[pos];
        pos += 1;
    }
    var nextBtn = " &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; ";
    try {
        if (curr+1 < query_count_total) {
            nextBtn = "<a style='float: right' href='javascript:iterateSpeciesInfoQuery("+(curr+1)+");'><img src='img/arrow_right.png' /></a>"; // next &rArr;
        }
    } catch (err) {}
    var prevBtn = " &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; ";
    try {
        if (curr > 0) {
            prevBtn = "<a href='javascript:iterateSpeciesInfoQuery("+(curr-1)+");'><img src='img/arrow_left.png' /></a>"; // &lArr; previous
        }
    } catch (err) {}

    var url = query_url[pos] + "&start=" + curpos;
    $.getJSON(proxy_script + URLEncode(url), function(data) {
        if (query_[pos].indexOf("qid:") < 0) {
            var ulyr = query_[pos];
            var ulyr_occ_id = data.occurrences[0].id;
            var ulyr_occ_lng = data.occurrences[0].longitude;
            var ulyr_occ_lat = data.occurrences[0].latitude;
            var ulyr_meta = data.metadata;

            var data = ulyr_meta.replace(/_n_/g,"<br />");

            var heading = "<h2>Occurrence information (" + (curr+1) + " of " + query_count_total + ")</h2>";
            if (query_count_total==1) {
                heading = "<h2>Occurrence information (1 occurrence)</h2>";
            }

            var infohtml = "<div id='sppopup'> " +
            heading + "Record id: " + ulyr_occ_id + "<br /> " + data + " <br /> <br />" +
            " Longitude: "+ulyr_occ_lng + " , Latitude: " + ulyr_occ_lat + " (<a href='javascript:goToLocation("+ulyr_occ_lng+", "+ulyr_occ_lat+", 15);relocatePopup("+ulyr_occ_lng+", "+ulyr_occ_lat+");'>zoom to</a>) <br/>" +
            "<div id=''>"+prevBtn+" &nbsp; &nbsp; &nbsp; &nbsp; "+nextBtn+"</div>";

            setTimeout(function(){
                if (document.getElementById("sppopup") != null) {
                    document.getElementById("sppopup").innerHTML = infohtml;
                }
            }, 50);
        } else {
            displaySpeciesInfo(data.occurrences[0], prevBtn, nextBtn, curr, query_count_total);            
        }
    });
}

var defaultSelectFeatureStyle = null;
function addFeatureSelectionTool() {
    removeAreaSelection();
    areaSelectOn = true;
    mapClickControl = null;

    OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
        defaultHandlerOptions: {
            'single': true,
            'double': false,
            'pixelTolerance': 0,
            'stopSingle': false,
            'stopDouble': false
        },

        initialize: function(options) {
            this.handlerOptions = OpenLayers.Util.extend(
            {}, this.defaultHandlerOptions
                );
            OpenLayers.Control.prototype.initialize.apply(
                this, arguments
                );
            this.handler = new OpenLayers.Handler.Click(
                this, {
                    'click': pointSearch
                }, this.handlerOptions
                );
        }
    });
    mapClickControl = new OpenLayers.Control.Click();
    mapClickControl.fallThrough = true;
    map.addControl(mapClickControl);
    mapClickControl.activate();
}

function pointSearch(e) {
    var lonlat = map.getLonLatFromViewPortPx(e.xy);
    parent.setSearchPoint(lonlat);
}

function removePointSearch() {
    //remove the point search map click control
    mapClickControl.deactivate();
    map.removeControl(mapClickControl);
    mapClickControl = null;
    //reload the species click control
    registerSpeciesClick();
}

var query_count_total;
function pointSpeciesSearch(e) {
    var lonlat = map.getLonLatFromViewPortPx(e.xy);
    lonlat.transform(map.projection, map.displayProjection);

    //handles point click in mapComposer
    //parent.setSpeciesSearchPoint(lonlat);

    //get all 'qid:' layers and open for paging
    var layers = map.getLayersByClass("OpenLayers.Layer.WMS");

    query_count_total = 0;
    query_ = new Array();
    query_size = new Array();
    query_url = new Array();
    var pos = 0;
    for(var i=layers.length-1;i>=0;i--) {
        var layer = layers[i];
        var p0 = layer.url.indexOf("qid:");
        var p1 = layer.url.indexOf("&", p0);
        if(p1 < 0) p1 = layer.url.indexOf(";", p0);
        if(p1 < 0) p1 = layer.url.length;
        var query = null;
        var userquery = null;
        if(p0 < 0 || p1 < 0 || layer.params == null) {
            var p0 = layer.url.indexOf("CQL_FILTER=");
            var p1 = layer.url.indexOf("&", p0);            
            if(p1 < 0) p1 = layer.url.indexOf(";", p0);            
            if(p1 < 0) p1 = layer.url.length;            
            if(p0 >= 0 && p1 >= 0 && layer.params != null) {
                userquery = layer.url.substring(p0 + 11,p1);
            }
        } else {
            query = layer.url.substring(p0,p1);
        }
        

        var size = 10;
        if(layer.params != null && layer.params.ENV != null) {
            var p2 = layer.params.ENV.indexOf("size:");
            p3 = layer.params.ENV.indexOf(";", p2);
            if(p3 < 0) p3 = layer.params.ENV.length;

            if(p2 >= 0 && p3 >= 0) {
                size = layer.params.ENV.substring(p2 + 5,p3)
            }
        }

        var data = null;
        if(query != null) data = getOccurrence(query, lonlat.lat, lonlat.lon, 0, pos, size);
        if(userquery != null) data = getOccurrenceUploaded(userquery, lonlat.lat, lonlat.lon, 0, pos, size);
        if(data != null) {
            query_count_total += query_size[pos];
            pos += 1;
        }
    }

    if (query_count_total == 0) {
        return null;
    }

    var lonlat = new OpenLayers.LonLat(lonlat.lon, lonlat.lat).transform(
    new OpenLayers.Projection("EPSG:4326"),
    map.getProjectionObject());

    setupPopup(query_count_total, lonlat);
    iterateSpeciesInfoQuery(0)

    var feature = popup;
    feature.popup = popup;
    popup.feature = feature;
    map.addPopup(popup, true);
}

function registerSpeciesClick() {

    OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
        defaultHandlerOptions: {
            'single': true,
            'double': false,
            'pixelTolerance': 0,
            'stopSingle': false,
            'stopDouble': false
        },

        initialize: function(options) {
            this.handlerOptions = OpenLayers.Util.extend(
            {}, this.defaultHandlerOptions
                );
            OpenLayers.Control.prototype.initialize.apply(
                this, arguments
                );
            this.handler = new OpenLayers.Handler.Click(
                this, {
                    'click': pointSpeciesSearch
                }, this.handlerOptions
                );
        }
    });
    mapClickControl = new OpenLayers.Control.Click();
    mapClickControl.fallThrough = true;
    map.addControl(mapClickControl);
    mapClickControl.activate();
    ///////////////////
    //  setVectorLayersSelectable();
    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = "red";
    layer_style.strokeColor = "red";

    featureSpeciesSelectLayer = new OpenLayers.Layer.Vector("Selected Species Layer", {
        style: layer_style
    });
    featureSpeciesSelectLayer.setVisibility(true);
    map.addLayer(featureSpeciesSelectLayer);
}

function addRadiusDrawingTool() {
    removeAreaSelection();
    radiusOptions = {
        sides: 40
    };

    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = "red";
    layer_style.strokeColor = "red";

    radiusLayer = new OpenLayers.Layer.Vector("Point Radius Layer Layer", {
        style: layer_style,
        eventListeners: {
            "sketchmodified": function(event) {
                var verts = event.vertex.getVertices();
                var gll = new Array();
                if (verts.length > 0) {
                    for (var v=0; v<verts.length; v++) {
                        var pt = verts[v].transform(map.projection, map.displayProjection);
                        gll.push(new google.maps.LatLng(pt.y, pt.x));
                    }
                }
                var currarea = ((google.maps.geometry.spherical.computeArea(gll)/1000)/1000);
                var currradius = Math.sqrt(currarea/Math.PI);
                currradius = Math.round(currradius*Math.pow(10,2))/Math.pow(10,2);
                $('#currradius').html(currradius);
            }
        }
    });
    radiusLayer.setVisibility(true);
    map.addLayer(radiusLayer);

    if(radiusControl != null){
        map.removeControl(radiusControl);
        radiusControl.destroy();
        radiusControl = null;
    }
    radiusControl = new OpenLayers.Control.DrawFeature(radiusLayer,OpenLayers.Handler.RegularPolygon,{
        'featureAdded':radiusAdded,
        handlerOptions:radiusOptions
    });
    map.addControl(radiusControl);
    radiusControl.activate();
    $('#currradius').html("0"); 
    $('#radiusDisplay').slideDown('slow');
}

function addPolygonDrawingTool() {
    removeAreaSelection();
    ////adding polygon control and layer
    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = "red";
    layer_style.strokeColor = "red";	

    polygonLayer = new OpenLayers.Layer.Vector("Polygon Layer", {
        style: layer_style
    });
    polygonLayer.setVisibility(true);
    map.addLayer(polygonLayer);
    if(polyControl != null){
        map.removeControl(polyControl);
        polyControl.destroy();
        polyControl = null;
    }
    polyControl = new OpenLayers.Control.DrawFeature(polygonLayer,OpenLayers.Handler.Polygon,{
        'featureAdded':polygonAdded
    });
    map.addControl(polyControl);
    polyControl.activate();	
//////
}

function removeAreaSelection() {
    if(polygonLayer != null){
        polygonLayer.destroy();
        polygonLayer = null;
        polyControl.deactivate();
    }
    if(boxLayer != null) {
        boxLayer.destroy();
        boxLayer = null;
        boxControl.deactivate();        
    }
    if(radiusLayer != null){
        radiusLayer.destroy();
        radiusLayer = null;
        radiusControl.deactivate();
        $('#radiusDisplay').slideUp('slow');
    }

    if(featureSelectLayer != null){
        featureSelectLayer.destroy();
        featureSelectLayer = null;
        areaSelectOn = false;
    }

    /* refreshes all the vector layers -> this is because the vector features
     * can dissappear after selection so need to redraw
     */
    for(var i in selectionLayers) {
        var layer = selectionLayers[i];
        for(var j in layer.features) {
            layer.drawFeature(layer.features[j]);
        }
    }

    if(mapClickControl != null) {
        mapClickControl.deactivate();
        map.removeControl(mapClickControl);
    }
}

function addBoxDrawingTool() {
    removeAreaSelection();
    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = "red";
    layer_style.strokeColor = "red";	

    boxLayer = new OpenLayers.Layer.Vector("Box Layer", {
        style : layer_style
    });

    boxControl = new OpenLayers.Control.DrawFeature(boxLayer,OpenLayers.Handler.Box,{
        'featureAdded':regionAdded
    });
    map.addControl(boxControl);

    boxControl.activate();	
}

function featureSelected(feature) {
    parent.setLayerGeometry(feature.layer.name);
    areaSelectOn = false;    
    removeAreaSelection();
    setVectorLayersSelectable();
}

function radiusAdded(feature) {
    parent.setPolygonGeometry(feature.geometry);
    removeAreaSelection();
    setVectorLayersSelectable();
}

// This function passes the region geometry up to javascript in index.zul which can then send it to the server.
function regionAdded(feature) {    
    //converting bounds from pixel value to lonlat - annoying!
    var geoBounds = new OpenLayers.Bounds();
    geoBounds.extend(map.getLonLatFromPixel(new OpenLayers.Pixel(feature.geometry.left,feature.geometry.bottom)));
    geoBounds.extend(map.getLonLatFromPixel(new OpenLayers.Pixel(feature.geometry.right,feature.geometry.top)));

    removeAreaSelection();
    setVectorLayersSelectable();
    parent.setRegionGeometry(geoBounds.toGeometry());
}

// This function passes the geometry up to javascript in index.zul which can then send it to the server.
function polygonAdded(feature) {
    parent.setPolygonGeometry(feature.geometry);
    removeAreaSelection();
    setVectorLayersSelectable();
}

function setVectorLayersSelectable() {
    try {
        var layersV = map.getLayersByClass('OpenLayers.Layer.Vector');
        if(selectControl != null){
            selectControl.deactivate();
            map.removeControl(selectControl);
            selectControl.destroy();
            selectControl = null;
        }
        selectControl = new OpenLayers.Control.SelectFeature(layersV);
        map.addControl(selectControl);
        selectControl.activate();
    } catch (err) {

    }
}

var currFeature;
var currFeatureCount; 

function setupPopup(count, centerlonlat) {
    var waitmsg = count + " occurrences found in this location <br /> Retrieving data... ";
    if (count == 1) {
        waitmsg = count + " occurrence found in this location <br /> Retrieving data... ";
    }
    popup = new OpenLayers.Popup.FramedCloud("featurePopup",
        centerlonlat,
        new OpenLayers.Size(100,150),
        "<div id='sppopup' style='width: 350px; height: 220px;'>" + waitmsg + "</div>"
        ,
        null, true, onPopupClose);
}

function onPopupClose(evt) {
    try {
        map.removePopup(this.feature.popup);
        this.feature.popup.destroy();
        this.feature.popup = null;

        selectControl.unselect(this.feature);
    } catch(err) {
    }
}

function relocatePopup(lon, lat) {
    popup.lonlat = new OpenLayers.LonLat(lon, lat).transform(
            new OpenLayers.Projection("EPSG:4326"),
            map.getProjectionObject());
    popup.updatePosition(); 
}

function displaySpeciesInfo(data, prevBtn, nextBtn, curr, total) {
    var occinfo = data;
    var bie = parent.jq('$bie_url')[0].innerHTML;
    var biocache = parent.jq('$biocache_webapp_url')[0].innerHTML;
    var rank = occinfo.taxonRank;
    var speciesname = occinfo.scientificName;
    var specieslsid = occinfo.taxonConceptID;
    species = (speciesname!=null)?speciesname:"";
    if (specieslsid != null) {
        species = '<a href="' + bie + '/species/'+specieslsid+'" target="_blank">'+species+'</a>';
    } else {
        species = species + ' (<i>Supplied as: "' + occinfo.scientificName + '"</i>) ';
    }

    var family = occinfo.family;
    if (occinfo.family != null) {
        family = '<a href="' + bie + '/species/'+occinfo.family+'" target="_blank">'+family+'</a>';
    }

    var kingdom = occinfo.kingdom;
    if (occinfo.kingdom != null) {
        kingdom = '<a href="' + bie + '/species/'+occinfo.kingdom+'" target="_blank">'+kingdom+'</a>';
    }

    var occurrencedate = "";
    if(occinfo.year && occinfo.month) {
        occurrencedate = occinfo.month + "/" + occinfo.year;
    } else if (occinfo.year) {
        occurrencedate = occinfo.year
    }
    var uncertainty = occinfo.coordinateUncertaintyInMeters;
    var uncertaintyText = uncertainty + " metres";
    if(uncertainty == "" || uncertainty == undefined || uncertainty == null) {
        uncertaintyText = "<b>Undefined! </b>"; // setting to 10km
        uncertainty = 10000;
    }
    var heading = "<h2>Occurrence information (" + (curr+1) + " of " + total + ")</h2>";
    if (total==1) {
        heading = "<h2>Occurrence information (1 occurrence)</h2>";
    }

    var infohtml = "<div id='sppopup'> " +
    heading +
    " Scientific name: " + species + " <br />" +
    " Kingdom: " + kingdom + " <br />" +
    " Family: " + family + " <br />" +
    " Data provider: <a href='http://collections.ala.org.au/public/show/" + occinfo.dataProviderUid + "' target='_blank'>" + occinfo.dataProviderName + "</a> <br />" +
    " Longitude: "+occinfo.decimalLongitude + " , Latitude: " + occinfo.decimalLatitude + " (<a href='javascript:goToLocation("+occinfo.decimalLongitude+", "+occinfo.decimalLatitude+", 15);relocatePopup("+occinfo.decimalLongitude+", "+occinfo.decimalLatitude+");'>zoom to</a>) <br/>" +
    " Spatial uncertainty in metres: " + uncertaintyText + "<br />" +
    " Occurrence date: " + occurrencedate + " <br />" +
    "Species Occurence <a href='" + biocache + "/occurrences/" + occinfo.uuid + "' target='_blank'>View details</a> <br /> <br />" +
    "<div id=''>"+prevBtn+" &nbsp; &nbsp; &nbsp; &nbsp; "+nextBtn+"</div>";

    if (document.getElementById("sppopup") != null) {
        document.getElementById("sppopup").innerHTML = infohtml;
    }
}

function addWKTFeatureToMap(featureWKT,name,hexColour,opacity) {
    var in_options = {
        'internalProjection': map.baseLayer.projection,
        'externalProjection': new OpenLayers.Projection("EPSG:4326")
    };
    var styleMap = new OpenLayers.StyleMap(OpenLayers.Util.applyDefaults(
    {
        fillColor: hexColour,
        fillOpacity: opacity,
        strokeOpacity: 1,
        strokeWidth: 2,
        strokeColor: hexColour
    },
    OpenLayers.Feature.Vector.style["new"]));

    var layer_style = OpenLayers.Util.extend({},OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = hexColour;
    layer_style.strokeColor = hexColour;
    layer_style.fillOpacity = opacity;

    var wktLayer = new OpenLayers.Layer.Vector(name, {
        style : layer_style
    });
    map.addLayer(wktLayer);
    var geom = new OpenLayers.Geometry.fromWKT(featureWKT);
    geom = geom.transform(map.displayProjection, map.projection);
    wktLayer.addFeatures([new OpenLayers.Feature.Vector(geom)]);

    wktLayer.isFixed = false;
    selectionLayers[selectionLayers.length] = wktLayer;

    removePointSearch();

    return wktLayer;
}
var myVector;
function addJsonFeatureToMap(feature, name, hexColour, radius, opacity, szUncertain) {
    var in_options = {
        'internalProjection': map.baseLayer.projection,
        'externalProjection': new OpenLayers.Projection("EPSG:4326")
    };
    
    var styleMap = new OpenLayers.StyleMap(OpenLayers.Util.applyDefaults(
    {
        fillColor: hexColour,
        fillOpacity: opacity,
        strokeOpacity: 1,
        strokeWidth: 2,
        strokeColor: hexColour
    },
    OpenLayers.Feature.Vector.style["new"]));

    var layer_style = OpenLayers.Util.extend({},OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = hexColour;
    layer_style.strokeColor = hexColour;
    layer_style.pointRadius = 0;
    layer_style.pointRadius = radius;
    layer_style.fillOpacity = opacity;
    layer_style.szUncertain = szUncertain;
    layer_style.fontWeight = "bold";

    var geojson_format = new OpenLayers.Format.GeoJSON(in_options);
    var vector_layer = new OpenLayers.Layer.Vector(name,{
        styleMap: styleMap
    });

    myVector = vector_layer;

    vector_layer.style = layer_style;
    vector_layer.isFixed = false;
    features = geojson_format.read(feature);

    fixAttributes(features, feature);

    //apply uncertainty to features
    vector_layer.events.register("featureadded", vector_layer, featureadd);
    vector_layer.addFeatures(features);
    
    vector_layer.events.register("featureselected", vector_layer, selected);

    window.setTimeout(function(){
        setVectorLayersSelectable();
    }, 2000);
        
    return vector_layer;
}

function selected (evt) {

    var feature = (evt.feature==null)?evt:evt.feature;
    var attrs = feature.attributes;
    currFeature = feature;

    if (areaSelectOn) {
        currFeature = null;
        featureSelected(feature);
        return;
    }
    else {
        //test to see if its occurrence data
        if (attrs["oi"] != null) {
            popup = new OpenLayers.Popup.FramedCloud("featurePopup",
                feature.geometry.getBounds().getCenterLonLat(),
                new OpenLayers.Size(100,150),
                "<div id='sppopup' style='width: 350px; height: 220px;'>Retrieving data... </div>"
                ,
                null, true, onPopupClose);

            //parent.showInfoOne();
            parent.setSpeciesSearchPoint(feature.geometry.getBounds().getCenterLonLat());

        } else if (attrs["count"] != null) {
            setupPopup(attrs["count"], feature.geometry.getBounds().getCenterLonLat());
            showClusterInfo(0);



        } else {
            var html = "<h2>Feature Details</h2>";

            if (attrs.Feature_Name) {
                html += "Feature name: " + attrs.Feature_Name + "<br />";
                html += "Feature ID: " + attrs.Feature_ID + "<br />";
                html += "GID: " + attrs.gid + "<br /><br />";

                if (attrs.Bounding_Box) {
                    html += "Bounding box: " + attrs.Bounding_Box + "<br />";
                    html += "Feature type: Polygon <br /><br />";
                }

                if (attrs.Point) {
                    html += "Point: " + attrs.Point + "<br />";
                    html += "Feature type: Point <br /><br />";
                }

                html += "Metadata: <a href='" + attrs.Layer_Metadata + "' target='_blank'>" + attrs.Layer_Metadata + "</a> <br />";
            } else {
                for (key in attrs) {
                    html += "<br>" +  key + " : "  + attrs[key];
                }
            }

            popup = new OpenLayers.Popup.FramedCloud("featurePopup",
                feature.geometry.getBounds().getCenterLonLat(),
                new OpenLayers.Size(100,150),
                html
                ,
                null, true, onPopupClose);

        }
        feature.popup = popup;
        popup.feature = feature;
        map.addPopup(popup, true);
    }

}

function featureadd(evt) {
    var max_map_bounds = new OpenLayers.Bounds(-180,-90, 180, 90);  // map.getMaxExtent();
    var feature = evt.feature;
    var fgeomt = feature.geometry.transform(map.projection,map.displayProjection);
    var isContains = max_map_bounds.contains(this.getDataExtent().left, this.getDataExtent().top); 
    /*
         * add a mirror point 360 to the west of the feature, so that it will be displayed
         * when the map's extent becomes < -180 as a result of the warpdateline function
         * of the base layer, this is only applicable for point
         */
    if(!feature.isMirror){
        if(!feature.onScreen()){
            if(!max_map_bounds.contains(this.getDataExtent().left, this.getDataExtent().top)){//feature.geometry.x > 0 &&
                var featureMirror = new OpenLayers.Feature.Vector(
                    new OpenLayers.Geometry.Point((fgeomt.x - max_map_bounds.getWidth()), fgeomt.y),
                    feature.attributes,
                    feature.style);
                featureMirror.isMirror = true;
                feature.isMirror = false;
                var fmgeomt = featureMirror.geometry.transform(map.projection,map.displayProjection);
                this.addFeatures([featureMirror]);
            }
        }
    }
    fgeomt = feature.geometry.transform(map.displayProjection,map.projection);
}

function addJsonUrlToMap(url, name, hexColour, radius, opacity, szUncertain) {
   
    $.getJSON(proxy_script + url, function(feature) {
        addJsonFeatureToMap(feature, name, hexColour, radius, opacity, szUncertain);

        var urlname = url + "::" + name;
        vector_layer.urlname = urlname;
        mapLayers[urlname] = vector_layer;
        registerLayer(mapLayers[urlname]);
        map.addLayer(mapLayers[urlname]);

        if(map.signalLayerLoaded != undefined
            && vector_layer.urlname != undefined)
            map.signalLayerLoaded(vector_layer.urlname);
    });
}

function appendJsonUrlToMap(url, original_url, name) {
    $.getJSON(proxy_script + url, function(feature) {

        var urlname = original_url + "::" + name;
        vector_layer = mapLayers[urlname];

        var in_options = {
            'internalProjection': map.baseLayer.projection,
            'externalProjection': new OpenLayers.Projection("EPSG:4326")
        };

        var geojson_format = new OpenLayers.Format.GeoJSON(in_options);
        features = geojson_format.read(feature);

        fixAttributes(features, feature)
    
        //apply uncertainty to features
        vector_layer.addFeatures(features);

        if(map.signalLayerLoaded != undefined
            && vector_layer.urlname != undefined)
            map.signalLayerLoaded(url);
    });
}

function removeFromSelectControl(lyrname) {
    if (selectControl==undefined || selectControl==null) {
        return;
    }
    var currentLayers = selectControl.layers;
    for (var li=0; li<currentLayers.length; li++) {
        if (currentLayers[li].name==lyrname) {
            currentLayers.splice(li,1);
            break;
        }
    }

    if (lyrname=="Active Area") {
        activeAreaPresent = false;
        parent.displayArea(map.getExtent().toGeometry().getGeodesicArea(map.projection)/1000/1000);
        var verts = map.getExtent().toGeometry().clone().getVertices();
        var gll = new Array();
        if (verts.length > 0) {
            for (var v=0; v<verts.length; v++) {
                var pt = verts[v].transform(map.projection, map.displayProjection);
                gll.push(new google.maps.LatLng(pt.y, pt.x));
            }
        }
        parent.displayArea2((google.maps.geometry.spherical.computeArea(gll)/1000)/1000);

    }

    var isActive = selectControl.active;
    try{
        selectControl.unselectAll();
    }catch(err){}
    selectControl.deactivate();
    if(selectControl.layers) {
        selectControl.layer.destroy();
        selectControl.layers = null;
    }
    initSelectControlLayers(currentLayers);
    selectControl.handlers.feature.layer = selectControl.layer;
    if (isActive) {
        selectControl.activate();
    }
}

function initSelectControlLayers(layers) {
    if(layers instanceof Array) {
        selectControl.layers = layers;
        selectControl.layer = new OpenLayers.Layer.Vector.RootContainer(
            selectControl.id + "_container", {
                layers: layers
            }
            );
    } else {
        selectControl.layer = layers;
    }
}

function redrawWKTFeatures(featureWKT, name,hexColour,opacity) {
    var layers = map.getLayersByName(name);
    
    var layer_style = OpenLayers.Util.extend({},OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = hexColour;
    layer_style.strokeColor = hexColour;
    layer_style.fillOpacity = opacity;

    for (key in layers) {

        if (layers[key] != undefined) {

            var layer = map.getLayer(layers[key].id);

            if (layer.name == name) {

                layer.destroyFeatures();
                layer.style = layer_style;
                var geom = new OpenLayers.Geometry.fromWKT(featureWKT);

                geom = geom.transform(map.displayProjection, map.projection);
                layer.addFeatures([new OpenLayers.Feature.Vector(geom)]);
                layer.isFixed = false;
                layer.addFeatures([new OpenLayers.Feature.Vector(geom)]);
            }
        }
    }
}

function redrawFeatures(name, hexColour, opacity, radius, szUncertain) {
    var in_options = {
        'internalProjection': map.baseLayer.projection,
        'externalProjection': new OpenLayers.Projection("EPSG:4326")
    };
    var gjLayers = map.getLayersByName(name);

    var layer_style = OpenLayers.Util.extend({},OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = hexColour;
    layer_style.strokeColor = hexColour;
    layer_style.fillOpacity = opacity;
    layer_style.pointRadius = radius;
    layer_style.szUncertain = szUncertain;
    layer_style.fontWeight = "bold";
    
    for (key in gjLayers) {
        if (gjLayers[key] != undefined) {
            var layer = gjLayers[key];
            if (layer.name == name) {
                layer.style = layer_style;                
                layer.redraw(true);
            }
        }
    }
}

function zoomBoundsGeoJSON(layerName) {
    //get the other geojson layers and force them to redraw
    var geoJsonLayers = map.getLayersByClass("OpenLayers.Layer.Vector");
    for (key in geoJsonLayers) {
        var layer = geoJsonLayers[key];

        if (layer.name == layerName) {
            map.zoomToExtent(layer.getDataExtent());
        } 
    }
}

function zoomBoundsLayer(layername) {
    var wmsLayers = map.getLayersByClass("OpenLayers.Layer.WMS");
    for (key in wmsLayers) {

        if (map.layers[key] != undefined) {

            var layer = map.getLayer(map.layers[key].id);

            if (layer.name == layername){
                map.zoomToExtent(layer.getExtent());
            }
        }
    }
}

function removeItFromTheList(layername) {
    var gjLayers = map.getLayersByName(layername);
    
    for (key in gjLayers) {        
        if (gjLayers[key] != undefined) {
            var layer = map.getLayer(gjLayers[key].id);
            if (layer.name == layername) {
                if(layer.removeFeatures != undefined) layer.removeFeatures();
                map.removeLayer(layer);
            }
        }
    }
}

function removeDeselectedLayers(layerIds) {
    for (var key in mapLayers) {
        var found = false;
        var i = 0;
        while (! found && i < layerIds.length) {
            if (key == layerIds[i]) {
                found = true;
            }
            i++;
        }

        if (! found) {
            map.removeLayer(mapLayers[key]);
            mapLayers[key] = null;
        }
    }
}

function getCurrentFeatureInfo() {
    return jQuery('#featureinfocontent').html();
}

function URLEncode (clearString) {
    var output = '';
    var x = 0;
    clearString = clearString.toString();
    var regex = /(^[a-zA-Z0-9_.]*)/;
    while (x < clearString.length) {
        var match = regex.exec(clearString.substr(x));
        if (match != null && match.length > 1 && match[1] != '') {
            output += match[1];
            x += match[1].length;
        } else {
            if (clearString[x] == ' ')
                output += '+';
            else {
                var charCode = clearString.charCodeAt(x);
                var hexVal = charCode.toString(16);
                output += '%' + ( hexVal.length < 2 ? '0' : '' ) + hexVal.toUpperCase();
            }
            x++;
        }
    }
    return output;
}

function fixAttributes(features, feature){
    //add feature (geojson string) properties to all features attributes
    //when first object (geometry) in features (geometry array) has no attributes
    //occurs when geojson type is GeometryCollection instead of Feature
    try{
        if(features.length > 0){
            var f = features[0];
            var i = 0;
            for(key in f.attributes) i++;
            if(i == 0){
                var json_format = new OpenLayers.Format.JSON();
                var json = json_format.read(feature);
                if(json.properties != undefined){
                    for(i=0;i<features.length;i++) {
                        features[i].attributes = json.properties;
                    }
                }
            }
            for (var j=0;j<features.length;j++) {
                features[j].isMirror = false; 
            }
        }
    }catch(err){}
}

function loadKmlFile(name, kmlurl) {
    //Defiine your KML layer//
    var kmlLayer= new OpenLayers.Layer.Vector(name, {
        //Set your projection and strategies//
        projection: new OpenLayers.Projection("EPSG:4326"),
        strategies: [new OpenLayers.Strategy.Fixed()],
        //set the protocol with a url//
        protocol: new OpenLayers.Protocol.HTTP({
            //set the url to your variable//
            url: kmlurl,
            //format this layer as KML//
            format: new OpenLayers.Format.KML({
                //maxDepth is how deep it will follow network links//
                maxDepth: 1,
                //extract styles from the KML Layer//
                extractStyles: true,
                //extract attributes from the KML Layer//
                extractAttributes: true
            })
        })
    });
    return kmlLayer; 

}

var prevHoverData = null;
var prevHoverRequest = null;

function envLayerInspection(e) {
    try {
        infoHtml = envLayerHover(e);
        if(infoHtml != null) {
            var pt = map.getLonLatFromViewPortPx(new
                OpenLayers.Pixel(e.xy.x, e.xy.y) );

            popup = new OpenLayers.Popup.FramedCloud("featurePopup",
                pt,
                new OpenLayers.Size(20,20),
                "<div id='sppopup' style='width: 350px; height: 50px;'>" + "Loading..." + "</div>"
                ,
                null, true, onPopupClose);

            var feature = popup;
            feature.popup = popup;
            popup.feature = feature;
            map.addPopup(popup, true);

            pt = pt.transform(map.projection, map.displayProjection);

            infoHtml = "<div id='sppopup'>"
                + "<table><tr><td>Point</td><td><b>" 
                + pt.lon.toPrecision(5) 
                + ", " 
                + pt.lat.toPrecision(5)
                + "</b></td></tr>" 
                + infoHtml
                + "</table>"
                + "</div>";

            if (document.getElementById("sppopup") != null) {
                document.getElementById("sppopup").innerHTML = infoHtml;
            }
        }
    }catch(err){
        alert(err);
    }
}

var last_hover_pos = null;
var last_hover_data = null;
function envLayerHover(e) {
    //This variable will contain the body text to be displayed in the popup.
    var body = "";

    var pt = map.getLonLatFromViewPortPx(new OpenLayers.Pixel(e.xy.x, e.xy.y) );
    pt = pt.transform(map.projection, map.displayProjection);

    var this_pos = pt.lat + "," + pt.lon;
    if(this_pos == last_hover_pos) {
        return last_hover_data;
    }
    last_hover_pos = this_pos;

    try {
        var layers = map.getLayersByClass("OpenLayers.Layer.WMS");

        //find first valid layer, if any
        var names = "";
        for(var i=layers.length-1;i>=0;i--) {
            var layer = layers[i];

            var p0 = layer.url.indexOf("geoserver");
            var p1 = layer.url.indexOf("ALA:");
            var p2 = layer.url.indexOf("&",p1+1);
            if(p0 < 0 || p1 < 0 || p1 < 0) {
                continue;
            }

            if(p2 < 0) p2 = layer.url.length;

            if(names.length > 0) {
                names = names + ",";
            }
            names = names + layer.url.substring(p1+4,p2);
        }

        if (names.length == 0) {
            return null;
        }

        var data = getLayerValue(names, pt.lat, pt.lon);
    
        if(data != null && data.length > 0) {
            for(i=0;i<data.length;i++) {
                body = body + "<tr><td>" + data[i].layername + "</td><td><b>" + data[i].value + "</b></td></tr>";
            }
            last_hover_data = body;
            return body;
        }  
    }catch(err){
    //console.error("an error has occurred!");
    }
    return null;
}
var hovercontrol = null;
var hovercontrolprevpos = null;
function initHover() {
    hovercontrol = new OpenLayers.Handler.Hover({
        'map': map
    }, {
        'pause': function(e) {
            var pt = map.getLonLatFromViewPortPx(new
                OpenLayers.Pixel(e.xy.x, e.xy.y) );

            pt = pt.transform(map.projection, map.displayProjection);

            var this_pos = pt.lat + "," + pt.lon;
            if(this_pos == hovercontrolprevpos) {
                return;
            }
            hovercontrolprevpos = this_pos;
    
            var output = parent.document.getElementById('hoverOutput');
            var data = envLayerHover(e);
            if(data != null) {
                output.innerHTML = "<table><tr><td>Point</td><td><b>" + pt.lon.toPrecision(5) + ", " + pt.lat.toPrecision(5) + "</b></td></tr>" + data + "</table>";
            } else {
                output.innerHTML = "No values to display";
            }
        },
        'delay': 200
    });    
    hovercontrol.fallThrough = true;
    hovercontrol.activate();
}
function toggleActiveHover() {
    if(hovercontrol != null) {
        hovercontrol.deactivate();
        hovercontrol = null;
        parent.jq('$hovertool')[0].style.display="none"
        document.getElementById("hoverTool").style.backgroundImage = "url('img/overview_replacement_off.gif')";
    } else {
        initHover();
        parent.document.getElementById('hoverOutput').innerHTML = "Hover cursor over map to view layer values";
        parent.jq('$hovertool')[0].style.display=""
        document.getElementById("hoverTool").style.backgroundImage = "url('img/overview_replacement.gif')";
    }
}

//Function to enable and disable the clickEventHandler
function toggleClickHandler(state){
    if (state == false){
        clickEventHandler.deactivate();
    }
    else{
        clickEventHandler.activate();
    }
}

//string extensions - put back in here - didn't seem to work when I added this code to index.js

if(typeof(String.prototype.capitalize) === "undefined"){
    String.prototype.capitalize = function() {
        return this.charAt(0).toUpperCase() + this.slice(1);
    }
}

if(typeof(String.prototype.trim) === "undefined"){
    String.prototype.trim = function()
    {
        return String(this).replace(/^\s+|\s+$/g, '');
    };
}

//backwards-compatible console logging

if (window['loadFirebugConsole']) {
    window.loadFirebugConsole();
} else {
    if (!window['console']) {
        window.console = {};
        window.console.info = function(msg){
            return;
        }
        window.console.log = function(msg){
            return;
        }
        window.console.warn = function(msg){
            alert("Console warning: " + msg);
        }
        window.console.error = function(msg){
            alert("Console error: " + msg);
        }
    }
}

function getLayerValue(layer, lat, lon) {
    var url = parent.jq('$layers_url')[0].innerHTML + "/intersect/" + layer + "/" + lat + "/" + lon;
    var ret = "";
    $.ajax({
        url: proxy_script + URLEncode(url),
        dataType: "json",
        success: function(data){
            ret = data;
        },
        async: false
    });
    return ret; 
}

function getOccurrence(query, lat, lon, start, pos, dotradius) {
    dotradius = dotradius*1 + 3
    var px = map.getViewPortPxFromLonLat(new OpenLayers.LonLat(lon,lat).transform(
            new OpenLayers.Projection("EPSG:4326"),map.getProjectionObject()));
    var lonlat = map.getLonLatFromViewPortPx(new OpenLayers.Pixel(px.x + dotradius, px.y + dotradius)).transform(
            map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));    
    var lonSize = Math.abs(lon - lonlat.lon);
    var latSize = Math.abs(lat - lonlat.lat);
    var url = parent.jq('$biocache_service_url')[0].innerHTML + "/webportal/occurrences?q=" + query
        + "&fq=longitude:[" + (lon-lonSize) + "%20TO%20" + (lon+lonSize) + "]"
        + "&fq=latitude:[" + (lat-latSize) + "%20TO%20" + (lat+latSize) + "]"
        + "&pageSize=1&facets=none";  
    var ret = null;
    $.ajax({
        url: proxy_script + URLEncode(url + "&start=" + start),
        dataType: "json",
        success: function(data){
            ret = data; 
        },
        async: false
    });
    query_size[pos] = 0;
    if(ret != null) {
        query_size[pos] = ret.totalRecords;
        query_[pos] = query;
        query_url[pos] = url;
        return ret.occurrences[0];
    } else {
        return null;
    }
}

function getOccurrenceUploaded(query, lat, lon, start, pos, dotradius) {
    dotradius = dotradius*1 + 3
    var px = map.getViewPortPxFromLonLat(new OpenLayers.LonLat(lon,lat).transform(
            new OpenLayers.Projection("EPSG:4326"),map.getProjectionObject()));
    var lonlat = map.getLonLatFromViewPortPx(new OpenLayers.Pixel(px.x + dotradius, px.y + dotradius)).transform(
            map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));
    var lonSize = Math.abs(lon - lonlat.lon);
    var latSize = Math.abs(lat - lonlat.lat);
    var url = parent.jq('$webportal_url')[0].innerHTML + "/ws/occurrences?q=" + query
        + "&box=" + (lon-lonSize) + "," + (lat-latSize) + "," + (lon+lonSize) + "," + (lat+latSize);
    var ret = null;
    $.ajax({
        url: proxy_script + URLEncode(url + "&start=" + start),
        dataType: "json",
        success: function(data){
            ret = data; 
        },
        async: false
    });
    query_size[pos] = 0;
    if(ret != null) {
        query_size[pos] = ret.totalRecords;
        query_[pos] = query;
        query_url[pos] = url;
        return ret.occurrences[0];
    } else {
        return null;
    }
}

