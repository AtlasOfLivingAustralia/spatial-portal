/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

import au.com.bytecode.opencsv.CSVReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Adam
 */
public class Facet implements Serializable {

    /**
     * Parse a facet. Used with uploaded data in UploadQuery and WMSService
     *
     * Recognised facets:
     *
     *  - single string field.  <field>:CSV formatted values
     *
     *  - inverse of 'single string field'.  -<field>:CSV formatted values
     *
     *  - one or more number fields.  <term1> AND <term2> AND ...
     *    where <term> is <field>:[<min> TO <max>]
     *          <min>, <max> may be '*'
     *
     *  - inverse of 'one or more number fields'.  -<term1> OR -<term2> OR ...
     *    where <term> is <field>:[<min> TO <max>]
     *          <min>, <max> may be '*'
     *
     *  - two number fields and their null value records.
     *    (<term1> AND <term2>) OR -<field1>:[* TO *] OR -<field2>:[* TO *]
     *
     *  - inverse of 'two number fields and their null value records'.
     *    (-<term1> OR -<term2>) AND <field1>:[* TO *] AND <field2>:[* TO *]

     * @param fq
     * @return
     */
    public static Facet parseFacet(String fq) {
        fq = fq.replace(" ", "%20");

        //tests
        boolean hasAnd = fq.contains("%20AND%20");
        boolean hasOr = fq.contains("%20OR%20");
        boolean hasStartBracket = fq.startsWith("(");
        boolean hasStartMinus = fq.startsWith("-");
        boolean has2ndCharMinus = fq.charAt(1) == '-';
        boolean hasSqBracket = fq.contains("[");

        //- single string field.  <field>:CSV formatted values
        if (!hasSqBracket && !hasAnd && !hasOr) {
            int offset = fq.startsWith("-") ? 1 : 0;
            String f = fq.substring(offset, fq.indexOf(':')).replace("%20"," ");
            String v = fq.substring(fq.indexOf(':') + 1).replace("%20"," ");
            return new Facet(f, v, offset == 0);
        } else //- one or more number fields.  <term1> AND <term2> AND ...
        if (!hasStartBracket && hasAnd && hasSqBracket) {
            return new Facet(fq, null, parseTerms("%20AND%20", fq), null);
        } else //- inverse of 'one or more number fields'.  -<term1> OR -<term2> OR ...
        if (hasStartMinus && hasOr && hasSqBracket) {
            return new Facet(fq, parseTerms("%20OR%20", fq), null, null);
        } else //- two number fields and their null value records.
        if (hasStartBracket && !has2ndCharMinus && hasAnd) {
            String[] parts = fq.split("%20OR%20");
            Facet[] firstTwo = parseTerms("%20AND%20", parts[0].substring(1, parts[0].length() - 1));
            Facet[] lastTwo = {parseTerms("%20OR%20", parts[1])[0], parseTerms("%20OR%20", parts[2])[0]};
            return new Facet(fq, null, firstTwo, lastTwo);
        } else //- inverse of 'two number fields and their null value records'.
        if (hasStartBracket && has2ndCharMinus && hasOr) {
            String[] parts = fq.split("%20AND%20");
            Facet[] firstTwo = parseTerms("%20OR%20", parts[0].substring(1, parts[0].length() - 1));
            Facet[] lastTwo = {parseTerms("%20AND%20", parts[1])[0], parseTerms("%20AND%20", parts[2])[0]};
            return new Facet(fq, firstTwo, lastTwo, null);
        }

        return null;
    }

    static Facet[] parseTerms(String separator, String fq) {
        String[] terms = fq.split(separator);
        Facet[] facets = new Facet[terms.length];
        for (int i = 0; i < terms.length; i++) {
            String ff = terms[i];
            int offset = ff.startsWith("-") ? 1 : 0;
            String f = ff.substring(offset, ff.indexOf(':')).replace("%20"," ");
            String v = ff.substring(fq.indexOf(':') + 1);
            String[] n = v.substring(1, v.length() - 1).split("%20TO%20");
            double[] d = {n[0].equals("*") ? Double.NEGATIVE_INFINITY : Double.parseDouble(n[0]),
                n[1].equals("*") ? Double.POSITIVE_INFINITY : Double.parseDouble(n[1])};
            facets[i] = new Facet(f, d[0], d[1], offset == 0);
        }

        return facets;
    }
    String field;
    String value;
    String[] valueArray;
    String parameter;
    double min;
    double max;
    boolean includeRange;

    public Facet(String field, String value, boolean includeRange) {
        this.field = field;
        this.value = value;
        this.includeRange = includeRange;
        this.parameter = null;
        this.min = Double.NaN;
        this.max = Double.NaN;
        this.valueArray = null;
        try {
            CSVReader csv = new CSVReader(new StringReader(this.value));
            this.valueArray = csv.readNext();
            csv.close();
        } catch (Exception e) {
        }
    }

