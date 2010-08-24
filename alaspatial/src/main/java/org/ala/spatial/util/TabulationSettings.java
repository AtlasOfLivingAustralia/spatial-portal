package org.ala.spatial.util;

import java.io.File;

/**
 * home of all relevant tabulation settings as loaded from
 * appropriate tabulation_settings.xml
 *
 * TODO (among others)
 * 		- move to variables to final for reset on instance load
 * instead of dynamically
 *
 * @author Adam Collins
 *
 * @see Layer
 * @see Field
 */
public class TabulationSettings { 
	/**
	 * postgis enabled database
	 */
	public static String db_connection_string;
	public static String db_username;
	public static String db_password;

	/**
	 * table housing species occurances
	 */
	public static String source_table_name;

	/**
	 * true if species occurances table has location in field the_geom
	 */
	public static boolean point_type;

	/**
	 * longitude used when <code>point_type == false</code>
	 */
	public static String longitude_field;

	/**
	 * latitude used when <code>point_type == false</code>
	 */
	public static String latitude_field;

	/**
	 * field in occurances to filter by, likely called <code>species</code>
	 */
	public static String key_field;

	/**
	 * optional query prefix for the <code>key_field</code> value in the
	 * select query:
	 * <code> WHERE key_field = concat(key_value_prefix,key_value,key_value_postfix)</code>
	 */
	public static String key_value_prefix;

	/**
	 * optional query postfix for the <code>key_field</code> value in the
	 * select query:
	 * <code> WHERE key_field = concat(key_value_prefix,key_value,key_value_postfix)</code>
	 */
	public static String key_value_postfix;

	/**
	 * array of additional fields available in the occurances table
	 * that may be useful, such as <code>family</code> and <code>sample date</code>
	 */
	public static Field [] additional_fields;

	/**
	 * array of <code>Layer</code> representing geo enabled tables
	 * aimed at contextual layers but can also be used for environmental
	 * layers
	 */
	public static Layer [] geo_tables;

	/**
	 * absolute file path for environmental .gri/.grd files
	 */
	public static String environmental_data_path;

        /**
	 * absolute file path for environmental .gri/.grd files created to
         * align to the <grd_definition>
	 */
	public static String environmental_data_path_common;

	/**
	 * listing of available .gri/.grd files available at the
	 * <code>environmental_data_path</code>
	 */
	public static Layer [] environmental_data_files;

	/**
	 * flagged true when a file is loaded without error
	 *
	 * TODO : getter
	 */
	public static boolean loaded = false;

	/**
	 * occurances csv file full path
	 *
	 */
	public static String occurances_csv;

	/**
	 * occurances csv fields for use
	 */
	public static String [] occurances_csv_fields;
       
        /**
	 * in the same order as occurances_csv_fields_to_index,
         * "0" to output
         * "1" to sort
         * "2" to sort and index
         * "3" to longitude
         * "4" to latitude
         * index field.
         *
	 */
	public static String [] occurances_csv_field_settings;

        /**
         * path for common names file;
         * - CSV format
         * - no header
         * - first column is ID
         * - second column is COMMONNAME
         */
        public static String common_names_csv;

	/**
	 * directory for indexes
	 */
	public static String index_path;
		
    /**
     * alaspatial url for services call
     */
    public static String alaspatial_path;
    public static String[] occurances_csv_fields_lookups;

    /**
     * gdal apps path
     */
    public static String gdal_apps_dir;

    /**
     * base output directory where all the generated files go into
     */
    public static String base_output_dir;
    public static String base_output_url;

    /**
     * maxent path
     */
    public static String maxent_cmdpth;

    /**
     * Maximum number of records to dump
     */
    public static int MAX_RECORD_COUNT;

    /**
     * common grid definition
     */
    public static double grd_xmin;
    public static double grd_xmax;
    public static double grd_ymin;
    public static double grd_ymax;
    public static int grd_nrows;
    public static int grd_ncols;
    public static double grd_xdiv;
    public static double grd_ydiv;

    /**
     * path of convert executable
     */
    public static String convert_path;
    
