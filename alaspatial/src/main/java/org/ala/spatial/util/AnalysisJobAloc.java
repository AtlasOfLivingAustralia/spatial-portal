/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.service.AlocService;
import org.ala.spatial.analysis.service.LayerImgService;

/**
 *
 * @author Adam
 */
public class AnalysisJobAloc extends AnalysisJob {

    Layer[] layers;
    int numberOfGroups;
    SimpleRegion region;
    LayerFilter[] envelope;
    String filename;
    String filepath;
    String currentPath;
    int cells;
    long[] stageTimes;
    public String area;

    public AnalysisJobAloc(String pid, String currentpath_, Layer[] layers_, int numberofgroups_, SimpleRegion region_, LayerFilter[] envelope_, String area_) {
        super(pid);
        currentPath = currentpath_;
        numberOfGroups = numberofgroups_;
        layers = layers_;
        region = region_;
        envelope = envelope_;
        area = area_;

        //TODO: remove rough estimate
        if (region != null) {
            cells = (int) Math.ceil((region.getWidth() / TabulationSettings.grd_xdiv)
                    * (region.getHeight() / TabulationSettings.grd_ydiv));
        } else {
            cells = 1000000; //or something
        }
        //cells = GridCutter.countCells(region, envelope);

        stageTimes = new long[4];
    }

    @Override
    public void run() {
        try {


            long start = System.currentTimeMillis();

            setCurrentState(RUNNING);

            setStage(0);

            filepath = currentPath + "output" + File.separator + "aloc" + File.separator + getName() + File.separator;
            filename = filepath + "aloc.png";
            File fDir = new File(filepath);
            fDir.mkdir();

            AlocService.run(filename, layers, numberOfGroups, region, envelope, getName(), this);

            if (isCancelled()) {
                return;
            }

            exportResults();

            long end = System.currentTimeMillis();
            setRunTime(end - start);

            setCurrentState(SUCCESSFUL);

            //write out infor for adjusting input parameters
            System.out.println("ALOC:" + cells + "," + numberOfGroups + "," + layers.length + " " + (stageTimes[1] - stageTimes[0]) + " " + (stageTimes[2] - stageTimes[0]) + " " + (stageTimes[3] - stageTimes[2]) + " " + (end - stageTimes[3]));
        } catch (Exception e) {
            setProgress(1, "failed: " + e.toString());
            setCurrentState(FAILED);
            System.out.println("ALOC ERROR");
            e.printStackTrace();
        }
    }

