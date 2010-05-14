package org.ala.spatial.util;

import java.io.Serializable;


public class SimpleRegion extends Object implements Serializable {
	private static final long serialVersionUID = 1L;
	
	int type; //0 = none, 1 = box, 2 = point & radius, 3 = polygon
	double [][] points;
	double [][] lines; //for polygons
	double [][] lines_long; //for polygons, lines longitude sorted
	double [][] lines_lat; //for polygons, lines latitude sorted
	double [][] bounding_box; //for polygons
	double radius;
	
	public SimpleRegion(){
		type = 0;
	}

	public int getNumberOfPoints(){
		return points.length;
	}

	public double [][] getBoundingBox(){
		return bounding_box;
	}
	
	public void setBox(double longitude1, double latitude1, double longitude2, double latitude2){
		type = 1;
		points = new double[2][2];
		points[0][0] = Math.min(longitude1,longitude2);
		points[0][1] = Math.min(latitude1,latitude2);
		points[1][0] = Math.max(longitude1,longitude2);
		points[1][1] = Math.max(latitude1,latitude2);
	}
	
	public void setNone(){
		type = 0;
	}
	
	public void setCircle(double longitude, double latitude, double radius_){
		type = 2;
		points = new double[1][2];
		points[0][0] = longitude;
		points[0][1] = latitude;
		radius = radius_;
	}
	
	/**
	 * long/lat pairs as double[][2]
	 */
	public void setPolygon(double [][] points_){
		if(points_ != null && points_.length > 1){
			type = 3;
			int i;
			
			/* ensure last points == first point */
			int len = points_.length-1;
			if(points_[0][0] != points_[len][0] || points_[0][1] != points_[len][1]){
				points = new double[points_.length+1][2];
				for(i=0;i<points_.length;i++){
					points[i][0] = points_[i][0];
					points[i][1] = points_[i][1];					
				}
				points[points_.length][0] = points_[0][0];
				points[points_.length][1] = points_[0][1];
			}else{
				points = points_.clone();
			}
			
			bounding_box = new double[2][2];
			bounding_box[0][0] = points[0][0];
			bounding_box[0][1] = points[0][1];
			bounding_box[1][0] = points[0][0];
			bounding_box[1][1] = points[0][1];
			
			for(i=1;i<points.length;i++){
				if(bounding_box[0][0] > points[i][0]){
					bounding_box[0][0] = points[i][0];
				}
				if(bounding_box[1][0] < points[i][0]){
					bounding_box[1][0] = points[i][0];
				}
				if(bounding_box[0][1] > points[i][1]){
					bounding_box[0][1] = points[i][1];
				}
				if(bounding_box[1][1] < points[i][1]){
					bounding_box[1][1] = points[i][1];
				}
			}			
			
			
			lines = new double[points.length-1][2];
			lines_long = new double[points.length-1][2];
			lines_lat = new double[points.length-1][2];
			for(i=0;i<points.length-1;i++){
				lines[i][0] = (points[i][1] - points[i+1][1])/(points[i][0]-points[i+1][0]);	//slope
				lines[i][1] = points[i][1] - lines[i][0]*points[i][0];				//intercept
				lines_long[i][0] = Math.min(points[i][0],points[i+1][0]);			//min longitude on line
				lines_long[i][1] = Math.max(points[i][0],points[i+1][0]);			//max longitude on line
				lines_lat[i][0] = Math.min(points[i][1],points[i+1][1]);			//min latitude on line
				lines_lat[i][1] = Math.max(points[i][1],points[i+1][1]);			//max latitude on line
			}
		}				
	}

	public boolean isWithin(double longitude, double latitude){
		switch(type){
		case 0:
			return true;
		case 1:
			return (longitude <= points[1][0] && longitude >= points[0][1]
				&& latitude <= points[1][1] && latitude >= points[0][1]);
		case 2:
			double x = longitude-points[0][0];
			double y = latitude-points[0][1];
			return Math.sqrt(x*x + y*y) <= radius;
		case 3:
			return isWithinPolygon(longitude, latitude);
		}
		return false;
	}

