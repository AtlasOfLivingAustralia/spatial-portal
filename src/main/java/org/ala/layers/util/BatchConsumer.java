/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;
import org.ala.layers.dao.LayerIntersectDAO;
import org.ala.layers.dao.LayerIntersectDAO;
import org.ala.layers.util.IntersectUtil;
import org.ala.layers.util.IntersectUtil;

/**
 *
 * @author Adam
 */
public class BatchConsumer {
    static BatchConsumerThread thread;
    static LinkedBlockingQueue<String> waitingBatchDirs;
    static LayerIntersectDAO layerIntersectDao;

    public static void start(LayerIntersectDAO layerIntersectDao) {
        if(thread != null) {
            BatchConsumer.layerIntersectDao = layerIntersectDao;
            BatchConsumer.waitingBatchDirs = new LinkedBlockingQueue<String>();
            BatchConsumer.thread = new BatchConsumerThread(waitingBatchDirs, layerIntersectDao);
        }
    }

    public static void addBatch(String batchDir) throws InterruptedException {
        waitingBatchDirs.put(batchDir);
    }

    static void end() {
        thread.interrupt();
    }
}

class BatchConsumerThread extends Thread {
    LinkedBlockingQueue<String> waitingBatchDirs;
    LayerIntersectDAO layerIntersectDao;

    public BatchConsumerThread(LinkedBlockingQueue<String> waitingBatchDirs, LayerIntersectDAO layerIntersectDao) {
        this.waitingBatchDirs = waitingBatchDirs;
        this.layerIntersectDao = layerIntersectDao;
    }

    @Override
    public void run() {
        while(true) {
            String currentBatch = null;
            try {
//                //get next batch to process
//                File f = new File(batchDir);
//                File [] files = f.listFiles();
//                java.util.Arrays.sort(files);
//
//                File nextFile = null;
//                for(int i=0;i<files.length;i++) {
//                    if(files[i].isDirectory()
//                            && !(new File(files[i].getPath() + File.separator + "error.txt")).exists()
//                            && !(new File(files[i].getPath() + File.separator + "done.txt")).exists()) {
//                        nextFile = files[i];
//                        break;
//                    }
//                }
//
//                if(nextFile == null) {
//                    //wait 10s
//                    this.wait(10000);
//                    continue;
//                }

                currentBatch = waitingBatchDirs.take();
                
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm:ss:SS");
                writeToFile(currentBatch + "status.txt","started at " + sdf.format(new Date()), true);

                String fids = readFile(currentBatch + "fids.txt");
                String points = readFile(currentBatch + "points.txt");

                ArrayList<String> sample = layerIntersectDao.sampling(fids, points);

                FileOutputStream fos = new FileOutputStream(currentBatch + "sample.csv.gz");
                GZIPOutputStream gzip = new GZIPOutputStream(fos);
                IntersectUtil.writeSampleToStream(fids.split(","), points.split(","), sample, gzip);
                gzip.close();
                fos.close();

                writeToFile("status.txt","finished at " + sdf.format(new Date()), true);                
                writeToFile("done.txt","finished at " + sdf.format(new Date()), true);
                
                currentBatch = null;
            } catch (Exception e) {
                if(currentBatch != null) {
                    try {
                        writeToFile(currentBatch + "error.txt", "error " + e.getMessage(), true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                e.printStackTrace();
            }
        }
    }

    private static void writeToFile(String filename, String string, boolean append) throws IOException {
        FileWriter fw = new FileWriter(filename, append);
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
}