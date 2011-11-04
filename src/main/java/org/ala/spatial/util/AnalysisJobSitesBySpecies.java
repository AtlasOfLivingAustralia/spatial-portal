/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.layers.OccurrenceDensity;
import org.ala.spatial.analysis.layers.Records;
import org.ala.spatial.analysis.layers.SitesBySpecies;
import org.ala.spatial.analysis.layers.SpeciesDensity;
import org.ala.spatial.analysis.legend.Legend;
import org.ala.spatial.analysis.legend.LegendEqualArea;

/**
 *
 * @author Adam
 */
public class AnalysisJobSitesBySpecies extends AnalysisJob {

    long[] stageTimes;
    String currentPath;
    String speciesq;
    String qname;
    double gridsize;
    boolean sitesbyspecies, occurrencedensity, speciesdensity;

    LayerFilter[] envelope;
    SimpleRegion region;

    public AnalysisJobSitesBySpecies(String pid, String currentPath_, String qname, String speciesq, double gridsize, SimpleRegion region_, LayerFilter[] filter_, boolean sitesbyspecies, boolean occurrencedensity, boolean speciesdensity) {
        super(pid);
        currentPath = currentPath_ + File.separator + pid + File.separator;
        new File(currentPath).mkdirs();
        this.qname = qname;
        this.speciesq = speciesq;
        region = region_;
        envelope = filter_;
        this.gridsize = gridsize;

        stageTimes = new long[2];

        this.sitesbyspecies = sitesbyspecies;
        this.occurrencedensity = occurrencedensity;
        this.speciesdensity = speciesdensity;
    }

