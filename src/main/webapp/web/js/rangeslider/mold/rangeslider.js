//modified copy of Slider.js

function (out) {
	var zcls = this.getZclass(),
		isScaleMold = this.inScaleMold() && !this.isVertical(),
		uuid = this.uuid;

	if(isScaleMold){
		out.push('<div id="', uuid, '" class="', zcls, '-tick">');
		this.uuid += '-real';
	}

	out.push('<div', this.domAttrs_(), '>');

	if(isScaleMold)
		this.uuid = uuid;

		out.push('<div id="', uuid, '-inner" class="', zcls, '-center">',
				'<div id="', uuid, '-min-btn" class="', zcls, '-btn"></div>',
				'<div id="', uuid, '-max-btn" class="', zcls, '-btn">',
				'</div></div>');

	if(isScaleMold)
		out.push('</div>');
}
