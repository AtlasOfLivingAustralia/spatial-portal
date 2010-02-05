/**
 * Instance of OpenLayers map
 */
var map;
var proxy_script="/webportal/RemoteRequest?url=";
var tmp_response; 

var request_id = 0;
var default_content = "Searching Databases...";



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


var layersLoading = 0;
var layername; // current layer name 

var attempts = 0;
var libraryLoaded = false;
var checkLibraryLoadedTimeout = null;
var libraryCheckIntervalMs=100;
var secondsToWaitForLibrary=30;
var maxAttempts = (secondsToWaitForLibrary * 1000) / libraryCheckIntervalMs;

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
					if (layersLoading > 0) {div.style.display=display;}
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
    function onOver(){ jQuery("div#mapinfo ").css("background-color","#3c668d"); }
    function onOut(){ jQuery("div#mapinfo ").css("background-color","transparent"); }
    jQuery("div#mapinfo").hover(onOver,onOut); 
    
    
    // proxy.cgi script provided by OpenLayers written in Python, must be on the same domain
    OpenLayers.ProxyHost = proxy_script;     
   
	// ---------- map setup --------------- //
    //var vlayer = new OpenLayers.Layer.Vector( "Editable" );
    map = new OpenLayers.Map('mapdiv', {			
        controls: [
        new OpenLayers.Control.PanZoomBar({div: document.getElementById('controlPanZoom')}),
        new OpenLayers.Control.LayerSwitcher(),
        //new OpenLayers.Control.EditingToolbar(vlayer,{div: document.getElementById('controlVector')}),
        new OpenLayers.Control.ScaleLine({div: document.getElementById('mapscale')}),
        //new OpenLayers.Control.Permalink('Map by IMOS Australia'),
        new OpenLayers.Control.OverviewMap({autoPan: true, minRectSize: 30, mapOptions:{resolutions: [  0.3515625, 0.17578125, 0.087890625, 0.0439453125, 0.02197265625, 0.010986328125, 0.0054931640625
                                             		         , 0.00274658203125, 0.001373291015625, 0.0006866455078125, 0.00034332275390625,  0.000171661376953125
                                             		        ]}}),
        //new OpenLayers.Control.KeyboardDefaults(),
		new OpenLayers.Control.Attribution(),
		new OpenLayers.Control.MousePosition({div: document.getElementById('mapcoords'),prefix: '<b>Lon:</b> ', separator: ' <BR><b>Lat:</b> '})
        ],
        theme: null,
		restrictedExtent: new OpenLayers.Bounds.fromString("-10000,-90,10000,90"),
//	   These are the resolutions we support: [ 0.3515625, 0.17578125, 0.087890625, 0.0439453125, 0.02197265625, 0.010986328125, 0.0054931640625
//	     		         , 0.00274658203125, 0.001373291015625, 0.0006866455078125, 0.00034332275390625, 0.000171661376953125
//	     		         , 8.58306884765625e-05, 4.291534423828125e-05, 2.1457672119140625e-05, 1.0728836059570312e-05, 5.3644180297851562e-06
//	     		         , 2.6822090148925781e-06, 1.3411045074462891e-06];
		resolutions: [  0.17578125, 0.087890625, 0.0439453125, 0.02197265625, 0.010986328125, 0.0054931640625
                  		         , 0.00274658203125, 0.001373291015625, 0.0006866455078125, 0.00034332275390625,  0.000171661376953125
                		        ]      
    });	
		
    // make OL compute scale according to WMS spec    
    OpenLayers.DOTS_PER_INCH = 25.4 / 0.28;
	// Stop the pink tiles appearing on error
	OpenLayers.Util.onImageLoadError = function() {  this.style.display = ""; this.src="img/blank.png"; }		
    
    var container = document.getElementById("navtoolbar");
    var pan = new OpenLayers.Control.Navigation({ id: 'himum', title: 'Pan Control' } );
    var zoom = new OpenLayers.Control.ZoomBox({ title: "Zoom and centre [shift + mouse drag]"});
    var panel = new OpenLayers.Control.Panel({ defaultControl: pan, div: container});
	panel.addControls( [zoom,pan ]);			
    map.addControl(panel);
    
    //map.addLayer(vlayer);

        
    
    // create a new event handler for single click query
    var clickEventHandler = new OpenLayers.Handler.Click({ 'map': map }, {
    'click': function(e) { 
		getpointInfo(e);
		mkpopup(e);
    	} 
    }); 
    clickEventHandler.activate();     
    
    // cursor mods
    map.div.style.cursor="pointer";
    jQuery("#navtoolbar div.olControlZoomBoxItemInactive ").click(function(){
    	map.div.style.cursor="crosshair";
    	clickEventHandler.deactivate();
    });
    jQuery("#navtoolbar div.olControlNavigationItemActive ").click(function(){
    	map.div.style.cursor="pointer";
    	clickEventHandler.activate();
    });
    
    
    map.events.register("moveend" , map, function (e) {
    	parent.setExtent();    	
        Event.stop(e);
    });

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
function getpointInfo(e) {

	tmp_response = '';
	timeSeriesPlotUri = null;
	layername = new Object();
	request_id = 0;
	
	OpenLayers.ProxyHost = proxy_script; 
	var wmsLayers = map.getLayersByClass("OpenLayers.Layer.WMS");	 
	var imageLayers = map.getLayersByClass("OpenLayers.Layer.Image");	
	wmsLayers = wmsLayers.concat(imageLayers);
	

	for (key in wmsLayers) {		
	
		if (map.layers[key] != undefined) {
			
			var layer = map.getLayer(map.layers[key].id);
			if ((! layer.isBaseLayer) && layer.queryable) {	
				var url = false;
				if (layer.animatedNcwmsLayer) {
					var lonlat = map.getLonLatFromViewPortPx(e.xy);
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
	
				
				if (url) {	
									
					
					var x =layer.featureInfoResponseType;
					if (is_ncWms(x)) {

						layername[layer.name+request_id] = new Object();						
						var a = layername[layer.name+request_id];
						a.setHTML_ncWMS = setHTML_ncWMS;
						a.imageUrl = timeSeriesPlotUri;	
						a.layername = ucwords(layer.name);
						a.unit = layer.ncWMSMetaData.unit;
						
						OpenLayers.loadURL(url, '', a , setHTML_ncWMS, setError);											
						timeSeriesPlotUri = null;
					}
					else if (isWms(x)) {
						OpenLayers.loadURL(url, '', this, setHTML2, setError);
					}
					else {
					  //OpenLayers.loadURL(url, '', this, setHTML2, setError);
					}
					request_id++;					
				}
			}
		}
	}
	// check for no change
	setTimeout('hidepopup()', 4000);	
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

/*
function getRSSFeatureinfo(layer) {
	
	
    var markerClick = function (evt) {
        if (this.popup == null) {
            this.popup = this.createPopup(true);
            map.addPopup(this.popup);
            this.popup.show();
        } else {
            this.popup.toggle();
        }
        currentPopup = this.popup;
        OpenLayers.Event.stop(evt);
    };
    
    layer.events.register("mousedown", feature, markerClick);
    
}
*/
Date.prototype.setISO8601 = function (string) {
    var regexp = "([0-9]{4})(-([0-9]{2})(-([0-9]{2})" +
        "(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\.([0-9]+))?)?" +
        "(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?";
    var d = string.match(new RegExp(regexp));

    var offset = 0;
    var date = new Date(d[1], 0, 1);

    if (d[3]) { date.setMonth(d[3] - 1); }
    if (d[5]) { date.setDate(d[5]); }
    if (d[7]) { date.setHours(d[7]); }
    if (d[8]) { date.setMinutes(d[8]); }
    if (d[10]) { date.setSeconds(d[10]); }
    if (d[12]) { date.setMilliseconds(Number("0." + d[12]) * 1000); }
    if (d[14]) {
        offset = (Number(d[16]) * 60) + Number(d[17]);
        offset *= ((d[15] == '-') ? 1 : -1);
    }

    offset -= date.getTimezoneOffset();
    time = (Number(date) + (offset * 60 * 1000));
    this.setTime(Number(time));
}		

function setHTML2(response) { 
	//alert(response.responseText);
	
	var pointInfo_str = '';
	// show the results div even though there may be no result yet	
	//jQuery(parent.featureinfo).show(500).fadeTo("slow", 0.9);
	
	tmp_response = response.responseText;	
	var html_content = "";
	var trimmed_content = "";
	
	// tested with Geoserver
	if (tmp_response.match(/<\/body>/m)) {
		// thanks Geoff		
		html_content  = tmp_response.match(/(.|\s)*?<body[^>]*>((.|\s)*?)<\/body>(.|\s)*?/m);
		if (html_content) {
			//trimmed_content= html_content[2].replace(/(\n|\r|\s)/mg, ''); // replace all whitespace			
			trimmed_content  = html_content[2].replace(/^\s+|\s+$/g, '');  // trim			
		}		
	}
		
	if (trimmed_content.length > 0) {
		trimmed_content = trimmed_content;
		setFeatureInfo(trimmed_content,"<hr></hr><br></br>\n\n");			
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
	            vals + "<BR />" + tmp_response+"</div>" ; 
        }
		
    } 
	else {
        html = "Can't get feature info data for this layer <a href='javascript:popUp('whynot.html', 200, 200)'>(why not?)</a>";
    }
    
    if (html != "") {
    	setFeatureInfo(html,"<hr></hr><br></br>\n\n");
    }
    
   
	   
}


function getCurrentFeatureInfo() {
	return jQuery('#featureinfocontent').html();
}

function setFeatureInfo(content,br) {	
	
	showpopup();
	
	if (getCurrentFeatureInfo() == default_content) {
		jQuery('#featureinfocontent').html(content).hide();
		map.popup.setSize(new OpenLayers.Size(430,240));
		jQuery('#featureinfocontent').fadeIn(200);		
	}
	else if (content.length > 0 ) {
		jQuery('#featureinfocontent').prepend(content+br).hide().fadeIn(200);		
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
    	
	var html = "<div class=\"closebox\"><a  href=\"#\" onclick=\"killpopup();\">"+
				"Close</a></div>\n<div id=\"featureinfocontent\">"+default_content+"</div>";
    popup = new OpenLayers.Popup.AnchoredBubble( "getfeaturepopup", 
    						pointclick,
    						new OpenLayers.Size(430,240),
                             html,
                             null, false, null);
    
    popup.panMapIfOutOfView = true; // pan works for size at creation
    map.popup = popup; 
    map.addPopup(popup);    
    map.popup.setSize(new OpenLayers.Size(230,40)); //shrink down while searching
    map.popup.setOpacity(0.9);
}

// only to be used from the close link in the popup
function killpopup() {    	
	if (map.popup != null) {		
		map.removePopup(map.popup);
		map.popup.destroy();
		map.popup = null;		
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
		jQuery('div.olPopup').show();
	}
}


//server might be down
function setError(response) {
	alert("The server is unavailable");  	 
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
    return (str+'').replace(/^(.)|\s(.)/g, function ( $1 ) { return $1.toUpperCase ( ); } );
}