    @Override
    public void run() {
        try {
            SpatialSettings ssets = new SpatialSettings();

            setCurrentState(RUNNING);
            setStage(0);

            double [] bbox = new double[4];
            bbox[0] = region.getBoundingBox()[0][0];
            bbox[1] = region.getBoundingBox()[0][1];
            bbox[2] = region.getBoundingBox()[1][0];
            bbox[3] = region.getBoundingBox()[1][1];

            // dump the species data to a file
            setProgress(0, "getting species data");
            Records records = new Records(TabulationSettings.biocache_service, speciesq + "%20AND%20geospatial_kosher:true", bbox, /*currentPath + File.separator + "raw_data.csv"*/ null);

            setStage(1);

            setProgress(0, "building sites by species matrix for " + records.getSpeciesSize() + " species in " + records.getRecordsSize() + " occurrences");

            if(sitesbyspecies) {
                SitesBySpecies sbs = new SitesBySpecies(0,gridsize,bbox);
                sbs.write(records, currentPath + File.separator);
            }

            Legend occurrencesLegend = null;
            if(occurrencedensity) {
                setProgress(0.3, "building occurrence density layer");
                OccurrenceDensity od = new OccurrenceDensity(9,gridsize,bbox);
                od.write(records, currentPath + File.separator, "occurrence_density");

                writeProjectionFile(currentPath + File.separator + "occurrence_density.prj");
                Hashtable htGeoserver = ssets.getGeoserverSettings();
                // if generated successfully, then add it to geoserver
                String url = (String) htGeoserver.get("geoserver_url") + "/rest/workspaces/ALA/coveragestores/odensity_" + getName() + "/file.arcgrid?coverageName=odensity_" + getName();
                String extra = "";
                String username = (String) htGeoserver.get("geoserver_username");
                String password = (String) htGeoserver.get("geoserver_password");
                // first zip up the file as it's going to be sent as binary
                //String ascZipFile = Zipper.zipFile(msets.getOutputPath() + "species.asc");
                String[] infiles = {currentPath + "occurrence_density.asc", currentPath + "occurrence_density.prj"};
                String ascZipFile = currentPath + "occurrence_density.zip";
                Zipper.zipFiles(infiles, ascZipFile);
                // Upload the file to GeoServer using REST calls
                System.out.println("Uploading file: " + ascZipFile + " to \n" + url);
                UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);
                
                //sld
                occurrencesLegend = produceSld(currentPath + File.separator + "occurrence_density");
                UploadSpatialResource.loadSld(url, extra, username, password, currentPath + File.separator + "occurrence_density.sld");
                //geoserver/rest/styles/add_nrm_style
                url = (String) htGeoserver.get("geoserver_url") + "/rest/styles/";
                UploadSpatialResource.loadCreateStyle(url, extra, username, password, "odensity_" + getName());
                url = (String) htGeoserver.get("geoserver_url") + "/rest/styles/odensity_" + getName();
                UploadSpatialResource.loadSld(url, extra, username, password, currentPath + File.separator + "occurrence_density.sld");
                //Apply style
                String data = "<layer><enabled>true</enabled><defaultStyle><name>odensity_" + getName() + "</name></defaultStyle></layer>";
                url = (String) htGeoserver.get("geoserver_url") + "/rest/layers/ALA:odensity_" + getName();
                UploadSpatialResource.assignSld(url, extra, username, password, data);

                occurrencesLegend.generateLegend(currentPath + File.separator + "occurrence_density_legend.png");
            }

            Legend speciesLegend = null;
            if(speciesdensity) {
                setProgress(0.6, "building species richness layer");
                SpeciesDensity sd = new SpeciesDensity(9,gridsize,bbox);
                sd.write(records, currentPath + File.separator, "species_richness");

                writeProjectionFile(currentPath + File.separator + "species_richness.prj");
                Hashtable htGeoserver = ssets.getGeoserverSettings();
                // if generated successfully, then add it to geoserver
                String url = (String) htGeoserver.get("geoserver_url") + "/rest/workspaces/ALA/coveragestores/srichness_" + getName() + "/file.arcgrid?coverageName=srichness_" + getName();
                String extra = "";
                String username = (String) htGeoserver.get("geoserver_username");
                String password = (String) htGeoserver.get("geoserver_password");
                // first zip up the file as it's going to be sent as binary
                //String ascZipFile = Zipper.zipFile(msets.getOutputPath() + "species.asc");
                String[] infiles = {currentPath + "species_richness.asc", currentPath + "species_richness.prj"};
                String ascZipFile = currentPath + "species_richness.zip";
                Zipper.zipFiles(infiles, ascZipFile);
                // Upload the file to GeoServer using REST calls
                System.out.println("Uploading file: " + ascZipFile + " to \n" + url);
                UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);

                //sld
                speciesLegend = produceSld(currentPath + File.separator + "species_richness");
                //geoserver/rest/styles/add_nrm_style
                url = (String) htGeoserver.get("geoserver_url") + "/rest/styles/";
                UploadSpatialResource.loadCreateStyle(url, extra, username, password, "srichness_" + getName());
                url = (String) htGeoserver.get("geoserver_url") + "/rest/styles/srichness_" + getName();
                UploadSpatialResource.loadSld(url, extra, username, password, currentPath + File.separator + "species_richness.sld");
                //Apply style
                String data = "<layer><enabled>true</enabled><defaultStyle><name>srichness_" + getName() + "</name></defaultStyle></layer>";
                url = (String) htGeoserver.get("geoserver_url") + "/rest/layers/ALA:srichness_" + getName();
                UploadSpatialResource.assignSld(url, extra, username, password, data);

                speciesLegend.generateLegend(currentPath + File.separator + "species_richness_legend.png");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            FileWriter fw = new FileWriter(currentPath + File.separator + "metadata.html");
            fw.append("<html><h1>Sites by species</h1>");
            fw.append("<table>");
            fw.append("<tr><td>Species selection " + qname + "</td></tr>");
            fw.append("<tr><td>Grid resolution " + gridsize + " degrees</td></tr>");
            fw.append("<tr><td>" + records.getSpeciesSize() + " species</td></tr>");
            fw.append("<tr><td>" + records.getRecordsSize() + " occurrences</td></tr>");
            fw.append("<tr><td>bounding box of the selected area " + bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3] + "</td></tr>");
            fw.append("<tr><td>Date/time " + sdf.format(new Date()) + "</td></tr>");
            if(occurrencedensity) {
                fw.append("<tr><td><br>Occurrence Density</td></tr><tr><td><img src='occurrence_density.jpg' width='300px' height='300px'></td><td><img src='occurrence_density_legend.png'></td></tr>");
            }
            if(speciesdensity) {
                fw.append("<tr><td><br>Species Richness</td></tr><tr><td><img src='species_richness.jpg' width='300px' height='300px'></td><td><img src='species_richness_legend.png'></td></tr>");
            }
            fw.append("</table>");
            fw.append("</html>");
            fw.close();

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
                currentPath, qname, speciesq, gridsize, region, envelope,
                sitesbyspecies, occurrencedensity, speciesdensity);
    }

    private void writeProjectionFile(String outputpath_prj) {
        try {
            FileWriter fw = new FileWriter(outputpath_prj);

            fw.append("GEOGCS[\"WGS 84\", ").append("\n");
            fw.append("    DATUM[\"WGS_1984\", ").append("\n");
            fw.append("        SPHEROID[\"WGS 84\",6378137,298.257223563, ").append("\n");
            fw.append("            AUTHORITY[\"EPSG\",\"7030\"]], ").append("\n");
            fw.append("        AUTHORITY[\"EPSG\",\"6326\"]], ").append("\n");
            fw.append("    PRIMEM[\"Greenwich\",0, ").append("\n");
            fw.append("        AUTHORITY[\"EPSG\",\"8901\"]], ").append("\n");
            fw.append("    UNIT[\"degree\",0.01745329251994328, ").append("\n");
            fw.append("        AUTHORITY[\"EPSG\",\"9122\"]], ").append("\n");
            fw.append("    AUTHORITY[\"EPSG\",\"4326\"]] ").append("\n");

            fw.close();
        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing prj file:");
            ex.printStackTrace(System.out);
        }
    }

    Legend produceSld(String gridfilename) {
        Grid g = new Grid(gridfilename);
        float[] d = g.getGrid();
        java.util.Arrays.sort(d);
        
        Legend legend = new LegendEqualArea();
        legend.generate(d);
        legend.determineGroupSizes(d);
        legend.evaluateStdDevArea(d);

        //must 'unsort' d
        d = null;
        g = null;
        System.gc();
        g = new Grid(gridfilename);
        d = g.getGrid();
        legend.exportImage(d, g.ncols, gridfilename + ".jpg", 1);
        legend.exportSLD(g, gridfilename + ".sld","", false);

        return legend;
    }
}
