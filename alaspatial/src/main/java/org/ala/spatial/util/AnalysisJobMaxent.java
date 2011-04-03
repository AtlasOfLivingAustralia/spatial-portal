/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Hashtable;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.maxent.MaxentServiceImpl;
import org.ala.spatial.analysis.maxent.MaxentSettings;
import org.ala.spatial.analysis.service.SamplingService;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

/**
 *
 * @author Adam
 */
public class AnalysisJobMaxent extends AnalysisJob {

    long[] stageTimes;
    String currentPath;
    String taxon;
    String area;
    String envlist;
    String txtTestPercentage;
    String chkJackknife;
    String chkResponseCurves;
    LayerFilter[] envelope;
    SimpleRegion region;
    int cells;
    Layer[] layers;
    int speciesCount;

    public AnalysisJobMaxent(String pid, String currentPath_, String taxon_, String envlist_, SimpleRegion region_, LayerFilter[] filter_, Layer[] layers_, String txtTestPercentage_, String chkJackknife_, String chkResponseCurves_) {
        super(pid);
        currentPath = currentPath_;
        taxon = taxon_;
        region = region_;
        envelope = filter_;
        layers = layers_;
        txtTestPercentage = txtTestPercentage_;
        chkJackknife = chkJackknife_;
        chkResponseCurves = chkResponseCurves_;
        envlist = envlist_;

        //TODO: remove rough estimate
        if (region != null) {
            cells = (int) Math.ceil(region.getWidth() / TabulationSettings.grd_xdiv
                    * region.getHeight() / TabulationSettings.grd_ydiv);
        } else {
            cells = 1000000; //or something
        }
        //cells = GridCutter.countCells(region, envelope);

        SamplingService ss = SamplingService.newForLSID(taxon);
        double[] p = ss.sampleSpeciesPoints(taxon, region, null);
        if (p != null) {
            speciesCount = p.length / 2;
        }
        stageTimes = new long[4];
    }

