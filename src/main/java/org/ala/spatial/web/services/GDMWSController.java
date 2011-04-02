/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.util.DomainGrid;
import org.ala.spatial.util.GridCutter;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.TabulationSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/gdm/")
public class GDMWSController {

    @RequestMapping(value = "/process", method = RequestMethod.POST)
    public
    @ResponseBody
    String process(HttpServletRequest req) {
        String output = "";

        try {

            String outputdir = "";
            long currTime = System.currentTimeMillis();

            // user params
            //String currentPath = TabulationSettings.base_output_dir;
            String taxon = URLDecoder.decode(req.getParameter("taxonid"), "UTF-8").replace("__", ".");
            String area = req.getParameter("area");
            String envlist = req.getParameter("envlist");
            String quantile = req.getParameter("quantile");
            String useDistance = req.getParameter("useDistance");
            String useSubSample = req.getParameter("useSubSample");
            String sitePairsSize = req.getParameter("sitePairsSize");

            Layer[] layers = getEnvFilesAsLayers(envlist);

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = FilteringService.getFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }


            // 1. create work/output directory
            //outputdir = TabulationSettings.base_output_dir + currTime + "/";
            outputdir = TabulationSettings.base_output_dir + "output" + File.separator + "gdm" + File.separator + currTime + File.separator;
            File outdir = new File(outputdir);
            outdir.mkdirs();

            System.out.println("gdm.outputpath: " + outputdir);

            // 2. generate species file
            String speciesFile = generateSpeciesFile(outputdir, taxon, region);

            System.out.println("gdm.speciesFile: " + speciesFile);

            // 3. cut environmental layers
            String cutDataPath = GridCutter.cut(layers, region, filter, null);

            System.out.println("gdm.cutDataPath: " + cutDataPath);

            // 4. produce domain grid
            DomainGrid.generate(cutDataPath, layers, region, outputdir);

            // 5. build parameters files for GDM
            String params = generateParamfile(cutDataPath, layers, quantile, useDistance, speciesFile, outputdir);

            //System.out.println("gdm.params: \n-------------------------\n" + params + "\n-------------------------\n");
            System.out.println("gdm.params: " + params);

            // 6. run GDM
            int exit = runGDM(params);
            System.out.println("gdm.exit: " + exit);

            // 7. process params file

            // 7.1 generate/display charts

            // 7.2 generate/display transform grid




        } catch (Exception e) {
            System.out.println("Error processing gdm request");
            e.printStackTrace(System.out);
        }

        return output;
    }

    private String generateSpeciesFile(String outputdir, String taxon, SimpleRegion region) {
        try {
            SamplingService ss = SamplingService.newForLSID(taxon);

            StringBuffer removedSpecies = new StringBuffer();
            double[] points = ss.sampleSpeciesPointsMinusSensitiveSpecies(taxon, region, null, removedSpecies);

            StringBuffer sbSpecies = new StringBuffer();
            // get the header
            sbSpecies.append("X, Y, Code");
            sbSpecies.append(System.getProperty("line.separator"));
            for (int i = 0; i < points.length; i += 2) {
                sbSpecies.append(points[i] + ", " + points[i + 1] + ", species");
                sbSpecies.append(System.getProperty("line.separator"));
            }

            File fDir = new File(outputdir);
            fDir.mkdir();

            File spFile = File.createTempFile("points_", ".csv", fDir);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            spWriter.write(sbSpecies.toString());
            spWriter.close();

            return spFile.getAbsolutePath();
        } catch (Exception e) {
            System.out.println("error generating species file");
            e.printStackTrace(System.out);
        }
        
        return null;
    }

    private String generateParamfile(String envPath, Layer[] layers, String qantile, String useEuclidean, String speciesfile, String outputdir) {
        try {
            StringBuilder envLayers = new StringBuilder();
            StringBuilder useEnvLayers = new StringBuilder();
            StringBuilder predSpline = new StringBuilder();
            for (int i = 0; i < layers.length; i++) {
                envLayers.append("EnvGrid").append(i + 1).append("=").append(envPath).append(layers[i].name);
                useEnvLayers.append("UseEnv").append(i + 1).append("=1");
                predSpline.append("PredSpl").append(i + 1).append("=3");
            }
            StringBuilder sbOut = new StringBuilder();
            sbOut.append("[GDMODEL]").append("\n").append("WorkspacePath=" + outputdir).append("\n").append("RespDataType=RD_SitePlusSpecies").append("\n").append("PredDataType=ED_GridData").append("\n").append("Quantiles=QUANTS_From" + qantile).append("\n").append("UseEuclidean=" + useEuclidean).append("\n").append("UseSubSample=0").append("\n").append("NumSamples=1953").append("\n").append("[RESPONSE]").append("\n").append("InputData=" + speciesfile).append("\n").append("UseWeights=0").append("\n").append("[PREDICTORS]").append("\n").append("DomainGrid=" + outputdir + "domain").append("\n").append("EuclSpl=3").append("\n").append("NumPredictors=" + layers.length).append("\n").append(envLayers).append("\n").append(useEnvLayers).append("\n").append(predSpline).append("\n");
            //File fDir = new File(outputdir);
            //fDir.mkdir();
            //File spFile = File.createTempFile("params_", ".csv", fDir);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputdir+"gdm_params.txt")));
            spWriter.write(sbOut.toString());
            spWriter.close();
            //return spFile.getAbsolutePath();
            return outputdir+"gdm_params.txt"; 
        } catch (IOException ex) {
            Logger.getLogger(GDMWSController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Layer[] getEnvFilesAsLayers(String envNames) {
        try {
            envNames = URLDecoder.decode(envNames, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace(System.out);
        }
        String[] nameslist = envNames.split(":");
        Layer[] sellayers = new Layer[nameslist.length];

        Layer[] _layerlist = TabulationSettings.environmental_data_files;

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

    private int runGDM(String params) {
        Runtime runtime = Runtime.getRuntime();
        Process proc;
        int exitValue = -1;
        try {
            String command = TabulationSettings.gdm_cmdpth + " -g " + params;
            System.out.println("Running gdm: " + command);

            return 111;

//            proc = runtime.exec(command);
//
//            InputStreamReader isre = new InputStreamReader(proc.getErrorStream());
//            BufferedReader bre = new BufferedReader(isre);
//            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
//            BufferedReader br = new BufferedReader(isr);
//            String line;
//
//            while ((line = bre.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            while ((line = br.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            int exitVal = proc.waitFor();
//
//            // any error???
//            exitValue = exitVal;

        } catch (Exception e) {
            System.out.println("Error executing GDM: ");
            e.printStackTrace(System.out);
        }

        return exitValue; 
    }
}
