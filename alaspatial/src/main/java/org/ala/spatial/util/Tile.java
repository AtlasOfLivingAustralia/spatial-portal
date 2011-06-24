package org.ala.spatial.util;

import java.io.Serializable;

/**
 * Tile is used where values against x,y as a single value (y*width + x)
 * 
 * value stored as float for serialization
 * 
 * @author Adam Collins
 */
public class Tile extends Object implements Serializable {
	
	static final long serialVersionUID = -6328185557180038354L;

	
	/**
	 * value as float. 
	 */
	public float value_;
	
	/**
	 * position; pos_ = y*width + x for a point (x,y)
	 */
	public int pos_;
	
	/**
	 * constructor to create with values
	 * 
	 * @param value	value of tile as float
	 * @param pos position of tile as int of (y*width + x)
	 */
	public Tile(float value, int pos){
		value_ = value;
		pos_ = pos;
	}
}