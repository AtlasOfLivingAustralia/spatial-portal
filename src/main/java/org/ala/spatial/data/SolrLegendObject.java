/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

import au.com.bytecode.opencsv.CSVReader;
import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam
 */
public class SolrLegendObject extends LegendObject {
    //[0] is colour, [1] is count

    HashMap<Double, int[]> categoriesNumeric;
    String csvLegend;
    String rawCsvLegend;
    String colourMode;

    public SolrLegendObject(String colourMode, String legend) {
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
            Logger.getLogger(SolrLegendObject.class.getName()).log(Level.SEVERE, null, ex);
        }

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
                    && p[1].equals(c[1]) && p[2].equals(c[2]) && p[3].equals(c[3])) {
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
    public String getTable() {
        return csvLegend;
    }

    public int getColour(String value) {
        int[] data = categories.get(value);

        if (data != null) {
            return data[0];
        } else {
            return DEFAULT_COLOUR;
        }
    }

    public int getColour(double value) {
        int[] data = categoriesNumeric.get(value);

        if (data != null) {
            return data[0];
        } else {
            return DEFAULT_COLOUR;
        }
    }

    public double[] getMinMax() {
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
}
