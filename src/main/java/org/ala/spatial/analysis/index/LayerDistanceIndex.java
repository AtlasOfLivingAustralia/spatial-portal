/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.layers.client.Client;
import org.ala.layers.intersect.Grid;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.GridCutter;

/**
 * Generates inter layer association distances for analysis environmental grid
 * files.
 *
 * Operates on layers-store prepared analysis grid files.
 *
 * Path to analysis grid files is set by layer.dir in alaspatial.properties.
 *
 * @author Adam
 */
public class LayerDistanceIndex {

    /**
     * Filename of the layer distances store.
     *
     * Actual file is located in workingdir set in alaspatial.properties.
     */
    final public static String LAYER_DISTANCE_FILE = "layerDistances.properties";

    /**
     *
     * @param threadcount number of threads to run analysis.
     * @param onlyThesePairs array of distances to run as fieldId1 + " " +
     * fieldId2 where fieldId1.compareTo(fieldId2) &lt 0 or null for all missing
     * distances.
     * @throws InterruptedException
     */
    public void occurrencesUpdate(int threadcount, String[] onlyThesePairs) throws InterruptedException {

        //create distances file if it does not exist.
        File layerDistancesFile = new File(AlaspatialProperties.getAnalysisWorkingDir()
                + LAYER_DISTANCE_FILE);
        if (!layerDistancesFile.exists()) {
            try {
                FileWriter fw = new FileWriter(layerDistancesFile);
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Map<String, Double> map = loadDistances();

        LinkedBlockingQueue<String> todo = new LinkedBlockingQueue();

        if (onlyThesePairs != null && onlyThesePairs.length > 0) {
            for (String s : onlyThesePairs) {
                todo.add(s);
            }
        } else {
            //find all environmental layer analysis files
            File root = new File(AlaspatialProperties.getAnalysisLayersDir());
            File[] dirs = root.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return pathname != null && pathname.isDirectory();
                }
            });

            HashMap<String, String> domains = new HashMap<String, String>();
            for (File dir : dirs) {
                //iterate through files so we get everything
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

                        //only operate on file names that are valid fields
                        if (Client.getFieldDao().getFieldById(file1) != null
                                && Client.getFieldDao().getFieldById(file2) != null) {

                            String domain1 = domains.get(file1);
                            if (domain1 == null) {
                                String pid1 = Client.getFieldDao().getFieldById(file1).getSpid();
                                domain1 = Client.getLayerDao().getLayerById(Integer.parseInt(pid1)).getdomain();
                                domains.put(file1, domain1);
                            }
                            String domain2 = domains.get(file2);
                            if (domain2 == null) {
                                String pid2 = Client.getFieldDao().getFieldById(file2).getSpid();
                                domain2 = Client.getLayerDao().getLayerById(Integer.parseInt(pid2)).getdomain();
                                domains.put(file2, domain2);
                            }

                            String key = (file1.compareTo(file2) < 0) ? file1 + " " + file2 : file2 + " " + file1;

                            //domain test
                            if (isSameDomain(parseDomain(domain1), parseDomain(domain2))) {
                                if (!map.containsKey(key) && !todo.contains(key)) {
                                    todo.put(key);
                                }
                            }
                        }
                    }
                }
            }
        }

        LinkedBlockingQueue<String> toDisk = new LinkedBlockingQueue<String>();
        CountDownLatch cdl = new CountDownLatch(todo.size());
        CalcThread[] threads = new CalcThread[threadcount];
        for (int i = 0; i < threadcount; i++) {
            threads[i] = new CalcThread(cdl, todo, toDisk);
            threads[i].start();
        }

        ToDiskThread toDiskThread = new ToDiskThread(AlaspatialProperties.getAnalysisWorkingDir()
                + LAYER_DISTANCE_FILE, toDisk);
        toDiskThread.start();

        cdl.await();

        for (int i = 0; i < threadcount; i++) {
            threads[i].interrupt();
        }

        toDiskThread.interrupt();
    }

    /**
     * Entry to start updating a specific or all missing layer distances.
     *
     * @param args
     * @throws InterruptedException
     */
    static public void main(String[] args) throws InterruptedException {
        System.out.println("args[0] = threadcount, e.g. 1");
        System.out.println("or");
        System.out.println("args[0] = threadcount, e.g. 1");
        System.out.println("args[1] = list of layer pairs to rerun, e.g. el813_el814,el813_el815,el814_el815");
        if (args.length < 1) {
            args = new String[]{"1"};//, "el1030_el1036","el1030_el775,el1036_el775"};
        }
        String[] pairs = null;
        if (args.length >= 2) {
            pairs = args[1].replace("_", " ").split(",");
        }
        LayerDistanceIndex ldi = new LayerDistanceIndex();
        ldi.occurrencesUpdate(Integer.parseInt(args[0]), pairs);
    }

    /**
     * Get all available inter layer association distances.
     *
     * @return Map of all available distances as Map&ltString, Double&gt. The
     * key String is fieldId1 + " " + fieldId2 where fieldId1 &lt fieldId2.
     */
    static public Map<String, Double> loadDistances() {
        Map<String, Double> map = new ConcurrentHashMap<String, Double>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(AlaspatialProperties.getAnalysisWorkingDir()
                    + LAYER_DISTANCE_FILE));

            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    String[] keyvalue = line.split("=");
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
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return map;
    }

    /**
     * Convert a domains string to a list of domains.
     *
     * @param domain comma separated domain list as String.
     * @return array of domains as String [].
     */
    static String[] parseDomain(String domain) {
        if (domain == null || domain.length() == 0) {
            return null;
        }
        String[] domains = domain.split(",");
        for (int i = 0; i < domains.length; i++) {
            domains[i] = domains[i].trim();
        }
        return domains;
    }

    /**
     * Test if two domain arrays contain a common domain.
     *
     * @param domain1 list of domains as String []
     * @param domain2 list of domains as String []
     * @return true iff the domains overlap.
     */
    static boolean isSameDomain(String[] domain1, String[] domain2) {
        if (domain1 == null || domain2 == null) {
            return true;
        }

        for (String s1 : domain1) {
            for (String s2 : domain2) {
                if (s1.equalsIgnoreCase(s2)) {
                    return true;
                }
            }
        }

        return false;
    }
}

// layer distance calculation thread.
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
            while (true) {
                String key = lbq.take();
                String[] layers = key.split(" ");

                try {
                    Double distance = calculateDistance(layers[0], layers[1]);
                    toDisk.put(key + "=" + distance);
                    System.out.println(key + "=" + distance);
                } catch (Exception e) {
                    System.out.println(key + ":error");
                    e.printStackTrace(System.out);
                }

                cdl.countDown();
            }
        } catch (InterruptedException e) {
        }
    }

    /**
     * Calculate the distance between two layers. Default analysis layer
     * resolution is used if available.
     *
     * @param layer1 fieldId of the first layer to compare as String.
     * @param layer2 fieldId of the second layer to compare as String.
     * @return distance between the two layers as Double in the range &gt= 0 and
     * &lt=1.
     */
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

//appends to the LayerDistanceIndex.LAYER_DISTANCE_FILE file.
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
                while (true) {
                    String s = toDisk.take();
                    fw.append(s + "\n");
                    fw.flush();
                }
            } catch (Exception e) {
                //This is expected to occur after all distances are
                //calculated and the calling of interrupt.  
                //Might as well attempt to finish up.
                try {
                    while (toDisk.size() > 0) {
                        String s = toDisk.take();
                        fw.append(s + "\n");
                        fw.flush();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
