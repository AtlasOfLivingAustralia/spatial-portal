/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.scatterplot;

import au.com.bytecode.opencsv.CSVReader;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;
import org.ala.layers.legend.Legend;
import org.ala.layers.legend.LegendBuilder;
import org.ala.layers.legend.LegendObject;
import org.ala.layers.legend.QueryField;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

/**
 * @author Adam
 */
public class BiocacheLegendObject extends LegendObject {
    private static Logger logger = Logger.getLogger(BiocacheLegendObject.class);
    //[0] is colour, [1] is count

    HashMap<Float, int[]> categoriesNumeric;
    String csvLegend;
    String rawCsvLegend;
    String colourMode;

    public BiocacheLegendObject(String colourMode, String legend) {
        super((Legend) null, null);

        this.colourMode = colourMode;
        rawCsvLegend = legend;
        categories = new HashMap<String, int[]>();
        categoriesNumeric = new HashMap<Float, int[]>();
        if (legend != null && legend.startsWith("name,red,green,blue,count")) {
            loadFromCsv(legend);
        } else {
            loadFromJson(legend);
        }

    }

    private void loadFromJson(String legend) {

        boolean isDecade = colourMode.startsWith("occurrence_year_decade") || colourMode.equals("decade");
        boolean isYear = colourMode.contains("occurrence_year") && !isDecade;

        int count = 0;
        int sum = 0;
        String colour = null;
        String line = null;
        StringBuilder sb = new StringBuilder();
        sb.append("name,red,green,blue,count");


        long start = System.currentTimeMillis();
        JSONArray items = JSONArray.fromObject(legend);
        JsonConfig cfg = new JsonConfig();
        cfg.setPropertySetStrategy(new IgnoreUnknownPropsStrategyWrapper(PropertySetStrategy.DEFAULT));
        cfg.setRootClass(LegendItemDTO.class);
        java.util.Collection<LegendItemDTO> c = JSONArray.toCollection(items, cfg);
        LegendItemDTO previous = null;
        logger.debug("*** time to parse legend to JSON was " + (System.currentTimeMillis() - start) + "ms");

        categoryNameOrder = new String[c.size()];

        int i = 0;
        for (LegendItemDTO item : c) {
            if (isYear && item.getName() != null) {
                item.setName(item.getName().replace("-01-01T00:00:00Z", ""));
                item.setName(item.getName().replace("-12-31T00:00:00Z", ""));
            } else if (isDecade && item.getName() != null) {
                for (int j = 0; j <= 9; j++) {
                    item.setName(item.getName().replace(j + "-01-01T00:00:00Z", "0"));
                    item.setName(item.getName().replace(j + "-12-31T00:00:00Z", "0"));
                }
            }


            if (item.getCount() == 0) {
                continue;
            }

            int[] value = {new Color(item.getRed(), item.getGreen(), item.getBlue()).getRGB(), item.getCount()};
            categories.put(item.getName(), value);
            categoryNameOrder[i] = item.getName();
            double d = Double.NaN;
            try {
                d = Double.parseDouble(item.getName());
            } catch (Exception e) {
            }
            categoriesNumeric.put((float) d, value);


            //check for endpoint (repitition of colour)
            if (previous != null
                    && previous.getRed() == item.getRed()
                    && previous.getGreen() == item.getGreen()
                    && previous.getBlue() == item.getBlue()
                    && !isDecade && !isYear) {
                if (count == 0) {
                    count = 1;
                    sum = previous.getCount();
                }
                count++;
                sum += item.getCount();
            } else {
                sb.append("\n");

                colour = item.getRed() + "," + item.getGreen() + "," + item.getBlue();
                line = "\"" + item.getName().replace("\"", "\"\"") + "\"," + colour + "," + item.getCount();

                if (item.getName().startsWith("Camponotus")) {
                    line = line;
                }
                sb.append(line);
            }
            previous = item;
            i++;
        }
        if (count > 0) { //replace last line
            csvLegend = sb.toString().replace(line, count + " more" + "," + colour + "," + sum);
        } else {
            csvLegend = sb.toString();
        }

//        for(int i=0;i<items.size();i++){
//            
//        }
        //ObjectMapper om = new ObjectMapper();
        //new TypeReference<HashMap<String,String>>() {})
        //om.readValue(new java.io.ByteArrayInputStream(legend.getBytes()), new TypeReference<List<java.util.Map<String,Object>>>());

    }

