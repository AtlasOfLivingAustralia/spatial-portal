package org.ala.spatial.util;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * home of all relevant tabulation settings as loaded from
 * appropriate tabulation_settings.xml
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
     * table housing species occurrences
     */
    public static String source_table_name;
    /**
     * true if species occurrences table has location in field the_geom
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
     * field in occurrences to filter by, likely called <code>species</code>
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
     * array of additional fields available in the occurrences table
     * that may be useful, such as <code>family</code> and <code>sample date</code>
     */
    public static Field[] additional_fields;
    /**
     * array of <code>Layer</code> representing geo enabled tables
     * aimed at contextual layers but can also be used for environmental
     * layers
     */
    public static Layer[] geo_tables;
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
    public static Layer[] environmental_data_files;
    /**
     * flagged true when a file is loaded without error
     *
     * TODO : getter
     */
    public static boolean loaded = false;
    /**
     * occurrences csv fields for use
     */
    public static String[] occurrences_csv_fields;
    /**
     * in the same order as occurrences_csv_fields_to_index,
     * "0" to output
     * "1" to sort
     * "2" to sort and index
     * "3" to longitude
     * "4" to latitude
     * index field.
     *
     */
    public static String[] occurrences_csv_field_settings;
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
    public static String[] occurrences_csv_fields_lookups;
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
    * base files directory where all the general files for links to users go
    */
    public static String base_files_dir;
    /**
     * maxent path
     */
    public static String maxent_cmdpth;
    /**
     * gdm path
     */
    public static String gdm_cmdpth;
    /**
     * Maximum number of records to dump
     */
    public static int MAX_RECORD_COUNT_CLUSTER;
    public static int MAX_RECORD_COUNT_DOWNLOAD;
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
     * maximum number of running jobs allowed
     */
    public static int jobs_maximum;
    /**
     * maximum number of whole grids that can be loaded into memory
     */
    public static int max_grids_load;
    /**
     * number of threads available to analysis
     */
    public static int analysis_threads;
    /**
     * aloc timings
     */
    public static double aloc_timing_0;
    public static double aloc_timing_1;
    public static double aloc_timing_2;
    public static double aloc_timing_3;
    public static double aloc_timing_4;
    public static double aloc_timing_5;
    public static double aloc_timing_6;
    /**
     * maxent timings
     */
    public static double maxent_timing_0;
    public static double maxent_timing_1;
    public static double maxent_timing_2;
    /**
     * smoothing factor for analysis job estimates
     */
    public static int process_estimate_smoothing;
    /**
     * species list includes one additional (e.g. Family) column in the export,
     * this is the column order or 2's in occurrences_csv_field_settings.
     */
    public static int species_list_first_column_index;
    /**
     * limit to number of occurrences to load, for testing.
     *
     * -1 for all (prod)
     */
    public static int occurrences_csv_max_records;
    /**
     * pairs of conceptId's and search field names
     */
    public static String[] occurrences_csv_field_pairs;
    /**
     * screen friendly names for 2's (hierarchy indexed columns in
     * occurrences file
     */
    public static String[] occurrences_csv_twos_names;
    /**
     * fields for geojson calls
     */
    public static int geojson_id;
    public static int geojson_longitude;
    public static int geojson_latitude;
    public static String[] geojson_property_names;
    public static String[] geojson_property_display_names;
    public static int[] geojson_property_fields;
    public static int[] geojson_property_types;
    public static String[] geojson_property_catagory;
    public static String[] geojson_property_units;
    /**
     * for sensitive coordinate handling
     */
    public static String occurrences_id_field;
    public static String occurrences_sen_long_field;
    public static String occurrences_sen_lat_field;
    public static int cluster_lookup_size;
    public static String occurrences_dr_uid;
    public static String citation_url_data_provider;
    public static String citation_url_layer_provider;
    public static String ala_logger_url;
    public static String spatial_logger_url;
    /**
     * occurrences_config_path for datasets/versions of occurrences_csv
     */
    public static String occurrences_config_path;
    /**
     * path to csv file containing:
     * shape_file_path, scientific name, [optional data, e.g. depth]
     */
    public static String shape_intersection_files;
    /**
     * path to store user generated data
     */
    public static String file_store;

    /**
     * loads settings form name of the appropriate xml resource file
     *
     * @param filename name xml resource file to load
     */
    public static void load() {

        if (loaded) {
            return;
        } else {
            loaded = true;
        }

        String filename = "";

        try {
            filename = TabulationSettings.class.getResource("/tabulation_settings.xml").getFile();
        } catch (Exception e) {
            SpatialLogger.log("Tabulation Settings", e.toString());
        }

//        System.out.println("tabulation_settings.xml: " + filename);
//        try {
//            RandomAccessFile raf = new RandomAccessFile(filename, "r");
//            byte[] b = new byte[(int) raf.length()];
//            String s = new String(b, "UTF-8");
//            System.out.println(s);
//            raf.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }



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
        String[] afname = {"additional_fields", "additional_field", "additional_field_name"};
        String[] afdisplay = {"additional_fields", "additional_field", "additional_field_display_name"};
        int[] afi = {0, 0, 0};
        fieldlist.clear();
        s1 = xr.getValue(afname, afi, 3);
        s2 = xr.getValue(afdisplay, afi, 3);
        System.out.println(s1 + " : " + s2);
        while (s1 != null) {
            System.out.println(s1 + " : " + s2);
            fieldlist.add(new Field(s1, s2, ""));
            afi[1]++;
            s1 = xr.getValue(afname, afi, 3);
            s2 = xr.getValue(afdisplay, afi, 3);
        }
        additional_fields = (Field[]) fieldlist.toArray(new Field[fieldlist.size()]);

        String[] gtname = {"geo_tables", "geo_table", "geo_table_name"};
        String[] gtdisplay = {"geo_tables", "geo_table", "geo_table_display_name"};
        String[] gtdescription = {"geo_tables", "geo_table", "geo_table_description"};
        String[] gttype = {"geo_tables", "geo_table", "geo_table_type"};
        String[] gtfields = {"geo_tables", "geo_table", "geo_fields_to_return"};
        int[] gti = {0, 0, 0};

        String[] gfname = {"geo_tables", "geo_table", "geo_fields_to_return", "geo_field", "geo_field_name"};
        String[] gfdisplay = {"geo_tables", "geo_table", "geo_fields_to_return", "geo_field", "geo_field_display_name"};
        int[] gfi = {0, 0, 0, 0, 0};

        layerlist.clear();
        s1 = xr.getValue(gtname, gti, 3);
        s2 = xr.getValue(gtdisplay, gti, 3);
        s3 = xr.getValue(gtdescription, gti, 3);
        s4 = xr.getValue(gttype, gti, 3);
        System.out.println(s1 + " : " + s2 + " : " + s3 + " : " + s4);
        while (s1 != null) {

            fieldlist.clear();
            gfi[3] = 0;
            s5 = xr.getValue(gfname, gfi, 5);
            s6 = xr.getValue(gfdisplay, gfi, 5);
            System.out.println(s5 + " : " + s6);
            while (s5 != null) {
                fieldlist.add(new Field(s5, s6, ""));
                gfi[3]++;
                s5 = xr.getValue(gfname, gfi, 5);
                s6 = xr.getValue(gfdisplay, gfi, 5);
                System.out.println(s5 + " : " + s6);
            }

            layerlist.add(new Layer(s1, s2, s3, s4, (Field[]) fieldlist.toArray(new Field[fieldlist.size()])));
            gti[1]++;
            gfi[1]++;

            s1 = xr.getValue(gtname, gti, 3);
            s2 = xr.getValue(gtdisplay, gti, 3);
            s3 = xr.getValue(gtdescription, gti, 3);
            s4 = xr.getValue(gttype, gti, 3);
            System.out.println(s1 + " : " + s2 + " : " + s3 + " : " + s4);
        }
        geo_tables = (Layer[]) layerlist.toArray(new Layer[layerlist.size()]);

        environmental_data_path = xr.getValue("environmental_data_path");
        System.out.println("env_data_pth: " + environmental_data_path);

        environmental_data_path_common = xr.getValue("environmental_data_path_common");
        System.out.println("env_data_pth_common: " + environmental_data_path_common);

        String[] edname = {"environmental_data_files", "environmental_data_file", "environmental_data_file_name"};
        String[] eddisplay = {"environmental_data_files", "environmental_data_file", "environmental_data_file_display_name"};
        String[] eddescription = {"environmental_data_files", "environmental_data_file", "environmental_data_file_description"};
        int[] edi = {0, 0, 0};
        layerlist.clear();
        s1 = xr.getValue(edname, edi, 3);
        s2 = xr.getValue(eddisplay, edi, 3);
        s3 = xr.getValue(eddescription, edi, 3);
        System.out.println(s1 + " : " + s2 + " : " + s3);
        while (s1 != null) {
            layerlist.add(new Layer(s1, s2, s3, "environmental", null));
            edi[1]++;

            s1 = xr.getValue(edname, edi, 3);
            s2 = xr.getValue(eddisplay, edi, 3);
            s3 = xr.getValue(eddescription, edi, 3);
            System.out.println(s1 + " : " + s2 + " : " + s3);

        }
        environmental_data_files = (Layer[]) layerlist.toArray(new Layer[layerlist.size()]);

        //additions for indexing
        occurrences_csv_fields = xr.getValue("occurrences_csv_fields").split(",");
        for (String ocf : occurrences_csv_fields) {
            System.out.println(ocf);
        }
        occurrences_csv_field_settings = xr.getValue("occurrences_csv_field_settings").split(",");
        for (String ocf : occurrences_csv_field_settings) {
            System.out.println(ocf);
        }

        occurrences_csv_fields_lookups = xr.getValue("occurrences_csv_fields_lookups").split(",");
        for (String ocf : occurrences_csv_fields_lookups) {
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

        base_files_dir = xr.getValue("base_files_dir");
        System.out.println("base_files_dir: " + base_files_dir);
        
        try {
            MAX_RECORD_COUNT_DOWNLOAD = Integer.parseInt(xr.getValue("max_record_count_download"));
        } catch (NumberFormatException nfe) {
            MAX_RECORD_COUNT_DOWNLOAD = 15000;
        } catch (Exception e) {
            MAX_RECORD_COUNT_DOWNLOAD = 15000;
        }

        try {
            MAX_RECORD_COUNT_CLUSTER = Integer.parseInt(xr.getValue("max_record_count_cluster"));
        } catch (NumberFormatException nfe) {
            MAX_RECORD_COUNT_CLUSTER = 1000000;
        } catch (Exception e) {
            MAX_RECORD_COUNT_CLUSTER = 1000000;
        }

        maxent_cmdpth = xr.getValue("cmdpth");
        System.out.println("maxent_cmdpth:" + maxent_cmdpth);

        gdm_cmdpth = xr.getValue("gdm_cmdpth");
        System.out.println("gdm_cmdpth:" + gdm_cmdpth);

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

        jobs_maximum = Integer.parseInt(xr.getValue("jobs_maximum"));

        max_grids_load = Integer.parseInt(xr.getValue("max_grids_load"));

        analysis_threads = Integer.parseInt(xr.getValue("analysis_threads"));

        aloc_timing_0 = Double.parseDouble(xr.getValue("aloc_timing_0"));
        aloc_timing_1 = Double.parseDouble(xr.getValue("aloc_timing_1"));
        aloc_timing_2 = Double.parseDouble(xr.getValue("aloc_timing_2"));
        aloc_timing_3 = Double.parseDouble(xr.getValue("aloc_timing_3"));
        aloc_timing_4 = Double.parseDouble(xr.getValue("aloc_timing_4"));
        aloc_timing_5 = Double.parseDouble(xr.getValue("aloc_timing_5"));
        aloc_timing_6 = Double.parseDouble(xr.getValue("aloc_timing_6"));

        maxent_timing_0 = Double.parseDouble(xr.getValue("maxent_timing_0"));
        maxent_timing_1 = Double.parseDouble(xr.getValue("maxent_timing_1"));
        maxent_timing_2 = Double.parseDouble(xr.getValue("maxent_timing_2"));

        System.out.println("species_list_first_column_index=" + xr.getValue("species_list_first_column_index"));
        species_list_first_column_index = Integer.parseInt(xr.getValue("species_list_first_column_index"));

        occurrences_csv_max_records = Integer.parseInt(xr.getValue("occurrences_csv_max_records"));

        occurrences_csv_field_pairs = xr.getValue("occurrences_csv_field_pairs").split(",");

        occurrences_csv_twos_names = xr.getValue("occurrences_csv_twos_names").split(",");

        geojson_id = Integer.parseInt(xr.getValue("geojson_id"));
        geojson_longitude = Integer.parseInt(xr.getValue("geojson_longitude"));
        geojson_latitude = Integer.parseInt(xr.getValue("geojson_latitude"));
        geojson_property_names = xr.getValue("geojson_property_names").split(",");
        geojson_property_display_names = xr.getValue("geojson_property_display_names").split(",");
        String[] pf = xr.getValue("geojson_property_fields").split(",");
        geojson_property_fields = new int[pf.length];
        for (int i = 0; i < pf.length; i++) {
            geojson_property_fields[i] = Integer.parseInt(pf[i]);
        }

        //double = 0, int = 1, boolean = 2, string = 3
        String[] pt = xr.getValue("geojson_property_types").split(",");
        geojson_property_types = new int[pt.length];
        for (int i = 0; i < pt.length; i++) {
            if (pt[i].equalsIgnoreCase("double")) {
                geojson_property_types[i] = 0;
            } else if (pt[i].equalsIgnoreCase("int")) {
                geojson_property_types[i] = 1;
            } else if (pt[i].equalsIgnoreCase("boolean")) {
                geojson_property_types[i] = 2;
            } else if (pt[i].equalsIgnoreCase("string")) {
                geojson_property_types[i] = 3;
            } else {
                //error
                System.out.println("unsupported <geojson_property_types>: " + pt[i]);
            }
        }

        geojson_property_catagory = xr.getValue("geojson_property_catagory").split(",");
        geojson_property_units = xr.getValue("geojson_property_units").split(",");

        process_estimate_smoothing = Integer.parseInt(xr.getValue("process_estimate_smoothing"));

        occurrences_id_field = xr.getValue("occurrences_id_field");
        occurrences_sen_long_field = xr.getValue("occurrences_sen_long_field");
        occurrences_sen_lat_field = xr.getValue("occurrences_sen_lat_field");

        cluster_lookup_size = Integer.parseInt(xr.getValue("cluster_lookup_size"));

        occurrences_dr_uid = xr.getValue("occurrences_dr_uid");
        citation_url_data_provider = xr.getValue("citation_url_data_provider");
        citation_url_layer_provider = xr.getValue("citation_url_layer_provider");
        ala_logger_url = xr.getValue("ala_logger_url");
        spatial_logger_url = xr.getValue("spatial_logger_url");

        occurrences_config_path = xr.getValue("occurrences_config_path");
        shape_intersection_files = xr.getValue("shape_intersection_files");

        file_store = xr.getValue("file_store");
    }

    static public String getPath(String layerName) {
        //check for common grid
        File file = new File(environmental_data_path_common + layerName + ".gri");
        if (!file.exists()) {
            file = new File(environmental_data_path_common + layerName + ".GRI");
        }
        if (file.exists()) {
            return environmental_data_path_common + layerName;
        } else {
            return environmental_data_path + layerName;
        }
    }
}
