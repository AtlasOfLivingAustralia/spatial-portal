/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.services;

import au.com.bytecode.opencsv.CSVReader;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.client.Client;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.intersect.IniReader;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.GridCutter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
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

    @RequestMapping(value = "/step1", method = RequestMethod.POST)
    public 
    @ResponseBody
    String processStep1(HttpServletRequest req) {

        try {

            String outputdir = "";
            long currTime = System.currentTimeMillis();

            String envlist = req.getParameter("envlist");
            String speciesdata = req.getParameter("speciesdata");
            String area = req.getParameter("area");

            //Layer[] layers = getEnvFilesAsLayers(envlist);

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }

            // 1. create work/output directory
            //outputdir = TabulationSettings.base_output_dir + currTime + "/";
            outputdir = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "gdm" + File.separator + currTime + File.separator;
            File outdir = new File(outputdir);
            outdir.mkdirs();

            System.out.println("gdm.outputpath: " + outputdir);

            // 2. generate species file
            //String speciesFile = generateSpeciesFile(outputdir, taxons, region);
            String speciesFile = generateSpeciesFile(outputdir, speciesdata);

            System.out.println("gdm.speciesFile: " + speciesFile);

            // 3. cut environmental layers
            //String cutDataPath = GridCutter.cut(layers, region, filter, null);
            //SpatialSettings ssets = new SpatialSettings();
            //String cutDataPath = ssets.getEnvDataPath();

            String resolution = req.getParameter("res");
            if (resolution == null) {
                resolution = "0.01";
            }
            String cutDataPath = GridCutter.cut2(envlist.split(":"), resolution, region, filter, null);

            System.out.println("CUTDATAPATH: " + region + " " + cutDataPath);

            System.out.println("gdm.cutDataPath: " + cutDataPath);

            // 4. produce domain grid
            //DomainGrid.generate(cutDataPath, layers, region, outputdir);

            // 5. build parameters files for GDM
            String params = generateStep1Paramfile(envlist.split(":"), cutDataPath, speciesFile, outputdir);

            
            // 6. run GDM
            int exit = runGDM(1,params);
            System.out.println("gdm.exit: " + exit);

            String output = "";
            output += Long.toString(currTime) + "\n";
//            BufferedReader cutpoint = new BufferedReader(new FileReader(outputdir + "Cutpoint.csv"));
//            String line = "";
//            while ((line = cutpoint.readLine()) != null) {
//                output += line;
//            }
            Scanner sc = new Scanner(new File(outputdir + "Cutpoint.csv"));
            while (sc.hasNextLine()) {
                output += sc.nextLine() + "\n";
            }



            // write the properties to a file so we can grab them back later
            Properties props = new Properties();
            props.setProperty("pid", Long.toString(currTime));
            props.setProperty("envlist", envlist);
            props.setProperty("area", area);
            //props.store(new PrintWriter(new BufferedWriter(new FileWriter(outputdir + "ala.properties"))), "");
            props.store(new FileOutputStream(outputdir + "ala.properties"), "ALA GDM Properties");


            return output;

        } catch (Exception e) {
            System.out.println("Error processing gdm request");
            e.printStackTrace(System.out);
        }

        return "";
    }

    @RequestMapping(value = "/step2", method = RequestMethod.POST)
    public
    @ResponseBody
    String processStep2(HttpServletRequest req) {
        String output = "";

        try {

            String outputdir = "";

            String pid = req.getParameter("pid");
            String cutpoint = req.getParameter("cutpoint");
            String useDistance = req.getParameter("useDistance");
            String weighting = req.getParameter("weighting");
            String useSubSample = req.getParameter("useSubSample");
            String sitePairsSize = req.getParameter("sitePairsSize");

            outputdir = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "gdm" + File.separator + pid + File.separator;


            Properties props = new Properties();
            props.load(new FileInputStream(outputdir + "ala.properties"));
            String envlist = props.getProperty("envlist");
            String area = props.getProperty("area");
            //Layer[] layers = getEnvFilesAsLayers(envlist);


            // 5. build parameters files for GDM
            String params = updateParamfile(cutpoint, useDistance, weighting, useSubSample, sitePairsSize, outputdir);

            //System.out.println("gdm.params: \n-------------------------\n" + params + "\n-------------------------\n");
            System.out.println("gdm.params: " + params);

            // 6. run GDM
            int exit = runGDM(2, params);
            System.out.println("gdm.exit: " + exit);


            // 7. process params file

            // 7.1 generate/display charts
            generateCharts(outputdir);
            generateMetadata(envlist.split(":"), area, pid, outputdir);

            // 7.2 generate/display transform grid

            return pid;


        } catch (Exception e) {
            System.out.println("Error processing gdm request");
            e.printStackTrace(System.out);
        }

        return output;
    }



    @RequestMapping(value = "/process2", method = RequestMethod.POST)
    public
    @ResponseBody
    String process2(HttpServletRequest req) {
        String output = "";

        try {

            String outputdir = "";
            long currTime = System.currentTimeMillis();

            String envlist = req.getParameter("envlist");
            String quantile = req.getParameter("quantile");
            String useDistance = req.getParameter("useDistance");
            String useSubSample = req.getParameter("useSubSample");
            String sitePairsSize = req.getParameter("sitePairsSize");
            String speciesdata = req.getParameter("speciesdata");
            String area = req.getParameter("area");

            //Layer[] layers = getEnvFilesAsLayers(envlist);

            LayerFilter[] filter = null;
            SimpleRegion region = null;
            if (area != null && area.startsWith("ENVELOPE")) {
                filter = LayerFilter.parseLayerFilters(area);
            } else {
                region = SimpleShapeFile.parseWKT(area);
            }


            // 1. create work/output directory
            //outputdir = TabulationSettings.base_output_dir + currTime + "/";
            outputdir = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "gdm" + File.separator + currTime + File.separator;
            File outdir = new File(outputdir);
            outdir.mkdirs();

            System.out.println("gdm.outputpath: " + outputdir);

            // 2. generate species file
            //String speciesFile = generateSpeciesFile(outputdir, taxons, region);
            String speciesFile = generateSpeciesFile(outputdir, speciesdata);

            System.out.println("gdm.speciesFile: " + speciesFile);

            // 3. cut environmental layers
            //String cutDataPath = GridCutter.cut(layers, region, filter, null);
            //SpatialSettings ssets = new SpatialSettings();
            //String cutDataPath = ssets.getEnvDataPath();

             String resolution = req.getParameter("res");
            if (resolution == null) {
                resolution = "0.01";
            }
            String cutDataPath = GridCutter.cut2(envlist.split(":"), resolution, region, filter, null);

            System.out.println("CUTDATAPATH: " + region + " " + cutDataPath);

            System.out.println("gdm.cutDataPath: " + cutDataPath);

            // 4. produce domain grid
            //DomainGrid.generate(cutDataPath, layers, region, outputdir);

            // 5. build parameters files for GDM
            String params = generateParamfile(envlist.split(":"), cutDataPath, useDistance, speciesFile, outputdir);

            //System.out.println("gdm.params: \n-------------------------\n" + params + "\n-------------------------\n");
            System.out.println("gdm.params: " + params);

            // 6. run GDM
            int exit = runGDM(params);
            System.out.println("gdm.exit: " + exit);


            // 7. process params file

            // 7.1 generate/display charts
            generateCharts(outputdir);
            generateMetadata(envlist.split(":"), area, area, outputdir);

            // 7.2 generate/display transform grid

            return Long.toString(currTime);


        } catch (Exception e) {
            System.out.println("Error processing gdm request");
            e.printStackTrace(System.out);
        }

        return output;
    }

    private String generateSpeciesFile(String outputdir, String speciesdata) {
        try {

            File fDir = new File(outputdir);
            fDir.mkdir();

            File spFile = new File(fDir, "species_points.csv");
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));

            spWriter.write(speciesdata);
            spWriter.close();

            return spFile.getAbsolutePath();

        } catch (Exception e) {
            System.out.println("error generating species file");
            e.printStackTrace(System.out);
        }

        return null;
    }

    private String generateStep1Paramfile(String[] layers, String layersPath, String speciesfile, String outputdir) {
        try {
            LayerDAO layerDao = Client.getLayerDao();
            StringBuilder envLayers = new StringBuilder();
            StringBuilder useEnvLayers = new StringBuilder();
            StringBuilder predSpline = new StringBuilder();
            for (int i = 0; i < layers.length; i++) {
                envLayers.append("EnvGrid").append(i + 1).append("=").append(layersPath).append(layers[i]).append("\n");
                envLayers.append("EnvGridName").append(i + 1).append("=").append(layerDao.getLayerByName(layers[i]).getDisplayname()).append("\n");
                useEnvLayers.append("UseEnv").append(i + 1).append("=1").append("\n");
                predSpline.append("PredSpl").append(i + 1).append("=3").append("\n");
            }
            
            StringBuilder sbOut = new StringBuilder();
            sbOut.append("[GDMODEL]").append("\n")
                    .append("WorkspacePath=" + outputdir).append("\n")
                    .append("RespDataType=RD_SitePlusSpecies").append("\n")
                    .append("PredDataType=ED_GridData").append("\n")
                    .append("Quantiles=QUANTS_FromData").append("\n")
                    .append("UseEuclidean=0").append("\n")
                    .append("UseSubSample=1").append("\n")
                    .append("NumSamples=10000").append("\n")
                    .append("[RESPONSE]").append("\n")
                    .append("InputData=" + speciesfile).append("\n")
                    .append("UseWeights=0").append("\n")
                    .append("[PREDICTORS]").append("\n") 
                    .append("EuclSpl=3").append("\n")
                    .append("NumPredictors=" + layers.length).append("\n")
                    .append(envLayers).append("\n")
                    .append(useEnvLayers).append("\n")
                    .append(predSpline).append("\n");
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputdir + "gdm_params.txt")));
            spWriter.write(sbOut.toString());
            spWriter.close();

            return outputdir + "gdm_params.txt";
        } catch (Exception e) {
            System.out.println("Unable to write the initial params file");
            e.printStackTrace(System.out);
        }

        return "";
    }
    
    private String generateParamfile(String[] layers, String layersPath, String useEuclidean, String speciesfile, String outputdir) {
        try {
            LayerDAO layerDao = Client.getLayerDao();
            StringBuilder envLayers = new StringBuilder();
            StringBuilder useEnvLayers = new StringBuilder();
            StringBuilder predSpline = new StringBuilder();
            for (int i = 0; i < layers.length; i++) {
                envLayers.append("EnvGrid").append(i + 1).append("=").append(layersPath).append(layers[i]).append("\n");
                envLayers.append("EnvGridName").append(i + 1).append("=").append(layerDao.getLayerByName(layers[i]).getDisplayname()).append("\n");
                useEnvLayers.append("UseEnv").append(i + 1).append("=1").append("\n");
                predSpline.append("PredSpl").append(i + 1).append("=3").append("\n");
            }
            StringBuilder sbOut = new StringBuilder();
            sbOut.append("[GDMODEL]").append("\n")
                    .append("WorkspacePath=" + outputdir).append("\n")
                    .append("RespDataType=RD_SitePlusSpecies").append("\n")
                    .append("PredDataType=ED_GridData").append("\n")
                    .append("Quantiles=QUANTS_FromData").append("\n")
                    .append("UseEuclidean=0").append("\n")
                    .append("UseSubSample=1").append("\n")
                    .append("NumSamples=10000").append("\n")
                    .append("[RESPONSE]").append("\n")
                    .append("InputData=" + speciesfile).append("\n")
                    .append("UseWeights=0").append("\n")
                    .append("[PREDICTORS]").append("\n") //.append("DomainGrid=/data/ala/runtime/output/gdm/test/domain").append("\n")
                    .append("EuclSpl=3").append("\n")
                    .append("NumPredictors=" + layers.length).append("\n")
                    .append(envLayers).append("\n")
                    .append(useEnvLayers).append("\n")
                    .append(predSpline).append("\n");
            //File fDir = new File(outputdir);
            //fDir.mkdir();
            //File spFile = File.createTempFile("params_", ".csv", fDir);
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputdir + "gdm_params.txt")));
            spWriter.write(sbOut.toString());
            spWriter.close();
            //return spFile.getAbsolutePath();
            return outputdir + "gdm_params.txt";
        } catch (IOException ex) {
            Logger.getLogger(GDMWSController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String updateParamfile(String cutpoint, String useDistance, String weighting, String useSubSample, String sitePairsSize, String outputdir) {
        try {
            IniReader ir = new IniReader(outputdir + "/gdm_params.txt");
            ir.setValue("GDMODEL","UseEuclidean", useDistance);
            ir.setValue("GDMODEL","UseSubSample",useSubSample);
            ir.setValue("GDMODEL","NumSamples",sitePairsSize);
            ir.setValue("GDMODEL","Cutpoint",cutpoint);
            ir.setValue("RESPONSE","UseWeights", weighting);
            ir.write(outputdir + "/gdm_params.txt");
        } catch (Exception e) {
            System.out.println("Unable to update params file");
            e.printStackTrace(System.out);
        }

        return outputdir + "gdm_params.txt";
    }

//    private Layer[] getEnvFilesAsLayers(String envNames) {
//        try {
//            envNames = URLDecoder.decode(envNames, "UTF-8");
//        } catch (UnsupportedEncodingException ex) {
//            ex.printStackTrace(System.out);
//        }
//        String[] nameslist = envNames.split(":");
//        Layer[] sellayers = new Layer[nameslist.length];
//
//        Layer[] _layerlist = TabulationSettings.environmental_data_files;
//
//        for (int j = 0; j < nameslist.length; j++) {
//            for (int i = 0; i < _layerlist.length; i++) {
//                if (_layerlist[i].display_name.equalsIgnoreCase(nameslist[j])
//                        || _layerlist[i].name.equalsIgnoreCase(nameslist[j])) {
//                    sellayers[j] = _layerlist[i];
//                    //sellayers[j].name = _layerPath + sellayers[j].name;
//                    System.out.println("Adding layer for GDM: " + sellayers[j].name);
//                    continue;
//                }
//            }
//        }
//
//        return sellayers;
//    }

    private int runGDM(String params) {
        return runGDM(0, params);
    }
    private int runGDM(int level, String params) {
        Runtime runtime = Runtime.getRuntime();
        Process proc;
        InputStreamReader isre = null, isr = null;
        BufferedReader bre = null, br = null;
        int exitValue = -1;

        try {
            String command = AlaspatialProperties.getAnalysisGdmCmd() + " -g" + level + " " + params;
            System.out.println("Running gdm: " + command);

//            return 111;

            proc = runtime.exec(command);

            isre = new InputStreamReader(proc.getErrorStream());
            bre = new BufferedReader(isre);
            isr = new InputStreamReader(proc.getInputStream());
            br = new BufferedReader(isr);
            String line;

            while ((line = bre.readLine()) != null) {
                System.out.println(line);
            }

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            int exitVal = proc.waitFor();

            // any error???
            exitValue = exitVal;

        } catch (Exception e) {
            System.out.println("Error executing GDM: ");
            e.printStackTrace(System.out);
        } finally {
            try {
                isre.close();
                bre.close();
                isr.close();
                br.close();
            } catch (IOException ioe) {
                System.out.println("Error closing output and error streams");
            }
        }

        return exitValue;
    }

    public static void generateCharts(String outputdir) {

        // Check if there is 'plots' dir. if not create it.
        File plots = new File(outputdir + "/plots/");
        plots.mkdirs();

        // generate the Observed vs. Prediction chart
        generateCharts123(outputdir);
        generateCharts45(outputdir);
    }

    public static void generateCharts123(String outputdir) {
        try {
            IniReader ir = new IniReader(outputdir + "/gdm_params.txt");
            double intercept = ir.getDoubleValue("GDMODEL", "Intercept");

            // 1. read the ObservedVsPredicted.csv file
            System.out.println("Loading csv data");
            CSVReader csv = new CSVReader(new FileReader(outputdir + "ObservedVsPredicted.csv"));
            List<String[]> rawdata = csv.readAll();
            double[][] dataCht1 = new double[2][rawdata.size() - 1];
            double[][] dataCht2 = new double[2][rawdata.size() - 1];

            // for Chart 1: obs count
            int[] obscount = new int[11];
            for (int i = 0; i < obscount.length; i++) {
                obscount[i] = 0;
            }

            System.out.println("populating data");
            for (int i = 1; i < rawdata.size(); i++) {
                String[] row = rawdata.get(i);
                double obs = Double.parseDouble(row[4]);
                dataCht1[0][i - 1] = Double.parseDouble(row[6]);
                dataCht1[1][i - 1] = obs;

                dataCht2[0][i - 1] = Double.parseDouble(row[5]) - intercept;
                dataCht2[1][i - 1] = obs;

                int obc = (int) Math.round(obs*10);
                obscount[obc]++;
            }

            DefaultXYDataset dataset1 = new DefaultXYDataset();
            dataset1.addSeries("", dataCht1);

            DefaultXYDataset dataset2 = new DefaultXYDataset();
            dataset2.addSeries("", dataCht2);

            DefaultCategoryDataset dataset3 = new DefaultCategoryDataset();
            for (int i = 0; i < obscount.length; i++) {
                String col = "0." + i + "-0." + (i + 1);
                if (i == 10) {
                    col = "0.9-1.0";
                }
                dataset3.addValue(obscount[i] + 100, "col", col);
            }
            generateChartByType("Response Histogram", "Observed Dissimilarity Class", "Number of Site Pairs", dataset3, outputdir, "bar", "resphist");


            XYDotRenderer renderer = new XYDotRenderer();
            //Shape cross = ShapeUtilities.createDiagonalCross(3, 1);
            //renderer.setSeriesShape(0, cross);
            renderer.setDotWidth(3);
            renderer.setDotHeight(3);
            renderer.setSeriesPaint(0, Color.BLACK);


            JFreeChart jChart1 = ChartFactory.createScatterPlot("Observed versus predicted compositional dissimilarity", "Predicted Compositional Dissimilarity", "Observed Compositional Dissimilarity", dataset1, PlotOrientation.VERTICAL, false, false, false);
            jChart1.getTitle().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

            XYPlot plot = (XYPlot) jChart1.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainZeroBaselineVisible(true);
            plot.setRangeZeroBaselineVisible(true);
            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlineStroke(new BasicStroke(0.5F, 0, 1));
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlineStroke(new BasicStroke(0.5F, 0, 1));
            plot.setRenderer(0, renderer);

            NumberAxis domain = (NumberAxis) plot.getDomainAxis();
            domain.setAutoRangeIncludesZero(false);
            domain.setAxisLineVisible(false);
            domain.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            NumberAxis range = (NumberAxis) plot.getRangeAxis();
            range.setAutoRangeIncludesZero(false);
            range.setAxisLineVisible(false);
            range.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            double dMinPred = domain.getRange().getLowerBound();
            double dMaxPred = domain.getRange().getUpperBound();

            double dMinObs = range.getRange().getLowerBound();
            double dMaxObs = range.getRange().getUpperBound();

            System.out.println("1..pred.min.max: " + dMinPred + ", " + dMaxPred);

            int regressionLineSegs = 10;
            double dInc = (dMaxPred - dMinPred) / regressionLineSegs;
            double[][] dataReg1 = new double[2][regressionLineSegs + 1];
            DefaultXYDataset dsReg1 = new DefaultXYDataset();
            int i = 0;
            for (double d = dMinPred; d <= dMaxPred; d += dInc, i++) {
                dataReg1[0][i] = d;
                dataReg1[1][i] = d;
            }
            dsReg1.addSeries("", dataReg1);
            XYSplineRenderer regressionRenderer = new XYSplineRenderer();
            regressionRenderer.setBaseSeriesVisibleInLegend(true);
            regressionRenderer.setSeriesPaint(0, Color.RED);
            regressionRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
            regressionRenderer.setBaseShapesVisible(false);
            plot.setDataset(1, dsReg1);
            plot.setRenderer(1, regressionRenderer);

            System.out.println("Writing image....");
            ChartUtilities.saveChartAsPNG(new File(outputdir + "plots/obspredissim.png"), jChart1, 600, 400);



            // For chart 3
            JFreeChart jChart2 = ChartFactory.createScatterPlot("Observed compositional dissimilarity vs predicted ecological distance", "Predicted ecological distance", "Observed Compositional Dissimilarity", dataset2, PlotOrientation.VERTICAL, false, false, false);
            jChart2.getTitle().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

            plot = (XYPlot) jChart2.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainZeroBaselineVisible(true);
            plot.setRangeZeroBaselineVisible(true);
            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlineStroke(new BasicStroke(0.5F, 0, 1));
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlineStroke(new BasicStroke(0.5F, 0, 1));
            plot.setRenderer(0, renderer);

            domain = (NumberAxis) plot.getDomainAxis();
            domain.setAutoRangeIncludesZero(false);
            domain.setAxisLineVisible(false);
            domain.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            range = (NumberAxis) plot.getRangeAxis();
            range.setAutoRangeIncludesZero(false);
            range.setAxisLineVisible(false);
            range.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            dMinPred = domain.getRange().getLowerBound();
            dMaxPred = domain.getRange().getUpperBound();

            dMinObs = range.getRange().getLowerBound();
            dMaxObs = range.getRange().getUpperBound();

            System.out.println("2.pred.min.max: " + dMinPred + ", " + dMaxPred);

            regressionLineSegs = 10;
            dInc = (dMaxPred - dMinPred) / regressionLineSegs;
            dataReg1 = new double[2][regressionLineSegs + 1];
            dsReg1 = new DefaultXYDataset();
            i = 0;
            for (double d = dMinPred; d <= dMaxPred; d += dInc, i++) {
                dataReg1[0][i] = d;
                dataReg1[1][i] = (1.0 - Math.exp(-d));
            }
            dsReg1.addSeries("", dataReg1);
            regressionRenderer.setBaseSeriesVisibleInLegend(true);
            regressionRenderer.setSeriesPaint(0, Color.RED);
            regressionRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
            regressionRenderer.setBaseShapesVisible(false);
            plot.setDataset(1, dsReg1);
            plot.setRenderer(1, regressionRenderer);

            System.out.println("Writing image....");
            ChartUtilities.saveChartAsPNG(new File(outputdir + "plots/dissimdist.png"), jChart2, 600, 400);


        } catch (Exception e) {
            System.out.println("Unable to generate charts 2 and 3:");
            e.printStackTrace(System.out);
        }
    }

    public static void generateCharts45(String outputdir) {
        try {
            // read the gdm_params.txt, and for each predictor Coeff (1, 2, 3),
            // add them up

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            IniReader ir = new IniReader(outputdir + "/gdm_params.txt");
            int numpreds = ir.getIntegerValue("PREDICTORS", "NumPredictors");
            System.out.println("ir.numpreds: " + numpreds);
            for (int i = 1; i < numpreds + 1; i++) {
                double sc = ir.getDoubleValue("PREDICTORS", "CoefSum" + i);

                String predictor = ir.getStringValue("PREDICTORS", "EnvGrid" + i);
                predictor = predictor.substring(predictor.lastIndexOf("/") + 1);

                System.out.println("Adding " + predictor + " coeffient ");
                dataset.addValue(sc, "predictor", predictor);

                String pname = ir.getStringValue("CHARTS", "PredPlotDataName" + i);
                String pdata = ir.getStringValue("CHARTS", "PredPlotDataPath" + i);
                generateChart5(outputdir, pname, pdata);
            }
            generateChartByType("Predictor Histogram", "Predictors", "Coefficient", dataset, outputdir, "bar", "predhist");

        } catch (Exception e) {
            System.out.println("Unable to generate charts 2 and 3:");
            e.printStackTrace(System.out);
        }
    }

    public static void generateChart5(String outputdir, String plotName, String plotData) {
        try {

            CSVReader csv = new CSVReader(new FileReader(plotData));
            List<String[]> rawdata = csv.readAll();
            double[][] dataCht = new double[2][rawdata.size() - 1];
            for (int i = 1; i < rawdata.size(); i++) {
                String[] row = rawdata.get(i);
                dataCht[0][i - 1] = Double.parseDouble(row[0]);
                dataCht[1][i - 1] = Double.parseDouble(row[1]);
            }

            DefaultXYDataset dataset = new DefaultXYDataset();
            dataset.addSeries("", dataCht);

            System.out.println("Setting up jChart for " + plotName);
            //generateChartByType(plotName, plotName, "f("+plotName+")", null, outputdir, "xyline", plotName);
            JFreeChart jChart = ChartFactory.createXYLineChart(plotName, plotName, "f(" + plotName + ")", dataset, PlotOrientation.VERTICAL, false, false, false);

            jChart.getTitle().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

            XYPlot plot = (XYPlot) jChart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainZeroBaselineVisible(true);
            plot.setRangeZeroBaselineVisible(true);
            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlineStroke(new BasicStroke(0.5F, 0, 1));
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlineStroke(new BasicStroke(0.5F, 0, 1));

            NumberAxis domain = (NumberAxis) plot.getDomainAxis();
            domain.setAutoRangeIncludesZero(false);
            domain.setAxisLineVisible(false);
            domain.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            NumberAxis range = (NumberAxis) plot.getRangeAxis();
            range.setAutoRangeIncludesZero(false);
            range.setAxisLineVisible(false);
            range.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            System.out.println("Writing image....");
            ChartUtilities.saveChartAsPNG(new File(outputdir + "plots/" + plotName + ".png"), jChart, 500, 500);
            ChartUtilities.saveChartAsPNG(new File(outputdir + "plots/" + plotName + "_thumb.png"), jChart, 210, 140);

        } catch (Exception e) {
            System.out.println("Unable to generate charts 2 and 3:");
            e.printStackTrace(System.out);
        }
    }

    public static void generateChartByType(String title, String xLabel, String yLabel, Dataset dataset, String outputdir, String type, String filename) throws IOException {
        JFreeChart jChart = null;
        if ("line".equalsIgnoreCase(type)) {
            jChart = ChartFactory.createLineChart(title, xLabel, yLabel, (CategoryDataset) dataset, PlotOrientation.VERTICAL, false, false, false);
        } else if ("bar".equalsIgnoreCase(type)) {
            System.out.println("Setting up jChart");
            jChart = ChartFactory.createBarChart(title, xLabel, yLabel, (CategoryDataset) dataset, PlotOrientation.VERTICAL, false, false, false);
            System.out.println("Writing image....");
        } else if ("xyline".equalsIgnoreCase(type)) {
            jChart = ChartFactory.createXYLineChart(title, xLabel, yLabel, (XYDataset) dataset, PlotOrientation.VERTICAL, false, false, false);
        }

        if ("xyline".equalsIgnoreCase(type)) {

            XYPlot plot = (XYPlot) jChart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeZeroBaselineVisible(true);
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlineStroke(new BasicStroke(0.5F, 0, 1));

            NumberAxis domain = (NumberAxis) plot.getDomainAxis();
            domain.setAxisLineVisible(false);
            domain.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            domain.setAutoRangeIncludesZero(false);

            NumberAxis range = (NumberAxis) plot.getRangeAxis();
            range.setAutoRangeIncludesZero(false);
            range.setAxisLineVisible(false);
            range.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            //System.out.println("dataset.getColumnCount(): " + dataset.getColumnCount());
            //System.out.println("dataset.getRowCount(): " + dataset.getRowCount());

        } else {

            CategoryPlot plot = (CategoryPlot) jChart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeZeroBaselineVisible(true);
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlineStroke(new BasicStroke(0.5F, 0, 1));

            CategoryAxis domain = (CategoryAxis) plot.getDomainAxis();
            domain.setAxisLineVisible(false);
            domain.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            NumberAxis range = (NumberAxis) plot.getRangeAxis();
            range.setAutoRangeIncludesZero(false);
            range.setAxisLineVisible(false);
            range.setLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            //System.out.println("dataset.getColumnCount(): " + dataset.getColumnCount());
            //System.out.println("dataset.getRowCount(): " + dataset.getRowCount());

        }

        jChart.getTitle().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        ChartUtilities.saveChartAsPNG(new File(outputdir + "plots/" + filename + ".png"), jChart, 900, 500);
    }

    private void generateMetadata(String[] layers, String area, String pid, String outputdir) {
        try {
            LayerDAO layerDao = Client.getLayerDao();
            int i = 0;
            System.out.println("Generating metadata...");
            StringBuilder sbMetadata = new StringBuilder();

            sbMetadata.append("<!doctype html><head><meta charset='utf-8'><title>Genralized Dissimilarity Model</title><meta name='description' content='ALA GDM Metadata'>");
            sbMetadata.append("<style type='text/css'>body{font-family:Verdana,'Lucida Sans';font-size:small;}div#core{display:block;clear:both;margin-bottom:20px;}section{width:95%;margin:0 15px;border-bottom:1px solid #000;}.clearfix:after{content:'.';display:block;clear:both;visibility:hidden;line-height:0;height:0;}.clearfix{display:inline-block;}html[xmlns] .clearfix{display:block;}* html .clearfix{height:1%;}</style>");
            sbMetadata.append("</head><body><div id=wrapper><header><h1>Genralized Dissimilarity Model</h1></header><div id=core class=clearfix><section><p>");
            sbMetadata.append("This GDM model was created Wed Feb 29 20:50:37 EST 2012. Data available in this folder is available for further analysis. </p>");
            sbMetadata.append("<h3>Your options:</h3><ul>");

            sbMetadata.append("<li>Model reference number:").append(pid).append("</li>");
            sbMetadata.append("<li>Species:").append("").append("</li>");
            sbMetadata.append("<li>Area:").append(area).append("</li>");

            sbMetadata.append("<li>Layers: <ul>");
            for (i = 0; i < layers.length; i++) {
                sbMetadata.append("<li>").append(layerDao.getLayerByName(layers[i]).getDisplayname()).append("</li>");
            }
            sbMetadata.append("</li></ul></li></ul></section>");

            sbMetadata.append("<section><h3>Response Histogram (observed dissimilarity class):</h3><p> The Response Histogram plots the distribution of site pairs within each observed dissimilarity class. The final column in the dissimilarity class > 1 represents the number of site pairs that are totally dissimilar from each other. This chart provides an overview of potential bias in the distribution of the response data. </p><p><img src='plots/resphist.png'/></p></section><section><h3>Observed versus predicted compositional dissimilarity (raw data plot):</h3><p> The 'Raw data' scatter plot presents the Observed vs Predicted degree of compositional dissimilarity for a given model run. Each dot on the chart represents a site-pair. The line represents the perfect 1:1 fit. (Note that the scale and range of values on the x and y axes differ). </p><p> This chart provides a snapshot overview of the degree of scatter in the data. That is, how well the predicted compositional dissimilarity between site pairs matches the actual compositional dissimilarity present in each site pair. </p><p><img src='plots/obspredissim.png'/></p></section><section><h3>Observed compositional dissimilarity vs predicted ecological distance (link function applied to the raw data plot):</h3><p> The 'link function applied' scatter plot presents the Observed compositional dissimilarity vs Predicted ecological distance. Here, the link function has been applied to the predicted compositional dissimilarity to generate the predicted ecological distance. Each dot represents a site-pair. The line represents the perfect 1:1 fit. The scatter of points signifies noise in the relationship between the response and predictor variables. </p><p><img src='plots/dissimdist.png'/></p></section><section><h3>Predictor Histogram:</h3><p> The Predictor Histogram plots the relative contribution (sum of coefficient values) of each environmental gradient layer that is relevant to the model. The sum of coefficient values is a measure of the amount of predicted compositional dissimilarity between site pairs. </p><p> Predictor variables that contribute little to explaining variance in compositional dissimilarity between site pairs have low relative contribution values. Predictor variables that do not make any contribution to explaining variance in compositional dissimilarity between site pairs (i.e., all coefficient values are zero) are not shown. </p><p><img src='plots/predhist.png'/></p></section><section><h3>Fitted Functions:</h3><p> The model output presents the response (compositional turnover) predicted by variation in each predictor. The shape of the predictor is represented by three I-splines, the values of which are defined by the environmental data distribution: min, max and median (i.e., 0, 50 and 100th percentiles). The GDM model estimates the coefficients of the I-splines for each predictor. The coefficient provides an indication of the total amount of compositional turnover correlated with each value at the 0, 50 and 100th percentiles. The sum of these coefficient values is an indicator of the relative importance of each predictor to compositional turnover. </p><p> The coefficients are applied to the ecological distance from the minimum percentile for a predictor. These plots of fitted functions show the sort of monotonic transformations that will take place to a predictor to render it in GDM space. The relative maximum y values (sum of coefficient values) indicate the amount of influence that each predictor makes to the total GDM prediction. </p><p><a href='plots/maxtx.png'><img src='plots/maxtx_thumb.png'/></a><a href='plots/minti.png'><img src='plots/minti_thumb.png'/></a><a href='plots/radnx.png'><img src='plots/radnx_thumb.png'/></a><a href='plots/rainx.png'><img src='plots/rainx_thumb.png'/></a></p></section></div><footer><p>&copy; <a href='http://www.ala.org.au/'>Atlas of Living Australia 2012</a></p></footer></div></body></html>");
            sbMetadata.append("");

            File spFile = new File(outputdir + "metadata.html");
            System.out.println("Writing metadata to: " + spFile.getAbsolutePath());
            PrintWriter spWriter;
            spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));
            spWriter.write(sbMetadata.toString());
            spWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(GDMWSController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    public static void main(String[] args) {
//        //generateCharts("/Users/ajay/Downloads/1328584218973/");
//        //generateCharts("/Users/ajay/Downloads/My_GDM_domain_sxs/");
//        generateCharts("/Users/ajay/projects/ala/code/other/gdm/testdata/");
//        //generateChart2and3("/Users/ajay/Downloads/My_GDM_domain_sxs/", Integer.parseInt(args[0]));
//
//        //updateParamfile("112", "1", "1", "0", "123456", "/Users/ajay/projects/ala/code/other/gdm/testdata/");
//    }
}
