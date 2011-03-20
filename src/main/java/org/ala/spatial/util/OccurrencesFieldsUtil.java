/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

/**
 *
 * @author adam
 */
public class OccurrencesFieldsUtil {
    public static String[] columnNames;
    public static int[] extraIndexes;
    public int longitudeColumn;
    public int latitudeColumn;
    public int speciesColumn;
    public int zeroCount;
    public int oneCount;
    public int twoCount;
    public int onetwoCount;
    public int [] zeros;
    public int [] ones;
    public int [] twos;
    public int [] onestwos;

    static public void load() {
        columnNames = (new OccurrencesFieldsUtil()).getOutputColumnNames();
        extraIndexes = (new OccurrencesFieldsUtil()).getExtraIndexesPos();
    }

    public OccurrencesFieldsUtil(){
        TabulationSettings.load();
        
        String[] columns = TabulationSettings.occurrences_csv_fields;
        String[] columnsSettings = TabulationSettings.occurrences_csv_field_settings;

        /* longitude, latitude and species columns defaults */
        longitudeColumn = 0;
        latitudeColumn = 0;
        speciesColumn = 0;
        zeroCount = 0;
        oneCount = 0;
        twoCount = 0;
        int i;
        for(i=0;i<columnsSettings.length;i++){
            //longitude column has columnSettings=3
            //latitude column has columnSettings=4
            //species is last indexable column, most likely
            if (columnsSettings[i].equals("3")) {
                longitudeColumn = i;
            } else if (columnsSettings[i].equals("4")) {
                latitudeColumn = i;
            } else if(columnsSettings[i].equals("2")) {
                speciesColumn = i;
                twoCount++;
            } else if(columnsSettings[i].equals("0")) {
                zeroCount++;
            } else if(columnsSettings[i].equals("1")) {
                oneCount++;
            }
        }
        zeros = new int[zeroCount];
        ones = new int[oneCount];
        twos = new int[twoCount];
        onestwos = new int[oneCount + twoCount];
        zeroCount = 0;
        oneCount = 0;
        twoCount = 0;
        onetwoCount = 0;
        for(i=0;i<columnsSettings.length;i++){
            if(columnsSettings[i].equals("2")) {
                twos[twoCount++] = i;
                onestwos[onetwoCount++] = i;
            } else if(columnsSettings[i].equals("0")) {
                zeros[zeroCount++] = i;
            } else if(columnsSettings[i].equals("1")) {
                ones[oneCount++] = i;
                onestwos[onetwoCount++] = i;
            }
        }
    }

    public String[] getOutputColumnNames(){
        String[] columns = TabulationSettings.occurrences_csv_fields;
        String[] columnsSettings = TabulationSettings.occurrences_csv_field_settings;

        String [] output = new String[columns.length];

        int i;

        int pos = 0;

        //onestwos
        for(i=0;i<onetwoCount;i++){
            output[pos++] = columns[onestwos[i]];
        }
        //zeros
        for(i=0;i<zeroCount;i++){
            output[pos++] = columns[zeros[i]];
        }

        //longitude
        output[pos++] = "longitude";

        //latitude
        output[pos++] = "latitude";

        //dump
    //    for (String s : output) {
     //       System.out.println(s + ", ");
     //   }
      //  System.out.println("\r\n");

        return output;
    }

    public int[] getExtraIndexesPos(){
        String[] columns = getOutputColumnNames();
        String[] lookups = TabulationSettings.occurrences_csv_fields_lookups;

        int [] output = new int[lookups.length];

        for (int i=0;i<lookups.length;i++) {
            for(int j=0;j<columns.length;j++) {
                if (columns[j].equalsIgnoreCase(lookups[i])) {
       //             System.out.println("extra index: " + columns[j] + " (" + j + ")");
                   output[i] = j;
                   break;
                }
            }
        }
        return output;
    }

    /**
     * return column_positions array with actual column positions
     * of named columns in the header.
     *
     * @param line header of occurrences.csv as String []
     */
    public static int [] getColumnPositions(String[] line) throws Exception {
        String[] columns = TabulationSettings.occurrences_csv_fields;
        int [] column_positions = new int[columns.length];
        int i;
        int j;

        for (i = 0; i < column_positions.length; i++) {
            column_positions[i] = -1;
        }

        for (i = 0; i < columns.length; i++) {
            for (j = 0; j < line.length; j++) {
                if (columns[i].equalsIgnoreCase(line[j])) {
                    column_positions[i] = j;
                    break;
                }
            }
        }

        StringBuffer msg = new StringBuffer();
        boolean error = false;
        for (i = 0; i < column_positions.length; i++) {
            if (column_positions[i] == -1) {
                msg.append("\r\n").append(columns[i]);
                error = true;
            }
        }
        if (error) {
            System.out.println("FILE HEADER:");
            for (String s : line) {
                System.out.print(s + ",");
            }
            throw new Error("occurrences file has no column: " + msg.toString());
        }

        return column_positions;
    }

    /**
     * returns column_positions array with actual column positions
     * of sensitive coordinate columns in the header.
     *
     * @param line header of occurrences.csv as String []
     */
    public static int[] getSensitiveColumnPositions(String[] line) {
        int[] sensitive_column_positions = new int[3];
        int j;

        for (j = 0; j < line.length; j++) {
            if (TabulationSettings.occurrences_id_field.equalsIgnoreCase(line[j])) {
                sensitive_column_positions[0] = j;
            } else if (TabulationSettings.occurrences_sen_lat_field.equalsIgnoreCase(line[j])) {
                sensitive_column_positions[2] = j;
            } else if (TabulationSettings.occurrences_sen_long_field.equalsIgnoreCase(line[j])) {
                sensitive_column_positions[1] = j;
            }
        }

        if (sensitive_column_positions[0] == -1
                || sensitive_column_positions[1] == -1
                || sensitive_column_positions[2] == -1) {
            System.out.println("cannot find column for one of: "
                    + TabulationSettings.occurrences_id_field
                    + ", " + TabulationSettings.occurrences_sen_long_field
                    + ", " + TabulationSettings.occurrences_sen_lat_field);

            return null;
        }

        return sensitive_column_positions;
    }

}
