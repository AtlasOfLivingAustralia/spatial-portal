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
            String species = req.getParameter("species");
            String removedSpecies = req.getParameter("removedspecies");
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
            writeFile(species, currentPath + "output" + File.separator + "maxent" + File.separator + pid + File.separator, "species_points.csv");
            if(removedSpecies != null) {
                writeFile(removedSpecies, currentPath + "output" + File.separator + "maxent" + File.separator + pid + File.separator, "removedSpecies.txt");
            }
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

    private String writeFile(String contents, String outputpath, String filename) {
        try {
            File fDir = new File(outputpath);
            fDir.mkdir();

            File spFile = new File(fDir, filename);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            spWriter.write(contents);
            spWriter.close();

            return spFile.getAbsolutePath();
        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }

        return null;
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