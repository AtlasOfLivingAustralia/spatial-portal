/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 *
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
        }catch (Exception e) {
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
        while((line = br.readLine()) != null) {
            if(sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    public static String getStatus(String batchPath, String batchId) throws IOException {
        String dir = batchPath + File.separator + batchId + File.separator;
        if(new File(dir).exists()) {
            if(new File(dir + "error.txt").exists()) {
                return readFile(dir + "error.txt");
            } else if(new File(dir + "done.txt").exists()) {
                return readFile(dir + "done.txt");
            } else if(new File(dir + "started.txt").exists()) {
                return readFile(dir + "started.txt");
            }
        }

        return "unknown batchId: " + batchId;
    }

    public static void addInfoToMap(String batchPath, String batchId, Map map) throws IOException {
        String dir = batchPath + File.separator + batchId + File.separator;
        if(new File(dir).exists()) {
            int count = 0;
            
            if(new File(dir + "error.txt").exists()) {
                count++;
                map.put("error",readFile(dir + "error.txt"));
            }
            if(new File(dir + "done.txt").exists()) {
                count++;
                map.put("finished",readFile(dir + "done.txt"));
            }
            if(new File(dir + "started.txt").exists()) {
                count++;
                map.put("started",readFile(dir + "started.txt"));
            }

            if(count == 0) {
                map.put("error","unknown batchId: " + batchId);
            }
        } else {
            map.put("error","unknown batchId: " + batchId);
        }
    }
}
