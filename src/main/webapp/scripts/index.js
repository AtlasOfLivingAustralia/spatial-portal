//zk.Widget.$(jq('$westContent')[0]).firstChild.lastChild.listen({onMouseUp: function () { console.log('map.pan');setTimeout("map.pan(1,1);",300); }});

$(".z-west-collapsed").click(function () {
    $(".menudiv").css("top", 0);
});


if (readCookie('ALA-Auth')) {
    $(".not_logged_in").hide();
    $(".logged_in").show();
} else {
    $(".not_logged_in").show();
    $(".logged_in").hide();
    //$(".login-bubble").show().delay(8000).fadeOut(2000);


    try {
        $(".login-bubble")[0].style.right  = ($(window).width() - $(".login-button").offset().left - $(".login-button").width()) + "px";
    } catch (e){

    }
}


function updateSafeToLoadMap(status) {
    try {
        zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'safeToLoadMap', status));
    } catch (err) {
        setTimeout(function () {
            updateSafeToLoadMap(status);
        }, 1000);
    }
}

function roundNumber(num, dec) {
    var result = Math.round(num * Math.pow(10, dec)) / Math.pow(10, dec);
    return result;
}

function setSearchPoint(lon, lat) {
    if (setSearchPointAnalysis != undefined) {
        setSearchPointAnalysis(lon, lat)
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
    zAu.send(new zk.Event(zk.Widget.$(jq('$pointcomparisonswindow')[0]), 'onMapClick', value));


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
        Math.round(num * Math.pow(10, dec)) / Math.pow(10, dec);
    return result;
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
    var size = map.getSize().w + "," + (document.body.clientHeight - 68);
    var extent = map.getExtent().toBBOX();
    var lhsWidth = 0;
    var basemap = currentbaselayertxt
    var mapHTML = size + "," + extent + "," + lhsWidth + "," + currentbaselayertxt;

    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$onPrint', mapHTML));
}
function changeBaseLayer(type) {
    if (undefined === map) {
        return;
    }

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
        navigator.geolocation.getCurrentPosition(function (position) {
            window.mapFrame.goToLocation(position.coords.longitude, position.coords.latitude, 15);
        });
    } else {
        alert("Unable to determine your location");
    }
}

function displayHTMLInformation(element, info) {
    $('#' + element).html(info);
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

function addSpeciesAction() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddSpecies', null));
}

function addAreaAction() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddArea', null));
}

function addLayerAction() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddLayer', null));
}

function addFacetAction() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddFacet', null));
}

function addWMSLayerAction() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddWMSLayer', null));
}

function runSpeciesList() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnSpeciesList', null));
}

function runSitesBySpecies() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnSitesBySpecies', null));
}

function runPhylogeneticDiversity() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnPhylogeneticDiversity', null));
}

function runAreaReport() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAreaReport', null));
}

function runAreaReportPDF() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAreaReportPDF', null));
}

function runNearestLocality() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'runNearestLocalityAction', null));
    mapFrame.toggleActiveNearest();
}

function runPointComparisons() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'runPointComparisons', null));
}


function runSamplingAction() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddSampling', null));
}

function runPrediction() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddMaxent', null));
}

function runAooEoo() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddAooEoo', null));
}

function runClassification() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddAloc', null));
}

function runScatterPlot() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddScatterplot', null));
}

function runScatterPlotList() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddScatterplotList', null));
}

function runTabulation() {
//  uncomment this to trigger an error window
//    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onSearchSpeciesPoint', null));
    //alert("Run Tabulation");
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'runTabulation', null));
}

function runGDM() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$btnAddGDM', null));
}

function runImportSpecies(type) {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'importSpecies', type));
}

function runGeneratePoints(type) {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'generatePoints', null));
}

function runImportAreas() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'importAreas', null));
}

function runImportAnalysis() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'importAnalysis', null));
}

function runExport() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'exportArea', null));
}

function resetMap() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$reloadPortal', null));
}

function loadHelp(page) {
    help_base_url = jq('$help_url')[0].innerHTML + "/spatial-portal-help";
    if (undefined === page) {
        page = 'spatial-portal-help-contents';
    }
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'openUrl', help_base_url + "/" + page));
}
function loadMetadataUrl(type) {
    var metaurl = "http://www.google.com/intl/en_au/help/terms_maps.html";
    if (type == "minimal") {
        metaurl = "openstreetmap_metadata.html";
    } else if (type == "outline") {
        metaurl = "http://www.naturalearthdata.com/about/terms-of-use";
    } else {
        metaurl = "http://www.google.com/intl/en_au/help/terms_maps.html";
    }
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'openUrl', metaurl));
}

function clearNearestMarkerLayer() {
    if (mapFrame != null && map != null && mapFrame.markers != null) {
        map.removeLayer(mapFrame.markers);
        mapFrame.markers = null;
    }
}

function openDistributions(lsids) {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$openDistributions', lsids));
}

function openChecklists(lsids) {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$openChecklists', lsids));
}

function openAreaChecklist(geom_idx) {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$openAreaChecklist', geom_idx));
}

var flaggedRecords = {};
function isFlaggedRecord(layer, id) {
    return flaggedRecords[layer + "\n" + id] != undefined
}
function flagRecord(layer, id, set) {
    if (isFlaggedRecord(layer, id) != set) {
        var key = layer + "\n" + id;
        if (set) flaggedRecords[key] = true
        else flaggedRecords[key] = undefined;

        zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'updateAdhocGroup', key + "\n" + set))
    }
}
function addFlaggedRecords(pairs) {
    var list = pairs.split('\n');
    for (i = 0; i < list.length; i++) {
        flaggedRecords[list[i]] = true
    }
}

function doNothing() {
}

function saveSession() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'saveUserSession', null));
}

function downloadFeaturesCSV() {
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'onClick$downloadFeaturesCSV', null));
}