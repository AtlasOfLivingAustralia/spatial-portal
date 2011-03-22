package org.ala.spatial.analysis.index;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.analysis.cluster.SpatialCluster3;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Tile;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * builder for species list index.
 *
 * requires OccurrencesIndex to be up to date.
 *
 * operates on GridFiles
 * operates on ShapeFiles
 *
 * TODO: incremental and partial update mechanism
 *
 * @author adam
 *
 */
public class FilteringIndex extends Object {

    /**
     * length of a grid record in bytes
     */
    static final int GRID_RECORD_LENGTH = 4 * 3 + 4; //3 floats + 1 int
    /**
     * length of a catagory record in bytes
     */
    static final int CATAGORY_RECORD_LENGTH = 4 * 2 + 4;//1 short + 2 floats + 1 int
    /**
     * mapping for LayerFilters copies, init since static
     */
    static Map<String, LayerFilter> map_layerfilters = new HashMap<String, LayerFilter>();
    /**
     * destination of loaded occurrences
     */
    ArrayList<String[]> occurrences;
    String index_path;

    /**
     * default constructor
     */
    public FilteringIndex(String directoryPath) {
        index_path = directoryPath;

        /* load settings file */
        TabulationSettings.load();
    }
    
    /**
     * performs update of 'indexing' for a new layer (grid or shapefile)
     *
     * @param layername name of the layer to update as String.  To update
     * all layers use null.
     */
    public void layersUpdate(String layername) {
        /* placeholder for patial update mechanism */
        makeSPL_GRID(layername);

        makeSPL_CATAGORIES(layername);

        /* for onscreen filtering server or client side */
        makeAllScaledShortImages(layername);
    }

    /**
     * method to determine if the index is up to date
     *
     * @return true if index is up to date
     */
    public boolean isUpToDate() {
        /* placeholder for patial update mechanism */
        return true;
    }

