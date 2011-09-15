/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.data;

import au.com.bytecode.opencsv.CSVReader;
import java.io.StringReader;

/**
 *
 * @author Adam
 */
public class Facet {
    
    public static Facet[] parseFacets(String fq) {
        String [] fqs = fq.split("%20AND%20");

        Facet [] facets = new Facet[fqs.length];

        for(int i=0;i<fqs.length;i++) {
            facets[i] = new Facet(fqs[i]);
        }

        return facets;
    }

    String field;
    String value;
    String [] valueArray;
    String parameter;
    double min;
    double max;
    boolean includeRange;

    public Facet(String field, String value) {
        this.field = field;
        this.value = value;        
        this.includeRange = true;
        this.parameter = null;
        this.min = Double.NaN;
        this.max = Double.NaN;
        try {
            this.valueArray = new CSVReader(new StringReader(this.value)).readNext();
        } catch (Exception e) {}
    }

    public Facet(String parameter) {
        this.parameter = parameter;

        try {
            String [] fv = parameter.split(":");
            this.includeRange = !fv[0].startsWith("-");
            this.field = this.includeRange?fv[0]:fv[0].substring(1);
            this.value = fv[1];
            try {
                this.valueArray = new CSVReader(new StringReader(this.value)).readNext();
            } catch (Exception e) {}
            if(fv[1].startsWith("[") && fv[1].endsWith("]")) {                
                String [] v = fv[1].substring(1,fv[1].length()-1).split("%20TO%20");
                if(v[0].equals("*")) {
                    this.min = Double.NEGATIVE_INFINITY;
                } else {
                    this.min = Double.parseDouble(v[0]);
                }
                if(v[1].equals("*")) {
                    this.max = Double.POSITIVE_INFINITY;
                } else {
                    this.max = Double.parseDouble(v[1]);
                }
            }
        } catch (Exception e) {}

    }

    public Facet(String field, double min, double max, boolean includeRange) {
        this.field = field;
        this.min = min;
        this.max = max;
        this.includeRange = includeRange;

        this.value = "["
                + String.valueOf(min)
                + " TO "
                + String.valueOf(max)
                + "]";

        this.parameter = null;
    }

    @Override
    public String toString() {
        if(parameter == null) {
            return (includeRange?"":"-") + field + ":" + value.replace(" ", "%20");
        } else {
            return parameter;
        }
    }

    public boolean isValid(String v) {
        if(Double.isNaN(max)) {
            for(int i=0;i<valueArray.length;i++) {
                if(valueArray[i].equals(v)) {
                    return true;
                }
            }

            return !includeRange;
        } else {
            try {
                double d = Double.parseDouble(v);
                boolean inside = d >= min && d <= max;
                return includeRange?inside:!inside;
            } catch (Exception e) {}
        }

        return false;
    }

    public boolean isValid(double d) {
        if(Double.isNaN(max)) {
            String v = String.valueOf(d);
            for(int i=0;i<valueArray.length;i++) {
                if(valueArray[i].equals(v)) {
                    return true;
                }
            }

            return !includeRange;
        } else {
            try {
                boolean inside = d >= min && d <= max;
                return includeRange?inside:!inside;
            } catch (Exception e) {}
        }

        return false;
    }

    public String getField() {
        return field;
    }
}
