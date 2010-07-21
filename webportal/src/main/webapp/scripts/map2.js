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
var drawinglayer; // OpenLayers.Layer.Vector layer for ncwms transects
var drawingLayerControl; // control wigit for drawinglayer
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

var polygonControl = null;
var radiusControl = null;  //for deactivate after drawing
var boxControl = null;	//for deactivate after drawing
var areaSelectControl = null;
var samplingPolygon = null;		//temporary for destroy option after display
var filteringPolygon = null;	//temporary for destroy option after display
var alocPolygon = null;			//temporary for destroy option after display
var polygonLayer = null;
var boxLayer = null;
var radiusLayer = null;
var featureSelectLayer = null;
var areaSelectOn = false;
var layersLoading = 0;
var layername; // current layer name

var attempts = 0;
var libraryLoaded = false;
var checkLibraryLoadedTimeout = null;
var libraryCheckIntervalMs=100;
var secondsToWaitForLibrary=30;
var maxAttempts = (secondsToWaitForLibrary * 1000) / libraryCheckIntervalMs;

var gazetteerURL = "http";
/*
var overCallback = {
    over: featureOver,
    out: hideTooltip
};
 */

var selecteFeature;

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
                "problem, please contact IMOS for assistance"
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

var bLayer;
function loadBaseMap() {

    // Google.v3 uses EPSG:900913 as projection, so we have to
    // transform our coordinates
    map.setCenter(
        new OpenLayers.LonLat(134, -25).transform(
            new OpenLayers.Projection("EPSG:4326"),
            map.getProjectionObject()),
        4);
}

function buildMap() {
    checkLibraryLoadedTimeout = setInterval('checkLibraryLoaded()', libraryCheckIntervalMs);
}

function buildMapReal() {


    var viewportwidth;
    var viewportheight;

    // the more standards compliant browsers (mozilla/netscape/opera/IE7) use window.innerWidth and window.innerHeight

    if (typeof window.innerWidth != 'undefined')
    {
        viewportwidth = window.innerWidth,
        viewportheight = window.innerHeight
    }

    // IE6 in standards compliant mode (i.e. with a valid doctype as the first line in the document)

    else if (typeof document.documentElement != 'undefined'
        && typeof document.documentElement.clientWidth !=
        'undefined' && document.documentElement.clientWidth != 0)
        {
        viewportwidth = document.documentElement.clientWidth,
        viewportheight = document.documentElement.clientHeight
    }

    // older versions of IE
    else
    {
        viewportwidth = document.getElementsByTagName('body')[0].clientWidth,
        viewportheight = document.getElementsByTagName('body')[0].clientHeight;
    }
    //alert('<p>Your viewport width is '+viewportwidth+'x'+viewportheight+'</p>');


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

    //jQuery("div#mapinfo ").css("opacity","0.5");
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
            div: document.getElementById('mapscale')
        }),
        /*
        new OpenLayers.Control.OverviewMap({
            autoPan: true,
            minRectSize: 30,
            mapOptions:{
                resolutions: [  0.3515625, 0.17578125, 0.087890625, 0.0439453125, 0.02197265625, 0.010986328125, 0.0054931640625
                , 0.00274658203125, 0.001373291015625, 0.0006866455078125, 0.00034332275390625,  0.000171661376953125
                ],
                projection: "EPSG:900913",
                units: 'm'
            }
        }),
        */
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

    /*
    yahooLayer = new OpenLayers.Layer.Yahoo( "Yahoo", {
        'sphericalMercator': true,
        maxExtent: new OpenLayers.Bounds(-20037508, -20037508, 20037508, 20037508.34),
        maxResolution: 156543.0339 
    });
    //map.addLayer(yahooLayer);
     */
    bLayer = new OpenLayers.Layer.Google("Google Hybrid",
    {
        type: G_HYBRID_MAP,
        'sphericalMercator': true
    });
    map.addLayer(bLayer);

    var dem = new OpenLayers.Layer.WMS( "ALA - dem9s",
        "http://spatial.ala.org.au/geoserver/wms", {
            layers: 'ALA:dem-9s',
            transparent:true
        }, {
            'opacity': 0.4,
            visibility: true,
            'isBaseLayer': false,
            'wrapDateLine': true
        } );

    //map.addLayer(dem);
    loadBaseMap(); 

    // create a new event handler for single click query
    clickEventHandler = new OpenLayers.Handler.Click({
        'map': map
    }, {
        'click': function(e) {
            getpointInfo(e);
            mkpopup(e);
        }
    });
    clickEventHandler.activate();
    clickEventHandler.fallThrough = false;

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

}










function createBoxDrawingTool() {
    //removeSpeciesSelection();
    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = "red";
    layer_style.strokeColor = "red";	

    boxLayer = new OpenLayers.Layer.Vector("Box Layer", {
        style : layer_style
    });

    boxControl = new OpenLayers.Control.DrawFeature(boxLayer,OpenLayers.Handler.Box,{
        'featureAdded':regionAdded,
        'displayClass':"olControlDrawFeatureItemActive"
    });
    //map.addControl(boxControl);
    //boxControl.activate();	
 
    return boxControl;
}

function createAreaToolsButton() {
    var button = new OpenLayers.Control.Button({
        'displayClass': '#navtoolbar div.olControlDrawFeatureZoomBoxItemInactive',
        trigger: addPolygonDrawingTool
    });
    return button;
}

function navigateToAreaTab() {
    parent.setAreaTabSelected();
}

