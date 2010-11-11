
function updateSafeToLoadMap(status) {                        
    zAu.send(new zk.Event(zk.Widget.$(jq('$mapPortalPage')[0]), 'safeToLoadMap', status));
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

