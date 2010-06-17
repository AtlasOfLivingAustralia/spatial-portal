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

    public OccurrencesFieldsUtil(){
        String[] columns = TabulationSettings.occurances_csv_fields;
        String[] columnsSettings = TabulationSettings.occurances_csv_field_settings;

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
        String[] columns = TabulationSettings.occurances_csv_fields;
        String[] columnsSettings = TabulationSettings.occurances_csv_field_settings;

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
        for (String s : output) {
            System.out.println(s + ", ");
        }
        System.out.println("\r\n");

        return output;
    }

}