	/**
	 * loads settings form name of the appropriate xml resource file
	 *
	 * @param filename name xml resource file to load
	 */
	public static void load(){
		SpatialLogger sl = new SpatialLogger();

		String filename = "";

		try{
			filename = TabulationSettings.class.getResource("/tabulation_settings.xml").getFile();
		}catch (Exception e){
			sl.log("Tabulation Settings",e.toString());

		}
		if(loaded){
			return;
		} else {
			loaded = true;
		}

		java.util.List<Field> fieldlist = new java.util.ArrayList<Field>();
		java.util.List<Layer> layerlist = new java.util.ArrayList<Layer>();

		String s1, s2, s3, s4, s5, s6;

		XmlReader xr = new XmlReader(filename);

		db_connection_string = xr.getValue("db_connection_string");
		db_username = xr.getValue("db_username");
		db_password = xr.getValue("db_password");
		source_table_name = xr.getValue("source_table_name");
		point_type = xr.getValue("point_type") == "true";
		longitude_field = xr.getValue("longitude_field");
		latitude_field = xr.getValue("latitude_field");
		key_field = xr.getValue("key_field");
		key_value_prefix = xr.getValue("key_value_prefix");
		key_value_postfix = xr.getValue("key_value_postfix");

		System.out.println(db_connection_string);
		System.out.println(db_username);
		System.out.println(db_password);
		System.out.println(source_table_name);
		System.out.println(point_type);
		System.out.println(longitude_field);
		System.out.println(latitude_field);
		System.out.println(key_field);
		System.out.println(key_value_prefix);
		System.out.println(key_value_postfix);

		//additional_fields
		String [] afname = {"additional_fields","additional_field","additional_field_name"};
		String [] afdisplay = {"additional_fields","additional_field","additional_field_display_name"};
		int [] afi = {0,0,0};
		fieldlist.clear();
		s1 = xr.getValue(afname,afi,3);
		s2 = xr.getValue(afdisplay,afi,3);
		System.out.println(s1 + " : " + s2);
		while(s1 != null){
			System.out.println(s1 + " : " + s2);
			fieldlist.add(new Field(s1,s2,""));
			afi[1]++;
			s1 = xr.getValue(afname,afi,3);
			s2 = xr.getValue(afdisplay,afi,3);
		}
		additional_fields = (Field[]) fieldlist.toArray(new Field[fieldlist.size()]);

		String [] gtname = {"geo_tables","geo_table","geo_table_name"};
		String [] gtdisplay = {"geo_tables","geo_table","geo_table_display_name"};
		String [] gtdescription = {"geo_tables","geo_table","geo_table_description"};
		String [] gttype = {"geo_tables","geo_table","geo_table_type"};
		String [] gtfields = {"geo_tables","geo_table","geo_fields_to_return"};
		int [] gti = {0,0,0};

		String [] gfname = {"geo_tables","geo_table","geo_fields_to_return","geo_field","geo_field_name"};
		String [] gfdisplay = {"geo_tables","geo_table","geo_fields_to_return","geo_field","geo_field_display_name"};
		int [] gfi = {0,0,0,0,0};

		layerlist.clear();
		s1 = xr.getValue(gtname,gti,3);
		s2 = xr.getValue(gtdisplay,gti,3);
		s3 = xr.getValue(gtdescription,gti,3);
		s4 = xr.getValue(gttype,gti,3);
		System.out.println(s1 + " : " + s2 + " : " + s3 + " : " + s4);
		while(s1 != null){

			fieldlist.clear();
			gfi[3] = 0;
			s5 = xr.getValue(gfname,gfi,5);
			s6 = xr.getValue(gfdisplay,gfi,5);
			System.out.println(s5 + " : " + s6);
			while(s5 != null){
				fieldlist.add(new Field(s5,s6,""));
				gfi[3]++;
				s5 = xr.getValue(gfname,gfi,5);
				s6 = xr.getValue(gfdisplay,gfi,5);
				System.out.println(s5 + " : " + s6);
			}

			layerlist.add(new Layer(s1,s2,s3,s4,(Field[]) fieldlist.toArray(new Field[fieldlist.size()])));
			gti[1]++;
			gfi[1]++;

			s1 = xr.getValue(gtname,gti,3);
			s2 = xr.getValue(gtdisplay,gti,3);
			s3 = xr.getValue(gtdescription,gti,3);
			s4 = xr.getValue(gttype,gti,3);
			System.out.println(s1 + " : " + s2 + " : " + s3 + " : " + s4);
		}
		geo_tables = (Layer[]) layerlist.toArray(new Layer[layerlist.size()]);

		environmental_data_path = xr.getValue("environmental_data_path");
		System.out.println("env_data_pth: " + environmental_data_path);

                environmental_data_path_common = xr.getValue("environmental_data_path_common");
		System.out.println("env_data_pth_common: " + environmental_data_path_common);

		String [] edname = {"environmental_data_files","environmental_data_file","environmental_data_file_name"};
		String [] eddisplay = {"environmental_data_files","environmental_data_file","environmental_data_file_display_name"};
		String [] eddescription =  {"environmental_data_files","environmental_data_file","environmental_data_file_description"};
		int [] edi = {0,0,0};
		layerlist.clear();
		s1 = xr.getValue(edname,edi,3);
		s2 = xr.getValue(eddisplay,edi,3);
		s3 = xr.getValue(eddescription,edi,3);
		System.out.println(s1 + " : " + s2 + " : " + s3);
		while(s1 != null){
			layerlist.add(new Layer(s1,s2,s3,"environmental",null));
			edi[1]++;

			s1 = xr.getValue(edname,edi,3);
			s2 = xr.getValue(eddisplay,edi,3);
			s3 = xr.getValue(eddescription,edi,3);
			System.out.println(s1 + " : " + s2 + " : " + s3);

		}
		environmental_data_files = (Layer[]) layerlist.toArray(new Layer[layerlist.size()]);

		//additions for indexing
		occurances_csv = xr.getValue("occurances_csv");
		System.out.println(occurances_csv);
		occurances_csv_fields = xr.getValue("occurances_csv_fields").split(",");
		for(String ocf : occurances_csv_fields){
			System.out.println(ocf);
		}               
                occurances_csv_field_settings = xr.getValue("occurances_csv_field_settings").split(",");
		for(String ocf : occurances_csv_field_settings){
			System.out.println(ocf);
		}

                occurances_csv_fields_lookups = xr.getValue("occurances_csv_fields_lookups").split(",");
		for(String ocf : occurances_csv_fields_lookups){
			System.out.println(ocf);
		}


                common_names_csv = xr.getValue("common_names_csv");
                System.out.println(common_names_csv);
                
		index_path = xr.getValue("index_path");
		System.out.println(index_path);
		
		alaspatial_path = xr.getValue("alaspatial_url");
                System.out.println("alaspatial_path");

                gdal_apps_dir = xr.getValue("gdal_apps_dir");
                System.out.println("gdal_apps_dir");

                base_output_dir = xr.getValue("base_output_dir");
                base_output_url = xr.getValue("base_output_url");
                System.out.println("base_output_dir: " + base_output_dir + " at " + base_output_url);

                try {
                    MAX_RECORD_COUNT = Integer.parseInt(xr.getValue("max_record_count"));
                } catch (NumberFormatException nfe) {
                    MAX_RECORD_COUNT = 100000; 
                } catch (Exception e) {
                    MAX_RECORD_COUNT = 100000; 
                }


                maxent_cmdpth = xr.getValue("cmdpth");
                System.out.println("maxent_cmdpth:" + maxent_cmdpth);

                convert_path = xr.getValue("convert_path");
                System.out.println("convert_path:" + convert_path);

                grd_xmin = Double.parseDouble(xr.getValue("grd_xmin"));
                grd_xmax = Double.parseDouble(xr.getValue("grd_xmax"));
                grd_ymin = Double.parseDouble(xr.getValue("grd_ymin"));
                grd_ymax = Double.parseDouble(xr.getValue("grd_ymax"));
                grd_nrows = Integer.parseInt(xr.getValue("grd_nrows"));
                grd_ncols = Integer.parseInt(xr.getValue("grd_ncols"));
                grd_xdiv = Double.parseDouble(xr.getValue("grd_xdiv"));
                grd_ydiv = Double.parseDouble(xr.getValue("grd_ydiv"));
	}

        static public String getPath(String layerName){
            //check for common grid
            File file = new File(environmental_data_path_common + layerName + ".gri");
            File file2 = new File(environmental_data_path_common + layerName + ".GRI");
            if(file.exists()){
                return environmental_data_path_common + layerName;
            } else {
                return environmental_data_path + layerName;
            }
        }


}