var defaultSelectFeatureStyle = null;
function addFeatureSelectionTool() {
    //    areaSelectControl = new OpenLayers.Control.SelectFeature(
    //        selectionLayers,
    //        {
    //            //            clickout: true,
    //            //            toggle: false,
    //            //            multiple: false,
    //            //            hover: false,
    //            //            toggleKey: "ctrlKey", // ctrl key removes from selection
    //            //            multipleKey: "shiftKey" // shift key adds to selection
    //            'onSelect': featureSelected
    //        }
    //        );
    //
    //    map.addControl(areaSelectControl);
    //    selectControl.deactivate();
    //    clickEventHandler.deactivate();
    //    areaSelectControl.activate();
    removeAreaSelection();
    areaSelectOn = true;
    //    clickEventHandler = new OpenLayers.Handler.Click({
    //        'map': map
    //    }, {
    //        'click': function(e) {
    //            getpointInfo(e);
    //            mkpopup(e);
    //        }
    //    });
    //    clickEventHandler.activate();
    //    clickEventHandler.fallThrough = false;
    //  setVectorLayersSelectable();
    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = "red";
    layer_style.strokeColor = "red";

    featureSelectLayer = new OpenLayers.Layer.Vector("Selected Feature Layer", {
        style: layer_style
    });
    featureSelectLayer.setVisibility(true);
    map.addLayer(featureSelectLayer);
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
        style: layer_style
    });
    radiusLayer.setVisibility(true);
    map.addLayer(radiusLayer);
    radiusControl = new OpenLayers.Control.DrawFeature(radiusLayer,OpenLayers.Handler.RegularPolygon,{
        'featureAdded':radiusAdded,
        handlerOptions:radiusOptions
    });
    map.addControl(radiusControl);
    radiusControl.activate();
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
    polyControl = new OpenLayers.Control.DrawFeature(polygonLayer,OpenLayers.Handler.Polygon,{
        'featureAdded':polygonAdded
    });
    map.addControl(polyControl);
    polyControl.activate();	
//////
}
////Copy for Sampling, ALOC, Filtering
//function addPolygonDrawingToolSampling() {
//    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
//    layer_style.fillColor = "blue";
//    layer_style.strokeColor = "blue";
//    samplingPolygon = new OpenLayers.Layer.Vector("Polygon Layer", {
//        style: layer_style
//    });
//
//    samplingPolygon.setVisibility(true);
//    map.addLayer(samplingPolygon);
//    polyControl =new OpenLayers.Control.DrawFeature(samplingPolygon,OpenLayers.Handler.Polygon,{
//        'featureAdded':polygonAddedSampling
//    });
//    map.addControl(polyControl);
//    polyControl.activate();
////////
//}
//function removePolygonSampling(){
//    if(samplingPolygon != null){
//        samplingPolygon.destroy();
//        samplingPolygon = null;
//    }
//}

//function addPolygonDrawingToolALOC() {
//    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
//    layer_style.fillColor = "green";
//    layer_style.strokeColor = "green";
//    alocPolygon = new OpenLayers.Layer.Vector("Polygon Layer", {
//        style: layer_style
//    });
//
//    alocPolygon.setVisibility(true);
//    map.addLayer(alocPolygon);
//    polyControl =new OpenLayers.Control.DrawFeature(alocPolygon,OpenLayers.Handler.Polygon,{
//        'featureAdded':polygonAddedALOC
//    });
//    map.addControl(polyControl);
//    polyControl.activate();
////////
//}
//function removePolygonALOC(){
//    if(alocPolygon != null){
//        alocPolygon.destroy();
//        alocPolygon = null;
//    }
//}

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

}

//function addPolygonDrawingToolFiltering() {
//    ////adding polygon control and layer
//    filteringPolygon = new OpenLayers.Layer.Vector("Polygon Layer");
//    filteringPolygon.setVisibility(true);
//    map.addLayer(filteringPolygon);
//    polyControl =new OpenLayers.Control.DrawFeature(filteringPolygon,OpenLayers.Handler.Polygon,{
//        'featureAdded':polygonAddedFiltering
//    });
//    map.addControl(polyControl);
//    polyControl.activate();
////////
//}
//function removePolygonFiltering(){
//    if(filteringPolygon != null){
//        filteringPolygon.destroy();
//        filteringPolygon = null;
//    }
//}

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
    // alert("win!");
   
    areaSelectOn = false;
    setVectorLayersSelectable();
    parent.setPolygonGeometry(feature.geometry);

// featureSelectLayer.addFeatures([new OpenLayers.Feature.Vector(feature.geometry)]);
//  polygonAddedGlobal(new OpenLayers.Feature.Vector(feature.geometry));
   
    
//  areaSelectControl.deactivate();
//  clickEventHandler.activate();
}

function radiusAdded(feature) {
    
    removeAreaSelection();
    // addWKTFeatureToMap(feature.geometry,"Test",'blue');
    radiusControl.deactivate();
    setVectorLayersSelectable();
    parent.setPolygonGeometry(feature.geometry);
}

