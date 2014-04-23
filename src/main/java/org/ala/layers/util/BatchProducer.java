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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @author Adam
 */
public class BatchProducer {
    static public String produceBatch(String batchPath, String requestInfo, String fids, String points) {
        String id = String.valueOf(System.currentTimeMillis());
        try {
            String dir = batchPath + File.separator + id + File.separator;
            new File(dir).mkdirs();

            writeToFile(dir + "request.txt", requestInfo);
            writeToFile(dir + "fids.txt", fids);
            writeToFile(dir + "points.txt", points);

            BatchConsumer.addBatch(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return id;
    }

    private static void writeToFile(String filename, String string) throws IOException {
        FileWriter fw = new FileWriter(filename);
        fw.write(string);
        fw.close();
    }

    private static String readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    public static String getStatus(String batchPath, String batchId) throws IOException {
        String dir = batchPath + File.separator + batchId + File.separator;
        if (new File(dir).exists()) {
            if (new File(dir + "error.txt").exists()) {
                return readFile(dir + "error.txt");
            } else if (new File(dir + "finished.txt").exists()) {
                return readFile(dir + "finished.txt");
            } else if (new File(dir + "started.txt").exists()) {
                return readFile(dir + "started.txt");
            }
        }

        return "unknown batchId: " + batchId;
    }

    public static void addInfoToMap(String batchPath, String batchId, Map map) throws IOException {
        String dir = batchPath + File.separator + batchId + File.separator;
        if (new File(dir).exists()) {
            int count = 0;

            if (new File(dir + "started.txt").exists()) {
                count++;
                map.put("started", readFile(dir + "started.txt"));
                map.put("status", "started");
            }
            if (new File(dir + "error.txt").exists()) {
                count++;
                map.put("error", readFile(dir + "error.txt"));
                map.put("status", "error");
            }
            if (new File(dir + "finished.txt").exists()) {
                count++;
                map.put("finished", readFile(dir + "finished.txt"));
                map.put("status", "finished");
            }

            if (count == 0) {
                map.put("waiting", "In queue");
                map.put("status", "waiting");
            }
        } else {
            map.put("error", "unknown batchId: " + batchId);
        }
    }
}
