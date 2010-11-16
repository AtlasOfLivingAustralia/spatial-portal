/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.analysis.heatmap.HeatMap;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;
import org.apache.commons.io.FileUtils;

/**
 * prebuild heat maps
 *
 * 1. for lsid's with > 5000 points in the region
 * 2. for other indexes with > 5000 points in the region
 *
 * Loads up whole index so it needs an appropriate -Xmx.
 *
 * usage: java -Xmx12000m org.ala.spatial.analysis.index.HeatMapIndex <cutoff>
 *
 * where <cutoff> is the minimum number of occurrences in the heat map region
 * before the heat map is generated.
 *
 * @author Adam
 */
public class HeatMapIndex {

    public static void main(String[] args) {
        if(args.length > 0 && args[0].equalsIgnoreCase("delete")){
            TabulationSettings.load();
            //delete any previous images
            cleanupHeatmapImages();
            System.out.println("deleted heatmap images");
        } else if(args.length > 0 && args[0].equalsIgnoreCase("build")){
            SpatialLogger.info("start");
            TabulationSettings.load();

            int threshold = 2000;
            try {
                threshold = Integer.parseInt(args[1]);
            } catch (Exception e) {
            }

            String area = "";
            area += "POLYGON((";
            area += "110.911 -44.778,";
            area += "110.911 -9.221,";
            area += "156.113 -9.221,";
            area += "156.113 -44.778,";
            area += "110.911 -44.778";
            area += "))";
            SimpleRegion region = SimpleShapeFile.parseWKT(area);

            OccurrencesIndex.loadIndexes();

            extraIndexes();

            LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<String>();
            CountDownLatch cdl = new CountDownLatch(OccurrencesIndex.single_index.length);

            //start producer threads
            LinkedBlockingQueue<IndexedRecord> lbqp = new LinkedBlockingQueue<IndexedRecord>();
            System.out.println(OccurrencesIndex.single_index.length);
            for (IndexedRecord r : OccurrencesIndex.single_index) {
                lbqp.add(r);
            }
            HeatMapIndexProducerThread[] hmipt = new HeatMapIndexProducerThread[TabulationSettings.analysis_threads];
            for (int i = 0; i < hmipt.length; i++) {
                hmipt[i] = new HeatMapIndexProducerThread(lbqp, lbq, threshold, cdl, region);
                hmipt[i].start();
            }

            //start consumer threads
            HeatMapIndexThread[] hmit = new HeatMapIndexThread[TabulationSettings.analysis_threads];
            for (int i = 0; i < hmit.length; i++) {
                hmit[i] = new HeatMapIndexThread(lbq, cdl, region);
                hmit[i].start();
            }

            try {
                cdl.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = 0; i < hmit.length; i++) {
                hmit[i].interrupt();
            }

            for (int i = 0; i < hmipt.length; i++) {
                hmipt[i].interrupt();
            }

            SpatialLogger.info("finished");
        }else {
            System.out.println("usage: java -Xmx15000m org.ala.spatial.analysis.index.HeatMapIndex <command> \n\n<command> is one of:\n\tdelete\n\t\tdelete all previously generated heatmaps\n\n\tbuild <threshold>\n\t\trebuild heatmaps where number of occurrences \n\t\tin AU region is > threshold (default threshold=2000)\n");
        }        
    }

