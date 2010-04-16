	/* image scaling, 1 to 2 */

	var images = new Array();
	var images_min = new Array();
	var images_max = new Array();
	var filters_min = new Array();
	var filters_max = new Array();
	var next_min = new Array();
	var next_max = new Array();
	var inputData = new Array();
	var input = new Array();
	var contexts = new Array();
	var canvas = new Array();
	var basefilter;
	var layerpresence = new Array();

	var inputDataI = new Array();
	var inputDataI2 = new Array();

	var max_value = 32768;

	function addLayer(n,name){
	/*	document.getElementById("log").innerHTML = "addLayer(" + n + "," + name + ")<br>" + document.getElementById("log").innerHTML;*/
		document.getElementById('inputs_div').innerHTML += 
			'<image id="i' + n + '" src="" onload="loadLayer(' + n + ',&quot;' + name + '&quot;)" />';

		document.getElementById("i" + n).src = name;
	}

	function loadLayer(n, name){	
		if(images[n] != null){
			return;
		}	
		/*document.getElementById("log").innerHTML = "loadLayer(" + n + "," + name + ")<br>" + document.getElementById("log").innerHTML;*/
		document.getElementById("i" + n).onload = "";

		// data management
		new_image = new Image();
		new_image.src = name;

		images[n] = new_image;
		filters_min[n] = 0;
		filters_max[n] = max_value;
		images_min[n] = 0;
		images_max[n] = max_value;
		
		// canvas management
		canvas[n] = document.getElementsByTagName('canvas')[n];
		contexts[n] = canvas[n].getContext('2d');
		contexts[n].drawImage(new_image, 0, 0);

		// get the image data to manipulate
		input[n] = contexts[n].getImageData(0, 0, canvas[n].width, canvas[n].height);
		inputData[n] = input[n].data;	
		n = n * 1;
		

		if(n>0){	//0 is base image
			// unpack data 
			index_offset = images[n].width*256*4;

			d = inputData[n];

			inputDataI[n] = new Array();
			di = inputDataI[n];

/* di2 not needed with correctly operating packaging */
			inputDataI2[n] = new Array();
			di2 = inputDataI2[n];

			width = canvas[n].width/2; //data fix will make this 1:1
			height = 255;//todo: fix after data fix
			for(i=1, p=0/* png data offset (fix when packaging settings fixed) */ ;i<index_offset;i+=8,p++){
/* same for /8 +=8, fix with data fix*/
				x = 1*d[i]*width + 1*d[i+1];
				y = height - ( 1*d[i+4]*width + 1*d[i+5]);

				di[p] = (y*width + x)*4;	
				di2[p] = (y*512 + x)*8; //will go away later
			}	
			/* if possible, destroy objects no longer required here */

		}else{
			portalRefresh();
		}

		// initial filtering, for missing values 
		if(n > 0 && n < 19){
			applyFilter(1*n-1, 0, 1);
		}else if(n >= 19){
			applyFilterCtx(1*n-1,-1,false);
		}
	}
	
	function checkForLayer(n){
	/*	document.getElementById("log").innerHTML = "checkForLayer(" + n + ")<br>" + document.getElementById("log").innerHTML;*/
		if(layerpresence[n] == null){
			layerpresence[n] = true;		
			if(n<10){
				addLayer(n,"images/00" + n + ".png");
			}else{
				addLayer(n,"images/0" + n + ".png");
			}
			return false;
		}else if(inputData[n] == null){
			return false;
		}else{
			return true;
		}
	}

	/* applyFilter takes n as 0.., i.e. it adds one */
	var applying_filter = false;
	
	function applyFilter(n, new_min, new_max){

		n = 1*n + 1;

		//make sure layer is loaded, or at least, loading
		if(!checkForLayer(n)){
			return;
		}

		//translate min/max
		if(1*new_min >= -1 && 1*new_min <= 0){
			new_min = 1;
		}else{
			new_min = Math.floor(new_min*(max_value-2) + 1);
		}
		new_max = Math.floor(new_max*max_value + 1);
	
		if(new_max > max_value-1){
			new_max = max_value-1;
		}

		if(new_max < new_min){			
			new_max = new_min;
		}
		//record repeat action
		next_min[n] = new_min;
		next_max[n] = new_max;

		//stop if currently processing	
		if(applying_filter){
			return;
		}		
		applying_filter = true;
	
		old_min = filters_min[n];
		old_max = filters_max[n];

		while(filters_min[n] != next_min[n] || filters_max[n] != next_max[n]){
			new_max = next_max[n];
			new_min = next_min[n];
			old_min = filters_min[n];
			old_max = filters_max[n];

			if(old_max < old_min){
				old_max = old_min;
			}

			d = inputData[n];
			outputData = inputData[0];

			//current idx
			index_offset = images[n].width*256*4;

			if(old_min <= 0){
				old_min_idx = 0;
			}else if(old_min >= max_value-1){
				old_min_idx = index_offset/4/2;
			}else{
				old_min_idx = (d[index_offset+4*old_min+1]*256 + 1*d[index_offset+4*old_min+2]);
			}

			if(new_min <= 0){
				new_min_idx = 0;
			}else if(new_min >= max_value-2){
				new_min_idx = index_offset/4/2;
			}else{			
				new_min_idx = (d[index_offset+4*new_min+1]*256 + 1*d[index_offset+4*new_min+2]);
			}
		
			old_max_idx = (d[index_offset+4*old_max+1]*256 + 1*d[index_offset+4*old_max+2]);
			new_max_idx = (d[index_offset+4*new_max+1]*256 + 1*d[index_offset+4*new_max+2]);
			if(old_max_idx == 0){
				old_max_idx = index_offset/4/2;
			}
			if(new_max_idx == 0){
				new_max_idx = index_offset/4/2;
			}

			if(old_min_idx > index_offset/4/2){
				old_min_idx = index_offset/4/2;
			}
			if(new_min_idx > index_offset/4/2){
				new_min_idx = index_offset/4/2;
			}
			if(old_max_idx > index_offset/4/2){
				old_max_idx = index_offset/4/2;
			}
			if(new_max_idx > index_offset/4/2){
				new_max_idx = index_offset/4/2;
			}

			di = inputDataI[n];

			di2 = inputDataI2[n];
			while(new_min_idx > old_min_idx){			
		
				i = di[old_min_idx];
				i2 = di2[old_min_idx]

				base_filter[i] |= 0x00000001 << n;

				// hide image pixel	
				outputData[i2 + 3] = 120;
				outputData[i2 + 7] = 120;	
				outputData[i2 + 2051] = 120;
				outputData[i2 + 2055] = 120;

				old_min_idx++;
			}
			while(new_min_idx < old_min_idx){
				i = di[old_min_idx];
				i2 = di2[old_min_idx]
			
				base_filter[i] &= ~(0x00000001 << n);

				// unhide image pixel	
				if (base_filter[i] == 0) {
					outputData[i2 + 3] = 255;
					outputData[i2 + 7] = 255;	
					outputData[i2 + 2051] = 255;
					outputData[i2 + 2055] = 255;
				}

				old_min_idx--;;
			}

			while(new_max_idx > old_max_idx){
				i = di[old_max_idx];
				i2 = di2[old_max_idx]

				base_filter[i] &= ~(0x00000001 << n);

				// unhide image pixel	
				if (base_filter[i] == 0) {
					outputData[i2 + 3] = 255;
					outputData[i2 + 7] = 255;	
					outputData[i2 + 2051] = 255;
					outputData[i2 + 2055] = 255;
				}			

				old_max_idx++;
			}
			while(new_max_idx < old_max_idx){
				i = di[old_max_idx];
				i2 = di2[old_max_idx]
	
				base_filter[i] |= 0x00000001 << n;

				// hide image pixel	
				outputData[i2 + 3] = 120;
				outputData[i2 + 7] = 120;	
				outputData[i2 + 2051] =120;
				outputData[i2 + 2055] = 120;				

				old_max_idx--;
			}

			// write back new min/max
			filters_min[n] = new_min;
			filters_max[n] = new_max;

			// write back image bytes
			contexts[0].putImageData(input[0], 0, 0);
		
			portalRefresh();
			
		}
			
		applying_filter = false;			
	}

	/* use contextual layer at [n], with 'value' and if show is true, make it visible
	 * 
	 */
	function applyFilterCtx(n, value, show){
		value = value*1;
		n = 1*n + 1;

		//make sure layer is loaded, or at least, loading
		if(!checkForLayer(n)){
			return;
		}

		d = inputData[n];
		outputData = inputData[0];
		
		index_offset = images[n].width*256*4;
		end_value = index_offset / 4 / 2;
		if(value == -2){
			start_idx = 0;
			end_idx = end_value;
		}else{
			if(value == 0){
				start_idx = 0;
			}else{
				start_idx =  (d[index_offset+4*value+1]*256 + 1*d[index_offset+4*value+2]);
			}
			end_idx = (d[index_offset+4*(value+1)+1]*256 + 1*d[index_offset+4*(value+1)+2]);
			if(end_idx == 0 || end_idx > index_offset/4/2){
				end_idx = end_value;
			}
		}
		di = inputDataI[n];
		di2 = inputDataI2[n];

		if(!show){
			for(p=start_idx;p<end_idx;p++){
				i = di[p];
				i2 = di2[p];

				base_filter[i] |= 0x00000001 << n;

				// hide image pixel	
				outputData[i2 + 3] = 120;
				outputData[i2 + 7] = 120;	
				outputData[i2 + 2051] = 120;
				outputData[i2 + 2055] = 120;

			}
		}else{
			for(p=start_idx;p<end_idx;p++){
				i = di[p];
				i2 = di2[p]

				base_filter[i] &= ~(0x00000001 << n);

				// unhide image pixel	
				if (base_filter[i] == 0) {
					outputData[i2 + 3] = 255;
					outputData[i2 + 7] = 255;	
					outputData[i2 + 2051] = 255;
					outputData[i2 + 2055] = 255;
				}
			}
		}
		// write back image bytes		
		contexts[0].putImageData(input[0], 0, 0);

		portalRefresh();			
	}

	var portal_html;
	var portal;

	function init(){			
		addLayer(0,"images/bluemarble.jpg",0,256);
			
		base_filter = new Array(256*256);

		portal_html = document.getElementById("portal");
		portal = portal_html.getContext('2d');

		portalRefit();
	}	
	
	var dx_ = 0;
	var dy_ = 0;
	var dwidth_ = 512;
	var dheight_ = 512;
	var pwidth_ = 0;
	var pheight_ = 0;
	var px_ = 0;
	var py_ = 0;

	function portalRefresh(){
		portal.clearRect(0,0,portal_html.width,portal_html.height);
		portal.drawImage(canvas[0],dx_,dy_,dwidth_,dheight_,px_,py_,portal_html.width + pwidth_,portal_html.height + pheight_);
	}

	var layer_long1 = 112;
	var layer_lat1 = -9;
	var layer_long2 = 154;
	var layer_lat2 = -51;
	var layer_width = 512;
	var layer_height = 512;

	var last_long1 = 112;
	var last_long2 = 154;
	var last_lat1 = -9;
	var last_lat2 = -51;


	function portalPosition(longitude1, latitude1, longitude2, latitude2){
		//fix edges within layer bounds
		if(longitude1 < layer_long1){
			longitude2 += (layer_long1-longitude1)
			longitude1 = layer_long1;
		}
		if(longitude2 > layer_long2){
			longitude1 += (layer_long2-longitude2)
			longitude2 = layer_long2;
		}
		if(latitude2 < layer_lat2){
			latitude1 += (layer_lat2 - latitude2)
			latitude2 = layer_lat2;
		}
		if(latitude1 > layer_lat1){
			latitude2 += (layer_lat1-latitude1)
			latitude1 = layer_lat1;
		}

		//visible layer resolution
		dx = (longitude2 - longitude1)/layer_width;
		dy = (latitude2 - latitude1)/layer_height;

		//layer resolution
		lx = (layer_long2 - layer_long1)/layer_width;
		ly = (layer_lat2 - layer_lat1)/layer_height;
	
		dx_ = -1*Math.floor((layer_long1 - longitude1)/lx);
		dwidth_ = Math.floor((longitude2 - longitude1)/lx);

		dy_ = -1*Math.floor((layer_lat1 - latitude1)/ly);
		dheight_ = Math.floor((latitude2 - latitude1)/ly);

		portalRefresh();	

		last_long1 = longitude1;
		last_lat1 = latitude1;
		last_long2 = longitude2;
		last_lat2 = latitude2;
	}

	var mouse_down = false;
	var mousex = 0;
	var mousey = 0;

	function portal_mousemove(e){
		if(mouse_down){
			//pan
			
			//x/y movement
			rx = (1*e.clientX-1*mousex)/(1*portal_html.width);
			ry = (1*e.clientY-1*mousey)/(1*portal_html.height);
		
			width = 1*last_long2 - 1*last_long1;
			height = 1*last_lat2 - 1*last_lat1;

			new_long1 = last_long1 - rx*width;
			new_lat1 = last_lat1 - ry*height;

			new_long2 = last_long2 - rx*width;
			new_lat2 = last_lat2 - ry*height;

			portalPosition(new_long1,new_lat1,new_long2,new_lat2);	

			mousex = e.clientX;
			mousey = e.clientY;						
		}
	}
	function portal_mousedown(e){
		mousex = e.clientX;
		mousey = e.clientY;

		mouse_down = true;
	
	}
	function portal_mouseup(e){
		mouse_down = false;
	}

	//zoom in/out (out with modifier SHIFT or CTRL)
	//does not work correctly with browser enlarge/reduce size
	function portal_dblclick(e){
		pos = findPos(portal_html);

		//zoom to point by 2x
		rx = (1*e.clientX-1*pos.x)/(1*portal_html.width);
		ry = (1*e.clientY-1*pos.y)/(1*portal_html.height);
		
		width = 1*last_long2 - 1*last_long1;
		height = 1*last_lat2 - 1*last_lat1;

		new_long = (1*last_long1 + Math.floor((width)*rx));
		new_lat = (1*last_lat1 + Math.floor((height)*ry));

		if(e.shiftKey || e.ctrlKey){
			width *= 2;
			height *= 2;
		}else{
			width /= 2;
			height /= 2;
		}

		new_long1 = new_long - width/2;
		new_lat1 = new_lat - height/2;

		new_long2 = new_long + width/2;
		new_lat2 = new_lat + height/2;

		portalPosition(new_long1,new_lat1,new_long2,new_lat2);					
	}

	//recentre
	function portal_click(e){
		pos = findPos(portal_html);

		//zoom to point by 2x
		rx = (1*e.clientX-1*pos.x)/(1*portal_html.width);
		ry = (1*e.clientY-1*pos.y)/(1*portal_html.height);
		
		width = 1*last_long2 - 1*last_long1;
		height = 1*last_lat2 - 1*last_lat1;

		new_long1 = (1*last_long1 + Math.floor((width)*rx)) - width/2;
		new_lat1 = (1*last_lat1 + Math.floor((height)*ry)) - height/2;

		new_long2 = new_long1 + width;
		new_lat2 = new_lat1 + height;

		portalPosition(new_long1,new_lat1,new_long2,new_lat2);					
	}

function findPos(obj)
{
 var left = !!obj.offsetLeft ? obj.offsetLeft : 0;
 var top = !!obj.offsetTop ? obj.offsetTop : 0;

 while(obj = obj.offsetParent)
 {
  left += !!obj.offsetLeft ? obj.offsetLeft : 0;
  top += !!obj.offsetTop ? obj.offsetTop : 0;
 }

 return{x:left, y:top};
}

function portalRefit(){
// set dimensions of window, also need a resize function somewhere here	
portalRefresh();
}