	private boolean isWithinPolygon(double longitude, double latitude){
		if(longitude <= bounding_box[1][0] && longitude >= bounding_box[0][0]
           		 && latitude <= bounding_box[1][1] && latitude >= bounding_box[0][1]){
			
			/*
				segments:

				0  |  2
			  _____|_____
				1  |  3
				   |

				+1 for clockwise movement
				-1 for anticlockwise movement
				closed is +4 (clockwise) or -4 (anticlockwise), 0 means long,lat is outside of loop
	
				if on point on line returns true
			*/


			double y;
			int segment;

			//initial segment
			if(points[0][0] > longitude){
				segment = 2;
			}else{
				segment = 0;
			}
			if(points[0][1] < latitude){
				segment++;
			}
			
			int i;
			int len = points.length;
			int new_segment;
			int score = 0;

			for(i=1;i<len;i++){	
				/* point on point */
				if(points[i][0] == longitude && points[i][1] == latitude){
					return true;
				}
			
				if(points[i][0] > longitude){
					new_segment = 2;
				}else{
					new_segment = 0;
				}
				if(points[i][1] < latitude){
					new_segment++;
				}


				if(segment == new_segment){
					continue;
				}	
				
				switch(segment){
				case 0:
					switch(new_segment){					
					case 1:	
						score--;
						break;
					case 2:
						score++;
						break;
					case 3:
						//diagonal						
						y = lines[i-1][0] * longitude + lines[i-1][1];
						if(y > latitude){
							score += 2;
						}else if(y < latitude){
							score -= 2;
						}else{
							return true;
						}
						break;
					}
					break;
				case 1:	
					switch(new_segment){					
					case 0:	
 						score++;
						break;
					case 2:
						//diagonal
						y = lines[i-1][0] * longitude + lines[i-1][1];
						if(y > latitude){
							score += 2;
						}else if(y < latitude){
							score -= 2;
						}else{
							return true;
						}
						break;
					case 3:
						score--;
						break;
					}
					break;
				case 2:
					switch(new_segment){					
					case 0:	
						score--;
						break;
					case 1:
						y = lines[i-1][0] * longitude + lines[i-1][1];
						if(y < latitude){
							score += 2;
						}else if(y > latitude){
							score -= 2;
						}else{
							return true;
						}
						break;
					case 3:
						score++;
						break;
					}
					break;
				case 3:
					switch(new_segment){					
					case 0:	
						//diagonal
						y = lines[i-1][0] * longitude + lines[i-1][1];
						if(y < latitude){
							score += 2;
						}else if(y > latitude){
							score -= 2;
						}else{
							return true;
						}
						break;
					case 1:
						score++;
						break;
					case 2:
						score--;
						break;
					}
					break;
				}
				segment = new_segment;
			}
			if(score == 0){
				return false;
			}else{
				return true;	
			}
		}
		return false;
	}
	
	/**
	 * returns (x,y) as double [][2] for each grid cell partially falling withing the specified region of the specified resolution
	 * 
	 * TODO: polygon capable
	 * 
	 * @param longitude1
	 * @param latitude1
	 * @param longitude2
	 * @param latitude2
	 * @param xres
	 * @param yres
	 * @return
	 */
	public int [][] getOverlapGridCells(double longitude1, double latitude1
		, double longitude2, double latitude2, int width, int height
		, byte [][] three_state_map){

		switch(type){
		case 0:
			return null;
		case 1:
			return getOverlapGridCells_Box(longitude1, latitude1, longitude2, latitude2, width, height);
		case 2:
			return null; /* todo: circle grid */
		case 3:
			return getOverlapGridCells_Polygon(longitude1, latitude1, longitude2, latitude2, width, height, three_state_map);
		}
		return null;	
		
	}
	int [][] getOverlapGridCells_Box(double longitude1, double latitude1, double longitude2, double latitude2, int width, int height){
			
		double xstep = Math.abs(longitude2 - longitude1) / (double)width;
		double ystep = Math.abs(latitude2 - latitude1) / (double)height;
		
		double maxlong = Math.max(longitude1, longitude2);
		double minlong = Math.min(longitude1, longitude2);
		double maxlat = Math.max(latitude1, latitude2);
		double minlat = Math.min(latitude1, latitude2);
		
		//case 1, bounding box
		int xstart = (int)Math.floor((points[0][0] - minlong)/xstep);
		int ystart = (int)Math.ceil((points[1][1] - maxlat)/ystep);
		int xend = (int)Math.ceil((points[1][0] - minlong)/xstep);
		int yend = (int)Math.floor((points[0][1] - maxlat)/ystep);
		
		if(xstart < 0){
			xstart = 0;
		}
		if(ystart < 0){
			ystart = 0;
		}
		if(xend > width){
			xend = width;
		}
		if(yend > height){
			yend = height;			
		}
		
		int out_width = xend - xstart;
		int out_height = yend - ystart;
		int [][] data = new int[out_width*out_height][2];
		int j,i,p = 0;
		for(j=ystart;j<=yend;j++){
			for(i=xstart;i<=xend;i++){
				data[p][0] = i;
				data[p][1] = j;
				p++;
			}
		}		
		
		int [][] d = new int[data.length][2];
		for(j=0;j<data.length;j++){
			d[j][0] = data[j][0];
			d[j][1] = data[j][1];
		}
		return d;
	}

