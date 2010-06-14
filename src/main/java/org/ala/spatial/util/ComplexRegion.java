package org.ala.spatial.util;

import java.util.ArrayList;

/**
 * ComplexRegion is a collection of SimpleRegion, expect POLYGONs for now.
 * 
 * treat as a shape file, overlapping regions cancel out presence.
 * 
 * TODO: clockwise/anticlockwise identification
 * 
 * @author Adam Collins
 */
public class ComplexRegion extends SimpleRegion {
	
	/**
	 * list of SimpleRegion members
	 */
	ArrayList<SimpleRegion> simpleregions;
	
	/**
	 * bounding box for all, see SimpleRegion boundingbox.
	 */
	double [][] boundingbox_all;
	
	/**
	 * value assigned 
	 */
	int value;
	
	/**
	 * array for speeding up isWithin
	 */
	byte [][] mask;
	
	/**
	 * mask height
	 */
	int mask_height;
	
	/**
	 * mask width
	 */
	int mask_width;
	
	/** 
	 * mask multiplier for longitude inputs
	 */
	double mask_long_multiplier;
	
	/**
	 * mask mulitplier for latitude inputs
	 */
	double mask_lat_multiplier;

	/**
	 * Constructor for empty ComplexRegion
	 */
	public ComplexRegion(){
		super();

		simpleregions = new ArrayList();
		boundingbox_all = new double[2][2];
		value = -1;
		mask = null;
	}

	/**
	 * sets integer value stored
	 * @param value_ as int
	 */
	public void setValue(int value_){
		value = value_;
	}
	
	/**
	 * gets integer value stored
	 * @return int
	 */
	public int getValue(){
		return value;
	}

        /**
         * gets the bounding box for shapes in this ComplexRegion
         * 
         * @return bounding box for ComplexRegion as double [][]
         */
        @Override
	public double [][] getBoundingBox(){
		return boundingbox_all;
	}

	/**
	 * adds a new polygon
	 * 
	 * note: if a mask is in use must call <code>useMask</code> again
	 * @param points_ points = double[n][2] 
	 * where
	 * 	n is number of points
	 *  [][0] is longitude
	 *  [][1] is latitude
	 */
	public void addPolygon(double [][] points_){
		SimpleRegion sr = new SimpleRegion();
		sr.setPolygon(points_);

		simpleregions.add(sr);

		/* update boundingbox_all */
		double [][] bb = sr.getBoundingBox();
		if (simpleregions.size() == 1 || boundingbox_all[0][0] > bb[0][0]) {
			boundingbox_all[0][0] = bb[0][0];
		}
		if (simpleregions.size() == 1 || boundingbox_all[1][0] < bb[1][0]) {
			boundingbox_all[1][0] = bb[1][0];
		}
		if (simpleregions.size() == 1 || boundingbox_all[0][1] > bb[0][1]) {
			boundingbox_all[0][1] = bb[0][1];
		}
		if (simpleregions.size() == 1 || boundingbox_all[1][1] < bb[1][1]) {
			boundingbox_all[1][1] = bb[1][1];
		}
	}	