    /**
     * make species list index for environmental files
     *
     * for each grid file intersect and export in points order
     *
     * uses OccurrencesIndex generated SAM_D_ files
     */
    void makeSPL_GRID(String layername) {

        Layer layer;
        int i;
        /* iterate for each environmental file */
        for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
            layer = TabulationSettings.environmental_data_files[i];

            if (layername != null
                    && !layername.equalsIgnoreCase(TabulationSettings.environmental_data_files[i].name)) {
                continue;
            }

            System.out.println("makeSPL_GRID: " + layer.display_name + " target?: " + layername);

            try {

                /* open input file for this layer's values in order of records*/
                RandomAccessFile sam_d_gridfile = new RandomAccessFile(
                        index_path + "SAM_D_" + layer.name + ".dat", "r");

                int number_of_records = (int) (sam_d_gridfile.length() / 4); //4 bytes in float
                byte[] b = new byte[(number_of_records) * 4];
                sam_d_gridfile.read(b);
                ByteBuffer bb = ByteBuffer.wrap(b);
                sam_d_gridfile.close();

                /* temporary variables */
                double value;

                ArrayList<SPLGridRecord> records = new ArrayList<SPLGridRecord>(number_of_records);

                /* maintain original record index number
                 * for unqiueness
                 */
                int p = 0;

                /* load all valid values and add to records with species number and
                 * original record index
                 */
                for (i = 0; i < number_of_records; i++) {
                    /* split up record line for extraction of:
                     *  species name (column at idx ofu.onetwoCount-1)
                     *  longitude (before last value)
                     *  latitude (last value)
                     */

                    value = bb.getFloat();

                    /* do not want NaN */
                    if (!Double.isNaN(value)) {
                        /* put it in a map to sort later */
                        records.add(new SPLGridRecord(value, p));
                    }

                    p++;	//increment for next original record
                }

                /* sort records by environmental value */
                java.util.Collections.sort(records,
                        new Comparator<SPLGridRecord>() {

                            public int compare(SPLGridRecord r1, SPLGridRecord r2) {
                                if (r2.value == r1.value) {
                                    return 0;
                                } else if (r1.value < r2.value) {
                                    return -1;
                                }
                                return 1;
                            }
                        });

                /* open output file and write */
                RandomAccessFile outputvalues = new RandomAccessFile(
                        index_path + "SPL_V_" + layer.name + ".dat", "rw");

                RandomAccessFile outputrecords = new RandomAccessFile(
                        index_path + "SPL_R_" + layer.name + ".dat", "rw");

                byte[] b1 = new byte[records.size() * 4]; //for float
                ByteBuffer bb1 = ByteBuffer.wrap(b1);
                byte[] b2 = new byte[records.size() * 8]; //for int
                ByteBuffer bb2 = ByteBuffer.wrap(b2);

                for (SPLGridRecord r : records) {
                    bb1.putFloat((float) r.value);
                    bb2.putInt(r.record_number);
                }
                outputvalues.write(b1);
                outputrecords.write(b2);

                outputvalues.close();
                outputrecords.close();

                SpatialLogger.log("makeSPL_GRID writing done > " + layer.name);

            } catch (Exception e) {
                SpatialLogger.log("makeSPL_GRID writing", ">" + layer.name + "> " + e.toString());
            }
        }

    }

    /**
     * make species list index for catagorical files
     *
     * for each catagory file, split by field index value
     *
     */
    void makeSPL_CATAGORIES(String layername) {
        Layer layer;
        int i;
        /* iterate for each shape file */
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            if (layername != null
                    && !layername.equalsIgnoreCase(TabulationSettings.geo_tables[i].name)) {
                continue;

            }

            layer = TabulationSettings.geo_tables[i];
            try {
                /* open catagorical value file */
                RandomAccessFile sam_i_layerfile = new RandomAccessFile(
                        index_path
                        + SamplingIndex.CATAGORICAL_PREFIX + layer.name
                        + SamplingIndex.VALUE_POSTFIX, "r");

                int number_of_records = (int) (sam_i_layerfile.length() / 2); //2 bytes in short
                byte[] b = new byte[(number_of_records) * 2];
                sam_i_layerfile.read(b);
                ByteBuffer bb = ByteBuffer.wrap(b);
                sam_i_layerfile.close();


                /* temporary variables */
                int value;
                ArrayList<SPLGridRecord> records = new ArrayList<SPLGridRecord>(number_of_records);

                int p = 0;			/* maintain original record index number
                 * for unqiueness
                 */

                /* load all valid values and add to records with species number and
                 * original record index*/
                for (int j = 0; j < number_of_records; j++) {

                    /* split up record line for extraction of:
                     *  species name (2 before last value)
                     *  longitude (before last value)
                     *  latitude (last value)
                     */
                    value = bb.getShort();

                    /* put it in a map to sort later */
                    records.add(new SPLGridRecord(value, p));

                    p++; 		//maintain original record number
                }

                /* sort by catagorical value */
                java.util.Collections.sort(records,
                        new Comparator<SPLGridRecord>() {

                            public int compare(SPLGridRecord r1, SPLGridRecord r2) {
                                if (r2.value == r1.value) {
                                    return r1.record_number - r2.record_number;
                                } else if (r1.value < r2.value) {
                                    return -1;
                                }
                                return 1;
                            }
                        });


                /* open output file and write */
                int idxpos = 0;
                int startpos = 0;

                /* iterate across records, exporting each group of records
                 * with the same value into a separate file with serialization
                 *
                 * exports as species_number and original_sorted_index (key)
                 *
                 * usage is whole of file
                 */
                int len = records.size();
                for (idxpos = 0; idxpos < len; idxpos++) {
                    if (idxpos + 1 == len || records.get(idxpos).value != records.get(idxpos + 1).value) {
                        //output the previous group of records into a new file

                        String filename =
                                index_path
                                + "SPL_" + layer.name + "_"
                                + records.get(idxpos).value + ".dat";


                        /* open output stream */
                        FileOutputStream fos = new FileOutputStream(filename);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        ObjectOutputStream oos = new ObjectOutputStream(bos);

                        /* build set */
                        int[] set = new int[idxpos - startpos + 1];
                        for (p = startpos; p <= idxpos; p++) {
                            set[p - startpos] = records.get(p).record_number;
                        }

                        /* sort by record_number (key) */
                        java.util.Arrays.sort(set);

                        /* write */
                        oos.writeObject(set);
                        oos.close();

                        /* preserve starting pos for end of next value */
                        startpos = idxpos + 1;
                    }
                }

                sam_i_layerfile.close();

                SpatialLogger.log("makeSPL_CATAGORIES writing done > " + layer.name);

            } catch (Exception e) {
                SpatialLogger.log("makeSPL_CATAGORIES writing", ">" + layer.name + "> " + e.toString());
            }
        }
    }

    /**
     * gets a default LayerFilter for a layer by layer name
     * @param layer name of layer
     * @return new default LayerFilter or null if does not exist
     */
    public static LayerFilter getLayerFilter(String layer) {
        /* if template already loaded copy it */
        LayerFilter layerfilter = map_layerfilters.get(layer);
        if (layerfilter == null) {
            layerfilter = getLayerFilter(Layers.getLayer(layer));
            map_layerfilters.put(layer, layerfilter);
        }

        /* if still not loaded, reutrn null */
        if (layerfilter == null) {
            SpatialLogger.info("getLayerFilter(" + layer + ")", ">layer does not exist");
            return null;
        }

        return layerfilter.copy();
    }

    /**
     * gets a default LayerFilter for a layer by layer object
     *
     * note: use getLayerFilter(String) for public function
     *
     * @param layer as Layer
     * @return new default LayerFilter
     */
    static private LayerFilter getLayerFilter(Layer layer) {
        if (layer == null) {
            return null;
        }

        // check that the tabulationsettings is loaded.
        // if not, load it.
        if (!TabulationSettings.loaded) {
            TabulationSettings.load();
        }

        /* is it continous (grid file) */
        File continous_file = new File(TabulationSettings.environmental_data_path
                + layer.name + ".gri");
        if (!continous_file.exists()) {
            continous_file = new File(TabulationSettings.environmental_data_path
                    + layer.name + ".GRI");
        }
        if (continous_file.exists()) {
            /* load grid file and fill layerfilter with min/max */
            try {
                Grid grid = new Grid(TabulationSettings.environmental_data_path
                        + layer.name);

                double minimum = grid.minval;
                double maximum = grid.maxval;

                return new LayerFilter(layer, null, null, minimum, maximum);

            } catch (Exception e) {
                SpatialLogger.info("getLayerFilter(" + layer + ")", ">error opening grid file");
                return null;
            }
        }

        /* otherwise, catagorical */

        /*
         * TODO: handle multiple fieldnames
         */
        String fieldname = layer.fields[0].name;
        int i;

        File catagories_file = new File(
                TabulationSettings.index_path
                + SamplingIndex.CATAGORY_LIST_PREFIX
                + layer.name + "_" + fieldname
                + SamplingIndex.CATAGORY_LIST_POSTFIX);
        if (catagories_file.exists()) {
            /* load catagories names and fill layerfilter with it */
            byte[] data = new byte[(int) catagories_file.length()];
            try {
                FileInputStream fis = new FileInputStream(catagories_file);
                fis.read(data);
                fis.close();

                /* insert (row number) as beginning of each line */
                String str = new String(data);
                data = null;

                String[] catagory_names = str.split("\n");
                int[] catagories = new int[catagory_names.length];

                for (i = 0; i < catagories.length; i++) {
                    catagories[i] = i;
                }

                return new LayerFilter(layer, catagories, catagory_names, 0, 0);

            } catch (Exception e) {
                SpatialLogger.log("getLayerExtents(" + layer.name + "), catagorical",
                        e.toString());
            }
        }

        return null;
    }

    /**
     * gets layer extents for a layer
     *
     * TODO: improve this function, move to LayerService.java
     *
     * @param layer_name layer name as String
     * @return formatted layer extents as String,
     * - min and max for grid
     * - list of values for shape
     */
    static public String getLayerExtents(String layer_name) {
        /* is it continous (grid file) */
        /*
         * TODO: handle multiple fieldnames
         */
        String fieldname = "";
        int i;
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            Layer l = TabulationSettings.geo_tables[i];
            if (l.name.equalsIgnoreCase(layer_name)) {
                fieldname = l.fields[0].name;
                break;
            }
        }
        File catagories_file = new File(
                TabulationSettings.index_path
                + SamplingIndex.CATAGORY_LIST_PREFIX
                + layer_name + "_" + fieldname
                + SamplingIndex.CATAGORY_LIST_POSTFIX);

        if (!catagories_file.exists()) {
            /* load grid file and get min/max */
            try {
                Grid grid = new Grid(TabulationSettings.environmental_data_path
                        + layer_name);

                double minimum = grid.minval;
                double maximum = grid.maxval;

                return ((float) minimum) + " to " + ((float) maximum);
            } catch (Exception e) {
                /* log error */
                //return nothing
                return "";
            }
        }

        /* otherwise, catagorical */
        if (catagories_file.exists()) {
            /* load catagories values file and format */
            byte[] data = new byte[(int) catagories_file.length()];
            try {
                FileInputStream fis = new FileInputStream(catagories_file);
                fis.read(data);
                fis.close();

                /* insert (row number) as beginning of each line */
                String str = new String(data);
                data = null;

                String[] lines = str.split("\n");
                str = null;

                StringBuffer output = new StringBuffer();

                /*
                 * TODO: add -1, missing
                 */
                int row = 0;					// 0..n indexing
                for (String s : lines) {
                    if (s.length() > 0) {
                        output.append(row + "," + s + "\r\n");
                        row = row + 1;
                    }
                }

                return output.toString().replace("\r\n", "<br>\r\n");
            } catch (Exception e) {
                SpatialLogger.log("getLayerExtents(" + layer_name + "), catagorical",
                        e.toString());
            }
        }

        return "";
    }

    /**
     * gets list of SpeciesRecord (species number and record number) from
     * a layer filter (LayerFilter), grid/environmental/continous data source
     * @param filter layer filter as LayerFilter
     * @return records within the filtered region defined as ArrayList<RecordKey>
     */
    public int[] getGridSampleSet(LayerFilter filter) {
        /* grid/env filter, binary searching by seeking */

        double dblvalue;

        int[] set = null;

        try {
            /* open VALUES file to determine records positions */
            RandomAccessFile raf = new RandomAccessFile(
                    index_path + "SPL_V_" + filter.layername + ".dat", "r");

            /* seek to first position */
            int length = (int) raf.length() / 4;                       //4 = sizeof float
            int recordpos = length / 2;
            int step = recordpos / 2;
            while (step > 1) {
                raf.seek(recordpos * 4);				//4 = sizeof float
                dblvalue = raf.readFloat();

                /* determine direction */
                if (dblvalue > filter.minimum_value) {
                    recordpos -= step;
                } else {
                    recordpos += step;
                }
                step /= 2;
            }
            /* roll back to before the list */
            raf.seek(recordpos * 4);					//4 = sizeof float
            while (recordpos > 0 && raf.readFloat() > filter.minimum_value) {
                recordpos--;
                raf.seek(recordpos * 4);				//4 = sizeof float
            }
            /* roll forwards to first record */
            raf.seek(recordpos * 4);					//4 = sizeof float
            while (recordpos < length && raf.readFloat() < filter.minimum_value) {
                recordpos++;
                raf.seek(recordpos * 4);				//4 = sizeof float
            }
            /* seek to actual position */
            raf.seek(recordpos * 4);					//4 = sizeof float

            int start_recordpos = recordpos;
            double over_max = filter.maximum_value + 0.000001; //TODO fix this as an actual small value

            /* seek to last position */
            length = (int) raf.length() / 4;                             //4 = sizeof float
            recordpos = length / 2;
            step = recordpos / 2;
            while (step > 1) {
                raf.seek(recordpos * 4);				//4 = sizeof float
                dblvalue = raf.readFloat();

                /* determine direction */
                if (dblvalue > over_max) {
                    recordpos -= step;
                } else {
                    recordpos += step;
                }
                step /= 2;
            }
            /* roll back to before the list */
            raf.seek(recordpos * 4);					//4 = sizeof float
            while (recordpos > 0 && raf.readFloat() > over_max) {
                recordpos--;
                raf.seek(recordpos * 4);				//4 = sizeof float

            }
            /* roll forwards to first record */
            raf.seek(recordpos * 4);					//4 = sizeof float
            while (recordpos < length && raf.readFloat() < over_max) {
                recordpos++;
                raf.seek(recordpos * 4);				//4 = sizeof float
            }
            /* seek to actual position */
            raf.seek(recordpos * 4);					//4 = sizeof float

            int end_recordpos = recordpos - 1;
            System.out.println("size: " + end_recordpos + " " + start_recordpos);

            raf.close();        //done with VALUES file


            /* open RECORDS file to get records numbers */
            raf = new RandomAccessFile(
                    index_path + "SPL_R_" + filter.layername + ".dat", "r");

            /* read between start_recordpos and end_recordpos */
            raf.seek(start_recordpos * 4);				//4 = sizeof int
            byte[] records = new byte[(end_recordpos - start_recordpos + 1) * 4]; //TODO: setup for correct use of +1 //4 = sizeof int

            raf.read(records);

            /* byte buffer */
            ByteBuffer bb = ByteBuffer.wrap(records);

            /* convert records to keys & add to system */
            long len = records.length / 4;                          //4 = sizeof int
            //float longitude, latitude;
            int record;
            int j;
            set = new int[(int) len];
            for (j = 0; j < len; j++) {
                set[j] = bb.getInt();
            }

            /* sort by key (record number) */
            java.util.Arrays.sort(set);

            raf.close();

        } catch (Exception e) {
            SpatialLogger.log("apply continous species list file",
                    "layer:" + filter.layername
                    + "> " + e.toString());
        }

        return set;

    }

    /**
     * gets list of SpeciesRecord (species number and record number) from
     * a layer filter (LayerFilter), catagorical/contextual/shapefile data source
     * @param filter layer filter as LayerFilter
     * @return records within the filtered region defined as ArrayList<SpeciesRecord>
     */
    public int[] getCatagorySampleSet(LayerFilter filter) {

        int[] set = null;

        /* catagory filter, iterate the whole file for each catagory */
        int value;
        int k = 0;
        int j;
        for (j = 0; j < filter.catagories.length; j++) {
            value = filter.catagories[j];
            try {
                String filename =
                        index_path + "SPL_" + filter.layername + "_"
                        + ((double) value) + ".dat";

                FileInputStream fos = new FileInputStream(filename);
                BufferedInputStream bos = new BufferedInputStream(fos);
                ObjectInputStream oos = new ObjectInputStream(bos);

                int[] newset;
                newset = (int[]) oos.readObject();
                oos.close();

                if (set != null) {
                    int[] joined = new int[set.length + newset.length];
                    System.arraycopy(set, 0, joined, 0, set.length);
                    System.arraycopy(newset, 0, joined, set.length, newset.length);
                } else {
                    set = newset;
                }
            } catch (Exception e) {
                SpatialLogger.log("apply catagory species list file",
                        "layer:" + filter.layername + " cat:" + value
                        + ": species_mask_layer len:"
                        + " : " + k + ">" + e.toString());
            }
        }
        if (filter.catagories.length > 1) {
            java.util.Arrays.sort(set);
        }
        return set;
    }

    /**
     * makes indexed tiles for filtering by a region
     *
     * TODO: dynamic
     */
    public void makeAllScaledShortImages(String layername) {
        /* ?? default long lat & dist */
        double longitude_start = 112;
        double longitude_end = 154;
        double latitude_start = -44;//-44;
        double latitude_end = -9;
        int height = 840; //210 42/210
        int width = 1008; //252

        int i, j;

        /* get all layers */
        int size = 0;
        Layer[] all_layers = new Layer[TabulationSettings.environmental_data_files.length
                + TabulationSettings.geo_tables.length];
        j = TabulationSettings.environmental_data_files.length;
        for (i = 0; i < j; i++) {
            if (layername != null
                    && !layername.equalsIgnoreCase(TabulationSettings.environmental_data_files[i].name)) {
                continue;

            }
            all_layers[size++] = TabulationSettings.environmental_data_files[i];
        }
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            if (layername != null
                    && !layername.equalsIgnoreCase(TabulationSettings.geo_tables[i].name)) {
                continue;
            }
            all_layers[size++] = TabulationSettings.geo_tables[i];
        }

        /* process all layers */
        for (i = 0; i < size; i++) {
            //makeScaledShortImageFromGrid(all_layers[i], longitude_start, longitude_end, latitude_start, latitude_end, width, height);
            makeScaledShortImageFromGridToMetresGrid(all_layers[i], longitude_start, longitude_end, latitude_start, latitude_end, width, height);
            i++;
        }
    }

    /**
     * filter tile creation...
     *
     * more in docs now
     *
     * @param l	layer as Layer
     * @param longitude_start longitude extent as double
     * @param longitude_end other longitude extent as double
     * @param latitude_start latitude extent as double
     * @param latitude_end other latitude extent as double
     * @param width width resoluion as int
     * @param height height resolution as in
     */
    public void makeScaledShortImageFromGrid(Layer l, double longitude_start, double longitude_end,
            double latitude_start, double latitude_end, int width, int height) {

        /* output data */
        Tile[] data;

        /* load raw */
        if (l.type.equals("environmental")) {
            data = getTileFromGrid(l.name, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        } else {
            data = getTileFromShape(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        }

        /* sort */
        java.util.Arrays.sort(data,
                new Comparator<Tile>() {

                    public int compare(Tile i1, Tile i2) {
                        if (i1.value_ < i2.value_) {
                            return -1;
                        } else if (i1.value_ > i2.value_) {
                            return 1;
                        }
                        return 0;
                    }
                });

        /* index, only used for non-enviornmental layers */
        boolean has_index = true;
        if (l.type.equals("environmental")) {
            has_index = false;
        }
        int[] index = null;
        if (has_index) {
            int i, j;
            int max = (int) data[data.length - 1].value_;

            index = new int[max + 2];
            int last_idx = 1;
            for (i = 1; i < data.length; i++) {
                if (data[i].value_ != data[i - 1].value_) {
                    for (j = last_idx; j <= data[i].value_; j++) {
                        index[j] = i;
                    }
                    last_idx = (int) data[i].value_ + 1;
                }
            }
            index[max + 1] = data.length;
        }

        /* write as object*/
        try {
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path + "SPL_IMG_T_" + l.name + ".dat");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.writeObject(new Boolean(has_index));
            if (has_index) {
                oos.writeObject(index);
            }
            oos.close();

        } catch (Exception e) {
            /* TODO: log error */
        }
    }

    /**
     * filter tile creation...
     *
     * more in docs now, meters grid from provided grid,
     *
     * TODO: better solution for projection change
     *
     * @param l	layer as Layer
     * @param longitude_start longitude extent as double
     * @param longitude_end other longitude extent as double
     * @param latitude_start latitude extent as double
     * @param latitude_end other latitude extent as double
     * @param width width resoluion as int
     * @param height height resolution as in
     */
    public void makeScaledShortImageFromGridToMetresGrid(Layer l, double longitude_start, double longitude_end,
            double latitude_start, double latitude_end, int width, int height) {

        /* output data */
        Tile[] data;

        /* load raw */
        if (l.type.equals("environmental")) {
            data = getTileFromGridToMetresGrid(l.name, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        } else {
            data = getTileFromShape(l, longitude_start, longitude_end, latitude_start, latitude_end, width, height);
        }

        /* sort */
        java.util.Arrays.sort(data,
                new Comparator<Tile>() {

                    public int compare(Tile i1, Tile i2) {
                        if (i1.value_ < i2.value_) {
                            return -1;
                        } else if (i1.value_ > i2.value_) {
                            return 1;
                        }
                        return 0;
                    }
                });

        /* index, only used for non-enviornmental layers */
        boolean has_index = true;
        if (l.type.equals("environmental")) {
            has_index = false;
        }
        int[] index = null;
        if (has_index) {
            int i, j;
            int max = (int) data[data.length - 1].value_;

            index = new int[max + 2];
            int last_idx = 1;
            for (i = 1; i < data.length; i++) {
                if (data[i].value_ != data[i - 1].value_) {
                    for (j = last_idx; j <= data[i].value_; j++) {
                        index[j] = i;
                    }
                    last_idx = (int) data[i].value_ + 1;
                }
            }
            index[max + 1] = data.length;
        }

        /* write as object*/
        try {
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path + "SPL_IMG_T_" + l.name + ".dat");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.writeObject(new Boolean(has_index));
            if (has_index) {
                oos.writeObject(index);
            }
            oos.close();

        } catch (Exception e) {
            /* TODO: log error */
        }
    }

    /**
     * gets Tile data from a grid file onto specified extents
     *
     * @param layer_name
     * @param longitude_start
     * @param longitude_end
     * @param latitude_start
     * @param latitude_end
     * @param longitude_steps
     * @param latitude_steps
     * @return Tile[]
     */
    public Tile[] getTileFromGrid(String layer_name,
            double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps) {

        Grid grid = new Grid(
                TabulationSettings.environmental_data_path
                + layer_name);

        /* make points to interrogate */
        double[][] points = new double[longitude_steps * latitude_steps][2];

        for (int j = 0; j < latitude_steps; j++) {
            for (int i = 0; i < longitude_steps; i++) {
                points[j * longitude_steps + i][0] = longitude_start
                        + i / (double) (longitude_steps - 1) * (longitude_end - longitude_start);
                points[j * longitude_steps + i][1] = latitude_end
                        - j / (double) (latitude_steps - 1) * (latitude_end - latitude_start);
            }
        }

        /* get layer data */
        float[] values = grid.getValues2(points);

        if (values != null && values.length > 0) {
            int i;

            /* copy values back to byte data */
            int countvalues = 0;
            for (i = 0; i < values.length; i++) {
                if (!Double.isNaN(values[i])) {
                    countvalues++;
                }
            }

            Tile[] data = new Tile[countvalues];

            int p = 0;
            for (i = 0; i < values.length; i++) {
                if (!Double.isNaN(values[i])) {
                    data[p++] = new Tile((float) values[i], i);
                }
            }

            /* return data */
            return data;
        }

        return null;
    }

    /**
     * gets Tile data from a grid file onto specified extents
     *
     * @param layer_name
     * @param longitude_start
     * @param longitude_end
     * @param latitude_start
     * @param latitude_end
     * @param longitude_steps
     * @param latitude_steps
     * @return Tile[]
     */
    public Tile[] getTileFromGridToMetresGrid(String layer_name,
            double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps) {

        Grid grid = new Grid(
                TabulationSettings.environmental_data_path
                + layer_name);

        /* make points to interrogate */
        double[][] points = new double[longitude_steps * latitude_steps][2];

        //get latproj and longproj between display projection and data projection
        SpatialCluster3 sc = new SpatialCluster3();
        int[] px_boundary = new int[4];
        px_boundary[0] = sc.convertLngToPixel(longitude_start);
        px_boundary[2] = sc.convertLngToPixel(longitude_end);
        px_boundary[1] = sc.convertLatToPixel(latitude_start);
        px_boundary[3] = sc.convertLatToPixel(latitude_end);

        double[] latproj = new double[latitude_steps];
        double[] longproj = new double[longitude_steps];
        for (int i = 0; i < latproj.length; i++) {
            latproj[i] = sc.convertPixelToLat((int) (px_boundary[1] + (px_boundary[3] - px_boundary[1]) * (i / (double) (latproj.length))));
        }
        for (int i = 0; i < longproj.length; i++) {
            longproj[i] = sc.convertPixelToLng((int) (px_boundary[0] + (px_boundary[2] - px_boundary[0]) * (i / (double) (longproj.length))));
        }

        //add half cell for sample center
        double latoffset = (latproj[1] - latproj[0]) / 2.0;
        for (int i = 0; i < latproj.length; i++) {
            latproj[i] += latoffset;
        }
        double longoffset = (longproj[1] - longproj[0]) / 2.0;
        for (int i = 0; i < longproj.length; i++) {
            longproj[i] += longoffset;
        }

        for (int j = 0; j < latitude_steps; j++) {
            for (int i = 0; i < longitude_steps; i++) {
                points[j * longitude_steps + i][0] = longproj[i];
                points[j * longitude_steps + i][1] = latproj[latitude_steps - 1 - j];
            }
        }

        /* get layer data */
        float[] values = grid.getValues2(points);

        if (values != null && values.length > 0) {
            int i;

            /* copy values back to byte data */
            int countvalues = 0;
            for (i = 0; i < values.length; i++) {
                if (!Double.isNaN(values[i])) {
                    countvalues++;
                }
            }

            Tile[] data = new Tile[countvalues];

            int p = 0;
            for (i = 0; i < values.length; i++) {
                if (!Double.isNaN(values[i])) {
                    data[p++] = new Tile((float) values[i], i);
                }
            }

            /* return data */
            return data;
        }

        return null;
    }

    /**
     * get Tiles from shape file layer
     *
     * @param l layer as Layer
     * @param longitude_start
     * @param longitude_end
     * @param latitude_start
     * @param latitude_end
     * @param longitude_steps
     * @param latitude_steps
     * @return Tile []
     */
    public Tile[] getTileFromShape(Layer l,
            double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps) {

        SimpleShapeFile ssf = new SimpleShapeFile(
                TabulationSettings.environmental_data_path
                + l.name);

        int column_idx = ssf.getColumnIdx(l.fields[0].name);

        Tile[] data = ssf.getTileList(column_idx, longitude_start, latitude_start, longitude_end, latitude_end, longitude_steps, latitude_steps);

        return data;
    }

    void occurrencesUpdate(boolean forceUpdate) {
        /* threaded building, needs more ram than one at a time */
        int threadcount = TabulationSettings.analysis_threads;
        ArrayList<String> layers = new ArrayList();
        int i;
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            if (forceUpdate || !isUpToDateCatagorical(TabulationSettings.geo_tables[i].name)) {
            //    layers.add(TabulationSettings.geo_tables[i].name);
            }
        }
        for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
            if (forceUpdate || !isUpToDateContinous(TabulationSettings.environmental_data_files[i].name)) {
                layers.add(TabulationSettings.environmental_data_files[i].name);
            }
        }

        if(layers.size() == 0) {
            return;
        }

        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue(layers);

        FilteringIndexThread[] it = new FilteringIndexThread[threadcount];

        for (i = 0; i < threadcount; i++) {
            it[i] = new FilteringIndexThread(lbq, index_path);
        }

        System.out.println("Start FilteringIndex.build_all (" + threadcount + " threads): " + System.currentTimeMillis());

        //height mapping here while it is running
        startBuildAreaSize();

        //wait until all done
        try {
            boolean alive = true;
            while (alive) {
                Thread.sleep(2000);
                alive = false;
                for (i = 0; i < threadcount; i++) {
                    if (it[i].isAlive()) {
                        alive = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //build vertical area file
    static void buildAreaSize(double longitude_start, double longitude_end,
            double latitude_start, double latitude_end,
            int longitude_steps, int latitude_steps,
            String filename) {

        //get latproj between display projection and data projection
        SpatialCluster3 sc = new SpatialCluster3();
        int[] px_boundary = new int[4];
        px_boundary[0] = sc.convertLngToPixel(longitude_start);
        px_boundary[2] = sc.convertLngToPixel(longitude_end);
        px_boundary[1] = sc.convertLatToPixel(latitude_start);
        px_boundary[3] = sc.convertLatToPixel(latitude_end);

        double[] latproj = new double[latitude_steps + 1];
        double[] longproj = new double[longitude_steps];
        for (int i = 0; i < latproj.length; i++) {
            latproj[i] = sc.convertPixelToLat((int) (px_boundary[1] + (px_boundary[3] - px_boundary[1]) * (i / (double) (latproj.length))));
        }
        for (int i = 0; i < longproj.length; i++) {
            longproj[i] = sc.convertPixelToLng((int) (px_boundary[0] + (px_boundary[2] - px_boundary[0]) * (i / (double) (longproj.length))));
        }

        //get area's
        double[] areaSize = new double[latitude_steps];

        double north, south, east, west;
        east = longproj[longproj.length - 2];
        west = longproj[longproj.length - 1];

        try {
            String wkt4326 = "GEOGCS[" + "\"WGS 84\"," + "  DATUM[" + "    \"WGS_1984\","
                    + "    SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],"
                    + "    TOWGS84[0,0,0,0,0,0,0]," + "    AUTHORITY[\"EPSG\",\"6326\"]],"
                    + "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
                    + "  UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],"
                    + "  AXIS[\"Lat\",NORTH]," + "  AXIS[\"Long\",EAST],"
                    + "  AUTHORITY[\"EPSG\",\"4326\"]]";
            String wkt3577 = "PROJCS[\"GDA94 / Australian Albers\","
                    + "    GEOGCS[\"GDA94\","
                    + "        DATUM[\"Geocentric_Datum_of_Australia_1994\","
                    + "            SPHEROID[\"GRS 1980\",6378137,298.257222101,"
                    + "                AUTHORITY[\"EPSG\",\"7019\"]],"
                    + "            TOWGS84[0,0,0,0,0,0,0],"
                    + "            AUTHORITY[\"EPSG\",\"6283\"]],"
                    + "        PRIMEM[\"Greenwich\",0,"
                    + "            AUTHORITY[\"EPSG\",\"8901\"]],"
                    + "        UNIT[\"degree\",0.01745329251994328,"
                    + "            AUTHORITY[\"EPSG\",\"9122\"]],"
                    + "        AUTHORITY[\"EPSG\",\"4283\"]],"
                    + "    UNIT[\"metre\",1,"
                    + "        AUTHORITY[\"EPSG\",\"9001\"]],"
                    + "    PROJECTION[\"Albers_Conic_Equal_Area\"],"
                    + "    PARAMETER[\"standard_parallel_1\",-18],"
                    + "    PARAMETER[\"standard_parallel_2\",-36],"
                    + "    PARAMETER[\"false_northing\",0],"
                    + "    PARAMETER[\"latitude_of_center\",0],"
                    + "    PARAMETER[\"longitude_of_center\",132],"
                    + "    PARAMETER[\"false_easting\",0],"
                    + "    AUTHORITY[\"EPSG\",\"3577\"],"
                    + "    AXIS[\"Easting\",EAST],"
                    + "    AXIS[\"Northing\",NORTH]]";

            CoordinateReferenceSystem wgsCRS = CRS.parseWKT(wkt4326);
            CoordinateReferenceSystem GDA94CRS = CRS.parseWKT(wkt3577);
            MathTransform transform = CRS.findMathTransform(wgsCRS, GDA94CRS);

            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
            WKTReader reader = new WKTReader(geometryFactory);

            for (int i = 0; i < latitude_steps; i++) {
                south = latproj[i];
                north = latproj[i + 1];     //latproj is len latitude_steps+1

                //backwards otherwise projection fails (?)
                String wkt = "POLYGON((" + south + " " + east
                        + ", " + north + " " + east
                        + ", " + north + " " + west
                        + ", " + south + " " + west
                        + ", " + south + " " + east + "))";

                Geometry geom = reader.read(wkt);
                Geometry geomT = JTS.transform(geom, transform);

                //images are top down, this is south to north.
                areaSize[latitude_steps - 1 - i] = geomT.getArea();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //export
        try {
            String fname = TabulationSettings.index_path + filename;

            FileOutputStream fos = new FileOutputStream(fname);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(areaSize);

            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public double[] getImageLatitudeArea() {
        String filename = TabulationSettings.index_path + "IMAGE_LATITUDE_AREA";

        //TODO: remove this temporary code
        //create it if it does not exist
        File file = new File(filename);
        if (!file.exists()) {
            startBuildAreaSize();
        }

        //import
        double[] areaSize = null;
        try {


            FileInputStream fis = new FileInputStream(filename);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);

            areaSize = (double[]) ois.readObject();

            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return areaSize;
    }

    static void startBuildAreaSize() {
        double longitude_start = 112;
        double longitude_end = 154;
        double latitude_start = -44;//-44;
        double latitude_end = -9;
        int height = 840; //210 42/210
        int width = 1008; //252

        buildAreaSize(longitude_start, longitude_end, latitude_start, latitude_end, width, height, "IMAGE_LATITUDE_AREA");
    }

    static public double[] getCommonGridLatitudeArea() {
        String filename = TabulationSettings.index_path + "IMAGE_LATITUDE_AREA_COMMON_GRID";

        //TODO: remove this temporary code
        //create it if it does not exist
        File file = new File(filename);
        if (!file.exists()) {
            buildAreaSize(TabulationSettings.grd_xmin,
                    TabulationSettings.grd_xmax,
                    TabulationSettings.grd_ymin,
                    TabulationSettings.grd_ymax,
                    TabulationSettings.grd_ncols,
                    TabulationSettings.grd_nrows,
                    "IMAGE_LATITUDE_AREA_COMMON_GRID");
        }

        //import
        double[] areaSize = null;
        try {
            FileInputStream fis = new FileInputStream(filename);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);

            areaSize = (double[]) ois.readObject();

            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return areaSize;
    }

    /**
     * determine if a GRD/GRI layer's filtering index is up to date.
     *
     * index_path + "SAM_D_" + layer.name + ".dat"
     * grid file
     *
     * both dated earlier than
     *
     * index_path + "SPL_V_" + layer.name + ".dat"
     * index_path + "SPL_R_" + layer.name + ".dat"
     *
     * @param fileName
     * @return true if up to date
     */
    private boolean isUpToDateContinous(String fileName) {
        //is it continous (grd/gri)
        File gri = new File(TabulationSettings.environmental_data_path
                + fileName + ".gri");
        File grd = new File(TabulationSettings.environmental_data_path
                + fileName + ".grd");

        //extension case alternative
        if (!gri.exists()) {
            gri = new File(TabulationSettings.environmental_data_path
                    + fileName + ".GRI");
        }
        if (!grd.exists()) {
            grd = new File(TabulationSettings.environmental_data_path
                    + fileName + ".GRD");
        }

        File samD = new File(index_path + "SAM_D_" + fileName + ".dat");

        File splV = new File(index_path + "SPL_V_" + fileName + ".dat");
        File splR = new File(index_path + "SPL_R_" + fileName + ".dat");

        if (gri.exists() && grd.exists() && samD.exists()
                && splV.exists() && splR.exists()) {
            long latestData = Math.max(Math.max(grd.lastModified(), gri.lastModified()), samD.lastModified());

            return splV.lastModified() > latestData
                    && splR.lastModified() > latestData;
        }

        return false;
    }

    /**
     * determine if a SHP/DBF layer's sampling index is up to date.
     *
     * index_path + "SAM_D_" + layer.name + ".dat"
     * index_path + SamplingIndex.CATAGORICAL_PREFIX + layer.name + SamplingIndex.VALUE_POSTFIX
     *
     * both dated earlier than
     *
     * index_path + "SPL_V_" + layer.name + ".dat"
     * index_path + "SPL_R_" + layer.name + ".dat"
     *
     * @param fileName
     * @return true if up to date
     */
    private boolean isUpToDateCatagorical(String fileName) {
        //is it continous (grd/gri)
        File sam = new File(index_path + SamplingIndex.CATAGORICAL_PREFIX + fileName + SamplingIndex.VALUE_POSTFIX);

        File spl0 = new File(index_path
                + "SPL_" + fileName + "_"
                + "0.0" + ".dat");

        if (spl0.exists() && sam.exists()) {
            return spl0.lastModified() > sam.lastModified();
        }

        return false;
    }
};