// This function passes the region geometry up to javascript in index.zul which can then send it to the server.
function regionAdded(feature) {
    

    //converting bounds from pixel value to lonlat - annoying!
    var geoBounds = new OpenLayers.Bounds();
    geoBounds.extend(map.getLonLatFromPixel(new OpenLayers.Pixel(feature.geometry.left,feature.geometry.bottom)));
    geoBounds.extend(map.getLonLatFromPixel(new OpenLayers.Pixel(feature.geometry.right,feature.geometry.top)));

    removeAreaSelection();
    //    var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
    //    layer_style.fillColor = "red";
    //    layer_style.strokeColor = "red";
    //    boxLayer = new OpenLayers.Layer.Vector("Box Layer", {
    //        style : layer_style
    //    });
    //    boxLayer.setVisibility(true);
    //    map.addLayer(boxLayer);
    //    boxLayer.addFeatures([new OpenLayers.Feature.Vector(geoBounds.toGeometry())]);
    boxControl.deactivate();
    setVectorLayersSelectable();
    parent.setRegionGeometry(geoBounds.toGeometry());


  
}

// This function passes the geometry up to javascript in index.zul which can then send it to the server.
function polygonAdded(feature) {
    removeAreaSelection();
    parent.setPolygonGeometry(feature.geometry);
   
    polyControl.deactivate();
    setVectorLayersSelectable();
  
}
//// Copy for Sampling, ALOC, Filtering, This function passes the geometry up to javascript in index.zul which can then send it to the server.
//function polygonAddedSampling(feature) {
//    parent.setPolygonGeometrySampling(feature.geometry);
//    polyControl.deactivate();
//}
//function polygonAddedALOC(feature) {
//    parent.setPolygonGeometryALOC(feature.geometry);
//    polyControl.deactivate();
//}
//function polygonAddedFiltering(feature) {
//    parent.setPolygonGeometryFiltering(feature.geometry);
//    polyControl.deactivate();
//}

function setVectorLayersSelectable() {
    try {
        var layersV = map.getLayersByClass('OpenLayers.Layer.Vector');
        selectControl = new OpenLayers.Control.SelectFeature(layersV);
        map.addControl(selectControl);
        selectControl.activate();
    } catch (err) {

    }
}
function forceRedrawVectorLayers() {
    try {
        var layersV = map.getLayersByClass('OpenLayers.Layer.Vector');
        for (var i = 0; i < layersV.length; i++) {
            console.log("redrawing: " + layersV[i].name);
            //layersV[i].display(false);
            //layersV[i].display(true);
            //layersV[i].display(true);
            var l = layersV[i];
            redrawFeatures(l.features, l.name, l.style.fillColor, l.style.fillOpacity, l.style.pointRadius)();
        }
    } catch (err) {

    }
}

function selected (evt) {

    var feature = (evt.feature==null)?evt:evt.feature;
    var attrs = feature.attributes;
    
    if (areaSelectOn) {
        featureSelected(feature);
    }
    else {
        //test to see if its occurrence data
        if (attrs["occurrenceid"] != null) {
            var species = "<a href='http://bie.ala.org.au/species/" + attrs["taxonconceptid"] + "' target='_blank'>" + attrs["scientificname"] + "</a>";
            if (!attrs["taxonconceptid"]) {
                species = attrs["scientificname"];
            }
            var occurrencedate = attrs["occurrencedate"];
            if (!occurrencedate) occurrencedate="";
            popup = new OpenLayers.Popup.FramedCloud("featurePopup",
                feature.geometry.getBounds().getCenterLonLat(),
                new OpenLayers.Size(100,100),
                "<h2>Occurrence information</h2>" +
                " Scientific name: " + species + "</a> <br />" +
                " Family: <a href='http://bie.ala.org.au/species/" + attrs["familyconceptid"] + "' target='_blank'>" + attrs["family"] + "</a> <br />" +
                " Longitude: "+attrs['longitude'] + " , Latitude: " + attrs['latitude'] + " <br/>" +
                " Occurrence date: " + occurrencedate + " <br />" +
                "Species Occurence <a href='http://biocache.ala.org.au/occurrences/" + attrs["occurrenceid"] + "' target='_blank'>View details</a>"
                ,
                null, true, onPopupClose);

        } else {


            var html = "<h2>Feature Details</h2>";


            for (key in attrs) {
                html = html + "<br>" +  key + " : "  + attrs[key];
            }

            popup = new OpenLayers.Popup.FramedCloud("featurePopup",
                feature.geometry.getBounds().getCenterLonLat(),
                new OpenLayers.Size(100,100),
                html
                ,
                null, true, onPopupClose);

        }
        feature.popup = popup;
        popup.feature = feature;
        map.addPopup(popup, true);
    }
   
}

function onFeatureUnselect(feature) {
    //alert("try");
    map.removePopup(feature.popup);
    feature.popup.destroy();
    feature.popup = null;
}

function onPopupClose(evt) {
    
    try {

        map.removePopup(this.feature.popup);
        this.feature.popup.destroy();
        this.feature.popup = null;
    
        selectControl.unselect(this.feature);

    } catch(err) {
    //alert(err);
    }
}

