/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.services;

import au.com.bytecode.opencsv.CSVReader;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.service.FilteringService;
import org.ala.spatial.util.GridCutter;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.TabulationSettings;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.FastScatterPlot;
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
            //String speciesFile = generateSpeciesFile(outputdir, taxons, region);
            String speciesFile = generateSpeciesFile(outputdir, speciesdata);

            System.out.println("gdm.speciesFile: " + speciesFile);

            // 3. cut environmental layers
            //String cutDataPath = GridCutter.cut(layers, region, filter, null);
            SpatialSettings ssets = new SpatialSettings();
            String cutDataPath = ssets.getEnvDataPath();

            cutDataPath = GridCutter.cut(layers, region, filter, null);

            System.out.println("CUTDATAPATH: " + region + " " + cutDataPath);

            System.out.println("gdm.cutDataPath: " + cutDataPath);

            // 4. produce domain grid
            //DomainGrid.generate(cutDataPath, layers, region, outputdir);

            // 5. build parameters files for GDM
            String params = generateParamfile(layers, cutDataPath, useDistance, speciesFile, outputdir);

            //System.out.println("gdm.params: \n-------------------------\n" + params + "\n-------------------------\n");
            System.out.println("gdm.params: " + params);

            // 6. run GDM
            int exit = runGDM(params);
            System.out.println("gdm.exit: " + exit);


            // 7. process params file

            // 7.1 generate/display charts

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

    private String generateParamfile(Layer[] layers, String layersPath, String useEuclidean, String speciesfile, String outputdir) {
        try {
            StringBuilder envLayers = new StringBuilder();
            StringBuilder useEnvLayers = new StringBuilder();
            StringBuilder predSpline = new StringBuilder();
            for (int i = 0; i < layers.length; i++) {
                envLayers.append("EnvGrid").append(i + 1).append("=").append(layersPath).append(layers[i].name).append("\n");
                useEnvLayers.append("UseEnv").append(i + 1).append("=1").append("\n");
                predSpline.append("PredSpl").append(i + 1).append("=3").append("\n");
            }
            StringBuilder sbOut = new StringBuilder();
            sbOut.append("[GDMODEL]").append("\n").append("WorkspacePath=" + outputdir).append("\n").append("RespDataType=RD_SitePlusSpecies").append("\n").append("PredDataType=ED_GridData").append("\n").append("Quantiles=QUANTS_FromData").append("\n").append("UseEuclidean=1").append("\n").append("UseSubSample=1").append("\n").append("NumSamples=1000000").append("\n").append("[RESPONSE]").append("\n").append("InputData=" + speciesfile).append("\n").append("UseWeights=0").append("\n").append("[PREDICTORS]").append("\n") //.append("DomainGrid=/data/ala/runtime/output/gdm/test/domain").append("\n")
                    .append("EuclSpl=3").append("\n").append("NumPredictors=" + layers.length).append("\n").append(envLayers).append("\n").append(useEnvLayers).append("\n").append(predSpline).append("\n");
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
                    System.out.println("Adding layer for GDM: " + sellayers[j].name);
                    continue;
                }
            }
        }

        return sellayers;
    }

    private int runGDM(String params) {
        Runtime runtime = Runtime.getRuntime();
        Process proc;
        InputStreamReader isre = null, isr = null;
        BufferedReader bre = null, br = null;
        int exitValue = -1;

        try {
            String command = TabulationSettings.gdm_cmdpth + " -g " + params;
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

        // generate the Observed vs. Prediction chart
        generateChart2(outputdir);

    }
    public static void generateChart2(String outputdir) {
        try {

            // 1. read the ObservedVsPredicted.csv file
            System.out.println("Loading csv data");
            CSVReader csv = new CSVReader(new FileReader(outputdir + "ObservedVsPredicted.csv"));
            List<String[]> rawdata = csv.readAll();
            float[][] data = new float[2][rawdata.size()-1];

            System.out.println("populating data");
            for(int i=1;i<rawdata.size(); i++) {
                String[] row = rawdata.get(i);
                data[0][i-1] = Float.parseFloat(row[5]);
                data[1][i-1] = Float.parseFloat(row[4]);
            }

            // 2.
            System.out.println("Setting x-axis");
            NumberAxis preAxis = new NumberAxis("Predicted Compositional Dissimilarity");
            //preAxis.setAutoRangeIncludesZero(false);

            System.out.println("Setting y-axis");
            NumberAxis obsAxis = new NumberAxis("Observed Compositional Dissimilarity");
            //obsAxis.setAutoRangeIncludesZero(false);

            System.out.println("setting up FSP");
            FastScatterPlot plot = new FastScatterPlot(data, preAxis, obsAxis);
            //plot.setDomainPannable(true);
            //plot.setRangePannable(true);
            System.out.println("Setting up jChart");
            JFreeChart jChart = new JFreeChart("Observed versus predicted compositional dissimilarity", plot);

            //System.out.println("setting up BI");
            //BufferedImage bi = jChart.createBufferedImage(800, 800, BufferedImage.TRANSLUCENT, new ChartRenderingInfo());
            //BufferedImage bi = jChart.createBufferedImage(1000,1000);

            System.out.println("Writing image....");
            //ImageIO.write(bi, "png", new File(outputdir+"plots/obspredissim.png"));
            ChartUtilities.saveChartAsPNG(new File(outputdir+"plots/obspredissim.png"), jChart, 1000, 1000);


        } catch (Exception e) {
            System.out.println("Unable to generate charts");
            e.printStackTrace(System.out);
        }
    }

    public static void generateChart5(String outputdir, String predictor, String label) {
        try {

            // 1. read the ObservedVsPredicted.csv file
            System.out.println("Loading csv data");
            CSVReader csv = new CSVReader(new FileReader(outputdir + predictor + ".csv"));
            List<String[]> rawdata = csv.readAll();
            float[][] data = new float[2][rawdata.size()-1];

            System.out.println("populating data");
            for(int i=1;i<rawdata.size(); i++) {
                String[] row = rawdata.get(i);
                data[0][i-1] = Float.parseFloat(row[0]);
                data[1][i-1] = Float.parseFloat(row[1]);
            }

            // 2.
            System.out.println("Setting x-axis");
            NumberAxis preAxis = new NumberAxis(label);
            //preAxis.setAutoRangeIncludesZero(false);

            System.out.println("Setting y-axis");
            NumberAxis obsAxis = new NumberAxis("f("+label+")");
            //obsAxis.setAutoRangeIncludesZero(false);

            System.out.println("setting up FSP");
            FastScatterPlot plot = new FastScatterPlot(data, preAxis, obsAxis);
            plot.setDomainPannable(true);
            plot.setRangePannable(true);
            System.out.println("Setting up jChart");
            JFreeChart jChart = new JFreeChart("Observed versus predicted compositional dissimilarity", plot);

            System.out.println("setting up BI");
            //BufferedImage bi = jChart.createBufferedImage(800, 800, BufferedImage.TRANSLUCENT, new ChartRenderingInfo());
            //BufferedImage bi = jChart.createBufferedImage(1000,1000);

            System.out.println("Writing image....");
            //ImageIO.write(bi, "png", new File(outputdir+"plots/obspredissim.png"));
            ChartUtilities.saveChartAsPNG(new File(outputdir+"plots/obspredissim.png"), jChart, 1000, 1000); 


        } catch (Exception e) {
            System.out.println("Unable to generate charts");
            e.printStackTrace(System.out);
        }
    }

    private void generateMetadata(Layer[] layers, String area, String pid, String outputdir) {
        try {
            int i = 0;
            StringBuilder sbMetadata = new StringBuilder();

            sbMetadata.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"> <html> <head> <meta http-equiv=\"Content-Type\" content=\"text/html; charset=MacRoman\"> <title>Layer information</title> <link rel=\"stylesheet\" href=\"/alaspatial/styles/style.css\" type=\"text/css\" media=\"all\" /> </head> ");
            sbMetadata.append("<body>");
            sbMetadata.append("<h1>").append("Classification").append("</h1>");

            sbMetadata.append("<p> <span class=\"title\">Model reference number:</span> <br /> ");
            sbMetadata.append(pid);
            sbMetadata.append("</p>");

            sbMetadata.append("<p> <span class=\"title\">Layers:</span> <br /> ");
            for (i = 0; i < layers.length; i++) {
                sbMetadata.append(layers[i].display_name);
                if (i < layers.length - 1) {
                    sbMetadata.append(", ");
                }
            }
            sbMetadata.append("</p>");

            sbMetadata.append("<p> <span class=\"title\">Area:</span> <br /> ");
            sbMetadata.append(area);
            sbMetadata.append("</p>");

            sbMetadata.append("<p> <span class=\"title\">Response Histogram (observed dissimilarity class):</span> <br /> ");
            sbMetadata.append("The Response Histogram plots the distribution of site pairs within each observed dissimilarity class. The final column in the dissimilarity class > 1 represents the number of site pairs that are totally dissimilar from each other. This chart provides an overview of potential bias in the distribution of the response data.");
            sbMetadata.append("<br />X axis = Observed Dissimilarity Class");
            sbMetadata.append("<br />Y axis = Number of Site Pairs");
            sbMetadata.append("<br /><img src='plots/resphist.png' />");
            sbMetadata.append("</p>");

            sbMetadata.append("<p> <span class=\"title\">Observed versus predicted compositional dissimilarity (raw data plot):</span> <br /> ");
            sbMetadata.append("The ‘Raw data’ scatter plot presents the Observed vs Predicted degree of compositional dissimilarity for a given model run. Each dot on the chart represents a site-pair. The line represents the perfect 1:1 fit. (Note that the scale and range of values on the x and y axes differ).");
            sbMetadata.append("<br />");
            sbMetadata.append("This chart provides a snapshot overview of the degree of scatter in the data. That is, how well the predicted compositional dissimilarity between site pairs matches the actual compositional dissimilarity present in each site pair.");
            sbMetadata.append("<br />X axis = Predicted Compositional Dissimilarity");
            sbMetadata.append("<br />Y axis = Observed Compositional Dissimilarity");
            sbMetadata.append("<br /><img src='plots/obspredissim.png' />");
            sbMetadata.append("</p>");

            sbMetadata.append("<p> <span class=\"title\">Observed compositional dissimilarity vs predicted ecological distance (link function applied to the raw data plot):</span> <br /> ");
            sbMetadata.append("The ‘link function applied’ scatter plot presents the Observed compositional dissimilarity vs Predicted ecological distance. Here, the link function has been applied to the predicted compositional dissimilarity to generate the predicted ecological distance. Each dot represents a site-pair. The line represents the perfect 1:1 fit. The scatter of points signifies noise in the relationship between the response and predictor variables.");
            sbMetadata.append("<br />X axis = Predicted Ecological Distance");
            sbMetadata.append("<br />Y axis = Observed Compositional Dissimilarity");
            sbMetadata.append("<br /><img src='plots/dissimdist.png' />");
            sbMetadata.append("</p>");

            sbMetadata.append("<p> <span class=\"title\">Predictor Histogram:</span> <br /> ");
            sbMetadata.append("The Predictor Histogram plots the relative contribution (sum of coefficient values) of each environmental gradient layer that is relevant to the model. The sum of coefficient values is a measure of the amount of predicted compositional dissimilarity between site pairs.");
            sbMetadata.append("<br />");
            sbMetadata.append("Predictor variables that contribute little to explaining variance in compositional dissimilarity between site pairs have low relative contribution values. Predictor variables that do not make any contribution to explaining variance in compositional dissimilarity between site pairs (i.e., all coefficient values are zero) are not shown.");
            sbMetadata.append("<br />X axis = Predictors");
            sbMetadata.append("<br />Y axis = Relative Contribution (sum of predictor coefficient values)");
            sbMetadata.append("<br />");
            sbMetadata.append("The y axis range is determined by the maximum ‘sum of coefficient values’ for the set of predictors in the model.");
            sbMetadata.append("<br /><img src='plots/predhist.png' />");
            sbMetadata.append("</p>");

            sbMetadata.append("<p> <span class=\"title\">Fitted Functions:</span> <br /> ");
            sbMetadata.append("The model output presents the response (compositional turnover) predicted by variation in each predictor. The shape of the predictor is represented by three I-splines, the values of which are defined by the environmental data distribution: min, max and median (i.e., 0, 50 and 100th percentiles). The GDM model estimates the coefficients of the I-splines for each predictor. The coefficient provides an indication of the total amount of compositional turnover correlated with each value at the 0, 50 and 100th percentiles. The sum of these coefficient values is an indicator of the relative importance of each predictor to compositional turnover.");
            sbMetadata.append("<br />");
            sbMetadata.append("The coefficients are applied to the ecological distance from the minimum percentile for a predictor. These plots of fitted functions show the sort of monotonic transformations that will take place to a predictor to render it in GDM space. The relative maximum y values (sum of coefficient values) indicate the amount of influence that each predictor makes to the total GDM prediction.");
            sbMetadata.append("<br />X axis = <predictor label>");
            sbMetadata.append("<br />Y axis = f(<predictor label>)");
            sbMetadata.append("<br /><img src='plots/fitted.png' />");
            sbMetadata.append("");
            sbMetadata.append("");
            sbMetadata.append("");
            sbMetadata.append("");
            sbMetadata.append("");
            sbMetadata.append("");
            sbMetadata.append("");
            sbMetadata.append("");
            sbMetadata.append("");
            sbMetadata.append("");

            File spFile = new File(outputdir + "metadata.html");
            PrintWriter spWriter;
            spWriter = new PrintWriter(new BufferedWriter(new FileWriter(spFile)));
            spWriter.write(sbMetadata.toString());
            spWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(GDMWSController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    public static void main(String[] args) {
//        generateCharts("/Users/ajay/Downloads/1328584218973/");
//    }
}