    public Facet(String field, double min, double max, boolean includeRange) {
        this.field = field.replace("%20"," ");
        this.min = min;
        this.max = max;
        this.includeRange = includeRange;

        this.value = "["
                + String.valueOf(min)
                + " TO "
                + String.valueOf(max)
                + "]";

        this.valueArray = null;

        this.parameter = null;
    }
    Facet[] orInAndTerms;
    Facet[] andTerms;
    Facet[] orTerms;

    public Facet(String fq, Facet[] orInAndTerms, Facet[] andTerms, Facet[] orTerms) {
        //make toString work
        parameter = fq;

        //make isValid and getFields work
        this.orInAndTerms = orInAndTerms;
        this.andTerms = andTerms;
        this.orTerms = orTerms;
    }

    @Override
    public String toString() {
        if (parameter == null) {
            return (includeRange ? "" : "-") + field + ":" + value.replace(" ", "%20");
        } else {
            return parameter;
        }
    }

    public String[] getFields() {
        Set<String> fieldSet = new HashSet<String>();
        if (field != null) {
            fieldSet.add(field);
        }
        if (orInAndTerms != null) {
            for (Facet f : orInAndTerms) {
                for (String s : f.getFields()) {
                    fieldSet.add(s);
                }
            }
        }
        if (andTerms != null) {
            for (Facet f : andTerms) {
                for (String s : f.getFields()) {
                    fieldSet.add(s);
                }
            }
        }
        if (orTerms != null) {
            for (Facet f : orTerms) {
                for (String s : f.getFields()) {
                    fieldSet.add(s);
                }
            }
        }

        String[] fields = new String[fieldSet.size()];
        fieldSet.toArray(fields);
        return fields;
    }

    public boolean isValid(String v) {
        if (valueArray != null) {
            for (int i = 0; i < valueArray.length; i++) {
                if (valueArray[i].equals(v)) {
                    return true;
                }
            }
            return !includeRange;
        } else {
            try {
                double d = Double.parseDouble(v);
                boolean inside = d >= min && d <= max;
                return includeRange ? inside : !inside;
            } catch (Exception e) {
            }
        }

        return false;
    }

    public boolean isValid(double d) {
        if (Double.isNaN(max)) {
            String v = String.valueOf(d);
            for (int i = 0; i < valueArray.length; i++) {
                if (valueArray[i].equals(v)) {
                    return true;
                }
            }

            return !includeRange;
        } else {
            try {
                boolean inside = d >= min && d <= max;
                return includeRange ? inside : !inside;
            } catch (Exception e) {
            }
        }

        return false;
    }

    /**
     * Facet type
     * 0 = numeric
     * 1 = string
     * 2 = group
     *
     * @return
     */
    public int getType() {
        if (orInAndTerms != null || andTerms != null || orTerms != null) {
            return 2;
        } else if (valueArray != null) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean isValid(List<QueryField> fields, int record) {
        if (getType() == 2) {
            boolean state = true;
            if (orInAndTerms != null) {
                state = sumTermTests(orInAndTerms, fields, record) > 0;
            }
            if (andTerms != null) {
                if (!state) {
                    //state = false;
                } else {
                    state = sumTermTests(andTerms, fields, record) == andTerms.length;
                }
            }
            if (orTerms != null) {
                if (state) {
                    return true;
                } else {
                    return sumTermTests(orTerms, fields, record) > 0;
                }
            } else {
                return state;
            }
        } else {
            for (QueryField qf : fields) {
                if (qf.getName().equals(field)) {
                    if (getType() == 1) {
                        return isValid(qf.getAsString(record));
                    } else {    //type == 0
                        switch (qf.getFieldType()) {
                            case DOUBLE:
                                return isValid(qf.getDouble(record));
                            case FLOAT:
                                return isValid(qf.getFloat(record));
                            case LONG:
                                return isValid((double) qf.getLong(record));
                            case INT:
                                return isValid(qf.getInt(record));
                            default:
                                return isValid(qf.getAsString(record));
                        }
                    }
                }
            }
            //if field not found, treat as outside of any specified range
            return !includeRange;
        }
    }

    private int sumTermTests(Facet[] andTerms, List<QueryField> fields, int record) {
        int sum = 0;
        for (int i = 0; andTerms != null && i < andTerms.length; i++) {
            if (andTerms[i].getType() == 2) {
                if (andTerms[i].isValid(fields, record)) {
                    sum++;
                }
            } else {
                if (andTerms[i].isValid(fields, record)) {
                    sum++;
                }
            }
        }
        return sum;
    }
}
