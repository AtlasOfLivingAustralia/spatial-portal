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
package org.ala.layers.grid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Adam
 */
public class GridCacheReader {

    ArrayList<GridGroup> groups;

    public GridCacheReader(String directory) {
        groups = new ArrayList<GridGroup>();

        if (directory != null) {
            File dir = new File(directory);
            if (dir != null && dir.exists() && dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    try {
                        if (f.getName().endsWith(".txt")) {
                            GridGroup g = new GridGroup(f.getPath());
                            groups.add(g);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public HashMap<String, Float> sample(double longitude, double latitude) throws IOException {
        HashMap<String, Float> map = new HashMap<String, Float>();
        for (GridGroup g : groups) {
            map.putAll(g.sample(longitude, latitude));
        }
        return map;
    }

    public static void main(String[] args) {
        //args = new String[] {"d:\\timing_test_sampling_diva_cache.csv", ""e:\\layers\\ready\\diva_cache"};
        System.out.println("Test sampling on a grid cache with random points.\n\nargs[0] = output test results\nargs[1] = ready/diva_cache path");
        try {
            FileWriter fw = new FileWriter(args[0]);
            for (int i = 1; i < 5000; i += 10) {
                long t = largerTest(i, args[1]);
                fw.append(String.valueOf(i)).append(",").append(String.valueOf(t)).append("\n");
            }
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //smallerTest();

        System.exit(0);
    }

    static void smallerTest(String diva_cache_path) {
        try {
            HashMap<String, Float> map = new GridCacheReader(diva_cache_path).sample(130, -22);

            for (String k : map.keySet()) {
                System.out.println(k + " > " + map.get(k));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static long largerTest(int size, String diva_cache_path) {
        try {
            ArrayList<Double> points = new ArrayList<Double>(2000);
            Random r = new Random(System.currentTimeMillis());
            for (int i = 0; i < size; i++) {
                points.add(r.nextDouble() * 40 + 110); //longitude
                points.add(r.nextDouble() * 30 - 40); //latitude
            }

            int threadCount = 100;
            final LinkedBlockingQueue<GridCacheReader> lbqReaders = new LinkedBlockingQueue<GridCacheReader>();
            final LinkedBlockingQueue<List<Double>> lbqPoints = new LinkedBlockingQueue<List<Double>>();
            Collection<Callable<ArrayList<HashMap<String, Float>>>> tasks = new ArrayList<Callable<ArrayList<HashMap<String, Float>>>>();

            int pos = 0;
            int step = points.size() / threadCount;
            if (step % 2 == 1) {
                step--;
            }
            for (int i = 0; i < threadCount; i++) {
                lbqReaders.add(new GridCacheReader(diva_cache_path));
                if (i == threadCount - 1) {
                    step = points.size();
                }
                lbqPoints.add(points.subList(pos, Math.min(points.size(), pos + step)));
                pos += step;

                tasks.add(new Callable<ArrayList<HashMap<String, Float>>>() {

                    public ArrayList<HashMap<String, Float>> call() throws Exception {
                        GridCacheReader gcr = lbqReaders.take();
                        List<Double> points = lbqPoints.take();

                        ArrayList<HashMap<String, Float>> list = new ArrayList<HashMap<String, Float>>();

                        for (int i = 0; i < points.size(); i += 2) {
                            HashMap<String, Float> map = gcr.sample(points.get(i), points.get(i + 1));
                            map.put("longitude", points.get(i).floatValue());
                            map.put("latitude", points.get(i + 1).floatValue());
                            list.add(map);
                        }

                        return list;
                    }
                });
            }

            System.out.println("starting...");
            long start = System.currentTimeMillis();

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            List<Future<ArrayList<HashMap<String, Float>>>> output = executorService.invokeAll(tasks);

            long end = System.currentTimeMillis() - start;
            System.out.println("sampling time " + end + "ms for " + points.size() / 2);

            return end;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static ArrayList<Double> loadPoints(String filename) {
        ArrayList<Double> points = new ArrayList<Double>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                if (line.length() > 0 && s.length == 2) {
                    try {
                        double latitude = Double.parseDouble(s[0]);
                        double longitude = Double.parseDouble(s[1]);
                        points.add(longitude);
                        points.add(latitude);
                    } catch (Exception e) {
                    }
                }
            }
            br.close();
        } catch (Exception e) {
        }
        return points;
    }

    public ArrayList<String> getFileNames() {
        ArrayList<String> fileNames = new ArrayList<String>();
        for (int i = 0; i < groups.size(); i++) {
            fileNames.addAll(groups.get(i).files);
        }
        return fileNames;
    }

    public void updateNames(String fileName, String name) {
        for (int i = 0; i < groups.size(); i++) {
            ArrayList<String> files = groups.get(i).files;
            for (int j = 0; j < files.size(); j++) {
                if (files.get(j).equals(fileName)) {
                    groups.get(i).names.set(j, name);
                    return;
                }
            }
        }
    }

    public int getGroupCount() {
        return groups.size();
    }
}
