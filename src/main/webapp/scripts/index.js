//zk.Widget.$(jq('$westContent')[0]).firstChild.lastChild.listen({onMouseUp: function () { setTimeout("map.pan(1,1);",300); }});

$(window.mapFrame).resize(function() {
  setTimeout("map.pan(1,1);",500);
});


function updateSafeToLoadMap(status) {                        
    try {
        zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'safeToLoadMap', status));
    } catch (err) {
        setTimeout(function() {
            updateSafeToLoadMap(status);
        }, 1000);
    }
}            
            
function roundNumber(num, dec) {
    var result = Math.round(num*Math.pow(10,dec))/Math.pow(10,dec);
    return result;
}
        
function setSearchPoint(lon,lat) {
    if (setSearchPointAnalysis != undefined) {
        setSearchPointAnalysis(lon,lat)
    }
}

function setSpeciesSearchPoint(lonlat) {
    if (setSpeciesSearchPointAnalysis != undefined) {
        setSpeciesSearchPointAnalysis(lonlat)
    }
}

function setExtent() {
    if (getGeographicExtentControls != undefined) {
        getGeographicExtentControls();
    }
}

function setPolygonGeometry(geometry) {
    if (setSelectionGeometry != undefined) {
        setSelectionGeometry(geometry);
    }
}

function setLayerGeometry(geometry) {
    if (setSelectionLayerGeometry != undefined) {
        setSelectionLayerGeometry(geometry);
    }
}

function setRegionGeometry(geometry) {
    if (setBoxGeometry != undefined) {
        setBoxGeometry(geometry);
    }
}

function reloadSpecies() {
    var value = "z=" + map.zoom + "&amp;b=" + map.getExtent().toBBOX();
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onReloadLayers', value));
}

function showInfo(curr) {
    window.mapFrame.showInfo(curr); 
}

function setSearchPointAnalysis(point_orig) {
    var point = point_orig.clone();
   
    // transform the point from Google Projection to EPSG:4326
    var mapObj = window.frames.mapFrame.map;
    point.transform(mapObj.projection, mapObj.displayProjection);
    var value = point.lon + "," + point.lat;
    zAu.send(new zk.Event(zk.Widget.$(jq('$areamappolygonwindow')[0]), 'onSearchPoint', value));
   // zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onSearchPoint', value));
}

function setSpeciesSearchPointAnalysis(point_orig) {
    var point = point_orig.clone();

    // transform the point from Google Projection to EPSG:4326
    var mapObj = window.frames.mapFrame.map;
    point.transform(mapObj.projection, mapObj.displayProjection);
    var value = point.lon + "," + point.lat;
    zAu.send(new zk.Event(zk.Widget.$(jq('$selectionwindow')[0]), 'onSearchSpeciesPoint', value));
    
}

function setSelectionGeometry(geometry_orig) {
    var geometry = geometry_orig.clone();

    // transform the geometry from Google Projection to EPSG:4326
    var mapObj = window.frames.mapFrame.map;
    geometry.transform(mapObj.projection, mapObj.displayProjection);
    var value = geometry.toString();
    zAu.send(new zk.Event(zk.Widget.$(jq('$areapolygonwindow')[0]), 'onSelectionGeom', value));
    zAu.send(new zk.Event(zk.Widget.$(jq('$areapointandradiuswindow')[0]), 'onSelectionGeom', value));
    
    
}

function setSelectionLayerGeometry(geometry_orig) {
    var geometry = geometry_orig;
    alert("Passing geom");
    var mapObj = window.frames.mapFrame.map;
    var value = "LAYER(" + geometry.toString() + ")";
    zAu.send(new zk.Event(zk.Widget.$(jq('$areamappolygonwindow')[0]), 'onSelectionGeom', value));
}

function setBoxGeometry(geometry_orig) {
    var geometry = geometry_orig.clone();
    // transform the geometry from Google Projection to EPSG:4326
    var mapObj = window.frames.mapFrame.map;
    geometry.transform(mapObj.projection, mapObj.displayProjection);
    var value = geometry.toString();
    zAu.send(new zk.Event(zk.Widget.$(jq('$areaboundingboxwindow')[0]), 'onBoxGeom', value));
}

function roundNumber(num, dec) {
    var result =
    Math.round(num*Math.pow(10,dec))/Math.pow(10,dec);
    return result;
}







        var retryFixExtent = 0;
        function fixExtent(a,b,c,d) {
            map.zoomToExtent(new OpenLayers.Bounds(a,b,c,d),true);

            //does not always stick
            if(retryFixExtent < 1) {
                retryFixExtent++;
                setTimeout(function() {fixExtent(a,b,c,d);}, 2000);
            }
        }



///////////////////////////////////////////////////
///////////////////////////////////////////////////
////  code for "onIframeMapFullyLoaded"
///////////////////////////////////////////////////
///////////////////////////////////////////////////



jQuery('.hidden_image :hidden').show();

//if (parent.location.href.indexOf("spatial-dev.ala.org.au") > -1 || parent.location.href.indexOf("spatial.ala.org.au") > -1 || parent.location.href.indexOf("spatial-test.ala.org.au") > -1 || parent.location.href.indexOf("localhost") > -1) {
//    jQuery('.z-north').show();
//}
if (parent.location.href.indexOf("explore") > -1 && parent.location.href.indexOf("species-maps") > -1) {
    jQuery('.nav-home').hide();
}

