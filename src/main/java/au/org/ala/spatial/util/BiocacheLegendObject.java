/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.Legend;
import au.org.ala.legend.LegendBuilder;
import au.org.ala.legend.LegendObject;
import au.org.ala.legend.QueryField;
import au.org.ala.spatial.StringConstants;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Adam
 */
public class BiocacheLegendObject extends LegendObject {
    private static final Logger LOGGER = Logger.getLogger(BiocacheLegendObject.class);
    //[0] is colour, [1] is count

    protected Map<Float, int[]> categoriesNumeric;
    private String csvLegend;
    private String colourMode;

    public BiocacheLegendObject(String colourMode, String legend) {
        super((Legend) null, null);

        this.colourMode = colourMode;
        categories = new HashMap<String, int[]>();
        categoriesNumeric = new HashMap<Float, int[]>();
        if (legend != null && legend.startsWith("name,red,green,blue,count")) {
            loadFromCsv(legend);
        } else {
            loadFromJson(legend);
        }

    }

    private void loadFromJson(String legend) {

        boolean isDecade = colourMode.startsWith(StringConstants.OCCURRENCE_YEAR_DECADE) || StringConstants.DECADE.equals(colourMode);
        boolean isYear = colourMode.contains(StringConstants.OCCURRENCE_YEAR) && !isDecade;

        int count = 0;
        int sum = 0;
        String colour = null;
        String line = null;
        StringBuilder sb = new StringBuilder();
        sb.append("name,red,green,blue,count");


        long start = System.currentTimeMillis();
        JSONParser jp = new JSONParser();
        JSONArray items = null;
        try {
            items = (JSONArray) jp.parse(legend);
        } catch (ParseException e) {
            LOGGER.error("failed to parse legend", e);
        }

        LOGGER.debug("*** time to parse legend to JSON was " + (System.currentTimeMillis() - start) + "ms");

        categoryNameOrder = new String[items.size()];

        int i = 0;
        for (Object item : items) {
            JSONObject jo = (JSONObject) item;
            String name = jo.containsKey("name") ? jo.get("name").toString() : "";
            String ccount = jo.containsKey("count") ? jo.get("count").toString() : null;
            String red = jo.containsKey("red") ? jo.get("red").toString() : "";
            String green = jo.containsKey("green") ? jo.get("green").toString() : "";
            String blue = jo.containsKey("blue") ? jo.get("blue").toString() : "";

            if (isYear && name != null) {
                name = (name.replace(StringConstants.DATE_TIME_BEGINNING_OF_YEAR, ""));
                name = (name.replace(StringConstants.DATE_TIME_END_OF_YEAR, ""));
            } else if (isDecade && name != null) {
                for (int j = 0; j <= 9; j++) {
                    name = (name.replace(j + StringConstants.DATE_TIME_BEGINNING_OF_YEAR, "0"));
                    name = (name.replace(j + StringConstants.DATE_TIME_END_OF_YEAR, "0"));
                }
            }


            if (ccount == null || Integer.parseInt(ccount) == 0) {
                continue;
            }

            int[] value = {new Color(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue)).getRGB(), Integer.parseInt(ccount)};
            categories.put(name, value);
            categoryNameOrder[i] = name;
            double d = Double.NaN;
            try {
                d = Double.parseDouble(name);
            } catch (Exception e) {
                //if fails to parse as double we do want the default Double.NaN
            }
            categoriesNumeric.put((float) d, value);

            sb.append("\n");

            colour = red + "," + green + "," + blue;
            line = "\"" + name.replace("\"", "\"\"").replace("\\", "\\\\") + "\"," + colour + "," + ccount;

            sb.append(line);

            i++;
        }
        //replace last line
        if (count > 0) {
            csvLegend = sb.toString().replace(line, count + " more" + "," + colour + "," + sum);
        } else {
            csvLegend = sb.toString();
        }
    }

    private void loadFromCsv(String legend) {
        List<String[]> csv = null;
        try {
            CSVReader csvReader = new CSVReader(new StringReader(legend));
            csv = csvReader.readAll();
            csvReader.close();
        } catch (IOException ex) {
            LOGGER.error("error reading legend: ", ex);
        }

        boolean isDecade = colourMode.startsWith(StringConstants.OCCURRENCE_YEAR_DECADE) || StringConstants.DECADE.equals(colourMode);
        boolean isYear = colourMode.contains(StringConstants.OCCURRENCE_YEAR) && !isDecade;

        int count = 0;
        int sum = 0;
        String colour = null;
        String line = null;
        StringBuilder sb = new StringBuilder();
        sb.append("name,red,green,blue,count");
        categoryNameOrder = new String[csv.size() - 1];
        for (int i = 1; i < csv.size(); i++) {
            String[] c = csv.get(i);

            String[] p = (i > 1) ? csv.get(i - 1) : null;

            if (isYear) {
                c[0] = c[0].replace(StringConstants.DATE_TIME_BEGINNING_OF_YEAR, "");
                c[0] = c[0].replace(StringConstants.DATE_TIME_END_OF_YEAR, "");
            } else if (isDecade) {
                for (int j = 0; j <= 9; j++) {
                    c[0] = c[0].replace(j + StringConstants.DATE_TIME_BEGINNING_OF_YEAR, "0");
                    c[0] = c[0].replace(j + StringConstants.DATE_TIME_END_OF_YEAR, "0");
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
                //will use default Double.NaN if parse fails
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
                line = "\"" + c[0].replace("\"", "\"\"") + "\"," + colour + "," + c[4];
                sb.append(line);
            }
        }
        //replace last line
        if (count > 0) {
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
            return new float[0];
        } else {
            return minmax;
        }
    }

    private int readColour(String red, String green, String blue) {
        return new Color(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue)).getRGB();
    }

    public LegendObject getAsIntegerLegend() {
        if (StringConstants.DECADE.equals(colourMode)) {
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
}
