/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

import au.com.bytecode.opencsv.CSVReader;
import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam
 */
public class BiocacheLegendObject extends LegendObject {
    //[0] is colour, [1] is count

    HashMap<Double, int[]> categoriesNumeric;
    String csvLegend;
    String rawCsvLegend;
    String colourMode;

    public BiocacheLegendObject(String colourMode, String legend) {
        super((Legend) null, null);

        this.colourMode = colourMode;
        rawCsvLegend = legend;
        categories = new HashMap<String, int[]>();
        categoriesNumeric = new HashMap<Double, int[]>();
        List<String[]> csv = null;
        try {
            CSVReader csvReader = new CSVReader(new StringReader(legend));
            csv = csvReader.readAll();
            csvReader.close();
        } catch (IOException ex) {
            Logger.getLogger(BiocacheLegendObject.class.getName()).log(Level.SEVERE, null, ex);
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
            String[] p = (i > 1) ? csv.get(i - 1) : null;

            if(isYear) {
                c[0] = c[0].replace("-01-01T00:00:00Z", "");
                c[0] = c[0].replace("-12-31T00:00:00Z", "");
            } else if(isDecade) {
                for(int j=0;j<=9;j++) {
                    c[0] = c[0].replace(j + "-01-01T00:00:00Z", "0");
                    c[0] = c[0].replace(j + "-12-31T00:00:00Z", "0");
                }
            }

            int rc = Integer.parseInt(c[4]);
            if(rc == 0) {
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
            categoriesNumeric.put(d, value);

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
     *
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
    public int getColour(double value) {
        int[] data = categoriesNumeric.get(value);

        if (data != null) {
            return data[0];
        } else {
            return DEFAULT_COLOUR;
        }
    }

    @Override
    public double[] getMinMax() {
        if(getNumericLegend() != null) {
            return super.getMinMax();
        }
        double[] minmax = new double[2];
        boolean first = true;
        for (Double d : categoriesNumeric.keySet()) {
            if (!Double.isNaN(d)) {
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
        int size = 0;
        for(double d : categoriesNumeric.keySet()) {
            int [] v = categoriesNumeric.get(d);
            size += v[1];
        }
        double [] values = new double[size];
        int pos = 0;
        for(double d : categoriesNumeric.keySet()) {
            int [] v = categoriesNumeric.get(d);
            for(int i=0;i<v[1];i++) {
                values[pos] = d;
                pos++;
            }
        }

        return LegendBuilder.legendFromDoubles(values, new QueryField(colourMode,QueryField.FieldType.INT));
    }
}
