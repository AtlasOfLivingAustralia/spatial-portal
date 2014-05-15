var markers, argos, aatams, geo, aatamsWA;
var lonlat, popupClass, popupContentHTML, iconName, itemLabel;
var popups = [];
var AutoSizeFramedCloud = OpenLayers.Class(OpenLayers.Popup.FramedCloud, {
    'autoSize': true,
    'panMapIfOutOfView': true
});
var AutoSizeFramedCloudMinSize = OpenLayers.Class(OpenLayers.Popup.FramedCloud, {
    'autoSize': true,
    'minSize': new OpenLayers.Size(400, 400),
    'panMapIfOutOfView': true
});
var AutoSizeFramedCloudMaxSize = OpenLayers.Class(OpenLayers.Popup.FramedCloud, {
    'autoSize': true,
    'maxSize': new OpenLayers.Size(100, 100),
    'panMapIfOutOfView': true
});

function addLayers() {

    // proxy.cgi script provided by OpenLayers written in Python, must be on the same domain
    OpenLayers.ProxyHost = '/cgi-bin/proxy.cgi?url=';

    var ausimage = new OpenLayers.Layer.Image("auslandia",
        "img/aus_default.jpg",
        new OpenLayers.Bounds(0, -90, 180, 0),
        new OpenLayers.Size(3600, 1800)
    );
    //map.addLayer(ausimage);

    //markers = new OpenLayers.Layer.Markers( "Markers", {isBaseLayer:false});
    //map.addLayer(markers);
    //markers.setVisibility(false);


    //   addArgos();
    //addtestArgos(); 
    //addAatams();	
    //addWMS130test();
    //addWFStest();	
    //addGeoRssAttams();
    //addTestMarkers();
    //addTestOverloadMarkers(); // NEEDS A PROXY SETUP TO USE
    //addMarkers();
    //addRssAIMS();


    /*
     // featureClass: OpenLayers.Feature.WFS, extractAttributes: true,
     var aatams = new OpenLayers.Layer.WFS("AATAMS","http://obsidian.bluenet.utas.edu.au:8080/deegree-wfs/services?service=WFS",
     {typename:'aatams:DeploymentSummary',
     namespace:'xmlns(aatams=http://www.imos.org.au/aatams)',
     outputformat: 'text/xml; subtype=gml/3.1.1'},
     { extractAttributes: true,isBaseLayer: false}
     );
     //map.addLayer(aatams);
     */

}

function addWFStest() {


    // featureClass: OpenLayers.Feature.WFS, extractAttributes: true, 
    var wfstest = new OpenLayers.Layer.WMS("WFS_test", "http://obsidian.bluenet.utas.edu.au:8080/geoserver/wms",
        {layers: 'slip_SWAN-001'},
        { extractAttributes: true, isBaseLayer: false}
    );
    map.addLayer(wfstest);

}

function addWMS130test() {
    // version 1.3.0 creation
    aatamsWA = new OpenLayers.Layer.WMS("aatamsWA", "http://obsidian.bluenet.utas.edu.au:8080/ncWMS/wms",
        {layers: '67/v',
            transparent: "true",
            version: "1.3.0",
            EXCEPTIONS: "application-vnd.ogc.se_xml",
            CRS: "EPSG:4326"

        },
        {isBaseLayer: false,
            version: "1.3.0",
            tileSize: new OpenLayers.Size(32, 32),
            maxExtent: new OpenLayers.Bounds(150, -90, 180, -30),
            displayOutsideMaxExtent: false
        });
    //aatamsWA.displayOutsideMaxExtent = false;

    map.addLayer(aatamsWA);
    //alert(aatamsWA.getFullRequestString());

}

function addRssAIMS() {
    geo = new OpenLayers.Layer.GeoRSS("AIMS-RSS", "http://data.aims.gov.au/gbroosdata/services/rss/project/1",
        {icon: getIcon('plain')});
    map.addLayer(geo);
    //geo.loadRSS();
    //getRSSFeatureinfo(geo);
}


function addtestArgos() {
    // Create 850 random features, and give them a "type" attribute that
    // will be used to style them by size.
    var features = new Array(850);
    for (var i = 0; i < features.length; i++) {
        lon = (360 * Math.random()) - 180;
        lat = (180 * Math.random()) - 90;
        label = 'id-' + [i] + ' at ' + lon + ' ' + lat;
        features[i] = {'label': label, 'lonlat': new OpenLayers.LonLat(lon, lat),
            'type': 15 + parseInt(15 * Math.random())};
    }

    // Create a styleMap to style your features for two different
    // render intents.  The style for the 'default' render intent will
    // be applied when the feature is first drawn.  The style for the
    // 'select' render intent will be applied when the feature is
    // selected.
    var myStyles = new OpenLayers.StyleMap({
        "default": new OpenLayers.Style({
            pointRadius: "${type}"//, // sized according to type attribute
        }),
        "select": new OpenLayers.Style({
        })
    });

    // Create a layer and give it your style map.
    var points = new OpenLayers.Layer.Markers(
        'Points', {styleMap: myStyles}
    );


    for (var i = 0; i < features.length; i++) {
        lonlat = features[i]['lonlat'];
        itemLabel = features[i]['label'];
        addMarker(argos, 'argo', lonlat, AutoSizeFramedCloudMinSize, '<p>this is ' + itemLabel + '</p>', itemLabel, false, false, false);
    }

}