	/**
	 * returns true when the point provided is within the ComplexRegion
	 * 
	 * uses <code>mask</code> when available
	 * 
	 * note: type UNDEFINED implies no boundary, always returns true.
	 * 
	 * @param longitude
	 * @param latitude
	 * @return true iff point is within or on the edge of this ComplexRegion
	 */
	@Override
	public boolean isWithin(double longitude, double latitude){
		if (simpleregions.size() == 1) {
			return simpleregions.get(0).isWithin(longitude, latitude);
		}
		if (boundingbox_all[0][0] > longitude || boundingbox_all[1][0] < longitude
				|| boundingbox_all[0][1] > latitude || boundingbox_all[1][1] < latitude) {
			return false;
		}
		
		/* use mask if exists */
		if (mask != null) {
			int long1 = (int) Math.floor((longitude - boundingbox_all[0][0])*mask_long_multiplier);
			int lat1 = (int) Math.floor((latitude - boundingbox_all[0][1])*mask_lat_multiplier);

			if(mask[lat1][long1] == SimpleRegion.GI_FULLY_PRESENT) {
				return true;
			} else if (mask[lat1][long1] == SimpleRegion.GI_UNDEFINED
                                || mask[lat1][long1] == SimpleRegion.GI_ABSENCE) {
				return false;
			}
		}
		
		/* check for all SimpleRegions */
		int count_in = 0;
		for (SimpleRegion sr : simpleregions) {
			if (sr.isWithin(longitude, latitude)) {
				count_in++;
			}
		}
		
		/* true iif within an odd number of regions */
		if (count_in % 2 == 1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * builds a grid (mask) to speed up isWithin.
	 * 
	 * TODO: split out shapes with large numbers of points in GI_PARTIALLY_PRESENT grid cells.
	 * 
	 * TODO: automatic(best) height/width specification
	 * 
	 * @param width
	 * @param height
	 */
	public void useMask(int width, int height){
		int i,j;
		
		/* class variables assignment */
		mask_width = width;
		mask_height = height;
		mask_long_multiplier = 
			mask_width / (double) (boundingbox_all[1][0] - boundingbox_all[0][0]);
		mask_lat_multiplier =
			mask_height / (double) (boundingbox_all[1][1] - boundingbox_all[0][1]);

		/* end result mask */
		mask = new byte[height][width];
		
		/* temp mask for current SimpleRegion */
		byte [][] shapemask = new byte[height][width];
		
		for (SimpleRegion sr : simpleregions) {
			sr.getOverlapGridCells(boundingbox_all[0][0], boundingbox_all[0][1]
				, boundingbox_all[1][0], boundingbox_all[1][1]
				, width, height
				, shapemask);

			//shapemask into mask
			for (i=0; i<height; i++) {
				for (j=0;j<width;j++) {
					if (shapemask[i][j] == 1 || mask[i][j] == 1) {
						mask[i][j] = 1;				//partially inside
					} else if (shapemask[i][j] == 2) {
						if (mask[i][j] == 2) {
							mask[i][j] = 3;			//completely inside
						} else {
							mask[i][j] = 2;			//completely outside (inside of a cutout region)
						}
					}
					
					/* reset shapemask for next part */
					shapemask[i][j] = 0;
				}
			}
		}	
	}
	
	/**
	 * determines overlap with a grid
	 * 
	 * for type POLYGON
	 * when <code>three_state_map</code> is not null populate it with one of:
	 * 	GI_UNDEFINED
	 * 	GI_PARTIALLY_PRESENT
	 * 	GI_FULLY_PRESENT
	 * 	GI_ABSENCE
	 * 
	 * @param longitude1
	 * @param latitude1
	 * @param longitude2
	 * @param latitude2
	 * @param xres number of longitude segements as int
	 * @param yres number of latitude segments as int
	 * @return (x,y) as double [][2] for each grid cell at least partially falling 
	 * within the specified region of the specified resolution beginning at 0,0 
	 * for minimum longitude and latitude through to xres,yres for maximums
	 * 
	 * TODO: fix for int[][] return value
	 */
	@Override
	public int [][] getOverlapGridCells(double longitude1, double latitude1
			, double longitude2, double latitude2, int width, int height
			, byte [][] three_state_map){

		if (three_state_map != null) {
			byte [][] shapemask = new byte[height][width];
			int i,j;

			for (SimpleRegion sr : simpleregions) {
				sr.getOverlapGridCells(longitude1, latitude1
					, longitude2, latitude2
					, width, height
					, shapemask);
	
				//merge shapemask into thee_state_map
				for (i=0;i<height;i++) {
					for (j=0;j<width;j++) {
						if (shapemask[i][j] == 1 || three_state_map[i][j] == 1) {
							three_state_map[i][j] = 1;				//partially inside
						} else if (shapemask[i][j] == 2) {
							if (three_state_map[i][j] == 2) {
								three_state_map[i][j] = 3;			//completely inside
							} else {
								three_state_map[i][j] = 2;			//completely outside (inside of a cutout region)
							}
						}
					}
				}
			}
		}

		return null;
	}
}