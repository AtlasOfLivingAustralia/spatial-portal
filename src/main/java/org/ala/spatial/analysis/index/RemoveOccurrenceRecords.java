/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * 1. extract occurrenceId's from an occurrences.csv (A)
 * 2. remove occurrenceId's from an occurrences.csv (B)
 * 3. export (B) with occurrenceId's removed.
 *
 * id is in first column of occurrences.csv's
 *
 * @author Adam
 */
public class RemoveOccurrenceRecords {

    static String match = null;

    /**
     * arg[0] is occurrences file for id extraction
     * arg[1] is occurrences file for id removal
     * arg[2] is occurrences file for export.
     * arg[3] is optional string to match for record removal.
     * 
     * @param args
     */
    static void main(String[] args) {
        if (args.length > 3) {
            match = args[3];
        }

        ArrayList<Long> ids = extractIds(args[0]);
        java.util.Collections.sort(ids);

        removeIds(args[1], args[2], ids);
    }

    private static ArrayList<Long> extractIds(String filename) {
        ArrayList<Long> ids = new ArrayList<Long>();

        /* read occurances_csv */
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));

            String s;
            String[] sa;

            while ((s = br.readLine()) != null) {
                //check for continuation line
                while (s != null && s.length() > 0 && s.charAt(s.length() - 1) == '\\') {
                    String spart = br.readLine();
                    if (spart == null) {  //same as whole line is null
                        break;
                    } else {
                        s.replace('\\', ' ');   //new line is same as 'space'
                        s += spart;
                    }
                }//repeat as necessary

                sa = s.split(",");

                try {
                    long l = Long.parseLong(sa[0].replace("\"", ""));

                    ids.add(l);
                } catch (Exception e) {
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ids;
    }

    private static void removeIds(String inputFilename, String outputFilename, ArrayList<Long> ids) {
        /* read occurances_csv */
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFilename));
            FileWriter fw = new FileWriter(outputFilename);

            String s;
            String[] sa;

            int idsRemoved = 0;
            int stringMatchesRemoved = 0;

            while ((s = br.readLine()) != null) {
                //check for continuation line
                while (s != null && s.length() > 0 && s.charAt(s.length() - 1) == '\\') {
                    String spart = br.readLine();
                    if (spart == null) {  //same as whole line is null
                        break;
                    } else {
                        s.replace('\\', ' ');   //new line is same as 'space'
                        s += spart;
                    }
                }//repeat as necessary

                sa = s.split(",");

                boolean export = true;
                try {
                    long l = Long.parseLong(sa[0].replace("\"", ""));

                    if (java.util.Collections.binarySearch(ids, l) >= 0) {
                        //don't export
                        export = false;
                        idsRemoved++;
                    } else if (stringMatch(s)) {
                        export = false;
                        stringMatchesRemoved++;
                    }
                } catch (Exception e) {
                }

                if (export) {
                    fw.append(s).append("\n");
                }
            }

            System.out.println("ids Removed: " + idsRemoved + ", string matches Removed: " + stringMatchesRemoved);
            br.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean stringMatch(String s) {
        return match == null || s.contains(match);
    }
}
