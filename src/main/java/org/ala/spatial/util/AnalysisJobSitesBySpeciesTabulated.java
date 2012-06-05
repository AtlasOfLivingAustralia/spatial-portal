/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;
import org.ala.layers.client.Client;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.Layer;
import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.layers.Records;
import org.ala.spatial.analysis.layers.SitesBySpeciesTabulated;

/**
 *
 * @author Adam
 */
public class AnalysisJobSitesBySpeciesTabulated extends AnalysisJob {

    long[] stageTimes;
    String currentPath;
    String speciesq;
    String qname;
    double gridsize;
    LayerFilter[] envelope;
    SimpleRegion region;
    String biocacheserviceurl;
    String[] bioregions;
    boolean decade;
    String facetName;

    public AnalysisJobSitesBySpeciesTabulated(String pid, String currentPath_, String qname, String speciesq, double gridsize, SimpleRegion region_, LayerFilter[] filter_, String biocacheserviceurl, String[] bioregions, boolean decade, String facetName) {
        super(pid);
        currentPath = currentPath_ + File.separator + pid + File.separator;
        new File(currentPath).mkdirs();
        this.qname = qname;
        this.speciesq = speciesq;
        region = region_;
        envelope = filter_;
        this.gridsize = gridsize;

        stageTimes = new long[2];
        setStage(0);
        setProgress(0);

        this.biocacheserviceurl = biocacheserviceurl;

        this.bioregions = bioregions;
        this.decade = decade;
        this.facetName = facetName;
    }

