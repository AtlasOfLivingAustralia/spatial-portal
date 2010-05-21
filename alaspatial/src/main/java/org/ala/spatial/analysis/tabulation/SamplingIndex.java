package org.ala.spatial.analysis.tabulation;

import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;

import org.ala.spatial.util.*;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * builder for sampling index.
 *
 * requires OccurancesIndex to be up to date.
 *
 * operates on GridFiles
 * operates on PostGIS geo tables
 *
 * @author adam
 *
 */
public class SamplingIndex implements AnalysisIndexService {
	/* constants - TODO, do this nicely */
	static final String CONTINOUS_PREFIX = "SAM_D_";
	static final String CATAGORICAL_PREFIX = "SAM_I_";
	static final String VALUE_POSTFIX = ".dat";
	static final String CATAGORY_LIST_PREFIX = "SAM_C_";
	static final String CATAGORY_LIST_POSTFIX = ".csv";

	/**
	 * postGIS database connection
	 */
	//Connection connection;

	/**
	 * destination of loaded occurances
	 */
	ArrayList<String []> occurances;

	/**
	 * default constructor
	 */
	public SamplingIndex(){
		TabulationSettings.load();
	}

	/**
	 * performs update of 'indexing' for new points data
	 */
	public void occurancesUpdate(){
		/*
		 * for grid files
		 */
		intersectGrid();

		/*
		 * for postgis geo tables of catagorical layers,
		 * shape files instead of grids
		 */
		//connectDatabase();
		//indexCatagories();
		intersectCatagories();
		//disconnectDatabase();
	}

	/**
	 * performs update of 'indexing' for a new layer (grid or shapefile)
	 *
	 * @param layername name of the layer to update as String.  To update
	 * all layers use null.
	 */
	public void layersUpdate(String layername){

	}

	/**
	 * method to determine if the index is up to date
	 *
	 * @return true if index is up to date
	 */
	public boolean isUpdated(){
		return true;
	}

	/**
	 * joins sorted points to GridFiles
	 */
	void intersectGrid(){
		double [][] points = OccurancesIndex.getPointsPairsGEO();
		int [] points_idx = OccurancesIndex.getPointsPairsGEOidx();
	
		int i;
		
		/* for each grid file intersect and export in points order */
		Layer layer;
		if(points != null){
			System.out.println("number of grid files: " + TabulationSettings.environmental_data_files.length);
			for(i=0;i<TabulationSettings.environmental_data_files.length;i++){
				layer = TabulationSettings.environmental_data_files[i];
				try{
					(new SpatialLogger()).log("intersecting with gridfile:",layer.name);
				
					Grid grid = new Grid(
							TabulationSettings.environmental_data_path
							+ layer.name);

					double [] values = grid.getValues(points);
					int c1 = 0;
					int c2 = 0;
					int c3 = 0;
					int c4 = 0;
					for(int k=0;k<values.length;k++){
						if(points[k][0] < 112 || points[k][0] > 154 ||
								points[k][1] > -9 || points[k][1] < -44){
							c1++;
							if(Double.isNaN(values[k])){
								c3++;
							}
						}else{
							c2++;
							if(Double.isNaN(values[k])){
								c4++;
							}
						}
					}
					System.out.println("c's out:" + c1 + " - " + c3 + " in:" + c2 + " - " + c4);

					/* export values - RAF for writeDouble() */
					RandomAccessFile raf = new RandomAccessFile(
							TabulationSettings.index_path
							+ "SAM_D_" + layer.name + ".dat","rw");

					float [] values_sorted = new float[values.length];
					for(int k=0;k<values.length;k++){
						values_sorted[points_idx[k]] = (float)values[k];
					}
									
					for(int j=0;j<values_sorted.length;j++){
			//			raf.writeFloat((float)values[points_idx[j]]);
						raf.writeFloat((float)values_sorted[j]);
					}
					raf.close();
					System.out.println("written:" + values_sorted.length);
				}catch(Exception e){
					(new SpatialLogger()).log("intersectGrid writing",e.toString());
					e.printStackTrace();
				}
			}
		}
		System.out.println("finished intersecting grids");
	}

	/**
	 * make index on catagorial values
	 *
	 * query to run on each table & column name collection to build
	 * list of catagory strings
	 *
	 */
//	void indexCatagories(){
//		String tablename;
//		String fieldname;
//
//		/*
//		 * TODO: fix assumption that only catagorical fields are specified
//		 * in the tables
//		 */
//		for(Layer l : TabulationSettings.geo_tables){
//
//			try {
//				tablename = l.name;
//
//				/*
//				 * TODO: operate on more than one field and remove assumption
//				 * that there is one
//				 */
//				fieldname = l.fields[0].name;
//
//				String query = "select " + fieldname + " from " + tablename + " group by " + fieldname;
//
//				Statement s = connection.createStatement();
//
//				ResultSet r = s.executeQuery(query);
//
//				FileWriter fw = new FileWriter(
//						TabulationSettings.index_path
//						+ CATAGORY_LIST_PREFIX + tablename + "_" + fieldname
//						+ CATAGORY_LIST_POSTFIX);
//
//				while(r.next()){
//					fw.append(r.getString(1) + "\n");
//				}
//				fw.close();
//			}catch(Exception e){
//				(new SpatialLogger()).log("indexCatagories",e.toString());
//			}
//		}
//	}

