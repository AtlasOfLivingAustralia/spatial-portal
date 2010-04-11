


package org.ala.spatial.analysis.tabulation;

import java.util.*;
import java.io.*;

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
	Connection connection;

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
		connectDatabase();
		indexCatagories();
		intersectCatagories();
		disconnectDatabase();
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
		double [][] points = null;
		int i;
		System.out.println("about to read points");
		try{
			RandomAccessFile raf = new RandomAccessFile(
					TabulationSettings.index_path
					+ OccurancesIndex.POINTS_FILENAME,"r");				//using RAF for readDouble()

			String s;
			points = new double[((int)raf.length())/4/2][2];		//divide by 4bytes first
			i = 0;
			while(raf.getFilePointer() < raf.length()) {
				points[i][0] = raf.readDouble();					//longitude
				points[i][1] = raf.readDouble();					//latitude
				i++;
			}
			raf.close();
System.out.println("read points for grid intersect");
		}catch(Exception e){
			(new SpatialLogger()).log("intersectGrid, loading",e.toString());
		}

		/* for each grid file intersect and export in points order */
		Layer layer;
		if(points != null){
			for(i=0;i<TabulationSettings.environmental_data_files.length;i++){
				layer = TabulationSettings.environmental_data_files[i];
				try{
					System.out.println("intersecting with gridfile: " + layer.name);
					Grid grid = new Grid(
							TabulationSettings.environmental_data_path
							+ layer.name);

					double [] values = grid.getValues(points);

					/* export values - RAF for writeDouble() */
					RandomAccessFile raf = new RandomAccessFile(
							TabulationSettings.index_path
							+ "SAM_D_" + layer.name + ".dat","rw");

					for(int j=0;j<values.length;j++){
						raf.writeFloat((float)values[j]);
					}
					raf.close();
				}catch(Exception e){
					(new SpatialLogger()).log("intersectGrid writing",e.toString());
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
	void indexCatagories(){
		String tablename;
		String fieldname;

		/*
		 * TODO: fix assumption that only catagorical fields are specified
		 * in the tables
		 */
		for(Layer l : TabulationSettings.geo_tables){

			try {
				tablename = l.name;

				/*
				 * TODO: operate on more than one field and remove assumption
				 * that there is one
				 */
				fieldname = l.fields[0].name;

				String query = "select " + fieldname + " from " + tablename + " group by " + fieldname;

				Statement s = connection.createStatement();

				ResultSet r = s.executeQuery(query);

				FileWriter fw = new FileWriter(
						TabulationSettings.index_path
						+ CATAGORY_LIST_PREFIX + tablename + "_" + fieldname
						+ CATAGORY_LIST_POSTFIX);

				while(r.next()){
					fw.append(r.getString(1) + "\n");
				}
				fw.close();
			}catch(Exception e){
				(new SpatialLogger()).log("indexCatagories",e.toString());
			}
		}
	}

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
		double [][] points = null;
		int i = 0;
		try{
			RandomAccessFile raf = new RandomAccessFile(
					TabulationSettings.index_path
					+ OccurancesIndex.POINTS_FILENAME,"r");				//using RAF for readDouble()

			String s;
			points = new double[((int)raf.length())/4/2][2];		//divide by 4bytes first
			i = 0;
			while(raf.getFilePointer() < raf.length()) {
				points[i][0] = raf.readDouble();					//longitude
				points[i][1] = raf.readDouble();					//latitude
				i++;
			}
			raf.close();

		}catch(Exception e){
			(new SpatialLogger()).log("intersectCatagories, loading",e.toString());
		}
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

				/*
				 * TODO: operate on more than one field and remove assumption
				 * that there is one
				 */
				fieldname = l.fields[0].name;

				/* load catagory text to index number */
				BufferedReader br = new BufferedReader(new FileReader(
						TabulationSettings.index_path
						+ CATAGORY_LIST_PREFIX + tablename + "_" + fieldname
						+ CATAGORY_LIST_POSTFIX));

				HashMap<String,Integer> index = new HashMap<String,Integer>();
				String str;
				i = 0;
				while((str = br.readLine()) != null) {
					index.put(str,new Integer(i));
					i = i + 1;
				}
				br.close();

				(new SpatialLogger()).log("intersectCatagories, about to do queries");
				/* export file */
				RandomAccessFile raf = new RandomAccessFile(
						TabulationSettings.index_path
						+ CATAGORICAL_PREFIX + tablename + VALUE_POSTFIX,"rw");

				int value;

				Statement s;
				ResultSet r;

				//repeat for each point
				(new SpatialLogger()).log("intersectCatagories, begin queries");
				for(i=0;i<points.length;i++){
					longitude = String.valueOf(points[i][0]);
					latitude = String.valueOf(points[i][1]);

					if(longitude.equals("NaN") || latitude.equals("NaN")){
						raf.writeShort((short)(-1));
					}else{
						query = "select " + fieldname + " from " + tablename
							+ " where ST_WITHIN(ST_MAKEPOINT("
							+ longitude + "," + latitude + "),the_geom)";

						s = connection.createStatement();

						r = s.executeQuery(query);

						/**
						 * TODO: handle duplicate intersects (also border cases)
						 */
						if(r.next()){
							value = index.get(r.getString(1)).intValue();
							raf.writeShort((short)value);
						}else{
							raf.writeShort((short)(-1));
						}
					}
				}
				raf.close();
			}catch(Exception e){
				(new SpatialLogger()).log("intersectCatagories",
						e.toString() + "\r\n>query="+ query + "\r\n>i=" + i
						+ "\r\n>longitude, latitude=" + longitude + "," + latitude);
			}
		}
	}


	/**
	 * postgres db connection load
	 * @return
	 */
	private boolean connectDatabase(){
		try {
			System.out.println(
					TabulationSettings.db_connection_string + " > " +
					TabulationSettings.db_username+ " > " +
					TabulationSettings.db_password);


			Class.forName("org.postgresql.Driver");

			connection = DriverManager.getConnection(
					TabulationSettings.db_connection_string,
					TabulationSettings.db_username,
					TabulationSettings.db_password);

		} catch (Exception e) {
			(new SpatialLogger()).log("connectDatabase",e.toString());
			return false;
		}
		return connection != null;
	}

	/**
	 * postgres db connection disconnect
	 */
	private void disconnectDatabase(){
		try {
			connection.close();
		} catch (Exception e) {
			(new SpatialLogger()).log("disconnectDatabase",e.toString());
		}
	}

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

			ArrayList<String> output = new ArrayList<String>(record_end-record_start+1);

			if((new File(filenameD)).exists()){
				RandomAccessFile raf = new RandomAccessFile(filenameD,"r");
				raf.seek(record_start*4);
				float f;
				for(i=record_start;i<=record_end;i++){
					f = raf.readFloat();
					if(Float.isNaN(f)){
						output.add("");
					}else{
						output.add(String.valueOf(f));
					}
				}
				raf.close();
			}else if((new File(filenameI)).exists()){
				RandomAccessFile raf = new RandomAccessFile(filenameI,"r");
				raf.seek(record_start*2);
				short v;
				for(i=record_start;i<=record_end;i++){
					v = raf.readShort();
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

}