    private static void cleanupHeatmapImages() {
        String pth = TabulationSettings.base_output_dir + "output" + File.separator + "sampling" + File.separator;
        File dir = new File(pth);
        String[] f = dir.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith("png");
            }
        });
        if (f != null) {
            for (int i = 0; i < f.length; i++) {
                FileUtils.deleteQuietly(new File(pth + f[i]));
            }
        }
    }

    private static class HeatMapIndexThread extends Thread {

        LinkedBlockingQueue<String> lbq;
        CountDownLatch cdl;
        SimpleRegion region;

        private HeatMapIndexThread(LinkedBlockingQueue<String> lbq_, CountDownLatch cdl_, SimpleRegion region_) {
            lbq = lbq_;
            cdl = cdl_;
            region = region_;
            setPriority(MIN_PRIORITY);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String lsid = lbq.take();
                    process(lsid);
                    cdl.countDown();
                }
            } catch (InterruptedException ie) {
            } catch (Exception e) {
                cdl.countDown();
                e.printStackTrace();
            }
        }

        private void process(String lsid) {
            long start = System.currentTimeMillis();
            String currentPath = TabulationSettings.base_output_dir;
            File baseDir = new File(currentPath + "output" + File.separator + "sampling" + File.separator);

            if (!lsid.equalsIgnoreCase("")) {
                String outputfile = baseDir + File.separator + lsid.replace(":", "_") + ".png";

                SamplingService ss = SamplingService.newForLSID(lsid);
                double[] points = ss.sampleSpeciesPoints(lsid, region, null);

                String value = lsid.replace(":", "_");

                //FileUtils.deleteQuietly(new File(baseDir.getAbsolutePath() + File.separator + value + ".png"));
                //FileUtils.deleteQuietly(new File(baseDir.getAbsolutePath() + File.separator + "legend_" + value + ".png"));

                HeatMap hm = new HeatMap(baseDir, value);
                hm.generateClasses(points);
                hm.drawOuput(outputfile, true);

                long end = System.currentTimeMillis();
                System.out.println("L," + (points.length/2) + "," + (end - start));
            }
        }
    }

    private static class HeatMapIndexProducerThread extends Thread {

        LinkedBlockingQueue<IndexedRecord> lbqp;
        LinkedBlockingQueue<String> lbq;
        CountDownLatch cdl;
        SimpleRegion region;
        int threshold;

        private HeatMapIndexProducerThread(LinkedBlockingQueue<IndexedRecord> lbqp_, LinkedBlockingQueue<String> lbq_, int threshold_, CountDownLatch cdl_, SimpleRegion region_) {
            lbqp = lbqp_;
            lbq = lbq_;
            threshold = threshold_;
            cdl = cdl_;
            region = region_;
            setPriority(MIN_PRIORITY);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    IndexedRecord r = lbqp.take();
                    process(r);
                }
            } catch (InterruptedException ie) {
            } catch (Exception e) {
                cdl.countDown();    //to be safe, it will skip a build
                e.printStackTrace();
            }
        }

        private void process(IndexedRecord r) {            
            if (r.record_end - r.record_start >= threshold) {
                SamplingService ss = SamplingService.newForLSID(r.name);
                double[] p = ss.sampleSpeciesPoints(r.name, region, null);
                if (p != null && (p.length / 2) > threshold) {
                    try{
                        lbq.put(r.name);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                } else {
                    cdl.countDown();
                }
            } else {
                cdl.countDown();
            }
        }
    }

    static void extraIndexes() {
        String pth = TabulationSettings.base_output_dir + "output" + File.separator + "sampling" + File.separator;
        File baseDir = new File(pth);
        String[] lookups = OccurrencesIndex.listLookups();
        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<String>();
        for (int i = 0; i < lookups.length && i < OccurrencesIndex.extra_indexes.length; i++) {
            System.out.println("extra index: " + lookups[i] + " size=" + OccurrencesIndex.extra_indexes[i].keySet().size());
            for (String value : OccurrencesIndex.extra_indexes[i].keySet()) {
                if(!value.contains("\\")){ //filter out occurrences if N\A
                    lbq.add(lookups[i] + "|" + value);
                }
            }
        }
        System.out.println("total extras count: " + lbq.size());
        CountDownLatch cdl = new CountDownLatch(lbq.size());

        ExtraThread [] et = new ExtraThread[TabulationSettings.analysis_threads];
        for(int i=0;i<et.length;i++){
            et[i] = new ExtraThread(baseDir,lbq,cdl);
            et[i].start();
        }

        try{
            cdl.await();
            }catch(Exception e){
                e.printStackTrace();
            }

        System.out.println("done extra indexes.");
        
        for(int i=0;i<et.length;i++){
            et[i].interrupt();
        }
    }

    private static class ExtraThread extends Thread {

        LinkedBlockingQueue<String> lbq;
        CountDownLatch cdl;
        File baseDir;

        private ExtraThread(File baseDir_, LinkedBlockingQueue<String> lbq_, CountDownLatch cdl_) {
            lbq = lbq_;
            cdl = cdl_;
            baseDir = baseDir_;
            setPriority(MIN_PRIORITY);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String s = lbq.take();
                    String [] p = s.split("\\|");
                    process(baseDir, p[0], p[1]);
                    cdl.countDown();
                }
            } catch (InterruptedException ie) {
            } catch (Exception e) {
                cdl.countDown();
                e.printStackTrace();
            }
        }

        void process(File baseDir, String key, String value) {
            long start = System.currentTimeMillis();
            String outputfile = baseDir + File.separator + value + ".png";

            int[] recs = OccurrencesIndex.lookup(key, value);
            //System.out.println("[" + key + "," + value + "," + ((recs == null)?0:recs.length) + "]");
            if (recs != null && recs.length > 500) {

                int[] finalRecs = recs;

                double[][] pts = OccurrencesIndex.getPointsPairs();

                double[] points = new double[finalRecs.length * 2];
                for (int i = 0; i < finalRecs.length * 2; i += 2) {
                    points[i] = pts[finalRecs[i / 2]][0];
                    points[i + 1] = pts[finalRecs[i / 2]][1];
                }

                //FileUtils.deleteQuietly(new File(baseDir.getAbsolutePath() + File.separator + value + ".png"));
                //FileUtils.deleteQuietly(new File(baseDir.getAbsolutePath() + File.separator + "legend_" + value + ".png"));

                HeatMap hm = new HeatMap(baseDir, value);

                hm.generateClasses(points);
                hm.drawOuput(outputfile, true);

                long end = System.currentTimeMillis();
                System.out.println("E," + recs.length + "," + (end - start));
            }
        }
    }
}
