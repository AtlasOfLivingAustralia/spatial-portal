
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

function showInfoOne() {
    window.mapFrame.showInfoOne();
}

function setSearchPointAnalysis(point_orig) {
    var point = point_orig.clone();
   
    // transform the point from Google Projection to EPSG:4326
    var mapObj = window.frames.mapFrame.map;
    point.transform(mapObj.projection, mapObj.displayProjection);
    var value = point.lon + "," + point.lat;
    zAu.send(new zk.Event(zk.Widget.$(jq('$areamappolygonwindow')[0]), 'onSearchPoint', value));
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