    void exportResults() {
        setStage(3); //exporting results

        String line;
        String outputfile_orig = filename;

        try {
            //get extents from aloc run
            StringBuffer extents = new StringBuffer();
            BufferedReader br = new BufferedReader(new FileReader(outputfile_orig + "extents.txt"));
            int width = Integer.parseInt(br.readLine());
            int height = Integer.parseInt(br.readLine());
            double xmin = Double.parseDouble(br.readLine());
            double ymin = Double.parseDouble(br.readLine());
            double xmax = Double.parseDouble(br.readLine());
            double ymax = Double.parseDouble(br.readLine());
            br.close();
            br = new BufferedReader(new FileReader(outputfile_orig + "extents.txt"));
            while ((line = br.readLine()) != null) {
                extents.append(line).append("\n");
            }
            br.close();
            setProgress(0.2);

            CoordinateTransformer.generateWorldFiles(filepath, "aloc",
                    String.valueOf((xmax - xmin) / (double) width),
                    "-" + String.valueOf((ymax - ymin) / (double) height),
                    String.valueOf(xmin),
                    String.valueOf(ymax));
            setProgress(0.3);

            String outputfile = CoordinateTransformer.transformToGoogleMercator(outputfile_orig);
            System.out.println("OUT2: " + outputfile);
            setProgress(0.4);

            /* register with LayerImgService */
            StringBuffer legend = new StringBuffer();
            System.out.println("legend path:" + filepath + "classification_means.csv");
            BufferedReader flegend = new BufferedReader(new FileReader(filepath + "classification_means.csv"));
            while ((line = flegend.readLine()) != null) {
                legend.append(line);
                legend.append("\r\n");
            }
            flegend.close();

            StringBuffer metadata = new StringBuffer();
            System.out.println("meatadata path:" + filepath + "classification.html");
            BufferedReader fmetadata = new BufferedReader(new FileReader(filepath + "classification.html"));
            while ((line = fmetadata.readLine()) != null) {
                metadata.append(line);
                metadata.append("\n");
            }
            fmetadata.close();

            // generate the readme.txt file
            CitationService.generateClassificationReadme(filepath);

            setProgress(0.5);

            System.out.println("registering layer image (A): pid=" + getName());
            if (!LayerImgService.registerLayerImage(currentPath, "" + getName(), outputfile, extents.toString(), legend.toString(), metadata.toString())) {
                //error
            }
            setProgress(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getEstimate() {
        //if(getProgress() == 0) return 0;

        long timeElapsed;
        long t1 = 0, t2 = 0, t3 = 0, t4 = 0;
        double prog;
        long progTime;
        synchronized (progress) {
            progTime = progressTime;
            timeElapsed = progressTime - stageTimes[getStage()];
            prog = progress;
        }
        long timeRemaining = 0;

        if (stage <= 0) { //data load; 0 to 0.2
            if (prog > 0.2) {
                t1 = (long) (timeElapsed * (.2 - prog) / prog); //projected
            }
            if (t1 <= 0 || prog <= 0.2) {
                t1 = (long) (cells * TabulationSettings.aloc_timing_0 * layers.length
                        + TabulationSettings.aloc_timing_1); //default
            }
        }
        if (stage <= 1) { //seeding; 0.2 to 0.3
            if (prog > 0.22) {
                //t2 = (long) (timeElapsed * (.1 - (prog-.2))/(prog-.2)) ;   //projected
                t2 = (long) ((cells * TabulationSettings.aloc_timing_2) * (double) layers.length * numberOfGroups
                        + TabulationSettings.aloc_timing_3); //default
                t2 = t2 + progTime - stageTimes[1];
            }
            if (t2 <= 0 || prog <= 0.22) {
                t2 = (long) ((cells * TabulationSettings.aloc_timing_2) * (double) layers.length * numberOfGroups
                        + TabulationSettings.aloc_timing_3); //default
            }
        }
        if (stage <= 2) { //iterations; 0.3 to 0.9
            if (prog > 0.3) {
                //t3 = (long) (timeElapsed  * (.6 - (prog-.3))/(prog-.3));   //projected

                t3 = (long) ((cells * TabulationSettings.aloc_timing_4) * (double) numberOfGroups * layers.length * layers.length
                        + TabulationSettings.aloc_timing_5); //default
                t3 = t3 + progTime - stageTimes[2];
            }
            if (t3 <= 0 || prog <= 0.3) {
                t3 = (long) ((cells * TabulationSettings.aloc_timing_4) * (double) numberOfGroups * layers.length * layers.length
                        + TabulationSettings.aloc_timing_5); //default
            }
        }
        if (stage <= 3) { //transforming data; 0.9 to 1.0
            if (prog > 0.9) {
                //t4 = (long) (timeElapsed  * (.1 - (prog-.9))/(prog-.9)); //projected
                t4 = (long) (2000 * TabulationSettings.aloc_timing_6); //default
                t4 = t4 + progTime - stageTimes[3];
            }
            if (t4 <= 0 || prog <= 0.9) {
                t4 = (long) (2000 * TabulationSettings.aloc_timing_6); //default
            }
        }

        timeRemaining = t1 + t2 + t3 + t4;
        return smoothEstimate(timeRemaining);
    }

    @Override
    public void setProgress(double d) {
        if (stage == 0) { //data load; 0 to 0.2
            progress = d / 5;
        } else if (stage == 1) { //seeding; 0.2 to 0.3
            progress = 0.2 + d / 10;
        } else if (stage == 2) { //iterations; 0.3 to 0.9
            progress = 0.3 + d * 6 / 10;
        } else {    //transforming data; 0.9 to 1.0
            progress = 0.9 + d / 10;
        }
        super.setProgress(progress);
    }

    @Override
    public String getStatus() {
        if (getProgress() < 1) {
            String msg;
            if (stage == 0) { //data load; 0 to 0.2
                msg = "Data preparation, ";
            } else if (stage == 1) { //seeding; 0.2 to 0.3
                msg = "Seed generation, ";
            } else if (stage == 2) { //iterations; 0.3 to 0.9
                msg = "Iterations, ";
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

    public void setCells(int i) {
        cells = i;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getName());
        sb.append("; Classification");
        sb.append("; state=").append(getCurrentState());
        sb.append("; status=").append(getStatus());
        sb.append("; grid cell count=").append(cells);
        sb.append("; number of groups=").append(numberOfGroups);
        sb.append("; number of layers=").append(layers.length);

        return sb.toString();
    }

    public String getImage() {
        return "output/aloc/" + getName() + "/t_aloc.png";
    }

    AnalysisJob copy() {
        return new AnalysisJobAloc(String.valueOf(System.currentTimeMillis()),
                currentPath, layers, numberOfGroups, region, envelope, area);
    }
}
