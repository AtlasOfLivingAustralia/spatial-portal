package org.ala.spatial.web.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.maxent.MaxentServiceImpl;
import org.ala.spatial.analysis.maxent.MaxentSettings;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.util.AnalysisJobMaxent;
import org.ala.spatial.util.AnalysisQueue;
import org.ala.spatial.util.GridCutter;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.UploadSpatialResource;
import org.ala.spatial.util.Zipper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/maxent/")
public class MaxentWSController {

    private SpatialSettings ssets;

    @RequestMapping(value = "/process", method = RequestMethod.GET)
    public
    @ResponseBody
    String process(HttpServletRequest req) {

        try {

            TabulationSettings.load(); 

            long currTime = System.currentTimeMillis();

            String currentPath = TabulationSettings.base_output_dir;

            String taxon = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");

            ssets = new SpatialSettings();

            // dump the species data to a file
            System.out.println("dumping species data");

            SamplingService ss = SamplingService.newForLSID(taxon);
            double [] points = ss.sampleSpeciesPointsSensitive(taxon, null, null);
            StringBuffer sbSpecies = new StringBuffer();
            // get the header
            sbSpecies.append("species, longitude, latitude");
            sbSpecies.append(System.getProperty("line.separator"));
            for(int i=0;i<points.length;i+=2){
                sbSpecies.append("species, " + points[i] + ", " + points[i+1]);
                sbSpecies.append(System.getProperty("line.separator"));
            }

            String envlist = req.getParameter("envlist");
            String[] envnameslist = envlist.split(":");
            String[] envpathlist = getEnvFiles(envlist);


            MaxentSettings msets = new MaxentSettings();
            //msets.setMaxentPath((String) session.getAttribute("maxentCmdPath"));
            msets.setMaxentPath(ssets.getMaxentCmd());
            //msets.setEnvList(Arrays.asList(envsel));
            //msets.setEnvList(Arrays.asList(req.getParameterValues("envsel")));
            msets.setEnvList(Arrays.asList(envpathlist));
            msets.setRandomTestPercentage(Integer.parseInt(req.getParameter("txtTestPercentage")));
            msets.setEnvPath(ssets.getEnvDataPath());
            msets.setEnvVarToggler("world");
            msets.setSpeciesFilepath(setupSpecies(sbSpecies.toString(), currentPath + "output" + File.separator + "maxent" + File.separator + currTime + File.separator));
            msets.setOutputPath(currentPath + "output" + File.separator + "maxent" + File.separator + currTime + File.separator);
            if (req.getParameter("chkJackknife") != null) {
                msets.setDoJackknife(true);
            }
            if (req.getParameter("chkResponseCurves") != null) {
                msets.setDoResponsecurves(true);
            }

            System.out.println("To run: " + msets.toString());


            MaxentServiceImpl maxent = new MaxentServiceImpl();
            maxent.setMaxentSettings(msets);
            int exitValue = maxent.process();

            System.out.println("Completed: " + exitValue);

            Hashtable htProcess = new Hashtable();
            if (exitValue == 0) {
                // TODO: Should probably move this part an external "parent"
                // function so can be used by other functions
                //

                // rename the env filenames to their display names
                for (int ei = 0; ei < envnameslist.length; ei++) {
                    readReplace(currentPath + "output" + File.separator + "maxent" + File.separator + currTime + File.separator + "species.html", envpathlist[ei], envnameslist[ei]);
                }

                writeProjectionFile(msets.getOutputPath());

                Hashtable htGeoserver = ssets.getGeoserverSettings();

                // if generated successfully, then add it to geoserver
                String url = (String) htGeoserver.get("geoserver_url") + "/rest/workspaces/ALA/coveragestores/maxent_" + currTime + "/file.arcgrid?coverageName=species_" + currTime;
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

                //extraout += "status: success"; ///  \n <br />
                //extraout += "file: " + "output/maxent/" + currTime + "/species.asc \n <br />";
                //extraout += "info: " + "output/maxent/" + currTime + "/species.html \n <br />";
                //extraout += "map: " + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + currTime + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers";
                //extraout += "";

                //mapframe.setSrc((String) htGeoserver.get("geoserver_url") + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + currTime + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers");
                //infoframe.setSrc("/output/maxent/" + currTime + "/species.html");
                //outputtab.setVisible(true);

                htProcess.put("status", "success"); ///
                htProcess.put("pid", currTime);
                htProcess.put("info", "/output/maxent/" + currTime + "/species.html");
                //htProcess.put("","");
                //htProcess.put("","");

                /*
                StringWriter sw = new StringWriter();

                JsonFactory f = new JsonFactory();
                JsonGenerator g = f.createJsonGenerator(sw);
                g.writeObject(htProcess);
                g.close();

                System.out.println("sw: \n" + sw.toString());
                 *
                 */


                //return "status:success;pid:" + currTime + ";info:"+"/output/maxent/" + currTime + "/species.html";

                return "status:success;pid:" + currTime + ";info:" + "/output/maxent/" + currTime + "/species.html";


            } else {
                //extraout += "Status: failure\n";
                //htProcess.put("status", "failure");
                return "status:failure;";

            }

        } catch (Exception e) {
            System.out.println("Error processing Maxent request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

    @RequestMapping(value = "/processgeo", method = RequestMethod.POST)
    public
    @ResponseBody
    String processgeo(HttpServletRequest req) {

        try {

            long currTime = System.currentTimeMillis();

            String currentPath = TabulationSettings.base_output_dir;

            String taxon = req.getParameter("taxonid");

            String envlist = req.getParameter("envlist");
            String[] envnameslist = envlist.split(":");
            String[] envpathlist = getEnvFiles(envlist);

            //handle cut layers
            String area = req.getParameter("area");
            System.out.println("MAXENT area:" + area);
            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = FilteringService.getFilters(req.getParameter("area"));
            } else {
                region = SimpleShapeFile.parseWKT(req.getParameter("area"));
            }
            String cutDataPath = ssets.getEnvDataPath();
            Layer [] layers = getEnvFilesAsLayers(req.getParameter("envlist"));
            cutDataPath = GridCutter.cut(layers, region, filter, null);

            System.out.println("CUTDATAPATH: " + region + " " + cutDataPath);

            ssets = new SpatialSettings();
            // dump the species data to a file
            System.out.println("dumping species data");
            SamplingService ss = SamplingService.newForLSID(taxon);
            double [] points = ss.sampleSpeciesPointsSensitive(taxon, region, null);
            StringBuffer sbSpecies = new StringBuffer();
            // get the header
            sbSpecies.append("species, longitude, latitude");
            sbSpecies.append(System.getProperty("line.separator"));
            for(int i=0;i<points.length;i+=2){
                sbSpecies.append("species, " + points[i] + ", " + points[i+1]);
                sbSpecies.append(System.getProperty("line.separator"));
            }

            MaxentSettings msets = new MaxentSettings();
            msets.setMaxentPath(ssets.getMaxentCmd());
            msets.setEnvList(Arrays.asList(envpathlist));
            msets.setRandomTestPercentage(Integer.parseInt(req.getParameter("txtTestPercentage")));
            msets.setEnvPath(cutDataPath);          //use (possibly) cut layers
            msets.setEnvVarToggler("world");
            msets.setSpeciesFilepath(setupSpecies(sbSpecies.toString(), currentPath + "output" + File.separator + "maxent" + File.separator + currTime + File.separator));
            msets.setOutputPath(currentPath + "output" + File.separator + "maxent" + File.separator + currTime + File.separator);
            if (req.getParameter("chkJackknife") != null) {
                msets.setDoJackknife(true);
            }
            if (req.getParameter("chkResponseCurves") != null) {
                msets.setDoResponsecurves(true);
            }

            System.out.println("To run: " + msets.toString());


            MaxentServiceImpl maxent = new MaxentServiceImpl();
            maxent.setMaxentSettings(msets);
            int exitValue = maxent.process();

            System.out.println("Completed: " + exitValue);

            Hashtable htProcess = new Hashtable();
            if (exitValue == 0) {
                // TODO: Should probably move this part an external "parent"
                // function so can be used by other functions
                //

                // rename the env filenames to their display names
                for (int ei = 0; ei < envnameslist.length; ei++) {
                    readReplace(currentPath + "output" + File.separator + "maxent" + File.separator + currTime + File.separator + "species.html", envpathlist[ei], envnameslist[ei]);
                }

                writeProjectionFile(msets.getOutputPath());

                Hashtable htGeoserver = ssets.getGeoserverSettings();

                // if generated successfully, then add it to geoserver
                String url = (String) htGeoserver.get("geoserver_url") + "/rest/workspaces/ALA/coveragestores/maxent_" + currTime + "/file.arcgrid?coverageName=species_" + currTime;
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

                //extraout += "status: success"; ///  \n <br />
                //extraout += "file: " + "output/maxent/" + currTime + "/species.asc \n <br />";
                //extraout += "info: " + "output/maxent/" + currTime + "/species.html \n <br />";
                //extraout += "map: " + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + currTime + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers";
                //extraout += "";

                //mapframe.setSrc((String) htGeoserver.get("geoserver_url") + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + currTime + "&styles=alastyles&bbox=112.0,-44.0,154.0,-9.0&width=700&height=500&srs=EPSG:4326&format=application/openlayers");
                //infoframe.setSrc("/output/maxent/" + currTime + "/species.html");
                //outputtab.setVisible(true);

                htProcess.put("status", "success"); ///
                htProcess.put("pid", currTime);
                htProcess.put("info", "/output/maxent/" + currTime + "/species.html");
                //htProcess.put("","");
                //htProcess.put("","");

                /*
                StringWriter sw = new StringWriter();

                JsonFactory f = new JsonFactory();
                JsonGenerator g = f.createJsonGenerator(sw);
                g.writeObject(htProcess);
                g.close();

                System.out.println("sw: \n" + sw.toString());
                 *
                 */


                //return "status:success;pid:" + currTime + ";info:"+"/output/maxent/" + currTime + "/species.html";

                return "status:success;pid:" + currTime + ";info:" + "/output/maxent/" + currTime + "/species.html";


            } else {
                //extraout += "Status: failure\n";
                //htProcess.put("status", "failure");
                return "status:failure;";

            }

        } catch (Exception e) {
            System.out.println("Error processing Maxent request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

    @RequestMapping(value = "/processgeoq", method = RequestMethod.POST)
    public
    @ResponseBody
    String processgeoq(HttpServletRequest req) {

        try {
            TabulationSettings.load();

            ssets = new SpatialSettings();

            long currTime = System.currentTimeMillis();

            String currentPath = TabulationSettings.base_output_dir;
            String taxon = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__",".");
            String taxonlsid = URLDecoder.decode(req.getParameter("taxonlsid"), "UTF-8").replace("__",".");
            String area = req.getParameter("area");
            String envlist = req.getParameter("envlist");
            String txtTestPercentage = req.getParameter("txtTestPercentage");
            String chkJackknife = req.getParameter("chkJackknife");
            String chkResponseCurves = req.getParameter("chkResponseCurves");

            Layer [] layers = getEnvFilesAsLayers(envlist);

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = FilteringService.getFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            String pid = Long.toString(currTime);
            AnalysisJobMaxent ajm = new AnalysisJobMaxent(pid, currentPath, taxon, envlist, region, filter, layers, txtTestPercentage,chkJackknife,chkResponseCurves);
            StringBuffer inputs = new StringBuffer();
            inputs.append("pid:").append(pid);
            inputs.append(";taxonid:").append(taxon);
            inputs.append(";taxonlsid:").append(taxonlsid);
            
            String [] n = OccurrencesCollection.getFirstName(taxonlsid);
            if(n != null){
                inputs.append(";scientificName:").append(n[0]);
                inputs.append(";taxonRank:").append(n[1]);
            }

            inputs.append(";area:").append(area);
            inputs.append(";envlist:").append(envlist);
            inputs.append(";txtTestPercentage:").append(txtTestPercentage);
            inputs.append(";chkJackknife:").append(chkJackknife);
            inputs.append(";chkResponseCurves:").append(chkResponseCurves);
            ajm.setInputs(inputs.toString());
            AnalysisQueue.addJob(ajm);

            return pid;

        } catch (Exception e) {
            System.out.println("Error processing Maxent request:");
            e.printStackTrace(System.out);
        }

        return "";

    }

    // Copies src file to dst file.
    // If the dst file does not exist, it is created
    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
        out.flush();
        in.close();
        out.close();
    }

    // Copies src file to dst file.
    // If the dst file does not exist, it is created
    private String copy(String speciesfile, String outputpath) throws IOException {
        File fDir = new File(outputpath);
        fDir.mkdir();

        File spFile = File.createTempFile("points_", ".csv", fDir);

        InputStream in = new FileInputStream(speciesfile);
        OutputStream out = new FileOutputStream(spFile);
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
        out.flush();
        in.close();
        out.close();

        return spFile.getAbsolutePath();
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

    private String[] getEnvFiles(String envNames) {
        String[] nameslist = envNames.split(":");
        String[] pathlist = new String[nameslist.length];

        for (int j = 0; j < nameslist.length; j++) {

            //Layer[] _layerlist = ssets.getEnvironmentalLayers();

            pathlist[j] = Layers.layerDisplayNameToName(nameslist[j]);
            /*
            for (int i = 0; i < _layerlist.length; i++) {
            if (_layerlist[i].display_name.equalsIgnoreCase(nameslist[j])) {
            pathlist[j] = _layerlist[i].name;
            continue;
            }
            }
             *
             */
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

    private Layer[] getEnvFilesAsLayers(String envNames) {
        try {
            System.out.println("envNames.pre: " + envNames);
            envNames = URLDecoder.decode(envNames, "UTF-8");
            System.out.println("envNames.post: " + envNames);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace(System.out);
        }
        String[] nameslist = envNames.split(":");
        Layer[] sellayers = new Layer[nameslist.length];

        Layer[] _layerlist = ssets.getEnvironmentalLayers();

        for (int j = 0; j < nameslist.length; j++) {
            for (int i = 0; i < _layerlist.length; i++) {
                if (_layerlist[i].display_name.equalsIgnoreCase(nameslist[j])
                        || _layerlist[i].name.equalsIgnoreCase(nameslist[j])) {
                    sellayers[j] = _layerlist[i];
                    //sellayers[j].name = _layerPath + sellayers[j].name;
                    System.out.println("Adding layer for ALOC: " + sellayers[j].name);
                    continue;
                }
            }
        }

        return sellayers;
    }
}