	/**
	 * join number for each catagory name with all points
	 *
	 * TODO: speed issue since performing 4M+ queries per layer
	 */
	void intersectCatagories(){
		String tablename;
		String fieldname;

		(new SpatialLogger()).log("intersectCatagories, loading points");
		/* load points in correct order */
		double [][] points = OccurancesIndex.getPointsPairs();
		int i = 0;
		
		(new SpatialLogger()).log("intersectCatagories, points> " + points.length);

		/*
		 * TODO: fix assumption that only catagorical fields are specified
		 * in the tables
		 *
		 * TODO: check if points loaded
		 */

		for(Layer l : TabulationSettings.geo_tables){
			String query = "";
			String longitude = "";
			String latitude = "";
			i = 0;
			try {
				tablename = l.name;
				
				SimpleShapeFile ssf = new SimpleShapeFile(
							TabulationSettings.environmental_data_path
							+ l.name);
				
				/* export catagories				
				 * TODO: operate on more than one field and remove assumption
				 * that there is one
				 */
				fieldname = l.fields[0].name;
				String filename_catagories = TabulationSettings.index_path
						+ CATAGORY_LIST_PREFIX + l.name + "_" + fieldname
						+ CATAGORY_LIST_POSTFIX;
				
				FileWriter fw = new FileWriter(filename_catagories);
				int column_idx = ssf.getColumnIdx(fieldname);
				
				System.out.println("field [" + fieldname + "] in column " + column_idx);
				
				/* TODO: error if column not found */
				if(column_idx < 0){
					column_idx = 0;
				}
				
				String [] catagories = ssf.getColumnLookup(column_idx);
				for(i=0;i<catagories.length;i++){
					fw.append(catagories[i]);
					fw.append("\n");
				}
				fw.close();
				
				/* export file */
				RandomAccessFile raf = new RandomAccessFile(
						TabulationSettings.index_path
						+ CATAGORICAL_PREFIX + tablename + VALUE_POSTFIX,"rw");

				int value;

				Statement s;
				ResultSet r;

				//repeat for each point
				(new SpatialLogger()).log("intersectCatagories, begin queries");
				
				int [] values = ssf.intersect(points, catagories, column_idx);
				
				for(i=0;i<values.length;i++){
					raf.writeShort((short)values[i]);
				}
				raf.close();
			}catch(Exception e){
				(new SpatialLogger()).log("intersectCatagories",
						e.toString() + "\r\n>query="+ query + "\r\n>i=" + i
						+ "\r\n>longitude, latitude=" + longitude + "," + latitude);
			}
		}
	}


//	/**
//	 * postgres db connection load
//	 * @return
//	 */
//	private boolean connectDatabase(){
//		try {
//			System.out.println(
//					TabulationSettings.db_connection_string + " > " +
//					TabulationSettings.db_username+ " > " +
//					TabulationSettings.db_password);
//
//
//			Class.forName("org.postgresql.Driver");
//
//			connection = DriverManager.getConnection(
//					TabulationSettings.db_connection_string,
//					TabulationSettings.db_username,
//					TabulationSettings.db_password);
//
//		} catch (Exception e) {
//			(new SpatialLogger()).log("connectDatabase",e.toString());
//			return false;
//		}
//		return connection != null;
//	}
//
//	/**
//	 * postgres db connection disconnect
//	 */
//	private void disconnectDatabase(){
//		try {
//			connection.close();
//		} catch (Exception e) {
//			(new SpatialLogger()).log("disconnectDatabase",e.toString());
//		}
//	}

