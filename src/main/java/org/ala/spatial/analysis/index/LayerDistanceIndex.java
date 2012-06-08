/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.layers.intersect.Grid;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.GridCutter;

/**
 *
 * @author Adam
 */
public class LayerDistanceIndex {

    final public static String LAYER_DISTANCE_FILE = "layerDistances.properties";
    double[][] min;
    double[][] max;
    double[][] range;
    static double[][] all_measures;

    public void occurrencesUpdate(int threadcount) throws InterruptedException {

        Map<String, Double> map = loadDistances();

        LinkedBlockingQueue<String> todo = new LinkedBlockingQueue();

        //find all environmental layer analysis files
        File root = new File(AlaspatialProperties.getAnalysisLayersDir());
        File [] dirs = root.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname != null && pathname.isDirectory();
            }

        });
        for(File dir : dirs) {
            //iterate through files
            File[] files = new File(dir.getPath()).listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".grd") && pathname.getName().startsWith("el");
                }
            });

            for (int i = 0; i < files.length; i++) {
                for (int j = i + 1; j < files.length; j++) {
                    String file1 = files[i].getName().replace(".grd", "");
                    String file2 = files[j].getName().replace(".grd", "");

                    String key = (file1.compareTo(file2) < 0) ? file1 + " " + file2 : file2 + " " + file1;

                    if (!map.containsKey(key)) {
                        todo.put(key);
                    }
                }
            }
        }

        LinkedBlockingQueue<String> toDisk = new LinkedBlockingQueue<String>();
        //int threadcount = 4;
        CountDownLatch cdl = new CountDownLatch(todo.size());
        CalcThread [] threads = new CalcThread[threadcount];
        for(int i=0;i<threadcount;i++) {
            threads[i] = new CalcThread(cdl,todo, toDisk);
            threads[i].start();
        }

        ToDiskThread toDiskThread = new ToDiskThread(AlaspatialProperties.getAnalysisWorkingDir()
                    + LAYER_DISTANCE_FILE, toDisk);
        toDiskThread.start();

        cdl.await();

        for(int i=0;i<threadcount;i++) {
            threads[i].interrupt();
        }

        //wait 10s and then close
        Thread.currentThread().wait(10*1000);

        toDiskThread.interrupt();
    }

    static public void main(String[] args) throws InterruptedException {
        System.out.println("args[0] = threadcount, e.g. 1");
        LayerDistanceIndex ldi = new LayerDistanceIndex();
        ldi.occurrencesUpdate(Integer.parseInt(args[0]));
    }

    static public Map<String, Double> loadDistances() {
        Map<String, Double> map = new ConcurrentHashMap<String, Double>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(AlaspatialProperties.getAnalysisWorkingDir()
                    + LAYER_DISTANCE_FILE));

            String line;
            while((line = br.readLine()) != null) {
                if(line.length() > 0) {
                    String [] keyvalue = line.split("=");
                    double d = Double.NaN;
                    try {
                        d = Double.parseDouble(keyvalue[1]);
                    } catch (Exception e) {
                        System.out.println("cannot parse value in " + line);
                    }
                    map.put(keyvalue[0], d);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return map;
    }
}

class CalcThread extends Thread {
    CountDownLatch cdl;
    LinkedBlockingQueue<String> lbq;
    LinkedBlockingQueue<String> toDisk;

    public CalcThread(CountDownLatch cdl, LinkedBlockingQueue<String> lbq, LinkedBlockingQueue<String> toDisk) {
        this.cdl = cdl;
        this.lbq = lbq;
        this.toDisk = toDisk;
    }

    public void run() {
        try {
            while(true) {
                String key = lbq.take();
                String [] layers = key.split(" ");

                try {
                    Double distance = calculateDistance(layers[0], layers[1]);
                    System.out.println(key + "=" + distance);
                    toDisk.put(key + "=" + distance);
                } catch (Exception e) {
                    System.out.println(key + ":error");
                    e.printStackTrace(System.out);
                }

                cdl.countDown();
            }
        } catch (InterruptedException e) {
        }
    }

    private Double calculateDistance(String layer1, String layer2) {
        Grid g1 = Grid.getGridStandardized(GridCutter.getLayerPath(AlaspatialProperties.getLayerResolutionDefault(), layer1));
        Grid g2 = Grid.getGridStandardized(GridCutter.getLayerPath(AlaspatialProperties.getLayerResolutionDefault(), layer2));

        double minx = Math.max(g1.xmin, g2.xmin);
        double maxx = Math.min(g1.xmax, g2.xmax);
        double miny = Math.max(g1.ymin, g2.ymin);
        double maxy = Math.min(g1.ymax, g2.ymax);

        float[] d1 = g1.getGrid();
        float[] d2 = g2.getGrid();

        int count = 0;
        double sum = 0;
        double v1, v2;

        for (double x = minx + g1.xres / 2.0; x < maxx; x += g1.xres) {
            for (double y = miny + g1.yres / 2.0; y < maxy; y += g1.xres) {
                v1 = d1[g1.getcellnumber(x, y)];
                v2 = d2[g2.getcellnumber(x, y)];
                if (!Double.isNaN(v1) && !Double.isNaN(v2)) {
                    count++;
                    sum += java.lang.Math.abs(v1 - v2);
                }
            }
        }
        return sum / count;
    }
}

class ToDiskThread extends Thread {
    String filename;
    LinkedBlockingQueue<String> toDisk;

    public ToDiskThread(String filename, LinkedBlockingQueue<String> toDisk) {
        this.filename = filename;
        this.toDisk = toDisk;
    }

    public void run() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(filename, true);
            try {
                while(true) {
                    String s = toDisk.take();
                    fw.append(s);
                    fw.append("\n");
                    fw.flush();
                }
            } catch (InterruptedException e) {
            }
        } catch (IOException ex) {
            Logger.getLogger(ToDiskThread.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}