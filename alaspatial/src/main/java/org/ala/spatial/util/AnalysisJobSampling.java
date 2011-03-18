/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.SamplingService;

/**
 *
 * @author Adam
 */
public class AnalysisJobSampling extends AnalysisJob {

    String[] layers;
    int numberOfGroups;
    SimpleRegion region;
    LayerFilter[] envelope;
    String species;
    ArrayList<OccurrenceRecordNumbers> records;
    long[] stageTimes;
    String envlist;
    String area;
    String currentPath;
    int points;

    public AnalysisJobSampling(String pid, String currentPath_, String species_, String envlist_, String area_) {
        super(pid);
        species = species_;
        currentPath = currentPath_;
        envlist = envlist_;
        area = area_;

        layers = getLayerFiles(envlist);
        records = null;
        region = null;
        if (area != null && area.startsWith("ENVELOPE")) {
            records = FilteringService.getRecords(area);
        } else {
            region = SimpleShapeFile.parseWKT(area);
        }

        //TODO: update for new occurrencescollection
        //points = OccurrencesService.getSpeciesCount(species_);

        stageTimes = new long[4];
    }

    @Override
    public void run() {
        try {
            long start = System.currentTimeMillis();

            setCurrentState(RUNNING);
            setStage(0);

            SpatialSettings ssets;
            ssets = new SpatialSettings();

            SamplingService ss = SamplingService.newForLSID(species);
            String datafile = ss.sampleSpeciesAsCSV(species, layers, region, records, ssets.getInt("max_record_count_download"), this);

            Vector<String> vFiles = new Vector<String>();
            vFiles.add(datafile);

            String[] files = (String[]) vFiles.toArray(new String[vFiles.size()]);

            Iterator it = vFiles.iterator();
            while (it.hasNext()) {
                System.out.println("Adding to download: " + it.next());
            }

            String outputpath = currentPath + File.separator + "output" + File.separator + "sampling" + File.separator;
            File fDir = new File(outputpath);
            fDir.mkdir();

            String[] n = OccurrencesCollection.getFirstName(species);
            String speciesName = "";
            if (n != null) {
                speciesName = n[0];
            }
            String outfile = fDir.getAbsolutePath() + File.separator + speciesName.replaceAll(" ", "_") + "_sample_" + getName() + ".zip";
            Zipper.zipFiles(files, outfile);

            //return "/output/sampling/" + species.replaceAll(" ", "_") + "_sample_" + getName() + ".zip";

            long end = System.currentTimeMillis();
            setRunTime(end - start);

            setCurrentState(SUCCESSFUL);

            //write out infor for adjusting input parameters
            System.out.println("Sampling:" + " " + (end - start));
        } catch (Exception e) {
            setProgress(1, "failed: " + e.getMessage());
            setCurrentState(FAILED);
            e.printStackTrace();
        }
    }

    @Override
    public long getEstimate() {
        if (getProgress() == 0) {
            return 0;
        }

        long timeElapsed;
        long t1 = 0;
        double prog;
        synchronized (progress) {
            timeElapsed = progressTime - stageTimes[getStage()];
            prog = progress;
        }
        long timeRemaining = 0;


        if (prog > 0) {
            t1 = (long) (timeElapsed * (.2 - prog) / prog); //projected
        }
        if (t1 <= 0 || prog <= 0) {
            t1 = (long) (1000); //default
        }

        timeRemaining = t1;
        return smoothEstimate(timeRemaining);
    }

    @Override
    public String getStatus() {
        if (getProgress() < 1) {
            return "est remaining: " + getEstimateInMinutes() + " min";
        } else {
            if (stage == -1) {
                return "not started, est: " + getEstimateInMinutes() + " min";
            } else {
                return "finished, total run time=" + Math.round(getRunTime() / 1000) + "s";
            }
        }
    }

    @Override
    public void setStage(int i) {
        super.setStage(i);

        if (i < 4) {
            stageTimes[i] = System.currentTimeMillis();
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getName());
        sb.append("; Sampling");
        sb.append("; state=").append(getCurrentState());
        sb.append("; status=").append(getStatus());
        sb.append("; species=").append(species);
        //sb.append("; layers list=").append(envlist);
        //sb.append("; area=").append(area);

        return sb.toString();
    }

    private String[] getLayerFiles(String envNames) {
        if (envNames.equals("none")) {
            return null;
        }
        String[] nameslist = envNames.split(":");
        String[] pathlist = new String[nameslist.length];

        //System.out.println("Got envlist.count: " + nameslist.length);

        for (int j = 0; j < nameslist.length; j++) {
            pathlist[j] = Layers.layerDisplayNameToName(nameslist[j]);

        }

        return pathlist;
    }
}
