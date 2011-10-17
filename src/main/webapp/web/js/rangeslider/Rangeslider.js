//modified copy of Slider.js
(function () {
	function _getBtnNewPos(wgt) {
		var btn = wgt.$n("min-btn");
		var btn2 = wgt.$n("max-btn");

		if (wgt._curpos > wgt._curmaxpos)
			wgt._curpos = wgt._curmaxpos;

		btn.title = wgt._curpos;
		wgt.updateFormData(wgt._curpos,wgt._curmaxpos);

		var isVertical = wgt.isVertical(),
			ofs = zk(wgt.getRealNode()).cmOffset(),
			ofs2 = zk(wgt.getRealNode()).cmOffset(),
			totalLen = isVertical ? wgt._getHeight(): wgt._getWidth(),
			totalLen2 = isVertical ? wgt._getMaxHeight(): wgt._getMaxWidth(),
			x1 = totalLen > 0 ? Math.round((wgt._curpos * totalLen) / wgt._maxpos) : 0;
			x2 = totalLen2 > 0 ? Math.round((wgt._curmaxpos * totalLen2) / wgt._maxpos) + 14: 0;

		ofs = zk(btn).toStyleOffset(ofs[0], ofs[1]);
		ofs = isVertical ? [0, (ofs[1] + x1)]: [(ofs[0] + x1), 0];

		ofs2 = zk(btn2).toStyleOffset(ofs2[0], ofs2[1]);
		ofs2 = isVertical ? [0, (ofs2[1] + x2)]: [(ofs2[0] + x2), 0];

		if (ofs[0] > ofs2[0])
			ofs[0] = ofs2[0];

		if (ofs[1] > ofs2[1])
			ofs[1] = ofs2[1];

		ofs = wgt._snap(ofs[0], ofs[1]);

		return ofs[(isVertical ? 1: 0)];
	}
	function _getBtnNewMaxPos(wgt) {
		var btn = wgt.$n("min-btn");
		var btn2 = wgt.$n("max-btn");

		if (wgt._curpos > wgt._curmaxpos)
			wgt._curmaxpos = wgt._curpos;

		btn2.title = wgt._curmaxpos;
		wgt.updateFormData(wgt._curpos,wgt._curmaxpos);

		var isVertical = wgt.isVertical(),
			ofs = zk(wgt.getRealNode()).cmOffset(),
			ofs2 = zk(wgt.getRealNode()).cmOffset(),
			totalLen = isVertical ? wgt._getHeight(): wgt._getWidth(),
			totalLen2 = isVertical ? wgt._getMaxHeight(): wgt._getMaxWidth(),
			x1 = totalLen > 0 ? Math.round((wgt._curpos * totalLen) / wgt._maxpos) : 0;
			x2 = totalLen2 > 0 ? Math.round((wgt._curmaxpos * totalLen2) / wgt._maxpos) +14: 0;

		ofs = zk(btn).toStyleOffset(ofs[0], ofs[1]);
		ofs = isVertical ? [0, (ofs[1] + x1)]: [(ofs[0] + x1), 0];

		ofs2 = zk(btn2).toStyleOffset(ofs2[0], ofs2[1]);
		ofs2 = isVertical ? [0, (ofs2[1] + x2)]: [(ofs2[0] + x2), 0];

		if (ofs[0] > ofs2[0])
			ofs2[0] = ofs[0];

		if (ofs[1] > ofs2[1])
			ofs2[1] = ofs[1];

		ofs2 = wgt._snapmax(ofs2[0], ofs2[1]);

		return ofs2[(isVertical ? 1: 0)];
	}
	function _getNextPos(wgt, offset) {
		var $btn = jq(wgt.$n("min-btn")),
			fum = wgt.isVertical()? ['top', 'height']: ['left', 'width'],
			newPosition = {};

		newPosition[fum[0]] = jq.px0(offset ?
			(offset + zk.parseInt($btn.css(fum[0])) - $btn[fum[1]]() / 2):
			_getBtnNewPos(wgt));

		return newPosition;
	}
	function _getNextMaxPos(wgt, offset) {
		var $btn = jq(wgt.$n("max-btn")),
			fum = wgt.isVertical()? ['top', 'height']: ['left', 'width'],
			newPosition = {};

		newPosition[fum[0]] = jq.px0(offset ?
			(offset + zk.parseInt($btn.css(fum[0])) - $btn[fum[1]]() / 2):
			_getBtnNewMaxPos(wgt));

		return newPosition;
	}


rangeslider.Rangeslider = zk.$extends(zul.Widget, {
	_orient: "horizontal",
	_curpos: 0,
	_curmaxpos: 0,
	_maxpos: 100,
	_pageIncrement: 10,
	_slidingtext: "{0}",
	_pageIncrement: -1,

	$define: {


		orient: function() {
			if (this.isVertical()) {
				this.setWidth("");
				this.setHeight("207px");
			} else {
				this.setWidth("207px");
				this.setHeight("");
			}
			this.rerender();
		},


		curpos: function() {
			if (this.desktop)
				this._fixPos();
		},
		curmaxpos: function() {
			if (this.desktop)
				this._fixPosmax();
		},


		maxpos: function() {
			if (this._curpos > this._maxpos) {
				this._curpos = this._maxpos;
				if (this.desktop) {
					this._fixPos();
					this._fixPosmax();
				}
			}
		},


		slidingtext: null,


		pageIncrement: null,


		name: function() {
			if (this.efield)
				this.efield.name = this._name;
		}
	},
	getZclass: function() {
		if (this._zclass != null)
			return this._zclass;

		var name = "z-slider";
		if (this.inScaleMold())
			return name + "-scale";
		else if (this.inSphereMold())
			return name + ("horizontal" == this._orient ? "-sphere-hor" : "-sphere-ver");
		else
			return name + ("horizontal" == this._orient ? "-hor" : "-ver");
	},
	doMouseOver_: function(evt) {

		var order = this._nearest(evt);
		order[0].addClass(this.getZclass() + "-btn-over");
		order[1].removeClass(this.getZclass() + "-btn-over");

		this.$supers('doMouseOver_', arguments);
	},
	doMouseOut_: function(evt) {
		jq(this.$n("min-btn")).removeClass(this.getZclass() + "-btn-over");
		jq(this.$n("max-btn")).removeClass(this.getZclass() + "-btn-over");
		this.$supers('doMouseOut_', arguments);
	},
	doMouseMove_: function(evt) {
		var order = this._nearest(evt);
		order[0].addClass(this.getZclass() + "-btn-over");
		order[1].removeClass(this.getZclass() + "-btn-over");
	},
	onup_: function(evt) {
		var btn = rangeslider.Rangeslider.down_btn, widget;
		if (btn) {
			widget = zk.Widget.$(btn);
			var	zcls = widget.getZclass();
			jq(btn).removeClass(zcls + "-btn-drag").removeClass(zcls + "-btn-over");
		}

		rangeslider.Rangeslider.down_btn = null;
		if (widget)
			jq(document).unbind("mouseup", widget.onup_);
	},
	doMouseDown_: function(evt) {
		var btn = this._nearest(evt)[0];
		jq(btn).addClass(this.getZclass() + "-btn-drag");
		jq(document).mouseup(this.onup_);
		rangeslider.Rangeslider.down_btn = btn;
		this.$supers('doMouseDown_', arguments);
	},
	_nearest: function(evt) {
		var $btnmin = jq(this.$n("min-btn")),
			posmin = $btnmin.zk.revisedOffset(),
			$btnmax = jq(this.$n("max-btn")),
			posmax = $btnmax.zk.revisedOffset(),
			isVertical = this.isVertical(),
			offsetmin = isVertical ? evt.pageY - posmin[1]: evt.pageX - posmin[0],
			offsetmax = isVertical ? evt.pageY - posmax[1]: evt.pageX - posmax[0],
			diff = Math.abs(offsetmin) - Math.abs(offsetmax);
		return (diff < 0 || (diff == 0 && offsetmin < 0)) ? [$btnmin, $btnmax] : [$btnmax, $btnmin];
	},
	doClick_: function(evt) {
		var $btnmin = jq(this.$n("min-btn")),
			posmin = $btnmin.zk.revisedOffset(),
			$btnmax = jq(this.$n("max-btn")),
			posmax = $btnmax.zk.revisedOffset(),
			wgt = this,
			pageIncrement = this._pageIncrement,
			moveToCursor = pageIncrement < 0,
			isVertical = this.isVertical(),
			offsetmin = isVertical ? evt.pageY - posmin[1]: evt.pageX - posmin[0],
			offsetmax = isVertical ? evt.pageY - posmax[1]: evt.pageX - posmax[0],
			diff = Math.abs(offsetmin) - Math.abs(offsetmax);

		if (!$btnmin[0] || $btnmin.is(':animated') || !$btnmax[0] || $btnmax.is(':animated')) return;

		if (diff < 0 || (diff == 0 && offsetmin < 0)) {
			if (!moveToCursor) {
				this._curpos += offsetmin > 0? pageIncrement: - pageIncrement;
				offsetmin = null;
			}

			$btnmin.animate(_getNextPos(this, offsetmin), "slow", function() {
				posmin = moveToCursor ? wgt._realpos(): wgt._curpos;
				if (posmin > wgt._curmaxpos)
					posmin = wgt._curmaxpos;
				wgt.fire("onScroll", (posmin * -1) - 1);
				if (moveToCursor)
					wgt._fixPos();
			});
		} else {
			if (!moveToCursor) {
				this._curmaxpos += offsetmax > 0? pageIncrement: - pageIncrement;
				offsetmax = null;
			}

			$btnmax.animate(_getNextMaxPos(this, offsetmax), "slow", function() {
				posmax = moveToCursor ? wgt._realmaxpos(): wgt._curmaxpos;
				if (posmax > wgt._maxpos)
					posmax = wgt._maxpos;
				if (posmax < wgt._curpos)
					posmax = wgt._curpos;
				wgt.fire("onScroll", posmax);
				if (moveToCursor)
					wgt._fixPosmax();
			});
		}
		this.$supers('doClick_', arguments);
	},
	_makeDraggable: function() {
		this._dragmin = new zk.Draggable(this, this.$n("min-btn"), {
			constraint: this._orient || "horizontal",
			starteffect: this._startDrag,
			change: this._dragging,
			endeffect: this._endDrag
		});

		this._dragmax = new zk.Draggable(this, this.$n("max-btn"), {
			constraint: this._orient || "horizontal",
			starteffect: this._startDragmax,
			change: this._draggingmax,
			endeffect: this._endDragmax
		});
	},
	_snap: function(x, y) {
		var btn = this.$n("min-btn"), ofs = zk(this.$n()).cmOffset();
		ofs = zk(btn).toStyleOffset(ofs[0], ofs[1]);
		if (x <= ofs[0]) {
			x = ofs[0];
		} else {
			var max = ofs[0] + this._getWidth();
			if (x > max)
				x = max;
		}
		if (y <= ofs[1]) {
			y = ofs[1];
		} else {
			var max = ofs[1] + this._getHeight();
			if (y > max)
				y = max;
		}
		return [x, y];
	},
	_snapmax: function(x, y) {
		var btn = this.$n("max-btn"), ofs = zk(this.$n()).cmOffset();
		ofs = zk(btn).toStyleOffset(ofs[0], ofs[1]);
		if (x <= ofs[0]) {
			x = ofs[0];
		} else {
			var max = ofs[0] + this._getMaxWidth() + 14;
			if (x > max)
				x = max;
		}
		if (y <= ofs[1]) {
			y = ofs[1];
		} else {
			var max = ofs[1] + this._getMaxHeight() + 14;
			if (y > max)
				y = max;
		}
		return [x, y];
	},
	_startDrag: function(dg) {
		var widget = dg.control;
		widget.$n('min-btn').title = "";
		widget.slidepos = widget._curpos;

		jq(document.body)
			.append('<div id="zul_slidetip" class="z-slider-pp"'
			+ 'style="position:absolute;display:none;z-index:60000;'
			+ 'background-color:white;border: 1px outset">' + widget.slidepos +
			'</div>');

		widget.slidetip = jq("#zul_slidetip")[0];
		if (widget.slidetip) {
			widget.slidetip.style.display = "block";
			zk(widget.slidetip).position(widget.$n(), widget.isVertical() ? "end_before" : "after_start");
		}
	},
	_dragging: function(dg) {
		var widget = dg.control,
			pos = widget._realpos();
		if (pos != widget.slidepos) {
			if (pos > widget._curmaxpos)
				pos = widget._curmaxpos;
			widget.slidepos = pos;
			if (widget.slidetip)
				widget.slidetip.innerHTML = widget._slidingtext.replace(/\{0\}/g, pos);
			widget.fire("onScrolling", (pos * -1) - 1);
		}
		widget._fixPos();
	},
	_endDrag: function(dg) {
		var widget = dg.control, pos = widget._realpos();

		widget.fire("onScroll", (pos * -1) - 1);

		widget._fixPos();
		jq(widget.slidetip).remove();
		widget.slidetip = null;
	},
	_realpos: function(dg) {
		var btnofs = zk(this.$n("min-btn")).cmOffset(), refofs = zk(this.getRealNode()).cmOffset(), maxpos = this._maxpos, pos;
		if (this.isVertical()) {
			var ht = this._getHeight();
			pos = ht ? Math.round(((btnofs[1] - refofs[1]) * maxpos) / ht) : 0;
		} else {
			var wd = this._getWidth();
			pos = wd ? Math.round(((btnofs[0] - refofs[0]) * maxpos) / wd) : 0;
		}
		return this._curpos = (pos >= 0 ? pos : 0);
	},
	_realmaxpos: function(dg) {
		var btnofs = zk(this.$n("max-btn")).cmOffset(), refofs = zk(this.getRealNode()).cmOffset(), maxpos = this._maxpos, pos;
		if (this.isVertical()) {
			var ht = this._getMaxHeight();
			pos = ht ? Math.round(((btnofs[1] - 14 - refofs[1]) * maxpos) / ht) : 0;
		} else {
			var wd = this._getMaxWidth();
			pos = wd ? Math.round(((btnofs[0] - 14 - refofs[0]) * maxpos) / wd) : 0;
		}
		return this._curmaxpos = (pos >= 0 ? pos : 0);
	},
	_startDragmax: function(dg) {
		var widget = dg.control;
		widget.$n('max-btn').title = "";
		widget.slidepos = widget._curmaxpos;

		jq(document.body)
			.append('<div id="zul_slidetip" class="z-slider-pp"'
			+ 'style="position:absolute;display:none;z-index:60000;'
			+ 'background-color:white;border: 1px outset">' + widget.slidepos +
			'</div>');

		widget.slidetip = jq("#zul_slidetip")[0];
		if (widget.slidetip) {
			widget.slidetip.style.display = "block";
			zk(widget.slidetip).position(widget.$n(), widget.isVertical() ? "end_before" : "after_start");
		}
	},
	_draggingmax: function(dg) {
		var widget = dg.control,
			pos = widget._realmaxpos();
		if (pos != widget.slidepos) {
			if (pos > widget._maxpos)
				pos = widget._maxpos;
			else if (pos < widget._curpos)
				pos = widget._curpos;
			widget.slidepos = pos;
			if (widget.slidetip)
				widget.slidetip.innerHTML = widget._slidingtext.replace(/\{0\}/g, pos);
			widget.fire("onScrolling", pos);
		}
		widget._fixPosmax();
	},
	_endDragmax: function(dg) {
		var widget = dg.control, pos = widget._realmaxpos();

		widget.fire("onScroll", pos);

		widget._fixPosmax();
		jq(widget.slidetip).remove();
		widget.slidetip = null;
	},
	_getWidth: function() {
		return this.getRealNode().clientWidth - this.$n("min-btn").offsetWidth - 7;
	},
	_getHeight: function() {
		return this.getRealNode().clientHeight - this.$n("min-btn").offsetHeight - 7;
	},
	_getMaxWidth: function() {
		return this.getRealNode().clientWidth - this.$n("max-btn").offsetWidth - 7;
	},
	_getMaxHeight: function() {
		return this.getRealNode().clientHeight - this.$n("max-btn").offsetHeight - 7;
	},
	_fixPos: _zkf = function() {
		this.$n("min-btn").style[this.isVertical()? 'top': 'left'] = jq.px0(_getBtnNewPos(this));
	},
	_fixPosmax: _zkf = function() {
		this.$n("max-btn").style[this.isVertical()? 'top': 'left'] = jq.px0(_getBtnNewMaxPos(this));
	},
	onSize: _zkf,
	onShow: _zkf,

	inScaleMold: function() {
		return this.getMold() == "scale";
	},

	inSphereMold: function() {
		return this.getMold() == "sphere";
	},

	isVertical: function() {
		return "vertical" == this._orient;
	},
	updateFormData: function(val,val2) {
		if (this._name) {
			val = val || 0;
			if (!this.efield)
				this.efield = jq.newHidden(this._name, val + "," + val2, this.$n());
			else
				this.efield.value = val + "," + val2;
		}

	},
	getRealNode: function () {
		return this.inScaleMold() ? this.$n("real") : this.$n();
	},
	bind_: function() {
		this.$supers(rangeslider.Rangeslider, 'bind_', arguments);
		var inner = this.$n("inner");

		if (this.isVertical()) {
			this.$n("min-btn").style.top = "0px";
			var het = this.getRealNode().clientHeight;
			if (het > 0)
				inner.style.height = (het + 7) + "px";
			else
				inner.style.height = "214px";
		}
		this._makeDraggable();

		zWatch.listen({onSize: this, onShow: this});
		this.updateFormData(this._curpos,this._curmaxpos);
		this._fixPos();
		this._fixPosmax();
	},
	unbind_: function() {
		this.efield = null;
		if (this._dragmin) {
			this._dragmin.destroy();
			this._dragmin = null;
		}
		if (this._dragmax) {
			this._dragmax.destroy();
			this._dragmax = null;
		}

		zWatch.unlisten({onSize: this, onShow: this});
		this.$supers(rangeslider.Rangeslider, 'unbind_', arguments);
	}
});
})();