function addWKTFeatureToMap(featureWKT,name,hexColour) {
    //    alert(name);
    var in_options = {
        'internalProjection': map.baseLayer.projection,
        'externalProjection': new OpenLayers.Projection("EPSG:4326")
    };
    var styleMap = new OpenLayers.StyleMap(OpenLayers.Util.applyDefaults(
    {
        fillColor: hexColour,
        fillOpacity: 0.6,
        strokeOpacity: 1,
        strokeWidth: 2,
        strokeColor: hexColour
    },
    OpenLayers.Feature.Vector.style["new"]));

    var layer_style = OpenLayers.Util.extend({},OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = hexColour;
    layer_style.strokeColor = hexColour;

    var wktLayer = new OpenLayers.Layer.Vector(name, {
        style : layer_style
    });
    map.addLayer(wktLayer);
    
    var geom = new OpenLayers.Geometry.fromWKT(featureWKT);
    geom = geom.transform(map.displayProjection, map.projection);
    wktLayer.addFeatures([new OpenLayers.Feature.Vector(geom)]);

    wktLayer.isFixed = false;
    selectionLayers[selectionLayers.length] = wktLayer;

    return wktLayer;
}

function addJsonFeatureToMap(feature, name, hexColour, radius) {
    //    alert(name);
    var in_options = {
        'internalProjection': map.baseLayer.projection,
        'externalProjection': new OpenLayers.Projection("EPSG:4326")
    };


    var styleMap = new OpenLayers.StyleMap(OpenLayers.Util.applyDefaults(
    {
        fillColor: hexColour,
        fillOpacity: 0.6,
        strokeOpacity: 1,
        strokeWidth: 2,
        strokeColor: hexColour
    },
    OpenLayers.Feature.Vector.style["new"]));

    var layer_style = OpenLayers.Util.extend({},OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = hexColour;
    layer_style.strokeColor = hexColour;
    layer_style.pointRadius = radius;


    var geojson_format = new OpenLayers.Format.GeoJSON(in_options);
    var vector_layer = new OpenLayers.Layer.Vector(name);

    vector_layer.style = layer_style;
    vector_layer.isFixed = false;
    features = geojson_format.read(feature);
    vector_layer.addFeatures(features);

    ////selectionLayers[selectionLayers.length] = vector_layer;
    vector_layer.events.register("featureselected", vector_layer, selected);
    //selectControl = new OpenLayers.Control.SelectFeature(vector_layer);
    //map.addControl(selectControl);
    //selectControl.activate();

    //setTimeout('setVectorLayersSelectable()',2000);

    //addToSelectControl(vector_layer);

    // need to do it this way due to passing an object.. closures :)
    window.setTimeout(function(){
        //addToSelectControl(vector_layer);
        setVectorLayersSelectable();
    }, 2000);
        
    return vector_layer;
}
/*
// this commented block is for OpenLayers 2.9.1
// but doesn't seem to completely work
function addToSelectControl(newlyr) {
    if (selectControl==null) {
        selectControl = new OpenLayers.Control.SelectFeature([newlyr],
        {
            onSelect: selected,
            onUnselect: onFeatureUnselect
        });
        map.addControl(selectControl);
        selectControl.activate();
    } else {
        var currentLayers = selectControl.layers;
        currentLayers.push(newlyr);
        selectControl.setLayer(currentLayers);
    }
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
    selectControl.setLayer(currentLayers);
}
*/
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


    var isActive = selectControl.active;
    selectControl.unselectAll();
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
///selectControl.setLayer(currentLayers);
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
                layer.addFeatures([new OpenLayers.Feature.Vector(geom)]);
               
            // layer.addFeatures(features);


            }
        }
    }
}

