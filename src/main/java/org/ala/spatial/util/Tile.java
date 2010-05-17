package org.ala.spatial.util;

import java.io.Serializable;

public class Tile extends Object implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public float value_;
	public int pos_;
	
	public Tile(float value, int pos){
		value_ = value;
		pos_ = pos;
	}
}