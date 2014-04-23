/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.ala.layers.client.Client;
import org.ala.layers.dto.Field;

/**
 * @author Adam
 */
public class IntersectUtil {

    static public void main(String[] args) {
        if (args.length < 3) {
            System.out.println("intersect a points file with fields");
            System.out.println("args[0] = points filename, a csv with first column latitude, second column longitude \n"
                    + "args[1] = comma separated field ids.  'all' can be used with local sampling, \n"
                    + "args[2] = output csv filename\n");
            System.out.println("\nnumber of threads for local sampling can be set to 4 with -DBATCH_THREAD_COUNT=4");
            System.out.println("\nremote sample can be set with -DLAYER_INDEX_URL=http://localhost:8082/layers-index");

            return;
        }

        try {
            String fields = args[1];
            if (args[1].equals("all")) {
                List<Field> ff = Client.getFieldDao().getFieldsByDB();
                fields = "";
                for (Field f : ff) {
                    fields = fields + (fields == "" ? "" : ",") + f.getId();
                }
            }
            String points = readPointsFile(args[0]);

            ArrayList<String> sample = Client.getLayerIntersectDao().sampling(fields, points);

            FileOutputStream fos = new FileOutputStream(args[2]);
            writeSampleToStream(fields.split(","), points.split(","), sample, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * write sample output into a stream.
     * <p/>
     * Output is a csv.  First two columns are latitude, longitude.  Remaining
     * columns are the fields.
     *
     * @param fields field ids as String [].
     * @param points array of latitude,longitude pairs.
     *               [even]=latitude, [odd]=longitude.  As String[].
     * @param sample sampling output from LayerIntersectDAO, as ArrayList<String>.
     * @param os     OutputStream.
     * @throws IOException
     */
    public static void writeSampleToStream(String[] fields, String[] points, ArrayList<String> sample, OutputStream os) throws IOException {
        int[] curPos = new int[sample.size()];
        for (int i = 0; i < curPos.length; i++) {
            curPos[i] = 0;
        }

        String charSet = "UTF-8";
        byte[] bNewLine = "\n".getBytes(charSet);
        byte[] bComma = ",".getBytes(charSet);
        byte[] bDblQuote = "\"".getBytes(charSet);

        os.write("latitude,longitude".getBytes(charSet));
        for (int i = 0; i < fields.length; i++) {
            os.write(bComma);
            os.write(fields[i].getBytes(charSet));
        }

        for (int i = 0; i < points.length; i += 2) {
            os.write(bNewLine);
            os.write(points[i].getBytes(charSet));
            os.write(bComma);
            os.write(points[i + 1].getBytes(charSet));

            for (int j = 0; j < sample.size(); j++) {
                os.write(bComma);
                int nextPos = sample.get(j).indexOf('\n', curPos[j]);
                if (nextPos == -1) {
                    nextPos = sample.get(j).length();
                }
                if (curPos[j] <= nextPos) {
                    String s = sample.get(j).substring(curPos[j], nextPos);
                    curPos[j] = nextPos + 1;

                    if (s != null) {
                        boolean useQuotes = false;
                        if (s.contains("\"")) {
                            s = s.replace("\"", "\"\"");
                            useQuotes = true;
                        } else if (s.contains(",")) {
                            useQuotes = true;
                        }
                        if (useQuotes) {
                            os.write(bDblQuote);
                            os.write(s.getBytes(charSet));
                            os.write(bDblQuote);
                        } else {
                            os.write(s.getBytes(charSet));
                        }
                    }
                }
            }
        }
    }

    private static String readPointsFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(line.replace("\"", ""));
        }
        return sb.toString();
    }
}