function printHack(o) {
    //session variables not in session
    var size = map.getSize().w + "," + (document.body.clientHeight-68);
    var extent = map.getExtent().toBBOX();
    var lhsWidth = 0;
    var basemap = currentbaselayertxt
    var mapHTML = size + "," + extent + "," + lhsWidth + "," + currentbaselayertxt;

    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$onPrint', mapHTML));
}
function changeBaseLayer(type) {
    currentbaselayertxt = type;

    $('li.bmapoption').removeClass('mapoptsel');

    if (type == 'normal') {
        map.setBaseLayer(bLayer2);
        $('#nor_mapoption').addClass('mapoptsel');
    } else if (type == 'hybrid') {
        map.setBaseLayer(bLayer);
        $('#sat_mapoption').addClass('mapoptsel');
    } else if (type == 'minimal') {
        map.setBaseLayer(bLayer3);
        $('#min_mapoption').addClass('mapoptsel');
    } else if (type == 'outline') {
        map.setBaseLayer(bLayer4);
        $('#out_mapoption').addClass('mapoptsel');
    }

    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onBaseMap', type));
}

function showSpeciesInfo(occids, lon, lat) {
    window.mapFrame.showSpeciesInfo(occids, lon, lat);
}

function goToUserLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position){
            window.mapFrame.goToLocation(position.coords.longitude, position.coords.latitude, 15);
        });
    } else {
        alert("Unable to determine your location");
    }
}

function displayBioStorCount(comp,val) {
    console.log("displayBioStorCount");
    console.log(comp);
    console.log(val);
    if (isNaN(val)) {
        $("#biostorrow").css('display','none');
    } else {
        $("#biostorrow").css('display','block');
        $("#"+comp).html(val);
    }
}
function displayHTMLInformation(element, info) {
    $('#'+element).html(info);
}
function displayArea(area) {
   $('#jsarea').html('OL area: ' + addCommas(area.toFixed(2)) + ' sq km');
}
function displayArea2(area) {
  $('#jsarea2').html(addCommas('' + area.toFixed(2)));
}
function addCommas(nStr) {
    nStr += '';
    x = nStr.split('.');
    x1 = x[0];
    x2 = x.length > 1 ? '.' + x[1] : '';
    var rgx = /(\d+)(\d{3})/;
    while (rgx.test(x1)) {
        x1 = x1.replace(rgx, '$1' + ',' + '$2');
    }
    return x1;
}


//Not required in zk 5.0.8
//capture mouse events during window resize or move over an iframe
//var overlayon = false;
//function needOverlay() {
//    var zkresizing = parent.document.getElementById("zk_ddghost");
//    var zkmoving = parent.document.getElementById("zk_wndghost");
//
//    if(zkresizing != null && zkresizing.className.indexOf('drop') >= 0) {
//        //do nothing with drag/drop
//    } else if(!overlayon && (zkmoving != null || zkresizing != null)) {
//        //sit above zk (z-index:1800)
//        parent.jq(parent.document.body).append("<div id='overlay_' style='position:absolute;width:100%;height:100%;top:0;left:0;z-index:1900;background-color:white;opacity:0.01;filter:alpha(opacity=1);'></div>");
//        overlayon = true;
//    } else if(overlayon && zkmoving == null && zkresizing == null) {
//        overlayon = false;
//        parent.jq(parent.document.getElementById("overlay_")).remove();
//    }
//    setTimeout(needOverlay, 500);
// }
//setTimeout(needOverlay, 500);  //start


//TODO: WIRE THESE INTO ZK

function addSpeciesAction(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddSpecies', null));
}

function addAreaAction(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddArea', null));
}

function addLayerAction(){
   zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddLayer', null));
}

function runSpeciesList(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnSpeciesList', null));
}

function runSitesBySpecies(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnSitesBySpecies', null));
}

function runAreaReport(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAreaReport', null));
}

function runNearestLocality(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'runNearestLocalityAction', null));
    mapFrame.toggleActiveNearest();
}

function runSamplingAction(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddSampling', null));
}

function runPrediction(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddMaxent', null));
}

function runClassification(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddAloc', null));
}

function runScatterPlot(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddScatterplot', null));
}

function runTabulation(){
//  uncomment this to trigger an error window
//    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onSearchSpeciesPoint', null));
    alert("Run Tabulation");
}

function runGDM(){
    alert("Run GDM");
}

function runImport(){
    alert("Run Import");
}

function runImportAnalysis() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'importAnalysis', null));
}

function runExport(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'exportArea', null));
}

function resetMap(){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$reloadPortal', null));
}

function loadHelp(page){
    help_base_url = jq('$help_url')[0].innerHTML + "/spatial-portal-help";
    if (undefined === page){
        page = 'spatial-portal-help-contents';
    }
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'openUrl', help_base_url + "/" + page));
}

function clearNearestMarkerLayer(){
    if(mapFrame != null && map != null && mapFrame.markers != null) {
        map.removeLayer(mapFrame.markers);
        mapFrame.markers = null;
    }
}

function openDistributionsChecklists(lsids){
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$openDistributionsChecklists', lsids));
}
