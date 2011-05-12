/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import org.ala.spatial.analysis.legend.Legend;
import org.ala.spatial.analysis.service.LayerImgService;
import org.ala.spatial.analysis.service.LoadedPointsService;
import org.ala.spatial.analysis.service.SamplingLoadedPointsService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.util.TabulationSettings;

/**
 * Retrieve attribute column records of varying types.
 * 
 * Can transform attribute values to an int ARGB colour (A = 0xFF).
 * 
 * Can generate legends for colours exported.
 *
 * @author Adam
 */
public class SpeciesColourOption {

    static final int startColour = 0xFFFFFF00;
    static final int endColour = 0xFFFF0000;
    int[] iArray;
    double[] dArray;
    boolean[] bArray;
    String[] sArray;
    int[] sArrayHash;
    String name;
    String displayName;
    int type;
    String key;
    boolean highlight;
    int taxon;
    boolean colourMode;
    double dMin, dMax;
    int iMin, iMax;
    Legend legend;
    int pos; //column in OCC_SORTED

    void makeSArrayHash() {
        sArrayHash = new int[sArray.length];
        for (int i = 0; i < sArray.length; i++) {
            sArrayHash[i] = 0xFF000000 | sArray[i].hashCode();
        }
    }

    SpeciesColourOption(String name, String displayName, int type, String key, boolean highlight, int taxon, boolean colourMode, int pos) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.key = key;
        this.highlight = highlight;
        this.taxon = taxon;
        this.colourMode = colourMode;
        this.pos = pos;
    }

    SpeciesColourOption(int idx, boolean colourMode) {
        this.name = TabulationSettings.geojson_property_names[idx];
        this.displayName = TabulationSettings.geojson_property_display_names[idx];
        this.type = TabulationSettings.geojson_property_types[idx];
        this.key = null;
        this.highlight = false;
        this.taxon = -1;
        this.colourMode = colourMode;
        this.pos = TabulationSettings.geojson_property_fields[idx];
    }

    public static String getColourLegend(String lsid, String colourMode) {
        return getColourLegend(lsid, colourMode, true);
    }

    public static String getColourLegend(String lsid, String colourMode, boolean writeToFile) {
        String legend = null;

        //legend for 'grid' colourMode
        if (colourMode.equalsIgnoreCase("grid")) {
            StringBuilder legendString = new StringBuilder();
            legendString.append(0).append(",").append(RGBcsv(Legend.getLinearColour(0, 0, 500, startColour, endColour))).append("\n");
            legendString.append("500+").append(",").append(RGBcsv(Legend.getLinearColour(500, 0, 500, startColour, endColour))).append("\n");
            legend = legendString.toString();
        } else {
            Object[] o = LayerVariableDistribution.getLsidDistribution(lsid, colourMode);
            if (o.length == 1) {
                //continous value legends
                Legend l = (Legend) o[0];
                StringBuilder legendString = new StringBuilder();
                if (l == null) {
                    //handle absence of a legend, e.g. all NaN values
                    legendString.append("unknown").append(",").append(RGBcsv(0xFFFFFFFF)).append("\n");
                } else {
                    float[] cutoffs = l.getCutoffFloats();
                    float[] minmax = l.getMinMax();
                    float min = minmax[0];
                    float max = minmax[1];
                    switch (SpeciesColourOption.fromName(colourMode, true).type) {
                        case 0: //double
                            legendString.append(min).append(",").append(RGBcsv(Legend.getLinearColour(min, min, max, startColour, endColour))).append("\n");
                            legendString.append(max).append(",").append(RGBcsv(Legend.getLinearColour(max, min, max, startColour, endColour))).append("\n");
                            legendString.append("unknown").append(",").append(RGBcsv(0xFFFFFFFF)).append("\n");
                            break;
                        case 1: //int
                            legendString.append(String.format("%.0f", min)).append(",").append(RGBcsv(Legend.getLinearColour(min, min, max, startColour, endColour))).append("\n");
                            legendString.append(String.format("%.0f", max)).append(",").append(RGBcsv(Legend.getLinearColour(max, min, max, startColour, endColour))).append("\n");
                            legendString.append("unknown").append(",").append(RGBcsv(0xFFFFFFFF)).append("\n");
                            break;

                    }
                }
                legend = legendString.toString();
            } else {
                //categorical value legends
                ArrayList<SpeciesColourOption> other = new ArrayList<SpeciesColourOption>();
                other.add(SpeciesColourOption.fromName(colourMode, true));
                SamplingService ss = SamplingService.newForLSID(lsid);
                double[] points = ss.sampleSpeciesPoints(lsid, null, null, other);

                if (points != null && points.length > 0) {
                    for (int j = 0; j < other.size(); j++) {
                        //colour mode!
                        legend = other.get(0).getLegendString();
                    }
                }
            }
        }
        if (legend != null) {
            try {
                String pid = String.valueOf(System.currentTimeMillis());

                StringBuilder str = new StringBuilder();
                str.append("name, red, green, blue").append("\n");
                str.append(legend);

                if (writeToFile) {
                    //register
                    LayerImgService.registerLayerLegend(TabulationSettings.base_output_dir, pid, str.toString());

                    return pid;
                } else {
                    return str.toString(); 
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static Object loadData(String index_path, int idx) {
        System.out.println("loading: idx=" + idx + ", name=" + TabulationSettings.geojson_property_names[idx]);
        Object data = null;
        try {
            FileInputStream fis = new FileInputStream(index_path
                    + TabulationSettings.geojson_property_names[idx]
                    + ".dat");
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);

            data = ois.readObject();

            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    static void saveData(String index_path, int idx, Object array) {
        System.out.println("saving: idx=" + idx + ", name=" + TabulationSettings.geojson_property_names[idx]);
        try {
            FileOutputStream fos = new FileOutputStream(index_path
                    + TabulationSettings.geojson_property_names[idx]
                    + ".dat");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            Object[] output;

            switch (TabulationSettings.geojson_property_types[idx]) {
                case 0: //double
                    //get global min/max
                    output = new Object[2];
                    output[0] = array;
                    output[1] = SpeciesColourOption.getMinMax((double[]) array);
                    oos.writeObject(output);
                    break;
                case 1: //int
                    //get global min/max
                    output = new Object[2];
                    output[0] = array;
                    output[1] = SpeciesColourOption.getMinMax((int[]) array);
                    oos.writeObject(output);
                    break;
                case 2: //boolean
                    output = new Object[1];
                    output[0] = array;
                    oos.writeObject(output);
                    break;
                case 3: //String
                    //convert to String [] for unique Strings and int [] for ref
                    output = SpeciesColourOption.toStringLookup((String[]) array);
                    oos.writeObject(output);
                    break;
            }

            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double[] getMinMax(double[] d) {
        double min = d[0];
        double max = d[0];
        for (int i = 1; i < d.length; i++) {
            if (!Double.isNaN(d[i])) {
                if (Double.isNaN(min) || min > d[i]) {
                    min = d[i];
                }
                if (Double.isNaN(max) || max < d[i]) {
                    max = d[i];
                }
            }
        }
        double[] output = new double[2];
        output[0] = min;
        output[1] = max;
        System.out.println(min + " " + max);

        return output;
    }

    private static int[] getMinMax(int[] d) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 1; i < d.length; i++) {
            if (d[i] != Integer.MIN_VALUE && min > d[i]) {
                min = d[i];
            }
            if (max < d[i]) {
                max = d[i];
            }
        }
        System.out.println(min + " " + max);
        int[] output = new int[2];
        output[0] = min;
        output[1] = max;

        return output;
    }

    private static Object[] toStringLookup(String[] strings) {
        //make unique list
        TreeSet<String> ts = new TreeSet<String>();
        for (String s : strings) {
            ts.add(s);
        }

        String[] sOutput = new String[ts.size()];
        ts.toArray(sOutput);
        java.util.Arrays.sort(sOutput);

        //make record to unique list lookup
        int[] iOutput = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            iOutput[i] = java.util.Arrays.binarySearch(sOutput, strings[i]);
        }

        Object[] output = new Object[2];
        output[0] = sOutput;
        output[1] = iOutput;

        return output;
    }

    static SpeciesColourOption fromSpeciesColourOption(SpeciesColourOption sco) {
        return new SpeciesColourOption(sco.name, sco.displayName, sco.type, sco.key, sco.highlight, sco.taxon, sco.colourMode, sco.pos);
    }

    public static SpeciesColourOption fromMode(String mode, boolean colourMode) {
        for (int i = 0; i < TabulationSettings.geojson_property_display_names.length; i++) {
            if (mode.equalsIgnoreCase(TabulationSettings.geojson_property_display_names[i])) {
                return new SpeciesColourOption(i, colourMode);
            }
        }

        for (int i = 1; i < TabulationSettings.occurrences_csv_field_pairs.length; i += 2) {
            if (mode.equalsIgnoreCase(TabulationSettings.occurrences_csv_field_pairs[i])) {
                return new SpeciesColourOption(mode, mode, 3, null, false, (i - 1) / 2, colourMode, 9 + (i - 1) / 2);
            }
        }

        if (mode.equalsIgnoreCase("10")) {   //general case for level 10
            return new SpeciesColourOption(mode, "General categories", 3, null, false, 10, colourMode, -1);
        }

        return null;
    }

    public static SpeciesColourOption fromName(String name, boolean colourMode) {
        for (int i = 0; i < TabulationSettings.geojson_property_display_names.length; i++) {
            if (name.equalsIgnoreCase(TabulationSettings.geojson_property_names[i])) {
                return new SpeciesColourOption(i, colourMode);
            }
        }

        for (int i = 1; i < TabulationSettings.occurrences_csv_field_pairs.length; i += 2) {
            if (name.equalsIgnoreCase(TabulationSettings.occurrences_csv_field_pairs[i])) {
                String mode = TabulationSettings.occurrences_csv_field_pairs[i];
                return new SpeciesColourOption(mode, mode, 3, null, false, (i - 1) / 2, colourMode, 9 + (i - 1) / 2);
            }
        }

        if (name.equalsIgnoreCase("10")) {   //general case for level 10
            return new SpeciesColourOption(name, "General categories", 3, null, false, 10, colourMode, -1);
        }

        return null;
    }

    public static String getColourOptions(String lsid) {
        //is is user loaded points?
        if (SamplingLoadedPointsService.isLoadedPointsLSID(lsid)) {
            return "";
        }

        //otherwise
        StringBuilder sb = new StringBuilder();

        sb.append("General Categories,10");

        for (int i = 1; i < TabulationSettings.occurrences_csv_field_pairs.length; i += 2) {
            sb.append(TabulationSettings.occurrences_csv_field_pairs[i]).append(",").append(TabulationSettings.occurrences_csv_field_pairs[i - 1]).append("\n");
        }

        for (int i = 0; i < TabulationSettings.geojson_property_display_names.length; i++) {
            sb.append(TabulationSettings.geojson_property_display_names[i]).append(",").append(TabulationSettings.geojson_property_names[i]).append("\n");
        }

        return sb.toString();
    }

    public void assignData(int[] r, Object list) {
        Object[] data = (Object[]) list;
        switch (type) {
            case 0: //double
                double[] input0 = (double[]) data[0];
                dMin = ((double[]) data[1])[0];
                dMax = ((double[]) data[1])[1];
                dArray = new double[r.length];
                for (int i = 0; i < r.length; i++) {
                    dArray[i] = input0[r[i]];
                }
                break;
            case 1: //int
                int[] input1 = (int[]) data[0];
                iMin = ((int[]) data[1])[0];
                iMax = ((int[]) data[1])[1];
                iArray = new int[r.length];
                for (int i = 0; i < r.length; i++) {
                    iArray[i] = input1[r[i]];
                }
                break;
            case 2: //boolean
                boolean[] input2 = (boolean[]) data[0];
                bArray = new boolean[r.length];
                for (int i = 0; i < r.length; i++) {
                    bArray[i] = input2[r[i]];
                }
                break;
            case 3: //string
                sArray = (String[]) data[0];
                int[] input3 = (int[]) data[1];
                iArray = new int[r.length];
                for (int i = 0; i < r.length; i++) {
                    iArray[i] = input3[r[i]];
                }
                if (isColourMode()) {
                    makeSArrayHash();
                }
                break;
        }
    }

    public void append(SpeciesColourOption sco) {
        if (sco.type != type) {
            //error
        }
        switch (type) {
            case 0: //double
                dMin = sco.getDMin();
                dMax = sco.getDMax();
                if (dArray == null) {
                    dArray = sco.getDblArray();
                } else {
                    double[] dAdd = sco.getDblArray();
                    double[] dNew = new double[dArray.length + dAdd.length];
                    System.arraycopy(dArray, 0, dNew, 0, dArray.length);
                    System.arraycopy(dAdd, 0, dNew, dArray.length, dAdd.length);
                    dArray = dNew;
                }
                break;
            case 1: //int
            case 3: //string, appending lookup values
                iMin = sco.getIMin();
                iMax = sco.getIMax();
                if (iArray == null) {
                    iArray = sco.getIntArray();
                    sArray = sco.getSArray();
                    sArrayHash = sco.getSArrayHash();
                } else {
                    int[] iAdd = sco.getIntArray();
                    int[] iNew = new int[iArray.length + iAdd.length];
                    System.arraycopy(iArray, 0, iNew, 0, iArray.length);
                    System.arraycopy(iAdd, 0, iNew, iArray.length, iAdd.length);
                    iArray = iNew;
                }
                break;
            case 2: //boolean
                if (bArray == null) {
                    bArray = sco.getBoolArray();
                } else {
                    boolean[] bAdd = sco.getBoolArray();
                    boolean[] bNew = new boolean[bArray.length + bAdd.length];
                    System.arraycopy(bArray, 0, bNew, 0, bArray.length);
                    System.arraycopy(bAdd, 0, bNew, bArray.length, bAdd.length);
                    bArray = bNew;
                }
                break;
        }
    }

    public String[] getStrArray() {
        String[] s = new String[iArray.length];
        for (int i = 0; i < iArray.length; i++) {
            s[i] = sArray[iArray[i]];
        }
        return s;
    }

    public boolean[] getBoolArray() {
        return bArray;
    }

    public int[] getIntArray() {
        return iArray;
    }

    public double[] getDblArray() {
        return dArray;
    }

    public String getName() {
        return name;
    }

    public boolean isHighlight() {
        return highlight;
    }

    void assignHighlightData(int[] records, int record_start, String hash) {
        String k = hash + key;
        boolean[] highlight = RecordSelectionLookup.getSelection(k);
        int len = records.length;
        bArray = new boolean[len];
        for (int i = 0; i < len; i++) {
            bArray[i] = highlight[records[i] - record_start];
        }
    }

    static public SpeciesColourOption fromHighlight(String key, boolean colourMode) {
        return new SpeciesColourOption("h", "Selection", 2, key, true, -1, colourMode, -1);
    }

    public boolean isTaxon() {
        return (taxon >= 0);
    }

    void assignTaxon(int[] r, int[] speciesNumberInRecordsOrder, int[] speciesIndexLookup) {
        iArray = new int[r.length];

        if (colourMode) {
            for (int i = 0; i < r.length; i++) {
                if (speciesNumberInRecordsOrder[r[i]] >= 0) {
                    //set alpha
                    //iArray[i] = 0xFFFFFFFF | SpeciesIndex.getHash(taxon, speciesIndexLookup[speciesNumberInRecordsOrder[r[i]]]);
                    iArray[i] = speciesIndexLookup[speciesNumberInRecordsOrder[r[i]]];
                } else {
                    //white
                    iArray[i] = -1;
                }
            }
        } else {
            for (int i = 0; i < r.length; i++) {
                iArray[i] = speciesIndexLookup[speciesNumberInRecordsOrder[r[i]]];
            }
        }
    }

    public int[] getColours(String lsid) {
        if (legend == null) {
            Object[] o = LayerVariableDistribution.getLsidDistribution(lsid, name);
            if (o.length == 1) {
                legend = (Legend) o[0];
            }
        }
        int[] colours = null;
        switch (type) {
            case 0: //double
                colours = dblColours();
                break;
            case 1: //int
                colours = intColours();
                break;
            case 2: //boolean
                colours = boolColours();
                break;
            case 3: //string
                if (isTaxon()) {
                    colours = taxonColours();
                } else {
                    colours = strColours();
                }
                break;
        }
        return colours;
    }

    int[] dblColours() {
        int[] c = new int[dArray.length];
        if (legend == null) {
            //double range = dMax - dMin;
            for (int i = 0; i < dArray.length; i++) {
                c[i] = Legend.getColour(dArray[i], dMin, dMax);
                //c[i] = 0xFFFFFFFF | (int) (((dArray[i] - dMin) / range) * 0x00FFFFFF);
            }
        } else {
            for (int i = 0; i < dArray.length; i++) {
                c[i] = legend.getColour((float) dArray[i]);
            }
        }
        return c;
    }

    int[] intColours() {
        int[] c = new int[iArray.length];
        if (legend == null) {
            double offset = 0;
            if (iMin < 1) {
                offset = 1 - iMin;
            }
            double logIMin = Math.log10(iMin + offset);
            if (logIMin < 0) {
                logIMin = 0;
            }
            double logIMax = Math.log10(iMax + offset);
            for (int i = 0; i < iArray.length; i++) {
                if (iArray[i] != Integer.MIN_VALUE) {
                    c[i] = Legend.getLinearColour(Math.log10(iArray[i] + offset), logIMin, logIMax, startColour, endColour);
                } else {
                    c[i] = 0xFFFFFFFF;
                }
                //c[i] = 0xFFFFFFFF | (int) (((iArray[i] - iMin) / range) * 0x00FFFFFF);
            }
        } else {
            double offset = 0;
            if (legend.getMinMax()[0] < 1) {
                offset = 1 - legend.getMinMax()[0];
            }
            double min = Math.log10(legend.getMinMax()[0] + offset + 1);
            double max = Math.log10(legend.getMinMax()[1] + offset);
            for (int i = 0; i < iArray.length; i++) {
                if (iArray[i] != Integer.MIN_VALUE) {
                    //c[i] = legend.getColour((float) iArray[i]);
                    c[i] = Legend.getLinearColour(Math.log10(iArray[i]) + offset, min, max, startColour, endColour);
                } else {
                    c[i] = 0xFFFFFFFF;
                }
            }
        }
        return c;
    }

    int[] boolColours() {
        int[] c = new int[bArray.length];
        for (int i = 0; i < bArray.length; i++) {
            if (bArray[i]) {
                c[i] = 0xFF000000;
            } else {
                c[i] = 0xFFFFFFFF;
            }
        }
        return c;
    }

    int[] strColours() {
        int[] c = new int[iArray.length];
        for (int i = 0; i < iArray.length; i++) {
            c[i] = sArrayHash[iArray[i]];
        }
        return c;
    }

    public void assignMissingData(int size) {
        switch (type) {
            case 0: //double
                dArray = new double[size];
                break;
            case 1: //int
                iArray = new int[size];
                break;
            case 2: //boolean
                bArray = new boolean[size];
                break;
            case 3: //string
                sArray = new String[1];
                iArray = new int[size];
                break;
        }
    }

    public boolean isColourMode() {
        return colourMode;
    }

    private String[] getSArray() {
        return sArray;
    }

    private int[] getSArrayHash() {
        return sArrayHash;
    }

    private int getIMin() {
        return iMin;
    }

    private int getIMax() {
        return iMax;
    }

    private double getDMin() {
        return dMin;
    }

    private double getDMax() {
        return dMax;
    }

    private String getLegendString() {
        StringBuilder legend = new StringBuilder();
        switch (type) {
            case 0: //double
                legend.append(dMin).append(",").append(RGBcsv(Legend.getLinearColour(dMin, dMin, dMax, startColour, endColour))).append("\n");
                legend.append(dMax).append(",").append(RGBcsv(Legend.getLinearColour(dMax, dMin, dMax, startColour, endColour))).append("\n");
                legend.append("unknown").append(",").append(RGBcsv(0xFFFFFFFF)).append("\n");
                break;
            case 1: //int
                legend.append(iMin).append(",").append(RGBcsv(Legend.getLinearColour(Math.log10(iMin), Math.log10(iMin), Math.log10(iMax), startColour, endColour))).append("\n");
                legend.append(iMax).append(",").append(RGBcsv(Legend.getLinearColour(Math.log10(iMax), Math.log10(iMin), Math.log10(iMax), startColour, endColour))).append("\n");
                legend.append("unknown").append(",").append(RGBcsv(0xFFFFFFFF)).append("\n");
                break;
            case 2: //boolean
                legend.append("true").append(",").append(RGBcsv(0xFFFFFFFF)).append("\n");
                legend.append("false").append(",").append(RGBcsv(0xFFFFFFFF)).append("\n");
                break;
            case 3: //string
                if (isTaxon()) {
                    legend.append(taxonLegend());
                } else {
                    legend.append(strLegend());
                }
                break;
        }

        return legend.toString();
    }

    static String RGBcsv(int colour) {
        int red = (colour >> 16) & 0x000000FF;
        int green = (colour >> 8) & 0x000000FF;
        int blue = (colour) & 0x000000FF;

        return String.format("%s,%s,%s", red, green, blue);
    }

    String strLegend() {
        StringBuilder legend = new StringBuilder();

        //flag presence
        boolean[] flag = new boolean[sArray.length];
        for (int i = 0; i < iArray.length; i++) {
            flag[iArray[i]] = true;
        }

        //calculate counts
        Object[] o = getCategoryBreakdown();
        String[] labels = (String[]) o[0];
        int[] counts = (int[]) o[1];

        //iterate and write
        for (int i = 0; i < flag.length; i++) {
            if (flag[i]) {
                //get count value
                int count = 0;
                for (int j = 0; j < labels.length; j++) {
                    if (labels[j].equalsIgnoreCase(sArray[i])) {
                        count = counts[j];
                        break;
                    }
                }

                legend.append(sArray[i]).append(",").append(RGBcsv(sArrayHash[i])).append(",").append(count).append("\n");
            }
        }

        return legend.toString();
    }

    String taxonLegend() {
        TreeMap<String, String> legendItems = new TreeMap<String, String>();
        StringBuilder legend = new StringBuilder();

        //copy & sort
        int[] iList = new int[iArray.length];
        for (int i = 0; i < iArray.length; i++) {
            iList[i] = SpeciesIndex.getParentPos(taxon, iArray[i]);
        }
        java.util.Arrays.sort(iList);

        //calculate counts
        Object[] o = getCategoryBreakdown();
        String[] labels = (String[]) o[0];
        int[] counts = (int[]) o[1];

        for (int i = 0; i < iList.length; i++) {
            if (i == iList.length - 1 || iList[i + 1] != iList[i]) {
                String label;
                int hash;
                hash = SpeciesIndex.getHash(taxon, iList[i]);
                if (taxon < 10) {
                    if (iList[i] >= 0) {
                        label = SpeciesIndex.getScientificName(iList[i]);
                    } else {
                        label = "unknown";
                    }
                } else {
                    if (iList[i] >= 0) {
                        label = SpeciesIndex.getScientificNameLevel10(iList[i]);
                    } else {
                        label = "unknown";
                    }
                }

                //get count value
                int count = 0;
                for (int j = 0; j < labels.length; j++) {
                    if (labels[j].equalsIgnoreCase(label)) {
                        count = counts[j];
                        break;
                    }
                }

                String value = legendItems.get(label);
                if (value != null) {
                    count += Integer.parseInt(value.split(",")[3].trim());
                }
                legendItems.put(label, (new StringBuilder()).append(RGBcsv(hash)).append(",").append(count).append("\n").toString());
            }
        }

        //merge to string
        for (Entry<String, String> e : legendItems.entrySet()) {
            if (taxon >= 10) {
                //remove sorting prefix (2 characters)
                legend.append(e.getKey().substring(2) + "," + e.getValue());
            } else {
                legend.append(e.getKey() + "," + e.getValue());
            }
        }

        return legend.toString();
    }

    private int[] taxonColours() {
        int[] colours = new int[iArray.length];
        for (int i = 0; i < iArray.length; i++) {
            if (iArray[i] >= 0) {
                //set alpha
                colours[i] = 0xFF000000 | SpeciesIndex.getHash(taxon, iArray[i]);
            } else {
                //white
                colours[i] = 0xFFFFFFFF;
            }
        }
        return colours;
    }

    boolean[] getFiltered(Object[] object) {
        boolean[] highlight = null;
        String[] sa;
        int[] ia;
        int imin, imax;
        double dmin, dmax;
        boolean b;
        int i, j;

        switch (type) {
            case 0: //double
                if (dArray != null) {
                    highlight = new boolean[dArray.length];
                    dmin = (Double) object[0];
                    dmax = (Double) object[1];
                    if (Double.isNaN(dmax) || Double.isNaN(dmin)) {
                        for (i = 0; i < dArray.length; i++) {
                            highlight[i] = Double.isNaN(dArray[i]);
                        }
                    } else {
                        for (i = 0; i < dArray.length; i++) {
                            highlight[i] = (dArray[i] <= dmax && dArray[i] >= dmin);
                            //|| (Double.isNaN(dArray[i]) && dmax >= 0 && dmin <= 0);
                        }
                    }
                }
                break;
            case 1: //int
                if (iArray != null) {
                    highlight = new boolean[iArray.length];
                    imin = (Integer) object[0];
                    imax = (Integer) object[1];
                    if (Integer.MIN_VALUE == imin) {
                        for (i = 0; i < dArray.length; i++) {
                            highlight[i] = Double.isNaN(dArray[i]);
                        }
                    } else {
                        for (i = 0; i < iArray.length; i++) {
                            highlight[i] = (iArray[i] <= imax && iArray[i] >= imin);
                            // || (iArray[i] == Integer.MIN_VALUE && imax >= 0 && imin <= 0);
                        }
                    }
                }
                break;
            case 3: //string, appending lookup values
                if (iArray != null) {
                    highlight = new boolean[iArray.length];
                    //strings to ints
                    sa = (String[]) object[0];
                    ia = new int[sa.length];
                    for (i = 0; i < sa.length; i++) {
                        ia[i] = java.util.Arrays.binarySearch(sArray, sa[i]);
                    }
                    for (i = 0; i < iArray.length; i++) {
                        for (j = 0; j < ia.length; j++) {
                            if (iArray[i] == ia[j]) {
                                highlight[i] = true;
                            }
                        }
                    }
                }
                break;
            case 2: //boolean
                if (bArray != null) {
                    b = (Boolean) object[0];
                    highlight = new boolean[iArray.length];
                    for (i = 0; i < iArray.length; i++) {
                        highlight[i] = bArray[i] == b;
                    }
                }
                break;
        }

        return highlight;
    }

    public Object parseFilter(String filter) {
        try {
            String[] parts = filter.split("\t");

            switch (type) {
                case 0: //double
                    Double[] o1 = {new Double(Double.parseDouble(parts[0])), new Double(Double.parseDouble(parts[1]))};
                    return o1;
                case 1: //int
                    Integer[] o2 = {new Integer((int) Double.parseDouble(parts[0])), new Integer((int) Double.parseDouble(parts[1]))};
                    return o2;
                case 3: //string, appending lookup values
                    Object[] o3 = {parts};
                    return o3;
                case 2: //boolean
                    Boolean[] o4 = {new Boolean(Boolean.parseBoolean(parts[0]))};
                    return o4;
            }
        } catch (Exception e) {
        }
        return null;
    }

    boolean isContinous() {
        return dArray != null || (iArray != null && sArray == null && !isTaxon());
    }

    boolean isDbl() {
        return dArray != null;
    }

    boolean isInt() {
        return iArray != null && sArray == null && !isTaxon();
    }

    boolean isStr() {
        return sArray != null || isTaxon();
    }

    boolean isBool() {
        return bArray != null;
    }

    Object[] getCategoryBreakdown() {
        int[] counts;
        String[] labels;
        int pos = 0;
        if (isTaxon()) {
            //copy & sort
            int[] iList = new int[iArray.length];
            int max = 0;
            for (int i = 0; i < iArray.length; i++) {
                iList[i] = SpeciesIndex.getParentPos(taxon, iArray[i]);
                if (iList[i] > max) {
                    max = iList[i];
                }
            }
            counts = new int[max + 2];    //+1 for array, +1 for 'unknown'
            for (int i = 0; i < iList.length; i++) {
                counts[iList[i] + 1]++;   //unknown has value -1
            }
            labels = new String[max + 2];
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > 0) {
                    counts[pos] = counts[i];
                    if (i == 0) { //unknown
                        labels[pos] = "unknown";
                    } else {
                        if (taxon < 10) {
                            labels[pos] = SpeciesIndex.getScientificName(i - 1);
                        } else {
                            labels[pos] = SpeciesIndex.getScientificNameLevel10(i - 1);
                        }
                    }
                    pos++;
                }
            }
        } else {
            counts = new int[sArray.length];
            int[] ia = getIntArray();
            for (int i = 0; i < ia.length; i++) {
                counts[ia[i]]++;
            }
            labels = new String[counts.length];
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > 0) {
                    counts[pos] = counts[i];
                    labels[pos] = sArray[i];
                    pos++;
                }
            }
        }
        counts = java.util.Arrays.copyOf(counts, pos);
        labels = java.util.Arrays.copyOf(labels, pos);

        Object[] o = {labels, counts};
        return o;
    }

    public int getPos() {
        return pos;
    }
}