    private void loadFromCsv(String legend) {
        List<String[]> csv = null;
        try {
//            org.apache.commons.csv.CSVParser parser =CSVFormat.EXCEL.parse(new StringReader(legend));
//            for(org.apache.commons.csv.CSVRecord record: parser.getRecords()){
//                String[] vs = new String[record.size()];
//                int i=0;
//                java.util.Iterator<String> it = record.iterator();
//                while(it.hasNext()){
//                    vs[i++] = it.next();
//                }
//                csv.add(vs);
//            }
            CSVReader csvReader = new CSVReader(new StringReader(legend));
            //org.gbif.file.CSVReader csvReader = new org.gbif.file.CSVReader(new java.io.ByteArrayInputStream(legend.getBytes()), "UTF-8",",",'\"',new Integer(1));//CSVReader(new StringReader(legend));
            csv = csvReader.readAll();
            csvReader.close();
        } catch (IOException ex) {
            logger.error("error reading legend: ", ex);
        }

        boolean isDecade = colourMode.startsWith("occurrence_year_decade") || colourMode.equals("decade");
        boolean isYear = colourMode.contains("occurrence_year") && !isDecade;

        int count = 0;
        int sum = 0;
        String colour = null;
        String line = null;
        StringBuilder sb = new StringBuilder();
        sb.append("name,red,green,blue,count");
        categoryNameOrder = new String[csv.size() - 1];
        for (int i = 1; i < csv.size(); i++) {
            String[] c = csv.get(i);
//            if(c.length !=5){
//                logger.debug("ISSUE: " + StringUtils.join(c, "$$$"));
//            }
            String[] p = (i > 1) ? csv.get(i - 1) : null;

            if (isYear) {
                c[0] = c[0].replace("-01-01T00:00:00Z", "");
                c[0] = c[0].replace("-12-31T00:00:00Z", "");
            } else if (isDecade) {
                for (int j = 0; j <= 9; j++) {
                    c[0] = c[0].replace(j + "-01-01T00:00:00Z", "0");
                    c[0] = c[0].replace(j + "-12-31T00:00:00Z", "0");
                }
            }

            int rc = Integer.parseInt(c[4]);
            if (rc == 0) {
                continue;
            }

            int[] value = {readColour(c[1], c[2], c[3]), rc};
            categories.put(c[0], value);
            categoryNameOrder[i - 1] = c[0];
            double d = Double.NaN;
            try {
                d = Double.parseDouble(c[0]);
            } catch (Exception e) {
            }
            categoriesNumeric.put((float) d, value);

            //check for endpoint (repitition of colour)
            if (p != null && c.length > 4 && p.length > 4
                    && p[1].equals(c[1]) && p[2].equals(c[2]) && p[3].equals(c[3])
                    && !isDecade && !isYear) {
                if (count == 0) {
                    count = 1;
                    sum = Integer.parseInt(p[4]);
                }
                count++;
                sum += Integer.parseInt(c[4]);
            } else {
                sb.append("\n");

                colour = c[1] + "," + c[2] + "," + c[3];
                line = "\"" + c[0] + "\"," + colour + "," + c[4];
                sb.append(line);
            }
        }
        if (count > 0) { //replace last line
            csvLegend = sb.toString().replace(line, count + " more" + "," + colour + "," + sum);
        } else {
            csvLegend = sb.toString();
        }
    }

    /**
     * Get legend as a table.
     * <p/>
     * CSV
     * (header) name, red, green, blue, count CR
     * (records) string, 0-255, 0-255, 0-255, integer CR
     *
     * @return
     */
    @Override
    public String getTable() {
        return csvLegend;
    }

    @Override
    public int getColour(String value) {
        int[] data = categories.get(value);

        if (data != null) {
            return data[0];
        } else {
            return DEFAULT_COLOUR;
        }
    }

    @Override
    public int getColour(float value) {
        int[] data = categoriesNumeric.get(value);

        if (data != null) {
            return data[0];
        } else {
            return DEFAULT_COLOUR;
        }
    }

    @Override
    public float[] getMinMax() {
        if (getNumericLegend() != null) {
            return super.getMinMax();
        }
        float[] minmax = new float[2];
        boolean first = true;
        for (Float d : categoriesNumeric.keySet()) {
            if (!Float.isNaN(d)) {
                if (first || minmax[0] > d) {
                    minmax[0] = d;
                }
                if (first || minmax[1] < d) {
                    minmax[1] = d;
                }
                first = false;
            }
        }
        if (!first) {
            return null;
        } else {
            return minmax;
        }
    }

    private int readColour(String red, String green, String blue) {
        return new Color(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue)).getRGB();
    }

    public LegendObject getAsIntegerLegend() {
        if (colourMode.equals("decade")) {
            double[] values = new double[categoriesNumeric.size()];
            int i = 0;
            for (double d : categoriesNumeric.keySet()) {
                values[i++] = d;
            }
            return LegendBuilder.legendForDecades(values, new QueryField(colourMode, QueryField.FieldType.INT));
        } else {
            int size = 0;
            for (float f : categoriesNumeric.keySet()) {
                int[] v = categoriesNumeric.get(f);
                size += v[1];
            }
            double[] values = new double[size];
            int pos = 0;
            for (float f : categoriesNumeric.keySet()) {
                int[] v = categoriesNumeric.get(f);
                for (int i = 0; i < v[1]; i++) {
                    values[pos] = f;
                    pos++;
                }
            }

            return LegendBuilder.legendFromDoubles(values, new QueryField(colourMode, QueryField.FieldType.INT));
        }
    }

    public static class IgnoreUnknownPropsStrategyWrapper extends PropertySetStrategy {

        private PropertySetStrategy original;

        public IgnoreUnknownPropsStrategyWrapper(PropertySetStrategy original) {
            this.original = original;
        }

        @Override
        public void setProperty(Object o, String string, Object o1) throws JSONException {
            try {
                original.setProperty(o, string, o1);
            } catch (Exception ex) {
                //ignore
            }
        }
    }
}