    @Override
    public void run() {
        try {

            setCurrentState(RUNNING);
            setStage(0);

            double[] bbox = new double[4];
            if (region == null) {
                bbox[0] = -180;
                bbox[1] = -90;
                bbox[2] = 180;
                bbox[3] = 90;
            } else {
                bbox[0] = region.getBoundingBox()[0][0];
                bbox[1] = region.getBoundingBox()[0][1];
                bbox[2] = region.getBoundingBox()[1][0];
                bbox[3] = region.getBoundingBox()[1][1];
            }

            // dump the species data to a file
            setProgress(0, "getting species data");
            Records records = new Records(biocacheserviceurl/*TabulationSettings.biocache_service*/,
                    speciesq, bbox, /*currentPath + File.separator + "raw_data.csv"*/ null, region, facetName);

            //update bbox with spatial extent of records
            double minx = 180, miny = 90, maxx = -180, maxy = -90;
            for (int i = 0; i < records.getRecordsSize(); i++) {
                minx = Math.min(minx, records.getLongitude(i));
                maxx = Math.max(maxx, records.getLongitude(i));
                miny = Math.min(miny, records.getLatitude(i));
                maxy = Math.max(maxy, records.getLatitude(i));
            }
            minx -= gridsize;
            miny -= gridsize;
            maxx += gridsize;
            maxy += gridsize;
            bbox[0] = Math.max(bbox[0], minx);
            bbox[2] = Math.min(bbox[2], maxx);
            bbox[1] = Math.max(bbox[1], miny);
            bbox[3] = Math.min(bbox[3], maxy);

            //test restrictions
            int occurrenceCount = records.getRecordsSize();
            int boundingboxcellcount = (int) ((bbox[2] - bbox[0])
                    * (bbox[3] - bbox[1])
                    / (gridsize * gridsize));
            System.out.println("SitesBySpecies for " + occurrenceCount + " occurrences in up to " + boundingboxcellcount + " grid cells.");
            String error = null;
            /*if (boundingboxcellcount > AlaspatialProperties.getAnalysisLimitGridCells()) {
            error = "Too many potential output grid cells.  Decrease area or increase resolution.";
            } else if (occurrenceCount > AlaspatialProperties.getAnalysisLimitOccurrences()) {
            error = "Too many occurrences for the selected species.  " + occurrenceCount + " occurrences found, must be less than " + AlaspatialProperties.getAnalysisLimitOccurrences();
            } else*/ if (occurrenceCount == 0) {
                error = "No occurrences found";
            }
            if (error != null) {
                setProgress(1, "failed: " + error);
                setCurrentState(FAILED);
                System.out.println("SitesBySpecies ERROR: " + error);
                setMessage(error);
                return;
            }

            setStage(1);

            setProgress(0, "building sites by species matrix for " + records.getSpeciesSize() + " species in " + records.getRecordsSize() + " occurrences");

            String envelopeFile = AlaspatialProperties.getAnalysisWorkingDir() + "envelope_" + getName();
            Grid envelopeGrid = null;
            if (envelope != null) {
                GridCutter.makeEnvelope(envelopeFile, AlaspatialProperties.getLayerResolutionDefault(), envelope);
                envelopeGrid = new Grid(envelopeFile);
            }

            SitesBySpeciesTabulated sbs = new SitesBySpeciesTabulated(gridsize, bbox);
            int prog = 0;
            int max_regions = (bioregions == null ? 0 : bioregions.length) + (decade ? 2 : 0);
            if (bioregions != null) {
                for (String bioregion : bioregions) {
                    setProgress(prog / (double) max_regions, "producing table for " + bioregion);
                    try {
                        prog++;
                        SimpleShapeFile ssf = null;
                        Grid grid = null;
                        String[] gridColumns = null;
                        Layer layer = Client.getLayerDao().getLayerByName(bioregion);
                        Field f = Client.getFieldDao().getFieldById(org.ala.spatial.util.Layers.getFieldId(bioregion));
                        if (f.getType().equals("c")) {
                            ssf = new SimpleShapeFile(Client.getLayerIntersectDao().getConfig().getLayerFilesPath() + layer.getPath_orig(), f.getSname());
                        } else {  //must be a or b
                            try {
                                Properties p = new Properties();
                                p.load(new FileReader(Client.getLayerIntersectDao().getConfig().getLayerFilesPath() + layer.getPath_orig() + ".txt"));
                                ArrayList<String> cols = new ArrayList<String>();
                                cols.add("n/a");    //unmatched value
                                for (Entry e : p.entrySet()) {
                                    String key = (String) e.getKey();
                                    if (key.length() > 0) {
                                        int k = Integer.parseInt(key) + 1;
                                        while (cols.size() <= k) {
                                            cols.add("n/a");
                                        }
                                        cols.set(k, (String) e.getValue());
                                    }
                                }
                                gridColumns = new String[cols.size()];
                                cols.toArray(gridColumns);
                                grid = new Grid(Client.getLayerIntersectDao().getConfig().getLayerFilesPath() + layer.getPath_orig());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        sbs.write(records, currentPath + File.separator, region, envelopeGrid, layer.getDisplayname(), ssf, grid, gridColumns, false);
                    } catch (Exception e) {
                        log("failed for " + bioregion);
                        e.printStackTrace();
                    }
                }
            }


            setProgress(prog / (double) max_regions, "producing tables for decades");
            sbs.write(records, currentPath + File.separator, region, envelopeGrid, null, null, null, null, true);

            setProgress(1, "finished");

            // generate the readme.txt file
            //CitationService.generateSitesBySpeciesReadme(currentPath + File.separator, sitesbyspecies, occurrencedensity, speciesdensity);

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
        return new AnalysisJobSitesBySpeciesTabulated(String.valueOf(System.currentTimeMillis()),
                currentPath, qname, speciesq, gridsize, region, envelope,
                biocacheserviceurl, bioregions, decade, facetName);
    }

    void writeMetadata(String filename, String title, Records records, double[] bbox, boolean odensity, boolean sdensity, int[] counts, String addAreaSqKm) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        FileWriter fw = new FileWriter(filename);
        fw.append("<html><h1>").append(title).append("</h1>");
        fw.append("<table>");
        fw.append("<tr><td>Date/time " + sdf.format(new Date()) + "</td></tr>");
        fw.append("<tr><td>Model reference number: " + getName() + "</td></tr>");
        fw.append("<tr><td>Species selection " + qname + "</td></tr>");
        fw.append("<tr><td>Grid: " + 1 + "x" + 1 + " moving average, resolution " + gridsize + " degrees</td></tr>");

        fw.append("<tr><td>" + records.getSpeciesSize() + " species</td></tr>");
        fw.append("<tr><td>" + records.getRecordsSize() + " occurrences</td></tr>");
        if (counts != null) {
            fw.append("<tr><td>" + counts[0] + " grid cells with an occurrence</td></tr>");
            fw.append("<tr><td>" + counts[1] + " grid cells in the area (both marine and terrestrial)</td></tr>");
        }

        fw.append("<tr><td>bounding box of the selected area " + bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3] + "</td></tr>");
        if (odensity) {
            fw.append("<tr><td><br>Occurrence Density</td></tr>");
            fw.append("<tr><td><img src='occurrence_density.png' width='300px' height='300px'><img src='occurrence_density_legend.png'></td></tr>");
        }
        if (sdensity) {
            fw.append("<tr><td><br>Species Richness</td></tr>");
            fw.append("<tr><td><img src='species_richness.png' width='300px' height='300px'><img src='species_richness_legend.png'></td></tr>");
        }
        fw.append("</table>");
        fw.append("</html>");
        fw.close();
    }
}
