	var images = new Array();
	var images_min = new Array();
	var images_max = new Array();
	var filters_min = new Array();
	var filters_max = new Array();
	var inputData = new Array();
	var input = new Array();
	var contexts = new Array();
	var canvas = new Array();
	var basefilter;

	function load_layer(n, name, min, max){

		// data management
		new_image = new Image();
		new_image.src = name;

		images[n] = new_image;
		filters_min[n] = min;
		filters_max[n] = max;
		images_min[n] = min;
		images_max[n] = max;

		canvas[n] = document.getElementsByTagName('canvas')[n];
		contexts[n] = canvas[n].getContext('2d');

		// canvas management
		contexts[n].drawImage(images[n], 0, 0);

		// get the image data to manipulate
		input[n] = contexts[n].getImageData(0, 0, canvas[n].width, canvas[n].height);
		inputData[n] = input[n].data;

		//do for base image
		if(n == 0){
			base_filter = new Array(input[0].width * input[0].height);
		}
	}

	//min/max supplied are between 0 and 1

	var applying_filter = false;
	function applyFilter(n, new_min, new_max){
if(n >= 19) return; 	//temporary for demo data

		if(applying_filter) return;
		applying_filter = true;

		new_min *= 255+2;	//temporary adjustment for now
		new_max *= 255+2;

		//get min/max
		old_min = filters_min[n];
		old_max = filters_max[n];

		if(new_min == old_min && new_max == old_max){
			applying_filter = false;
			return;
		}

		//do a block on the map
		var w = input[0].width, h = input[0].height;
		var length = w * h;

		// 4 cases:
		// 1. new_min lower
		// 2. new_min higher
		// 3. new_max lower
		// 4. new_max higher

		layer_idx = n;
		length4 = length*4;
		d = inputData[1+Math.floor(layer_idx/4)];
		d_offset = layer_idx%4;
		i_offset = 3 - d_offset;
		base = inputData[0];
		if (new_min < old_min) {// show more
			for (/*i = 0,*/ j=d_offset; j/*i*/ < length4; /*i++,*/ j+=4) {
				if (d[j] >= new_min
						&& d[j] < old_min) {
					i = Math.floor(j / 4);
					// reverse bit array (hopefully it was correct in before
				//	// here)
					base_filter[i] &= ~(0x00000001 << layer_idx);

					if (base_filter[i] == 0) {
						// unhide image pixel
						base[j+i_offset] = 255;
					}
				}
			}
		} else if (new_min > old_min) {// show less
			for (/*i = 0,*/ j=d_offset; j/*i*/ < length4; /*i++,*/ j+=4) {
				if (d[j] >= old_min &&
					d[j] < new_min) {
					// reverse bit array

					base_filter[Math.floor(j/4)] |= 0x00000001 << layer_idx;

					// hide image pixel
					base[j+i_offset] = 0;
				}
			}
		}
		if (new_max < old_max) {// show less
			for (/*i = 0,*/ j=d_offset; j/*i*/ < length4; /*i++,*/ j+=4) {
				if (d[j] > new_max
						&& d[j] <= old_max) {
					// reverse bit array
					base_filter[Math.floor(j/4)] |= 0x00000001 << layer_idx;

					// hide image pixel
					base[j+i_offset] = 0;
				}
			}
		} else if (new_max > old_max) {// show more
			for (/*i = 0,*/ j=d_offset; j/*i*/ < length4; /*i++,*/ j+=4) {
				if (d[j] > old_max &&
					d[j] <= new_max) {
					// reverse bit array (hopefully it was correct in before
					// here)
					i = Math.floor(j/4);
					base_filter[i] &= ~(0x00000001 << layer_idx);

					if (base_filter[i] == 0) {
						// unhide image pixel
						base[j+i_offset] = 255;
					}
				}
			}
		}

		// write back new min/max
		filters_min[n] = new_min;
		filters_max[n] = new_max;

		// write back image bytes
		contexts[0].putImageData(input[0], 0, 0);

		applying_filter = false;
	}

	function getValue(n,i){
		//will change with correctly prepared data
		return inputData[1+Math.floor(n/4)][i*4+n%4];
	}

	function init(){
		//for offline only
		//try{
		//	netscape.security.PrivilegeManager.enablePrivilege("UniversalBrowserRead");
		//}catch(e){
		//}

		document.getElementById('i0').src = "images/bluemarble.jpg";
		document.getElementById('i1').src = "images/b1-4t.png";
		document.getElementById('i2').src = "images/b5-8t.png";
		document.getElementById('i3').src = "images/b9-12t.png";
		document.getElementById('i4').src = "images/b13-16t.png";
		document.getElementById('i5').src = "images/b17-20t.png";

		//layer 0 is base image
	/*	load_layer(0,"images/b1-4t.png",-10000,40000);	//layer 0
		load_layer(1,"images/b1-4t.png",-10000,40000);
		/*load_layer(2,"images/b5-8t.png",-10000,40000);
		load_layer(3,"images/b9-12t.png",-10000,40000);	//layer 3
		load_layer(4,"images/b13-16t.png",-10000,100000);	//layer 3
		load_layer(5,"images/b17-20t.png",-10000,100000);	//layer 3

		load_layer(2,"images/b1-4t.png",-10000,40000);
		load_layer(3,"images/b1-4t.png",-10000,40000);	//layer 3
		load_layer(4,"images/b1-4t.png",-10000,100000);	//layer 3
		load_layer(5,"images/b1-4t.png",-10000,100000);	//layer 3*/

	}