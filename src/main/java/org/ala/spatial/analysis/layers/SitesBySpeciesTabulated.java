package org.ala.spatial.analysis.layers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.json.simple.JSONObject;

/**
 *
 * @author Adam
 */
public class SitesBySpeciesTabulated {

    Records records;
    int gridSize;
    double resolution;
    double[] bbox;
    int width, height;

    public SitesBySpeciesTabulated(int gridSize, double resolution, double[] bbox) {
        this.gridSize = gridSize;
        this.resolution = resolution;
        this.bbox = bbox;

        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    void setResolution(double resolution) {
        this.resolution = resolution;
        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    void setBBox(double[] bbox) {
        this.bbox = bbox;
        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    public void write(Records records, String outputDirectory, SimpleRegion region, Grid envelopeGrid, SimpleShapeFile ssf, Grid grid, String [] gridColumns, boolean decade) throws IOException {

        String[] columns = null;
        int [] gridIntersections = null;
        int numberOfBioregions = 0;
        if (ssf != null) {
            columns = ssf.getColumnLookup();            
        } else if (grid != null) {
            columns = gridColumns;
            gridIntersections = new int[records.getRecordsSize()];
            double [][] points = new double[records.getRecordsSize()][2];
            for(int i=0;i<records.getRecordsSize();i++) {
                points[i][0] = records.getLongitude(i);
                points[i][1] = records.getLatitude(i);
            }
            float [] f = grid.getValues(points);
            for(int i=0;i<f.length;i++) {
                gridIntersections[i] = (int) f[i];
                if (gridIntersections[i] < 0 || gridIntersections[i] >= gridColumns.length + 1) {
                    gridIntersections[i] = -1;
                }
            }
            f = null;
            points = null;
        }
        if (columns != null) {
            numberOfBioregions = columns.length + 1;
        }

        int uniqueSpeciesCount = records.getSpeciesSize();

        short[] decadeIdx = getDecadeIdx(records);
        int numberOfDecades = decadeIdx[decadeIdx.length - 1] + 1;

        HashMap<Integer, Integer>[] bioMap = new HashMap[numberOfBioregions];
        HashMap<Integer, Integer>[] decMap = new HashMap[numberOfDecades];
        for (int i = 0; i < bioMap.length; i++) {
            bioMap[i] = new HashMap<Integer, Integer>();
        }
        for (int i = 0; i < decMap.length; i++) {
            decMap[i] = new HashMap<Integer, Integer>();
        }

        records.sortedStarts(bbox[1], bbox[0], resolution);

        BitSet[] bsDecades = new BitSet[numberOfDecades];
        BitSet[] bsBioregions = new BitSet[numberOfBioregions];
        for (int j = 0; j < numberOfBioregions; j++) {
            bsBioregions[j] = new BitSet(uniqueSpeciesCount);
        }
        for (int j = 0; j < numberOfDecades; j++) {
            bsDecades[j] = new BitSet(uniqueSpeciesCount);
        }
        System.out.println("finished setup of bsBioregions and bsDecades");
        for (int pos = 0; pos < records.getRecordsSize(); ) {
            //find end pos
            int x = (int) (records.getLongitude(pos) - bbox[0] / resolution);
            int y = (int) (records.getLatitude(pos) - bbox[1] / resolution);
            int endPos = pos + 1;
            while(endPos < records.getRecordsSize()
                    && x == (int) (records.getLongitude(endPos) - bbox[0] / resolution)
                    && y == (int) (records.getLatitude(pos) - bbox[1] / resolution)) {
                endPos++;
            }

            double longitude = (x + 0.5) * resolution;
            double latitude = (y + 0.5) * resolution;
            if ((region == null || region.isWithin_EPSG900913(longitude, latitude))
                    && (envelopeGrid == null || envelopeGrid.getValues2(new double[][]{{longitude, latitude}})[0] > 0)) {
                //process this cell
                getNextIntArrayRow(records, pos, endPos, bsBioregions, bsDecades, ssf, gridIntersections, decadeIdx);

                for (int j = 0; j < numberOfBioregions; j++) {
                    int group = bsBioregions[j].cardinality();
                    if (group > 0) {
                        Integer count = bioMap[j].get(group);
                        bioMap[j].put(group, count == null ? 1 : count + 1);
                    }
                }
                for (int j = 0; j < numberOfDecades; j++) {
                    int group = bsDecades[j].cardinality();
                    if (group > 0) {
                        Integer count = decMap[j].get(group);
                        decMap[j].put(group, count == null ? 1 : count + 1);
                    }
                }
            }

            pos = endPos;
        }

        writeBioregions(outputDirectory, columns, bioMap);
        writeDecades(outputDirectory, decadeIdx, decMap);
    }

    void getNextIntArrayRow(Records records, int start, int end, BitSet[] bsBioregion, BitSet[] bsDecade, SimpleShapeFile ssf, int [] gridIntersections, short[] decadeIdx) {
        //translate into bitset for each grid cell
        if (bsBioregion != null) {
            for (int j = 0; j < bsBioregion.length; j++) {
                bsBioregion[j].clear();
            }
        }
        if (bsDecade != null) {
            for (int j = 0; j < bsDecade.length; j++) {
                bsDecade[j].clear();
            }
        }

        for (int i = start; i < end; i++) {
            //bioregion
            if (bsBioregion != null) {
                if (ssf != null) {
                    int bioregion = ssf.intersectInt(records.getLongitude(i), records.getLatitude(i));
                    bsBioregion[bioregion + 1].set(records.getSpeciesNumber(i));
                } else if (gridIntersections != null) {
                    bsBioregion[gridIntersections[i] + 1].set(records.getSpeciesNumber(i));
                }
            }
            //decades
            if (bsDecade != null) {
                bsDecade[decadeIdx[records.getYear(i)]].set(records.getSpeciesNumber(i));
            }
        }
    }

    private short[] getDecadeIdx(Records records) {
        short min = (short) (Calendar.getInstance().get(Calendar.YEAR));
        short max = 0;
        for (int i = 0; i < records.getRecordsSize(); i++) {
            min = (short) Math.min(min, records.getYear(i));
            max = (short) Math.max(max, records.getYear(i));
        }
        max = (short) Math.min(max, (Calendar.getInstance().get(Calendar.YEAR)));

        System.out.println("min year: " + min + ", max year: " + max);

        short[] idx = new short[max + 1];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = (short) Math.ceil(i / 10.0);
        }

        return idx;
    }

    private Map writeDecades(String outputDirectory, short[] decadeIdx, HashMap<Integer, Integer>[] decMap) {
        Map map = new HashMap();
        ArrayList array = new ArrayList();

        try {
            FileWriter fw = new FileWriter(outputDirectory + File.separator + "decades.csv");

            //identify column numbers
            TreeMap<Integer, Integer> tm = new TreeMap();
            for (int i = 0; i < decMap.length; i++) {
                tm.putAll(decMap[i]);
            }
            Integer[] cols = new Integer[tm.size()];
            tm.keySet().toArray(cols);

            for (int j = 0; j < cols.length; j++) {
                fw.write(",\"" + cols[j] + "\"");
            }

            //bioregion rows
            for (int i = 0; i < decMap.length; i++) {
                if (decMap[i].size() > 0) {
                    ArrayList array2 = new ArrayList();
                    int pos = java.util.Arrays.binarySearch(decadeIdx, (short) i);
                    //seek to first
                    while (pos > 0 && decadeIdx[pos - 1] == i) {
                        pos--;
                    }
                    String rowname = "not in bioregion";
                    if (i > 0) {
                        rowname = pos + " to " + (pos + 9);
                    }
                    fw.write("\n\"" + rowname + "\"");
                    //count columns
                    for (int j = 0; j < cols.length; j++) {
                        Integer v = decMap[i].get(cols[j]);
                        fw.write(",");
                        if (v != null) {
                            fw.write(v.toString());
                            Map m2 = new HashMap();
                            m2.put(cols[j] + " species", v);
                            array2.add(m2);
                        }
                    }
                    Map m3 = new HashMap();
                    m3.put("name", rowname);
                    m3.put("site_counts", array2);
                    array.add(m3);
                }
            }

            map.put("decades", array);

            fw.close();

            fw = new FileWriter(outputDirectory + File.separator + "decades.json");
            JSONObject.writeJSONString(map, fw);
            fw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    private Map writeBioregions(String outputDirectory, String[] columns, HashMap<Integer, Integer>[] bioMap) {
        Map map = new HashMap();
        ArrayList array = new ArrayList();
        try {
            FileWriter fw = new FileWriter(outputDirectory + File.separator + "bioregions.csv");

            //identify column numbers
            TreeMap<Integer, Integer> tm = new TreeMap();
            for (int i = 0; i < columns.length; i++) {
                tm.putAll(bioMap[i]);
            }
            Integer[] cols = new Integer[tm.size()];
            tm.keySet().toArray(cols);

            for (int j = 0; j < cols.length; j++) {
                fw.write(",\"" + cols[j] + "\"");
            }

            //bioregion rows
            for (int i = 0; i < columns.length + 1; i++) {
                if (bioMap[i].size() > 0) {
                    ArrayList array2 = new ArrayList();
                    String rowname = "Undefined";
                    if (i > 0) {
                        rowname = columns[i - 1];
                    }
                    fw.write("\n\"" + rowname + "\"");
                    //count columns
                    for (int j = 0; j < cols.length; j++) {
                        Integer v = bioMap[i].get(cols[j]);
                        fw.write(",");
                        if (v != null) {
                            fw.write(v.toString());
                            Map m2 = new HashMap();
                            m2.put(cols[j] + " species", v);
                            array2.add(m2);
                        }
                    }
                    Map m3 = new HashMap();
                    m3.put("name", rowname);
                    m3.put("site_counts", array2);
                    array.add(m3);
                }
            }

            map.put("bioregions", array);

            fw.close();

            fw = new FileWriter(outputDirectory + File.separator + "bioregions.json");
            JSONObject.writeJSONString(map, fw);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}