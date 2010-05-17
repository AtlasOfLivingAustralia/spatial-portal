
    	var map;
		var polygon = "";
		var polycontrol;
		var vlayer;
	var player;
		var counter = 0;

        function mapinit(){   

            map = new OpenLayers.Map('map');

            var wms = new OpenLayers.Layer.WMS( "OpenLayers WMS",
                    "http://labs.metacarta.com/wms/vmap0?", {layers: 'basic'});

      	    vlayer = new OpenLayers.Layer.Vector( "Editable" );
      	    player = new OpenLayers.Layer.Vector( "Species Occurances" );      

            map.addLayers([wms, player, vlayer]);

            map.addControl(new OpenLayers.Control.LayerSwitcher());

	    	polycontrol = new OpenLayers.Control.DrawFeature(vlayer,
				OpenLayers.Handler.Polygon, {'featureAdded': setPolygonLocation});
	    
	    	map.addControl(polycontrol);
	    
            map.zoomToExtent(new OpenLayers.Bounds(112,-44,154,-9));


        }

	 	function createPolygon(){
	  	    vlayer.destroyFeatures();
		    polycontrol.activate();
		}
	 	
	 	function clearPolygon(){
	  	    vlayer.destroyFeatures();
		    polygon = "";
		    /* feedback to zk */
		    var mye = document.getElementsByTagName('a');
     		    for(i=0;i < mye.length; i++){
			comm.sendEvent(mye[ i ], 'onUser', polygon);
		    }
		}
	
		function setPolygonLocation(obj){
	      	    polygon = obj.geometry.toString();
		    while(polygon.search(" ") >= 0){
			    polygon = polygon.replace(" ",":");
		    }
		    polygon = polygon.replace("POLYGON((","");
		    polygon = polygon.replace(")","");
		    polygon = polygon.replace(")","");
	       	    polycontrol.deactivate();

		    /* feedback to zk */
		    var mye = document.getElementsByTagName('a');
     		    for(i=0;i < mye.length; i++){
			comm.sendEvent(mye[ i ], 'onUser', polygon);
		    }

		}

		function getALOCimage(img, long1, lat1, long2, lat2, width, height){			
			var options = {isBaseLayer: false, visibility: true};

			counter++;
			var aloc = new OpenLayers.Layer.Image(
			        'ALOC' + counter,
			        img,
			        new OpenLayers.Bounds(long1, lat2, long2, lat1),	//swap latitude so lower right is first
			        new OpenLayers.Size(width, height),
			        options
			    );	
			map.addLayer(aloc);
	
			// shuffle above editable layer
			var idx = map.getLayerIndex(aloc);	
			map.setLayerIndex(aloc,idx-2);		
		}
		
		function drawCircles(points){
			player.destroyFeatures();
			var pairs = points.split(",");
			var minlong, minlat, maxlong, maxlat;
			var count = 0;
			for(i=0;i<pairs.length;i++){
				longlat = pairs[i].split(":");
				if(longlat.length == 2 && longlat[0].length > 0 && longlat[1].length > 0){
					count++;
					longlat[0] *= 1;					
					longlat[1] *= 1;
					var circle = OpenLayers.Geometry.Polygon.createRegularPolygon(
							new OpenLayers.Geometry.Point(longlat[0],longlat[1])
							,0.1, 50);			
					player.addFeatures(new OpenLayers.Feature.Vector(circle));
					if(i == 0){
						minlong = longlat[0];
						maxlong = longlat[0];
						minlat = longlat[1];
						maxlat = longlat[1];
					}
					if(minlong > longlat[0]) minlong = longlat[0];
					if(maxlong < longlat[0]) maxlong = longlat[0];
					if(minlat > longlat[1]) minlat = longlat[1];
					if(maxlat < longlat[1]) maxlat = longlat[1];
				}
			}	
			if(count > 0){
				map.zoomToExtent(new OpenLayers.Bounds(minlong-.2,minlat-.2,maxlong+.2,maxlat+.2));
			}

				/* feedback to zk */
				var mye = document.getElementsByTagName('b');
		     		for(i=0;i < mye.length; i++){
					comm.sendEvent(mye[ i ], 'onUser', count);
				 }
		}

		function addImageLayer(img, layer_name, long1, lat1, long2, lat2, width, height){

			var layer = map.getLayersByName(layer_name);
					
			var options = {isBaseLayer: false, visibility: true};

			counter++;
			var imglayer = new OpenLayers.Layer.Image(
			        layer_name,
			        img,
			        new OpenLayers.Bounds(long1, lat2, long2, lat1),	//swap latitude so lower right is first
			        new OpenLayers.Size(width, height),
			        options
			    );	
			imglayer.setOpacity(0.5);
			map.addLayer(imglayer);
	
			// shuffle above editable layer
			var idx = map.getLayerIndex(imglayer);	
			map.setLayerIndex(imglayer,idx-2);

			if(layer.length > 0){
				layer[0].destroy();
			}		
		}

		function removeImageLayer(layer_name){
			var layer = map.getLayersByName(layer_name);

			if(layer.length > 0){
				layer[0].destroy();
			}
		}