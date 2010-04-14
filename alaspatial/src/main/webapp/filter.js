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
		filters_max[n] = 255;
		images_min[n] = 0;
		images_max[n] = 255;

		// canvas management
		canvas[n] = document.getElementsByTagName('canvas')[n];
		contexts[n] = canvas[n].getContext('2d');
		contexts[n].drawImage(new_image, 0, 0);

		// get the image data to manipulate
		input[n] = contexts[n].getImageData(0, 0, canvas[n].width, canvas[n].height);
		inputData[n] = input[n].data;

		if(n > 0 && n < 19){
			applyFilter(1*n-1, 1/254.0, 1);
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
			new_min = Math.floor(new_min*254 + 1);
		}
		new_max = Math.floor(new_max*254 + 1);

		if(new_max > 255){
			new_max = 255;
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
	/*	document.getElementById("log").innerHTML = "applyFilter(" + n + "," + new_min + "," + new_max + ")<br>" +
document.getElementById("log").innerHTML;
*/
old_min = filters_min[n];
old_max = filters_max[n];
/*
document.getElementById("txt").innerHTML = n + " " + old_min + " " + old_max + "\r\n<br>";
document.getElementById("txt").innerHTML += "*" + new_min + " " + new_max + "\r\n<br>";
document.getElementById("txt").innerHTML += "@" + filters_min[n] + " " + filters_max[n] + "<br>";
*/
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
			index_offset = 256*256*4;
			if(old_min <= 0){
				old_min_idx = 0;
			}else if(old_min >= 255){
				old_min_idx = index_offset;
			}else{
				old_min_idx = 4*(d[index_offset+4*old_min+1]*256 + 1*d[index_offset+4*old_min+2]);
			}
			if(new_min <= 0){
				new_min_idx = 0;
			}else if(new_min >= 255){
				new_min_idx = index_offset;
			}else{
				new_min_idx = 4*(d[index_offset+4*new_min+1]*256 + 1*d[index_offset+4*new_min+2]);
			}

			old_max_idx = 4*(d[index_offset+4*old_max+1]*256 + 1*d[index_offset+4*old_max+2]);
			new_max_idx = 4*(d[index_offset+4*new_max+1]*256 + 1*d[index_offset+4*new_max+2]);
			if(old_max_idx == 0){
				old_max_idx = index_offset;
			}
			if(new_max_idx == 0){
				new_max_idx = index_offset;
			}
/*
document.getElementById("txt").innerHTML += "$" + old_min_idx + " " + new_min_idx + "\r\n";
document.getElementById("txt").innerHTML += "$" + old_max_idx + " " + new_max_idx + "\r\n";
*/
			if(new_min < 0){
				/*document.getElementById("log").innerHTML = "~~~~~ reset layer " + n + "~~~~~~~~~" + 				document.getElementById("log").innerHTML;*/

			}

			while(new_min_idx > old_min_idx){
				x = d[old_min_idx];
				y = d[old_min_idx+2];

				i = (y*256 + x)*4;
				i2 = (y*512 + x)*8;

				base_filter[i] |= 0x00000001 << n;

				// hide image pixel
				outputData[i2 + 3] = 0;
			//	outputData[i2 + 7] = 0;
			//	outputData[i2 + 2051] = 0;
				//outputData[i2 + 2055] = 0;

				old_min_idx+=4;
			}
			while(new_min_idx < old_min_idx){
				x = d[old_min_idx];
				y = d[old_min_idx+2];

				i = (y*256 + x)*4;
				i2 = (y*512 + x)*8;

				base_filter[i] &= ~(0x00000001 << n);

				// unhide image pixel
				if (base_filter[i] == 0) {
					outputData[i2 + 3] = 255;
			//		outputData[i2 + 7] = 255;
			//		outputData[i2 + 2051] = 255;
			//		outputData[i2 + 2055] = 255;
				}

				old_min_idx-=4;
			}

			while(new_max_idx > old_max_idx){
				x = d[old_max_idx];
				y = d[old_max_idx+2];

				i = (y*256 + x)*4;
				i2 = (y*512 + x)*8;

				base_filter[i] &= ~(0x00000001 << n);

				// unhide image pixel
				if (base_filter[i] == 0) {
					outputData[i2 + 3] = 255;
			//		outputData[i2 + 7] = 255;
			//		outputData[i2 + 2051] = 255;
			//		outputData[i2 + 2055] = 255;
				}

				old_max_idx+=4;
			}
			while(new_max_idx < old_max_idx){
				x = d[old_max_idx];
				y = d[old_max_idx+2];

				i = (y*256 + x)*4;

				i2 = (y*512 + x)*8;

				base_filter[i] |= 0x00000001 << n;

				// hide image pixel
				outputData[i2 + 3] = 0;
			//	outputData[i2 + 7] = 0;
			//	outputData[i2 + 2051] = 0;
			//	outputData[i2 + 2055] = 0;

				old_max_idx-=4;
			}

			// write back new min/max
			filters_min[n] = new_min;
			filters_max[n] = new_max;

			// write back image bytes
			contexts[0].putImageData(input[0], 0, 0);
		}

		applying_filter = false;
	}

	/* use contextual layer at [n], with 'value' and if show is true, make it visible
	 *
	 */
	function applyFilterCtx(n, value, show){
		value = value*1+1;
		n = 1*n + 1;

		//make sure layer is loaded, or at least, loading
		if(!checkForLayer(n)){
			return;
		}

/*		document.getElementById("log").innerHTML = "applyFilterCtx(" + n + "," + value + "," + show + ")<br>" +
document.getElementById("log").innerHTML;
*/
		d = inputData[n];
		outputData = inputData[0];

		index_offset = 256*256*4;

		if(value == -1){
			start_idx = 0;
			end_idx = index_offset;
		}else{
			if(value == 0){
				start_idx = 0;
			}else{
				start_idx = 4*(d[index_offset+4*(value)+1]*256 + 1*d[index_offset+4*(value)+2]);
			}
			end_idx = 4*(d[index_offset+4*(value+1)+1]*256 + 1*d[index_offset+4*(value+1)+2]);
			if(end_idx == 0){
				end_idx = index_offset;
			}
		}

/*document.getElementById("txt").innerHTML += "start/end:" + start_idx + " " + end_idx + " " + show + "\r\n<br>";
*/
		if(!show){
			for(p=start_idx;p<end_idx;p+=4){
				x = d[p];
				y = d[p+2];

				i2 = (y*512 + x)*8;
				i = (y*256 + x)*4;

				base_filter[i] |= 0x00000001 << n;

				// hide image pixel
				outputData[i2 + 3] = 0;
			//	outputData[i2 + 7] = 0;
			//	outputData[i2 + 2051] = 0;
			//	outputData[i2 + 2055] = 0;

			}
		}else{
			for(p=start_idx;p<end_idx;p+=4){
				x = d[p];
				y = d[p+2];

				i = (y*256 + x)*4;
				i2 = (y*512 + x)*8;

				base_filter[i] &= ~(0x00000001 << n);

				// unhide image pixel
				if (base_filter[i] == 0) {
					outputData[i2 + 3] = 255;
				//	outputData[i2 + 7] = 255;
				//	outputData[i2 + 2051] = 255;
				//	outputData[i2 + 2055] = 255;
				}
			}
		}
		// write back image bytes
		contexts[0].putImageData(input[0], 0, 0);

	}

	function init(){
			/*document.getElementById("log").innerHTML = "init()<br>" +
document.getElementById("log").innerHTML;	*/
		addLayer(0,"images/bluemarble.jpg",0,256);

	/*	addLayer(0,"images/001.png",0,256);
		addLayer(1,"images/002.png",0,256);
		addLayer(2,"images/003.png",0,256);
		addLayer(3,"images/004.png",0,256);
		addLayer(4,"images/005.png",0,256);
		addLayer(5,"images/006.png",0,256);
		addLayer(6,"images/007.png",0,256);
		addLayer(7,"images/008.png",0,256);
		addLayer(8,"images/009.png",0,256);
		addLayer(9,"images/010.png",0,256);
		addLayer(10,"images/011.png",0,256);
		addLayer(11,"images/012.png",0,256);
		addLayer(12,"images/013.png",0,256);
		addLayer(13,"images/014.png",0,256);
		addLayer(14,"images/015.png",0,256);
		addLayer(15,"images/016.png",0,256);
		addLayer(16,"images/017.png",0,256);
		addLayer(17,"images/018.png",0,256);
		addLayer(18,"images/019.png",0,256);*/

		base_filter = new Array(256*256);
	}

	