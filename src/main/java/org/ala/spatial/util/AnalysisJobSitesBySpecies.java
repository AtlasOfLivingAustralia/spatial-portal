/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.File;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.layers.Records;
import org.ala.spatial.analysis.layers.SitesBySpecies;

/**
 *
 * @author Adam
 */
public class AnalysisJobSitesBySpecies extends AnalysisJob {

    long[] stageTimes;
    String currentPath;
    String speciesq;
    double gridsize;

    LayerFilter[] envelope;
    SimpleRegion region;

    public AnalysisJobSitesBySpecies(String pid, String currentPath_, String speciesq, double gridsize, SimpleRegion region_, LayerFilter[] filter_) {
        super(pid);
        currentPath = currentPath_ + File.separator + pid + File.separator;
        new File(currentPath).mkdirs();
        this.speciesq = speciesq;
        region = region_;
        envelope = filter_;
        this.gridsize = gridsize;

        stageTimes = new long[2];
    }

    @Override
    public void run() {
        try {
            setCurrentState(RUNNING);
            setStage(0);

            double [] bbox = new double[4];
            bbox[0] = region.getBoundingBox()[0][0];
            bbox[1] = region.getBoundingBox()[0][1];
            bbox[2] = region.getBoundingBox()[1][0];
            bbox[3] = region.getBoundingBox()[1][1];

            // dump the species data to a file
            setProgress(0, "dumping species data");
            Records records = new Records(TabulationSettings.biocache_service, speciesq + "%20AND%20geospatial_kosher:true", bbox, /*currentPath + File.separator + "raw_data.csv"*/ null);

            setStage(1);

            setProgress(0, "building sites by species matrix for " + records.getSpeciesSize() + " species in " + records.getRecordsSize() + " occurrences");
            SitesBySpecies sbs = new SitesBySpecies(0,gridsize,bbox);
            sbs.write(records, currentPath + File.separator);

            setProgress(1, "finished");

            setCurrentState(SUCCESSFUL);

            System.out.println("finished building sites by species matrix");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed with exception: " + e.getMessage());
            setProgress(1, "failed: " + e.getMessage());
            setCurrentState(FAILED);
            setMessage("Error processing your Sites By Species request. Please try again or if problem persists, contact the Administrator\nPlease quote the Prediction ID: " + getName());
        }
    }

    @Override
    public long getEstimate() {
        if (getProgress() == 0) {
            return 0;
        }

        long progTime;
        synchronized (progress) {
            progTime = progressTime;
        }
        long timeRemaining = 0;
        long t1 = 0, t2 = 0, t3 = 0;

        if (stage <= 0) { //data load; 0 to 0.2
            t1 = t1 + progTime - stageTimes[0];
        }
        if (stage <= 1) { //running; 0.2 to 0.9
            if (stage == 1) {
                t2 = t2 + progTime - stageTimes[1];
            }
        }

        timeRemaining = t1 + t2 + t3;

        return timeRemaining; //smoothEstimate(timeRemaining);
    }

    @Override
    public void setProgress(double d) {
        if (stage == 0) { //data load; 0 to 0.2
            progress = d / 5.0;
        } else if (stage == 1) { //running; 0.2 to 0.9
            progress = 0.2 + 10 * d / 7.0;
        } else { //exporting/done
            progress = 0.9 + d / 10.0;
        }
        super.setProgress(progress);
    }

    @Override
    public double getProgress() {
        //return expected progress since cannot track internals

        long currentTime = System.currentTimeMillis();

        long progTime;
        synchronized (progress) {
            progTime = progressTime;
        }

        long t1 = 0, t2 = 0;
        double d1, d2;

        //progress is [time passed] / [time expected]
        if (stage <= 0) { //data load; 0 to 0.2
            t1 += 0;
            d1 = (currentTime - stageTimes[0]) / (double) t1;
            if (d1 > 0.9) {
                d1 = 0.9;
            }
            d1 *= 0.2; //range limit
        } else {
            d1 = 0.2;
        }
        if (stage <= 1) { //running; 0.2 to 0.9
            t2 += 0; //default
            if (stage == 1) {
                d2 = (currentTime - stageTimes[1]) / (double) t2;
            } else {
                d2 = 0;
            }
            if (d2 > 0.9) {
                d2 = 0.9;
            }
            d2 *= 0.7; //range limit
        } else {
            d2 = 0.7;
        }

        return d1 + d2;
    }

    @Override
    public String getStatus() {
        if (getProgress() < 1) {
            String msg;
            if (stage == 0) { //data load; 0 to 0.2
                msg = "Data preparation, ";
            } else if (stage == 1) { //seeding; 0.2 to 0.9
                msg = "Running, ";
            } else {    //transforming data; 0.9 to 1.0
                msg = "Exporting results, ";
            }
            return msg + "est remaining: " + getEstimateInMinutes() + " min";
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
    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getName());
        sb.append("; SitesBySpecies");
        sb.append("; state=").append(getCurrentState());
        sb.append("; status=").append(getStatus());
        sb.append("; resolution=").append(gridsize);
        sb.append("; speciesq=").append(speciesq);

        return sb.toString();
    }

    @Override
    public String getImage() {
        return "";
    }

    @Override
    AnalysisJob copy() {
        return new AnalysisJobSitesBySpecies(String.valueOf(System.currentTimeMillis()),
                currentPath, speciesq, gridsize, region, envelope);
    }
}