	int [][] getOverlapGridCells_Polygon(double longitude1, double latitude1
		, double longitude2, double latitude2, int width, int height
		, byte [][] three_state_map){	
	
		double xstep = Math.abs(longitude2 - longitude1) / (double)width;
		double ystep = Math.abs(latitude2 - latitude1) / (double)height;
		
		double maxlong = Math.max(longitude1, longitude2);
		double minlong = Math.min(longitude1, longitude2);
		double maxlat = Math.max(latitude1, latitude2);
		double minlat = Math.min(latitude1, latitude2);
		
		//case 1, bounding box
		int xstart = (int)Math.floor((bounding_box[0][0] - minlong)/xstep);
		int ystart = (int)Math.floor((bounding_box[0][1] - minlat)/ystep);
		int xend = (int)Math.ceil((bounding_box[1][0] - minlong)/xstep);
		int yend = (int)Math.ceil((bounding_box[1][1] - minlat)/ystep);

		if(xstart < 0){
			xstart = 0;
		}
		if(ystart < 0){
			ystart = 0;
		}
		if(xend > width){
			xend = width;
		}
		if(yend > height){
			yend = height;			
		}

		if(xstart > width || xend < 0 || ystart > height || yend < 0){
			return null;
		}
		
		int out_width = xend - xstart;
		int out_height = yend - ystart;
		int [][] data = new int[out_width*out_height][2];
		int j,i,p = 0;
		boolean inside;
		boolean cross;
		boolean shapeinside;
		int len = lines.length;

		for(j=ystart;j<yend;j++){
			for(i=xstart;i<xend;i++){
				/* test for this grid box */
				double long1 = i*xstep + minlong;
				double long2 = (i+1)*xstep + minlong;
				double lat1 = j*ystep + minlat;
				double lat2 = (j+1)*ystep + minlat;

				inside = false;
				cross = false;

				/* box contained within shape */
				if(long1 <= bounding_box[0][0] && bounding_box[1][0] <= long2
					&& lat1 <= bounding_box[0][1] && bounding_box[1][1] <= lat2){
					shapeinside = true;
					cross = true;	
					inside = true;
				}else{
					shapeinside = false;
				}

				/* any lines cross */
				cross = false;
				double q;
				int k =0;
				if(!shapeinside){
					for(k=0;k<len;k++){				
						if(lines_long[k][0] <= long2 && lines_long[k][1] >= long1
							&& lines_lat[k][0] <= lat2 && lines_lat[k][1] >= lat1){
							// is any gridcell line crossed by this polygon border? 
							q = lines[k][0] * long1 + lines[k][1];
							if(lines_lat[k][0] <= q && q <= lines_lat[k][1] &&
									lat1 <= q && q <= lat2){
								cross = true;
								break;
							}
							q = lines[k][0] * long2 + lines[k][1];
							if(lines_lat[k][0] <= q && q <= lines_lat[k][1] &&
									lat1 <= q && q <= lat2){
								cross = true;
								break;
							}
							if(lines[k][0] != 0){
								q = (lat1 - lines[k][1]) / lines[k][0];
								if(lines_long[k][0] <= q && q <= lines_long[k][1] &&
										long1 <= q && q <= long2){
									cross = true;
									break;
								}
								q = (lat2 - lines[k][1]) / lines[k][0];
								if(lines_long[k][0] <= q && q <= lines_long[k][1] &&
										long1 <= q && q <= long2){
									cross = true;
									break;
								}
							}
						}
						
					}
				
					if(!cross){
						/* first point inside, for zero cross & therefore box contained within shape*/			
						if(isWithinPolygon(long1, lat1)){
							inside = true;							
						}else{
							inside = false;
						}
					}
				}
				if(three_state_map != null){
					if(cross){
						three_state_map[j][i] = 1; //partially inside
					}else if(inside){
						three_state_map[j][i] = 2; //completely inside
					}else{
						three_state_map[j][i] = 0; //completely outside
					}
				}

				if(cross || inside){	
					data[p][0] = i;
					data[p][1] = j;
					//System.out.print("(" + i + "," + j + ")");
					p++;
				}					 
			}
		}
		
		int [][] d = new int[data.length][2];
		for(j=0;j<data.length;j++){
			d[j][0] = data[j][0];
			d[j][1] = data[j][1];
		}
		return d;
	}

	/*
	 * x position of intercept of two lines, one on the polygon, + one slope/intercept
	 */
	boolean lines_cross(int lines_idx, double long1, double lat1, double long2, double lat2){
		double m = (lat1 - lat2) / (long1 - long2);
		double a = lat1 - m * long1;
		double den = m-lines[lines_idx][0];
		double x;
		if(den > 0){
			x = (lines[lines_idx][1] - a) / den;
			if(long1 <= x 
				&& x <= long2
				&& lines_long[lines_idx][0] <= x
				&& lines_long[lines_idx][1] >= x){
				return true;
			}
		}
		return false;	
	}
	
	public static  SimpleRegion parseSimpleRegion(String pointsString){
		if(pointsString.equalsIgnoreCase("none")){
			return null;
		}
    	SimpleRegion simpleregion = new SimpleRegion();
    	String [] pairs = pointsString.split(",");
    	
    	double [][] points = new double[pairs.length][2];
    	for(int i=0;i<pairs.length;i++){    		
    		String [] longlat = pairs[i].split(":");
    		if(longlat.length == 2){
    			try{
	    			points[i][0] = Double.parseDouble(longlat[0]);
	    			points[i][1] = Double.parseDouble(longlat[1]);
    			}catch (Exception e){
    				e.printStackTrace();
    			}
	    		System.out.print("(" + points[i][0] + "," + points[i][1] + ")");
    		}else{
    			System.out.print("err:" + pairs[i]);
    		}
    		
    	}
    	
    	simpleregion.setPolygon(points);
    	
    	return simpleregion;
    }
}