    @Override
    public void run() {
        SpatialSettings ssets = new SpatialSettings();

        try {
            setCurrentState(RUNNING);
            setStage(0);

            // dump the species data to a file
            setProgress(0, "dumping species data");

            SamplingService ss = SamplingService.newForLSID(taxon);

            StringBuffer removedSpecies = new StringBuffer();
            double[] points = ss.sampleSpeciesPointsMinusSensitiveSpecies(taxon, region, null, removedSpecies);

            StringBuffer sbSpecies = new StringBuffer();
            // get the header
            sbSpecies.append("species, longitude, latitude");
            sbSpecies.append(System.getProperty("line.separator"));
            for (int i = 0; i < points.length; i += 2) {
                sbSpecies.append("species, " + points[i] + ", " + points[i + 1]);
                sbSpecies.append(System.getProperty("line.separator"));
            }

            setProgress(0, "preparing input files and run parameters");

            String[] envnameslist = envlist.split(":");
            String[] envpathlist = getEnvFiles(envlist);

            String cutDataPath = ssets.getEnvDataPath();

            cutDataPath = GridCutter.cut(layers, region, envelope, null);

            System.out.println("CUTDATAPATH: " + region + " " + cutDataPath);

            MaxentSettings msets = new MaxentSettings();
            msets.setMaxentPath(ssets.getMaxentCmd());
            msets.setEnvList(Arrays.asList(envpathlist));
            msets.setRandomTestPercentage(Integer.parseInt(txtTestPercentage));
            msets.setEnvPath(cutDataPath);          //use (possibly) cut layers
            msets.setEnvVarToggler("world");
            msets.setSpeciesFilepath(setupSpecies(sbSpecies.toString(), currentPath + "output" + File.separator + "maxent" + File.separator + getName() + File.separator));
            msets.setOutputPath(currentPath + "output" + File.separator + "maxent" + File.separator + getName() + File.separator);
            if (chkJackknife != null) {
                msets.setDoJackknife(true);
            }
            if (chkResponseCurves != null) {
                msets.setDoResponsecurves(true);
            }

            MaxentServiceImpl maxent = new MaxentServiceImpl();
            maxent.setMaxentSettings(msets);

            System.out.println("To run: " + msets.toString());

            setStage(1);

            setProgress(0, "running Maxent");

            int exitValue = maxent.process(this);
            System.out.println("Completed: " + exitValue);

            setProgress(1, "Maxent finished with exit value=" + exitValue);

            setStage(2);

            setProgress(0, "exporting results");

            Hashtable htProcess = new Hashtable();

            String[] imgExtensions = {".png", "_only.png", "_only_thumb.png", "_thumb.png"};

            if (isCancelled()) {
                //
            } else if (exitValue == 0) {
                // rename the env filenames to their display names
                String pth_plots = currentPath + "output" + File.separator + "maxent" + File.separator + getName() + File.separator + "plots" + File.separator;
                String pth = currentPath + "output" + File.separator + "maxent" + File.separator + getName() + File.separator;
//                for (int ei = 0; ei < envnameslist.length; ei++) {
//                    readReplace(pth + "species.html", ".*?\\b"+envpathlist[ei]+"\\b.*?", Layers.layerNameToDisplayName(envnameslist[ei].replace(" ", "_")) + "("+envnameslist[ei]+")" );
//                    for (int j = 0; j < imgExtensions.length; j++) {
//                        try {
//                            FileUtils.moveFile(
//                                    new File(pth_plots + "species_" + envpathlist[ei] + imgExtensions[j]),
//                                    new File(pth_plots + "species_" + Layers.layerNameToDisplayName(envnameslist[ei].replace(" ", "_")) + "("+envnameslist[ei]+")" + imgExtensions[j]));
//                        } catch (Exception ex) {
//                        }
//                    }
//                }

//                //remove species image path in output species.html
//                readReplaceBetween(pth + "species.html", "<HR><H2>Pictures of the model", "<HR>","<HR>");
//                readReplace(pth + "species.html", "<a href = \"plots/\"> <img src=\"plots/\" width=600></a>", "see map window");
//                readReplace(pth + "species.html", "plots\\\\", "plots/");
//
//                readReplace(pth + "species.html", "<a href = \"species_samplePredictions.csv\">The prediction strength at the training and (optionally) test presence sites</a><br>", "");
                String input = getInputs();
                String sciname = input.substring(input.indexOf("scientificName:") + 15, input.indexOf(";", input.indexOf("scientificName:") + 15));
                String scirank = input.substring(input.indexOf("taxonRank:") + 10, input.indexOf(";", input.indexOf("taxonRank:") + 10));
                readReplace(pth + "species.html", "Maxent model for species", "Maxent model for " + sciname);

                String paramlist = "Model reference number: " + getName()
                        + "<br>Species: " + sciname + " (" + scirank + ")" + "<br>Layers: <ul>";
                for (int ei = 0; ei < envnameslist.length; ei++) {
                    paramlist += "<li>" + Layers.layerNameToDisplayName(envnameslist[ei].replace(" ", "_")) + " (" + envpathlist[ei] + ")</li>";
                }
                paramlist += "</ul>";

                readReplace(pth + "species.html", "end of this page.<br>", "end of this page.<br><p>" + paramlist + "</p>");
                readReplace(pth + "species.html", msets.getOutputPath(), "");
                readReplaceBetween(pth + "species.html", "Command line", "<br>", "");
                readReplaceBetween(pth + "species.html", "Command line", "<br>", "");

                readReplaceBetween(pth + "species.html", "<br>Click <a href=species_explain.bat", "memory.<br>", "");
                readReplaceBetween(pth + "species.html", "(A link to the Explain", "additive models.)", "");

                if (removedSpecies.length() > 0) {
                    String header = "'Sensitive species' have been masked out of the model. See: http://www.ala.org.au/about/program-of-projects/sds/\r\n\r\nLSID,Species scientific name,Taxon rank";
                    writeToFile(header + removedSpecies.toString(),
                            currentPath + "output" + File.separator + "maxent" + File.separator + getName() + File.separator + "maskedOutSensitiveSpecies.csv");

                    String insertBefore = "<a href = \"species.asc\">The";
                    String insertText = "<b><a href = \"maskedOutSensitiveSpecies.csv\">'Sensitive species' masked out of the model</a></br></b>";
                    readReplace(pth + "species.html", insertBefore, insertText + insertBefore);
                }

//                //delete image
//                FileUtils.deleteQuietly(new File(pth_plots + "species.png"));
//                FileUtils.deleteQuietly(new File(pth + "species_samplePredictions.csv"));
//                FileUtils.deleteQuietly(new File(pth + "maxent.log"));
//                FileUtils.deleteQuietly(new File(msets.getSpeciesFilepath()));

                writeProjectionFile(msets.getOutputPath());

                Hashtable htGeoserver = ssets.getGeoserverSettings();

                // if generated successfully, then add it to geoserver
                String url = (String) htGeoserver.get("geoserver_url") + "/rest/workspaces/ALA/coveragestores/maxent_" + getName() + "/file.arcgrid?coverageName=species_" + getName();
                String extra = "";
                String username = (String) htGeoserver.get("geoserver_username");
                String password = (String) htGeoserver.get("geoserver_password");

                // first zip up the file as it's going to be sent as binary
                //String ascZipFile = Zipper.zipFile(msets.getOutputPath() + "species.asc");
                String[] infiles = {msets.getOutputPath() + "species.asc", msets.getOutputPath() + "species.prj"};
                String ascZipFile = msets.getOutputPath() + "species.zip";
                Zipper.zipFiles(infiles, ascZipFile);

                // Upload the file to GeoServer using REST calls
                System.out.println("Uploading file: " + ascZipFile + " to \n" + url);
                UploadSpatialResource.loadResource(url, extra, username, password, ascZipFile);

                htProcess.put("status", "success"); ///
                htProcess.put("pid", getName());
                htProcess.put("info", "/output/maxent/" + getName() + "/species.html");

                setStage(3);

                setProgress(1, "finished");

                setCurrentState(SUCCESSFUL);

                //write out infor for adjusting input parameters
                System.out.println("MAXENT:" + cells + "," + layers.length + " " + speciesCount + " " + (stageTimes[1] - stageTimes[0]) + " " + (stageTimes[2] - stageTimes[0]) + " " + (stageTimes[3] - stageTimes[2]));
            } else {
                setProgress(1, "failed");
                setCurrentState(FAILED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setProgress(1, "failed: " + e.getMessage());
            setCurrentState(FAILED);
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
            t1 += (cells * TabulationSettings.maxent_timing_0) * layers.length; //default
            t1 = t1 + progTime - stageTimes[0];
        }
        if (stage <= 1) { //running; 0.2 to 0.9            
            t2 += (cells * TabulationSettings.maxent_timing_1) * layers.length; //default
            if (stage == 1) {
                t2 = t2 + progTime - stageTimes[1];
            }
        }
        if (stage > 1) { //data export + done
            t3 += 5000 * TabulationSettings.maxent_timing_2; //default
            if (stage == 2) {
                t3 = t3 + progTime - stageTimes[2];
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

        long t1 = 0, t2 = 0, t3 = 0;
        double d1, d2, d3;

        //progress is [time passed] / [time expected]
        if (stage <= 0) { //data load; 0 to 0.2
            t1 += (cells * TabulationSettings.maxent_timing_0) * layers.length; //default
            d1 = (currentTime - stageTimes[0]) / (double) t1;
            if (d1 > 0.9) {
                d1 = 0.9;
            }
            d1 *= 0.2; //range limit
        } else {
            d1 = 0.2;
        }
        if (stage <= 1) { //running; 0.2 to 0.9
            t2 += (cells * TabulationSettings.maxent_timing_1) * layers.length; //default
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
        if (stage > 1) { //data export + done
            t3 += 5000 * TabulationSettings.maxent_timing_2; //default
            if (stage == 2) {
                d3 = (currentTime - stageTimes[2]) / (double) t3;
            } else {
                d3 = 0;
            }
            if (d3 > 0.9) {
                d3 = 0.9;
            }
            d3 *= 0.1; //range limit
        } else {
            d3 = 0.1;
        }

        return d1 + d2 + d3;
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

    public void setCells(int i) {
        cells = i;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getName());
        sb.append("; Maxent");
        sb.append("; state=").append(getCurrentState());
        sb.append("; status=").append(getStatus());
        sb.append("; grid cell count=").append(cells);
        sb.append("; number of layers=").append(layers.length);

        return sb.toString();
    }

    private String[] getEnvFiles(String envNames) {
        String[] nameslist = envNames.split(":");
        String[] pathlist = new String[nameslist.length];

        for (int j = 0; j < nameslist.length; j++) {
            pathlist[j] = Layers.layerDisplayNameToName(nameslist[j]);
        }

        return pathlist;
    }

    public void readReplace(String fname, String oldPattern, String replPattern) {
        String line;
        StringBuffer sb = new StringBuffer();
        try {
            FileInputStream fis = new FileInputStream(fname);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll(oldPattern, replPattern);
                sb.append(line + "\n");
            }
            reader.close();
            BufferedWriter out = new BufferedWriter(new FileWriter(fname));
            out.write(sb.toString());
            out.close();
        } catch (Throwable e) {
            System.err.println("*** exception ***");
            e.printStackTrace(System.out);
        }
    }

    public void readReplaceBetween(String fname, String startOldText, String endOldText, String replText) {
        String line;
        StringBuffer sb = new StringBuffer();
        try {
            FileInputStream fis = new FileInputStream(fname);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            int start, end;
            start = sb.indexOf(startOldText);
            if (start >= 0) {
                end = sb.indexOf(endOldText, start + 1);
                sb.replace(start, end + endOldText.length(), replText);
            }
            reader.close();
            BufferedWriter out = new BufferedWriter(new FileWriter(fname));
            out.write(sb.toString());
            out.close();
        } catch (Throwable e) {
            System.err.println("*** exception ***");
            e.printStackTrace(System.out);
        }
    }

    private String setupSpecies(String speciesList, String outputpath) {
        try {
            File fDir = new File(outputpath);
            fDir.mkdir();

            File spFile = File.createTempFile("points_", ".csv", fDir);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            //spWriter.write("spname, longitude, latitude \n");
            spWriter.write(speciesList);
            spWriter.close();

            return spFile.getAbsolutePath();
        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }

        return null;
    }

    private void writeToFile(String text, String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.append(text);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeProjectionFile(String outputpath) {
        try {
            File fDir = new File(outputpath);
            fDir.mkdir();

            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputpath + "species.prj")));

            StringBuffer sbProjection = new StringBuffer();
            sbProjection.append("GEOGCS[\"WGS 84\", ").append("\n");
            sbProjection.append("    DATUM[\"WGS_1984\", ").append("\n");
            sbProjection.append("        SPHEROID[\"WGS 84\",6378137,298.257223563, ").append("\n");
            sbProjection.append("            AUTHORITY[\"EPSG\",\"7030\"]], ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"6326\"]], ").append("\n");
            sbProjection.append("    PRIMEM[\"Greenwich\",0, ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"8901\"]], ").append("\n");
            sbProjection.append("    UNIT[\"degree\",0.01745329251994328, ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"9122\"]], ").append("\n");
            sbProjection.append("    AUTHORITY[\"EPSG\",\"4326\"]] ").append("\n");

            //spWriter.write("spname, longitude, latitude \n");
            spWriter.write(sbProjection.toString());
            spWriter.close();

        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }
    }

    public String getImage() {
        return "output/maxent/" + getName() + "/plots/species_hidden.png";
    }

    AnalysisJob copy() {
        return new AnalysisJobMaxent(String.valueOf(System.currentTimeMillis()),
                currentPath, taxon, envlist, region, envelope, layers,
                txtTestPercentage, chkJackknife, chkResponseCurves);
    }
}