	/**
	 * TODO: read properly - needs a change to the write functions as well
	 *
	 * @param layer
	 * @param record_start
	 * @param record_end
	 * @return
	 */
	public static String[] getRecords(String layer_name, int record_start, int record_end){
		/*
		 * gridded data is 4byte double
		 * catagorical data is 4byte int
		 */

		try{
			//byte [] data = new byte[(record_end-record_start+1)*4];
			int i;
			String filenameD = TabulationSettings.index_path
				+ "SAM_D_" + layer_name + ".dat";
			String filenameI = TabulationSettings.index_path
				+ CATAGORICAL_PREFIX + layer_name + VALUE_POSTFIX;

			String [] output = new String[record_end - record_start + 1];
			int p = 0;
			
			String [] lookup_values = SamplingIndex.getLayerCatagories(
				SamplingService.getLayer(layer_name));			
			
			if((new File(filenameD)).exists()){
				RandomAccessFile raf = new RandomAccessFile(filenameD,"r");
				raf.seek(record_start*4);
				float f;
				for(i=record_start;i<=record_end;i++){
					f = raf.readFloat();
					if(Float.isNaN(f)){
						output[p++] = "";
					}else{
						output[p++] = String.valueOf(f);
					}
				}
				raf.close();
			}else if((new File(filenameI)).exists()){
				RandomAccessFile raf = new RandomAccessFile(filenameI,"r");
				raf.seek(record_start*2);
				short v;
				for(i=record_start;i<=record_end;i++){
					v = raf.readShort();
					if(v >= 0 && v < lookup_values.length){
						output[p++] = lookup_values[v];
					}else{
						output[p++] = "";
					}
				}
				raf.close();
			}
			
			//byte [] data = new byte[(record_end-record_start+1)*4];
				
			
			return output;
		}catch(Exception e){
			(new SpatialLogger()).log("getRecords",e.toString());
		}
		/*
		 * TODO: make safe
		 */
		return null;
	}
	
	public int [] getRecordsByRegion(SimpleRegion region){
		int [] records = null;
		int i;
		System.out.println("about to read points");
		try{
			double [][] points = OccurancesIndex.getPointsPairs();
			
			String s;
			records = new int[points.length];		//divide by 4bytes first
			i = 0;
			int p = 0;
			int j;
			for(j=0;j<points.length;j++){				
				if(region.isWithin(points[j][0], points[j][1])){
					records[i] = p;
					i++;
				}
				p++;
			}			
			
			System.out.println("got " + i + " samples in the region");
			
			System.out.println("read points for grid intersect");
			
			return java.util.Arrays.copyOfRange(records,0,i);			

		}catch(Exception e){
			(new SpatialLogger()).log("getrecordsbyregion, loading points",e.toString());
		}
		
		return null;
	}
	
	/**
	 * TODO: read properly - needs a change to the write functions as well
	 *
	 * @param layer
	 * @param record_start
	 * @param record_end
	 * @return
	 */
	public static String[] getRecords(String layer_name, int [] records){
		/*
		 * gridded data is 4byte float
		 * catagorical data is 4byte int
		 */
		try{
			//byte [] data = new byte[(record_end-record_start+1)*4];
			int i;
			String filenameD = TabulationSettings.index_path
				+ "SAM_D_" + layer_name + ".dat";
			String filenameI = TabulationSettings.index_path
				+ CATAGORICAL_PREFIX + layer_name + VALUE_POSTFIX;

			ArrayList<String> output = new ArrayList<String>(records.length);

			if((new File(filenameD)).exists()){
				RandomAccessFile raf = new RandomAccessFile(filenameD,"r");
				for(int k=0;k<records.length;k++){
					raf.seek(records[k]*4);
					float f;
					
					f = raf.readFloat();
					if(Float.isNaN(f)){
						output.add("");
					}else{
						output.add(String.valueOf(f));
					}					
					raf.close();
				}
			}else if((new File(filenameI)).exists()){
				RandomAccessFile raf = new RandomAccessFile(filenameI,"r");
				for(int k=0;k<records.length;k++){
					raf.seek(records[k]);
					short v = raf.readShort();
					if(v >= 0){
						output.add(String.valueOf(v));
					}else{
						output.add("");
					}
				}
				raf.close();
			}

			if(output.size() > 0){
				String str [] = new String [output.size ()];
				output.toArray (str);
				return str;
			}
		}catch(Exception e){
			(new SpatialLogger()).log("getRecords",e.toString());
		}
		/*
		 * TODO: make safe
		 */
		return null;
	}
	
	static public String[] getLayerCatagories(Layer layer){
		if(layer == null || layer.fields == null || layer.fields.length < 1){
			return null;
		}
		File catagories_file = new File(
                TabulationSettings.index_path
                + SamplingIndex.CATAGORY_LIST_PREFIX
                + layer.name + "_" + layer.fields[0].name
                + SamplingIndex.CATAGORY_LIST_POSTFIX);
        if (catagories_file.exists()) {
            byte[] data = new byte[(int) catagories_file.length()];
            try {
                FileInputStream fis = new FileInputStream(catagories_file);
                fis.read(data);
                fis.close();

                /* insert (row number) as beginning of each line */
                String str = new String(data);
                data = null;

                String[] lines = str.split("\n");
                return lines;
            } catch (Exception e) {
                (new SpatialLogger()).log("getLayerExtents(" + layer.name + "), catagorical",
                        e.toString());
                e.printStackTrace();
            }
        }
        return null;

	}

}