function addMarkers() {

    var icon = getIcon('plain');


    var err = new OpenLayers.Layer.Markers("test2", {parentMenuId: "test", menuId: "special1"});
    err.addMarker(new OpenLayers.Marker(new OpenLayers.LonLat(137.309937, -39.838604), icon.clone()));
    err.addMarker(new OpenLayers.Marker(new OpenLayers.LonLat(120, -40.6), icon.clone()));
    map.addLayer(err);

    var ch = new OpenLayers.Layer.Markers("test3", {parentMenuId: "test", menuId: "special2"});
    ch.addMarker(new OpenLayers.Marker(new OpenLayers.LonLat(136.309937, -35.838604), icon.clone()));
    ch.addMarker(new OpenLayers.Marker(new OpenLayers.LonLat(129, -39.6), icon.clone()));
    map.addLayer(ch);


}

function addTestMarkers() {


    //anchored bubble popup lonlat contents autosize minsize
    lonlat = new OpenLayers.LonLat(150, -20);
    popupClass = AutoSizeFramedCloudMinSize;
    iconName = 'plain';
    itemLabel = "A test";
    popupContentHTML = '<iframe src="http://www.google.com"></iframe>';
    addMarker(markers, iconName, lonlat, popupClass, popupContentHTML, itemLabel, false, true, false);

    //anchored bubble popup smalonlat contents autosize minsize closebox
    lonlat = new OpenLayers.LonLat(155.5, -30);
    popupClass = AutoSizeFramedCloud;
    iconName = 'plain';
    itemLabel = "A test";
    popupContentHTML = '<div><h4>Test</h4><p><b>Lon:</b> 28.4765625<br><b>Lat:</b> -29.1796875<br><b>Value:</b> test of possibilities</p></div>';
    addMarker(markers, iconName, lonlat, popupClass, popupContentHTML, itemLabel, true, true, false);

    //anchored bubble popup bigger contents autosize closebox
    lonlat = new OpenLayers.LonLat(149.9, -40);
    popupClass = AutoSizeFramedCloud;
    iconName = 'plain';
    itemLabel = "A test framed cloud";
    popupContentHTML = '<iframe src="http://www.google.com"></iframe>';
    addMarker(markers, iconName, lonlat, popupClass, popupContentHTML, itemLabel, false, true, false);


}

function addTestOverloadMarkers() {


    var test = new OpenLayers.Layer.WMS("marine-geo", "http://www.marine-geo.org/exe/mapserv",
        {map: '/home/mgds/web/www.marine-geo.org/htdocs/services/ogc/wms.map',
            typename: 'Stations-World'
        },
        { extractAttributes: false, isBaseLayer: false}
    );
    map.addLayer(test);

}

function addArgostest() {

    // create the parent layer
    argos = new OpenLayers.Layer.Markers("Argo Floats", {isBaseLayer: false});
    map.addLayer(argos);

    // add markers to argos layer
    lonlat = new OpenLayers.LonLat(152.9, -45);
    popupClass = false;
    iconName = 'argo'; // open new window
    itemLabel = "Argo 1900047";
    popupContentHTML = 'http://www.cmar.csiro.au/remotesensing/oceancurrents/profiles/1900047/latest.html';
    addMarker(argos, iconName, lonlat, popupClass, popupContentHTML, itemLabel, false, true, true);
}


function addMarker(thelayer, iconName, lonlat, popupClass, popupContentHTML, itemLabel, closeBox, overflow, window) {


    var icon = getIcon(iconName);

    var feature = new OpenLayers.Feature(markers, lonlat, {icon: icon});
    feature.closeBox = closeBox;
    feature.popupClass = popupClass;
    feature.data.popupContentHTML = popupContentHTML;
    feature.data.overflow = (overflow) ? "auto" : "hidden";

    var marker = feature.createMarker();

    var markerClick = function (evt) {
        if (this.popup == null) {
            this.popup = this.createPopup(this.closeBox);
            map.addPopup(this.popup);
            this.popup.show();
        } else {
            this.popup.toggle();
        }
        currentPopup = this.popup;
        OpenLayers.Event.stop(evt);
    };
    var markerTooltip = function (evt) {

        feature.popupClass = OpenLayers.Class(OpenLayers.Popup.Anchored, {
            'autoSize': true, 'panMapIfOutOfView': true, 'closeBox': false});
        feature.data.popupContentHTML = markerTooltipDiv(itemLabel);
        if (this.popup == null) {
            this.popup = this.createPopup(this.closeBox);
            map.addPopup(this.popup);
            this.popup.show();
        } else {
            this.popup.toggle();
        }
        currentPopup = this.popup;
        OpenLayers.Event.stop(evt);

    }

    var markerURLClick = function (evt) {
        open(popupContentHTML, 'thiswindow');
    };

    if (popupClass) {
        marker.events.register("mousedown", feature, markerClick);
    }
    else {
        marker.events.register("mouseover", feature, markerTooltip);
        marker.events.register("mouseout", feature, markerTooltip);
        marker.events.register("mousedown", feature, markerURLClick);
    }
    thelayer.addMarker(marker);
}