function redrawFeatures(feature, name, hexColour, opacity, radius) {
    var in_options = {
        'internalProjection': map.baseLayer.projection,
        'externalProjection': new OpenLayers.Projection("EPSG:4326")
    };
    var gjLayers = map.getLayersByName(name);
    var geojson_format = new OpenLayers.Format.GeoJSON(in_options);
    features = geojson_format.read(feature);

    var layer_style = OpenLayers.Util.extend({},OpenLayers.Feature.Vector.style['default']);
    layer_style.fillColor = hexColour;
    layer_style.strokeColor = hexColour;
    layer_style.fillOpacity = opacity;
    layer_style.pointRadius = radius;
    
    for (key in gjLayers) {

        if (gjLayers[key] != undefined) {

            var layer = map.getLayer(gjLayers[key].id);

            if (layer.name == name) {

                layer.destroyFeatures();
                layer.style = layer_style;
                layer.addFeatures(features);


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

     
//var layer = map.getLayer(key);
     
}

function removeItFromTheList(layername) {
    var gjLayers = map.getLayersByName(layername);
    
    for (key in gjLayers) {        
        if (gjLayers[key] != undefined) {
            var layer = map.getLayer(gjLayers[key].id);
            if (layer.name == layername) {
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

/*---------------*/

function getpointInfo(e) {

    tmp_response = '';
    timeSeriesPlotUri = null;
    layername = new Object();
    queries = new Object(); // abandon all old queries
    queries_valid_content = false;
    timestamp = new Date().getTime(); // unique to this click
    requestCount = 0; // reset to keep layer count
    var lonlat = map.getLonLatFromPixel(e.xy);
    X = Math.round(lonlat.lon * 1000) / 1000;
    Y = Math.round(lonlat.lat * 1000) / 1000;

    var wmsLayers = map.getLayersByClass("OpenLayers.Layer.WMS");
    var imageLayers = map.getLayersByClass("OpenLayers.Layer.Image");
    //var geoJsonLayers = map.getLayersByClass("OpenLayers.Layer.Vector");
    wmsLayers = wmsLayers.concat(imageLayers);
    //wmsLayers = wmsLayers.concat(geoJsonLayers);

    var url = false;
    
    if (parent.disableDepthServlet == false) {
        getDepth(e);
    }


    
   

    for (key in wmsLayers) {

        if (map.layers[key] != undefined) {

            var layer = map.getLayer(map.layers[key].id);

            

            if ((! layer.isBaseLayer) && layer.queryable) {
                
                

                if (layer.animatedNcwmsLayer) {

                    if (layer.tile.bounds.containsLonLat(lonlat)) {
                        url = layer.baseUri +
                        "&EXCEPTIONS=application/vnd.ogc.se_xml" +
                        "&BBOX=" + layer.getExtent().toBBOX() +
                        "&I=" + e.xy.x +
                        "&J=" + e.xy.y +
                        "&INFO_FORMAT=text/xml" +
                        "&CRS=EPSG:4326" +
                        "&WIDTH=" + map.size.w +
                        "&HEIGHT=" +  map.size.h +
                        "&BBOX=" + map.getExtent().toBBOX();


                        timeSeriesPlotUri =
                        layer.timeSeriesPlotUri +
                        "&I=" + e.xy.x +
                        "&J=" + e.xy.y +
                        "&WIDTH=" + layer.map.size.w +
                        "&HEIGHT=" +  layer.map.size.h +
                        "&BBOX=" + map.getExtent().toBBOX();

                    }
                }
                else if (layer.params.VERSION == "1.1.1") {
                    url = layer.getFullRequestString({
                        REQUEST: "GetFeatureInfo",
                        EXCEPTIONS: "application/vnd.ogc.se_xml",
                        BBOX: layer.getExtent().toBBOX(),
                        X: e.xy.x,
                        Y: e.xy.y,
                        INFO_FORMAT: 'text/html',
                        QUERY_LAYERS: layer.params.LAYERS,
                        FEATURE_COUNT: 50,
                        BUFFER: layer.getFeatureInfoBuffer,
                        SRS: 'EPSG:4326',
                        WIDTH: layer.map.size.w,
                        HEIGHT: layer.map.size.h
                    });
                }

                else if (layer.params.VERSION == "1.1.0") {
                    url = layer.getFullRequestString({
                        REQUEST: "GetFeatureInfo",
                        EXCEPTIONS: "application/vnd.ogc.se_xml",
                        BBOX: layer.getExtent().toBBOX(),
                        X: e.xy.x,
                        Y: e.xy.y,
                        INFO_FORMAT: 'text/html',
                        QUERY_LAYERS: layer.params.LAYERS,
                        FEATURE_COUNT: 50,
                        BUFFER: layer.getFeatureInfoBuffer,
                        SRS: 'EPSG:4326',
                        WIDTH: layer.map.size.w,
                        HEIGHT: layer.map.size.h
                    });
                }

                else if (layer.params.VERSION == "1.3.0") {
                    url = layer.getFullRequestString({

                        REQUEST: "GetFeatureInfo",
                        EXCEPTIONS: "application/vnd.ogc.se_xml",
                        BBOX: layer.getExtent().toBBOX(),
                        I: e.xy.x,
                        J: e.xy.y,
                        INFO_FORMAT: 'text/xml',
                        QUERY_LAYERS: layer.params.LAYERS,
                        //Styles: '',
                        CRS: 'EPSG:4326',
                        BUFFER: layer.getFeatureInfoBuffer,
                        WIDTH: layer.map.size.w,
                        HEIGHT: layer.map.size.h
                    });
                }

                url = url.replace("GetMap", "GetFeatureInfo");
                url = url.replace("format=image/png", "");


                if (url) {

                    

                    // append unique ids to each query
                    var uuid = map.layers[key].id + "_" + timestamp;
                    var a;

                    var x =layer.featureInfoResponseType;
                    if (is_ncWms(x)) {

                        layername[layer.name+requestCount] = new Object();
                        a = layername[layer.name+requestCount];
                        a.setHTML_ncWMS = setHTML_ncWMS;
                        a.imageUrl = timeSeriesPlotUri;
                        a.layername = ucwords(layer.name);
                        a.unit = layer.ncWMSMetaData.unit;
                        a.uuid = uuid; // debug
                        a.responseFunction = setHTML_ncWMS;
                        a.url = url;
                        queries[uuid] = a;
                        timeSeriesPlotUri = null;
                    }
                    else if (isWms(x)) {

                        layername[layer.name+requestCount] = new Object();
                        a = layername[layer.name+requestCount];
                        a.layername = ucwords(layer.name);
                        a.uuid = uuid; // debug
                        a.responseFunction = setHTML2;
                        a.url = url;
                        queries[uuid] = a;
                    }

                    requestCount++;


                }
            } else {

               

                
        }
        }
    }


    for (theobj in queries) {
        OpenLayers.loadURL(queries[theobj].url, '', queries[theobj] , queries[theobj].responseFunction, setError);
    }

//setTimeout('hidepopup()', 4000);
}


function handleQueryStatus(theobj) {

    var c = "";
    var cnt = 0;
    var inaarray = false;
    var title;
    var body;

    // check its in the current click query
    for (x in queries) {
        if (queries[x].uuid == theobj.uuid) {
            inaarray = true;
        }
    }

    if (inaarray) {

        delete queries[theobj.uuid];

        for (k in queries) {
            cnt++;
        }


        if (requestCount != 1 ){
            c="s";
        } else{
            c=" ";
        }

        if (cnt > 0) {

            if (requestCount != 1 ){
                c="s";
            } else{
                c=" ";
            }
            title = "<h4>Searching <b>" + requestCount + "</b> layer" + c + "</h4>";
            if (cnt != 1 ){
                c="s";
            } else{
                c=" ";
            }
            body = "Waiting on the response for <b>" +
            cnt + "</b> layer" + c +
            "<img src=\"img/loading_small.gif\" />";

        }
        else {

            var tics = new Date().getTime(); // unique to this click
            tics =  tics - timestamp;
            var d = new Date(parseInt(tics));
            var milli = Math.round(60 * (d.getMilliseconds()/1000));

            tics =  "<b>" + d.getSeconds() + ":" + milli + "</b> seconds" ;

            if (queries_valid_content) {
                title = "<h4>Layer Search Finished</h4>";
            }
            else {                
                title = "<h4>No layer information found</h4>";
            }
            
            body = "<small>" + requestCount + " layer" + c + " responded in " + tics + "</small>";

        }
        // try to get the general information
        // setDepth will try to set 'featureinfoGeneral' as well
        if (parent.disableDepthServlet == false) {
            body = "<div id=\"featureinfoGeneral\">" + jQuery('#featureinfodepth').html() + "</div>" + body;
        }
        
            
        jQuery('#featureinfoheader').html(title).fadeIn(1200);
        jQuery('#featureinfostatus').html(body).fadeIn(400);
        return true;
    }
    else {
        return false;
    }

}

function is_ncWms(type) {
    return ((type == parent.ncwms)||
        (type == parent.thredds));
}

function isWms(type) {
    return (
        (type == parent.wms100) ||
        (type == parent.wms110) ||
        (type == parent.wms111) ||
        (type == parent.wms130) ||
        (type == parent.ncwms) ||
        (type == parent.thredds));
}


function getDepth(e) {

    var I= e.xy.x; //pixel on map
    var J= e.xy.y; // pixel on map
    var click = map.getLonLatFromPixel(new OpenLayers.Pixel(I,J));

    var url = "DepthServlet?" +
    "lon=" + click.lon +
    "&lat="  + click.lat ;
    
    var request = OpenLayers.Request.GET({
        url: url,
        headers: {
            "Content-Type": "application/xml"
        },
        callback: setDepth
    });
}

function setDepth(response) {

    var i = 0;
    var total_depths = 0;
    var xmldoc = response.responseXML;
    var depth = parseFloat(xmldoc.getElementsByTagName('depth')[0].firstChild.nodeValue);
    var desc = (depth > 0) ? "Altitude " : "Depth ";  
    var str = desc + "<b>" + Math.abs(depth) + "m</b>" ;

    str = str + " Lon:<b> " + X + "</b> Lat:<b> " + Y + "</b>";
    jQuery('#featureinfodepth').html(str);
    
    // if this id is available populate it and hide featureinfodepth
    if (jQuery('#featureinfoGeneral')) {
        jQuery('#featureinfoGeneral').html(str).fadeIn(400);
        jQuery('#featureinfodepth').hide();
    }
    

}

// designed for Geoserver valid response
function setHTML2(response) {

    var pointInfo_str = '';

    tmp_response = response.responseText;
    var html_content = "";

    if (tmp_response.match(/<\/body>/m)) {

        html_content  = tmp_response.match(/(.|\s)*?<body[^>]*>((.|\s)*?)<\/body>(.|\s)*?/m);
        if (html_content) {
            //trimmed_content= html_content[2].replace(/(\n|\r|\s)/mg, ''); // replace all whitespace
            html_content  = html_content[2].replace(/^\s+|\s+$/g, '');  // trim
        }
    }

    if (html_content.length > 0) {
        // at least one valid query
        queries_valid_content = true;
        this.layer_data = true;
    }
        
    if (handleQueryStatus(this)) {
        setFeatureInfo(html_content,true);
    }
    

}

function setHTML_ncWMS(response) {


    var xmldoc = response.responseXML;
    var lon  = parseFloat(xmldoc.getElementsByTagName('longitude')[0].firstChild.nodeValue);
    var lat  = parseFloat(xmldoc.getElementsByTagName('latitude')[0].firstChild.nodeValue);
    var startval  = parseFloat(xmldoc.getElementsByTagName('value')[0].firstChild.nodeValue);
    var x    = xmldoc.getElementsByTagName('value');
    var vals = "";
    var time = xmldoc.getElementsByTagName('time')[0].firstChild.nodeValue;

    if (x.length > 1) {
        var endval = parseFloat(xmldoc.getElementsByTagName('value')[x.length -1].childNodes[0].nodeValue);
        var endtime = xmldoc.getElementsByTagName('time')[x.length -1].firstChild.nodeValue;
    }

    var html = "";
    if (lon) {  // We have a successful result

        if (!isNaN(startval) ) {  // may have no data at this point
            var layer_type = " - ncWMS Layer";

            var human_time = new Date();
            human_time.setISO8601('2009-05-01T02:32:00.000Z');

            // ncwms timeseries plot image
            if (this.imageUrl != null) {
                tmp_response =
                "<image height=\"300\"width=\"325\"class=\"spaced\" src='" + this.imageUrl + "' " +
                "title='time series plot for "+this.layername+"' " +
                "alt='time series plot "+this.layername+"' />";
                layer_type = " - ncWMS Animated Layer";
            }
            if (endval == null) {
                vals = "<br /><b>Value at </b>"+human_time.toUTCString()+": <b>" + toNSigFigs(startval, 4)+"</b>"+this.unit;
            }
            else {

                var human_endtime = new Date();
                human_endtime.setISO8601(endtime);
                vals = "<br /><b>Start date:</b>"+human_time.toUTCString()+": <b>" + toNSigFigs(startval, 4)+"</b>"+this.unit;
                vals += "<br /><b>End date:</b>"+human_endtime.toUTCString()+":<b> " + toNSigFigs(endval, 4)+"</b>"+this.unit;
                vals += "<BR />";
            }

            lon = toNSigFigs(lon, 7);
            lat = toNSigFigs(lat, 7);

            layer_type = this.layername + layer_type;


            html = "<h3>"+layer_type+"</h3><div class=\"feature\"><b>Lon:</b> " + lon + "<br /><b>Lat:</b> " + lat +
            vals + "\n<BR />" + tmp_response;

            // to do add transect drawing here
            //
            html = html +"<BR><h6>Get a graph of the data along a transect via layer options!</h6>\n";
            // html = html +" <div  ><a href="#" onclick=\"addLineDrawingLayer('ocean_east_aus_temp/temp','http://emii3.its.utas.edu.au/ncWMS/wms')\" >Turn on transect graphing for this layer </a></div>";

            html = html +"</div>" ;
        }

    }
    else {
        html = "Can't get feature info data for this layer <a href='javascript:popUp('whynot.html', 200, 200)'>(why not?)</a>";
    }

    setFeatureInfo(html,true);
    queries_valid_content = true;
    handleQueryStatus(this);
	



}

function getCurrentFeatureInfo() {
    return jQuery('#featureinfocontent').html();
}

function setFeatureInfo(content,line_break) {

    showpopup();
    var br = "";
    if (line_break == true ) {
        br = "<hr>\n\n";
    }
    //jQuery('#featureinfocontent').html(content).hide();
    if (content.length > 0 ) {
        jQuery('#featureinfocontent').prepend(content+br).hide().fadeIn(400);
    }
    if (jQuery('#featureinfocontent').html() != "") {
        map.popup.setSize(new OpenLayers.Size(popupWidth,popupHeight));
    //
    }

    jQuery('#featureinfocontent').fadeIn(400);

}

// Special popup for ncwms transects
function mkTransectPopup(inf) {

    killTransectPopup(); // kill previous unless we can make these popups draggable
    var posi = map.getLonLatFromViewPortPx(new OpenLayers.Geometry.Point(60,20));

    var html = "<div id=\"transectImageheader\">" +
    "</div>" +
    "<div id=\"transectinfostatus\">" +
    "<h3>" + inf.label + "</h3><h5>Data along the transect: </h5>" + inf.line +  "</div>" +
    "<BR><img src=\"" + inf.transectUrl + "\" />" +
    "</div>" ;

    popup2 = new OpenLayers.Popup.AnchoredBubble( "transectfeaturepopup",
        posi,
        new OpenLayers.Size(popupWidth,60),
        html,
        null, true, null);

    popup2.autoSize = true;
    map.popup2 = popup2;
    map.addPopup(popup2);

    
}

function killTransectPopup() {
    if (map.popup2 != null) {
        map.removePopup(map.popup2);
        map.popup2.destroy();
        map.popup2 = null;
    }
}

// called when a click is made
function mkpopup(e) {
    
    var point = e.xy;
    var pointclick = map.getLonLatFromViewPortPx(point.add(2,0));

    // kill previous popup to startover at new location
    if (map.popup != null) {
        map.removePopup(map.popup);
        map.popup.destroy();
        map.popup = null;
    }

    

    var html = "<div id=\"featureinfoheader\"><h4>New Query:</h4></div>" +
    "<div id=\"featureinfostatus\">" +
    "Waiting on the response for <b>" + requestCount + "</b> layers..." +
    "<img src=\"/img/loading_small.gif\" /></div>"  +
    "<div id=\"featureinfodepth\"></div>" +
    "<div class=\"spacer\" style=\"clear:both;height:10px;\">&nbsp;</div>" +
    "<div id=\"featureinfocontent_topborder\"><img id=\"featureinfocontent_topborderimg\" src=\"img/mapshadow.png\" />\n" +
    "<div id=\"featureinfocontent\"></div>\n</div>" ;
    popup = new OpenLayers.Popup.AnchoredBubble( "getfeaturepopup",
        pointclick,
        new OpenLayers.Size(popupWidth,popupHeight),
        html,
        null, true, null);



    if (requestCount == "0") {
           
    //nothing to see here
    //move along
    // a prompt for stupid people
    //  jQuery('#featureinfostatus').html("<font class=\"error\">Please choose a queryable layer from the menu..</font>").fadeIn(400);
    } else {

        popup.panMapIfOutOfView = true;
        //popup.autoSize = true;
        map.popup = popup;
        map.addPopup(popup);
        map.popup.setOpacity(0.9);

        /* shrink back down while searching.
         * popup will always pan into view with previous size.
         * close image always therefore visible
         */
        map.popup.setSize(new OpenLayers.Size(popupWidth,60));

    }
}

function hidepopup() {

    if (map.popup != null && (getCurrentFeatureInfo() == default_content)) {
        jQuery('div.olPopup').fadeOut(500, function() {
            // if content has come in during fadeOut
            if (getCurrentFeatureInfo() != default_content) {
                showpopup();
            }
        });
    }
}

function showpopup() {

    if ((map.popup != null)) {
        map.popup.setOpacity(1);
        //jQuery('div.olPopup').show(200);
        setTimeout('imgSizer()', 500); // ensure the popup is ready
    }
// zoom into view
}

//server might be down
function setError(response) {
    alert("The server is unavailable");
}

Date.prototype.setISO8601 = function (string) {
    var regexp = "([0-9]{4})(-([0-9]{2})(-([0-9]{2})" +
    "(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\.([0-9]+))?)?" +
    "(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?";
    var d = string.match(new RegExp(regexp));

    var offset = 0;
    var date = new Date(d[1], 0, 1);

    if (d[3]) {
        date.setMonth(d[3] - 1);
    }
    if (d[5]) {
        date.setDate(d[5]);
    }
    if (d[7]) {
        date.setHours(d[7]);
    }
    if (d[8]) {
        date.setMinutes(d[8]);
    }
    if (d[10]) {
        date.setSeconds(d[10]);
    }
    if (d[12]) {
        date.setMilliseconds(Number("0." + d[12]) * 1000);
    }
    if (d[14]) {
        offset = (Number(d[16]) * 60) + Number(d[17]);
        offset *= ((d[15] == '-') ? 1 : -1);
    }

    offset -= date.getTimezoneOffset();
    time = (Number(date) + (offset * 60 * 1000));
    this.setTime(Number(time));
}

//Formats the given value to numSigFigs significant figures
//WARNING: Javascript 1.5 only!
function toNSigFigs(value, numSigFigs) {
    if (!value.toPrecision) {
        return value;
    } else {
        return value.toPrecision(numSigFigs);
    }
}

function ucwords( str ) {
    // Uppercase the first character of every word in a string
    return (str+'').replace(/^(.)|\s(.)/g, function ( $1 ) {
        return $1.toUpperCase ( );
    } );
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

function imgSizer(){
    //Configuration Options
    var max_width = popupWidth -80 ; 	//Sets the max width, in pixels, for every image
    var selector = 'div#featureinfocontent .feature img';

    //destroy_imagePopup(); // make sure there is no other
    var tics = new Date().getTime();

    $(selector).each(function(){
        var width = $(this).width();
        var height = $(this).height();
        //alert("here");
        if (width > max_width) {

            //Set variables	for manipulation
            var ratio = (max_width / width );
            var new_width = max_width;
            var new_height = (height * ratio);
            //alert("(popupwidth "+max_width+" "+width + ") " +height+" * "+ratio);

            //Shrink the image and add link to full-sized image
            $(this).animate({
                width: new_width
            }, 'slow').width(new_height);
               
            $(this).hover(function(){
                $(this).attr("title", "This image has been scaled down.")
            //$(this).css("cursor","pointer");
            });

        } //ends if statement
    }); //ends each function


}

function destroy_imagePopup(imagePopup) {
    //map.removePopup(imagePopup);
    //map.imagePopup.destroy();
    //map.imagePopup = null;
    jQuery("#" + imagePopup ).hide();

}


/*jQuery showhide (toggle visibility of element)
 *  param: the dom element
 *  ie: #theId or .theClass
 */
function showhide(css_id) {
    $(css_id).toggle(450);
}
/*jQuery show 
 *  param: the dom element
 *  ie: #theId or .theClass
 */
function show(css_id) {
    $(css_id).show(450);
}

function dressUpMyLine(line){

    var x = line.split(",");
    var newString = "";

    for(i = 0; i < x.length; i++){
        var latlon = x[i].split(" ");
        var lon = latlon[0].substring(0, latlon[0].lastIndexOf(".") + 4);
        var lat = latlon[1].substring(0, latlon[1].lastIndexOf(".") + 4);
        newString = newString + "Lon:" + lon + " Lat:" +lat + ",<BR>";
    }
    return newString;
}

// called via argo getfeatureinfo results
function drawSingleArgo(base_url, argo_id) {
     
    if (IsInt(argo_id)) {        
        //alert("true " + IsInt(argo_id));
        parent.setExtWmsLayer(base_url +'/geoserver/wms?styles=argo_large','Argo -'+ argo_id + '','1.1.1','argo_float','','platform_number = '+ argo_id + '');
    }
    else {
        alert("Please enter an Argo ID number");
    }
    
}

function IsInt(sText) {

    var ValidChars = "0123456789";
    var IsInt= true;
    sText = sText.trim();
    var Char;
    if (sText.length == "0") {
        IsInt = false;
    }
    else {
        for (i = 0; i < sText.length && IsInt == true; i++) {
            Char = sText.charAt(i);
            if (ValidChars.indexOf(Char) == -1) {
                IsInt = false;
            }
        }
    }
    return IsInt;

}


function acornHistory(request_string,div) {
    if (window.XMLHttpRequest)  {
        xhttp=new XMLHttpRequest();
    }
    else // Internet Explorer 5/6
    {
        xhttp=new ActiveXObject("Microsoft.XMLHTTP");
    }
    theurl = URLEncode(request_string);
    xhttp.open("GET","RemoteRequest?url=" + theurl,false);
    //&cql_filter=position_index={feature.position_index.value}
    xhttp.send("");
    xmlDoc=xhttp.responseXML;
    str= "";
        
    var x=xmlDoc.getElementsByTagName("topp:acorn_gbr");
        
    if (x.length > 1) {
        str = str + ("<table class=\"featureInfo\">");
        str =str + ("<tr><th>Date/Time</th><th>Speed</th><th>Direction</th></tr>");
        for (i=0;i<x.length;i++)
        {
            str = str + ("<tr><td>");
            str = str + (x[i].getElementsByTagName("topp:timecreated")[0].childNodes[0].nodeValue);
            str = str + ("</td><td>");
            str = str + (x[i].getElementsByTagName("topp:speed")[0].childNodes[0].nodeValue) + "m/s";
            str = str + ("</td><td>");
            str = str + (x[i].getElementsByTagName("topp:direction")[0].childNodes[0].nodeValue) + "&#176;N";
            str = str + ("</td></tr>");
        }
        str = str + ("</table>");
    }
    else {
        str="<p class=\"error\">No previous results.</p>";
    }
    jQuery("#acorn"+div).html(str);
    //alert(jQuery(".acorn"+div).html());
    return false;
}
