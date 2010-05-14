package org.ala.spatial.util;

import java.util.ArrayList;

public class ComplexRegion extends SimpleRegion {
	ArrayList<SimpleRegion> simpleregions;
	double [][] boundingbox_all;
	int value;
	byte [][] mask;
	long mask_height, mask_width;
	double mask_long_multiplier;
	double mask_lat_multiplier;

	public ComplexRegion(){
		super();

		simpleregions = new ArrayList();
		boundingbox_all = new double[2][2];
		value = -1;
		mask = null;
	}

	public void setValue(int value_){
		value = value_;
	}
	
	public int getValue(){
		return value;
	}

	public void addPolygon(double [][] points_){
		SimpleRegion sr = new SimpleRegion();
		sr.setPolygon(points_);

		simpleregions.add(sr);

		/* update boundingbox_all */
		double [][] bb = sr.getBoundingBox();
		if(simpleregions.size() == 1 || boundingbox_all[0][0] > bb[0][0]){
			boundingbox_all[0][0] = bb[0][0];
		}
		if(simpleregions.size() == 1 || boundingbox_all[1][0] < bb[1][0]){
			boundingbox_all[1][0] = bb[1][0];
		}
		if(simpleregions.size() == 1 || boundingbox_all[0][1] > bb[0][1]){
			boundingbox_all[0][1] = bb[0][1];
		}
		if(simpleregions.size() == 1 || boundingbox_all[1][1] < bb[1][1]){
			boundingbox_all[1][1] = bb[1][1];
		}
	}	

int mask_usage_counter = 0;
	@Override
	public boolean isWithin(double longitude, double latitude){
		if(simpleregions.size() == 1){
			return simpleregions.get(0).isWithin(longitude, latitude);
		}
		if(boundingbox_all[0][0] > longitude || boundingbox_all[1][0] < longitude
			|| boundingbox_all[0][1] > latitude || boundingbox_all[1][1] < latitude){
			return false;
		}
		if(mask != null){
			int long1 = (int) Math.floor((longitude - boundingbox_all[0][0])*mask_long_multiplier);
			int lat1 = (int) Math.floor((latitude - boundingbox_all[0][1])*mask_lat_multiplier);

			if(mask[lat1][long1] == 2){
				return true;
			}else if(mask[lat1][long1] == 0){		//is 0 or 3
				return false;
			}
		}
		int count_in = 0;
		for(SimpleRegion sr : simpleregions){
			if(sr.isWithin(longitude, latitude)){
				count_in++;
			}
		}
		if(count_in % 2 == 1){
			return true;
		}else{
			return false;
		}
	}

	public void useMask(int width, int height){
		mask_width = width;
		mask_height = height;

		mask_long_multiplier = mask_width/(double)(boundingbox_all[1][0]-boundingbox_all[0][0]);
		mask_lat_multiplier = mask_height/(double)(boundingbox_all[1][1]-boundingbox_all[0][1]);

		mask = new byte[height][width];
		byte [][] shapemask = new byte[height][width];
		int i,j;
//System.out.println("\r\nshapes:" + simpleregions.size());
		for(SimpleRegion sr : simpleregions){
//System.out.print(sr.getNumberOfPoints() + " ");
			sr.getOverlapGridCells(boundingbox_all[0][0], boundingbox_all[0][1]
				, boundingbox_all[1][0], boundingbox_all[1][1]
				, width, height
				, shapemask);

			//merge
			for(i=0;i<height;i++){
				for(j=0;j<width;j++){
					if(shapemask[i][j] == 1 || mask[i][j] == 1){
						mask[i][j] = 1;				//partially inside
					}else if(shapemask[i][j] == 2){
						if(mask[i][j] == 2){
							mask[i][j] = 3;			//completely inside
						}else{
							mask[i][j] = 2;			//completely outside (inside of a cutout region)
						}
					}
				}
			}
		}
//System.out.print("mask : ");
		int count1=0,count2=0,count3=0,count4=0;
		for(i=0;i<height;i++){
			for(j=0;j<width;j++){
				if(mask[i][j] == 1){
					count1++;
				}else if(mask[i][j] == 2){
					count2++;
				}else if(mask[i][j] == 3){
					count3++;
				}
			}
		}
//System.out.print("c1=" + count1 + " c2=" + count2 + " c3=" + count3 + "\r\n");

	/*	
		// print ASCII mask...
		for(i=0;i<height;i++){
			for(j=0;j<width;j++){
				System.out.print(mask[height-i-1][j]);
			}
			System.out.print("\r\n");
		}
*/
	}
	
	/**
	 * TODO: fix for int[][] return value
	 */
	@Override
	public int [][] getOverlapGridCells(double longitude1, double latitude1
			, double longitude2, double latitude2, int width, int height
			, byte [][] three_state_map){

		if(three_state_map != null){
			byte [][] shapemask = new byte[height][width];
			int i,j;

			for(SimpleRegion sr : simpleregions){
				sr.getOverlapGridCells(longitude1, latitude1
					, longitude2, latitude2
					, width, height
					, shapemask);
	
				//merge
				for(i=0;i<height;i++){
					for(j=0;j<width;j++){
						if(shapemask[i][j] == 1 || three_state_map[i][j] == 1){
							three_state_map[i][j] = 1;				//partially inside
						}else if(shapemask[i][j] == 2){
							if(three_state_map[i][j] == 2){
								three_state_map[i][j] = 3;			//completely inside
							}else{
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