function markerTooltipDiv(text) {
    return "<div class=\"spacer\">" + text + "</div>";
}

function getIcon(iconName) {

    var iconArray = new Array();
    var size, offset;

    size = new OpenLayers.Size(18, 18);
    offset = new OpenLayers.Pixel(-(size.w / 2 - 8), -size.h + 6);
    iconArray['action'] = new OpenLayers.Icon('img/blank.png', size, offset);

    size = new OpenLayers.Size(20, 18);
    offset = new OpenLayers.Pixel(-(size.w / 2 - 8), -size.h + 6);
    iconArray['plain'] = new OpenLayers.Icon('img/vector-marker.png', size, offset);

    size = new OpenLayers.Size(30, 24);
    offset = new OpenLayers.Pixel(-(size.w / 2), -size.h + 6);
    iconArray['argo'] = new OpenLayers.Icon('img/argo_float.png', size, offset);

    return iconArray[iconName];
}


function addAatams() {

    aatams = new OpenLayers.Layer.WMS("AATAMS-wms", "http://obsidian.bluenet.utas.edu.au:8080/geoserver/wms",
        {layers: 'DEPLOYMENT_SUMMARY2',
            transparent: "true"
        },
        {isBaseLayer: false});

    map.addLayer(aatams);

}

function addArgos() {

    argos = new OpenLayers.Layer.WMS("ARGOS", "http://obsidian.bluenet.utas.edu.au:8080/geoserver/wms",
        {layers: 'LAST_PROFILE_POINT',
            transparent: "true"
        },
        {isBaseLayer: false,
            tileSize: new OpenLayers.Size(32, 32),
            maxExtent: new OpenLayers.Bounds(150, -90, 180, -30),
            displayOutsideMaxExtent: false});

    map.addLayer(argos);

}

function addGeoRssAttams() {
    geo = new OpenLayers.Layer.GeoRSS("AATAMS-geoRSS", "http://obsidian.bluenet.utas.edu.au:8080/geoserver/wms/reflect?layers=DEPLOYMENT_SUMMARY2&format=rss",
        {icon: getIcon('plain')});
    map.addLayer(geo);
    geo.loadRSS();
}

function crap_to_delete() {


    var size = new OpenLayers.Size(150, 150);
    var tempPopup = new OpenLayers.Popup("tpop",
        lonlat,
        size,
        "Loading... please wait...",
        true, null
    );
    tempPopup.panMapIfOutOfView = true;
    tempPopup.autoSize = true;
    tempPopup.keepInMap = true;
    tempPopup.setBorder("3px solid white");
    tempPopup.updateSize();
    map.addPopup(tempPopup);


    //updateFeatureInfoFilters(url);
    OpenLayers.loadURL(url, '', this, setHTML, anError);
    //OpenLayers.loadURL(url, '', this, setHTML_inpop, anError);

}


function setHTML(response) {


    resultPopup.setContentHTML(response.responseText);

    setTimeout("clearPopups()", 1000);
}


function setHTML_inpop(response) {

    resultPopup = new OpenLayers.Popup("pop",
        lonlat,
        size,
        "<div><p>No data at </p></div>",
        true, close);


    if (true) {
        resultPopup.setContentHTML(response.responseText);
    }
    else {
        var closepopup = true;
    }
    resultPopup.autoSize = true;
    resultPopup.panMapIfOutOfView = true;
    resultPopup.keepInMap = true;
    resultPopup.updateSize();
    resultPopup.setBorder("10px solid white");
    map.addPopup(resultPopup, true); // closes all other popups
    popups.push(resultPopup);
    if (closepopup) {
        setTimeout("clearPopups()", 3000);
    }
}


// server might be down
function anError(response) {
    resultPopup.setContentHTML("<div><h3>ERROR</h3>" + response.responseText + "</div>");
}


//Clear the popups
function clearPopups() {
    for (var i = 0; i < popups.length; i++) {
        map.removePopup(popups[i]);
    }
    popups = [];
}












