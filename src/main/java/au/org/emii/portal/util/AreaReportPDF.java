package au.org.emii.portal.util;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.wms.WMSStyle;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.layers.legend.Facet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * Created by a on 15/08/2014.
 */
public class AreaReportPDF {
    private static final Logger LOGGER = Logger.getLogger(AreaReportPDF.class);

    private static final String[] SPECIES_GROUPS = new String[]{"Algae", "Amphibians", "Angiosperms", "Animals", "Anthropods", "Bacteria"
            , "Birds", "Bryophytes", "Chromista", "Crustaceans", "Dicots", "FernsAndAllies", "Fish", "Fungi"
            , "Gymnosperms", "Insects", "Mammals", "Molluscs", "Monocots", "Plants", "Protozoa", "Reptiles" };


    private String wkt;
    private MapLayer mlArea;
    private String areaPid;
    private String areaName;
    private Map<String, String> counts;
    private Map<String, String> csvs;
    private Map<String, String> speciesLinks;
    private BiocacheQuery query;
    private String[] checklists;
    private Map<String, byte[]> imageMap;
    private RemoteMap remoteMap;
    private Map tabulation;
    private int fileNumber;

    private String filePath;

    public AreaReportPDF(String wkt, String areaName) {
        this.wkt = wkt;
        this.areaName = areaName;

        query = new BiocacheQuery(null, wkt, null, null, false, new boolean[]{true, true, true});

        remoteMap = new RemoteMapImpl();
        ((RemoteMapImpl) remoteMap).setLayerUtilities(new LayerUtilitiesImpl());

        filePath = "/data/webportal/data/area_" + System.currentTimeMillis();

        try {
            FileUtils.forceMkdir(new File(filePath + "/"));
        } catch (Exception e) {
            LOGGER.error("failed to create directory for PDF: " + filePath, e);
        }

        //query for images and data
        init();

        //transform data into html
        makeHTML();

        //transform html into pdf
        savePDF();
    }

    public static void main(String[] args) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("/data/webportal/config/webportal-config.properties"));
            CommonData.init(p);
        } catch (Exception e) {
            LOGGER.error("failed to load properties", e);
        }

        //String wkt = "POLYGON((112.0 -44.0,112.0 -11.0,154.0 -11.0,154.0 -44.0,112.0 -44.0))";
        String wkt = "POLYGON((149.26687622068 -35.258741390775,149.35579681395 -35.298540090399,149.33657073973 -35.320673151768,149.28404235838 -35.336638814392,149.24559020995 -35.322914136746,149.26687622068 -35.258741390775))";

        new AreaReportPDF(wkt, "My area");
    }

    public byte[] getPDF() {
        try {
            return FileUtils.readFileToByteArray(new File(filePath + "/output.pdf"));
        } catch (Exception e) {
            LOGGER.error("failed to get PDF from: " + filePath + "/output.pdf", e);
        }

        return null;
    }

    private void savePDF() {
        try {
            String[] inputHtmls = new String[fileNumber - 2];
            for (int i = 2; i < fileNumber; i++) {
                inputHtmls[i - 2] = filePath + "/report." + i + ".html";
            }

            makePDF(filePath + "/report.1.html", inputHtmls, filePath + "/output.pdf");

        } catch (Exception e) {
            LOGGER.error("failed to produce PDF", e);
        }
    }

    private void makeHTML() {
        //make report
        fileNumber = 1;
        try {
            //read data

            JSONObject tabulations = JSONObject.fromObject(FileUtils.readFileToString(new File(filePath + "/tabulations.json"), "UTF-8"));
            JSONObject csvs = JSONObject.fromObject(FileUtils.readFileToString(new File(filePath + "/csvs.json")));
            JSONObject counts = JSONObject.fromObject(FileUtils.readFileToString(new File(filePath + "/counts.json"), "UTF-8"));

            //header
            String filename = filePath + "/report.html";
            FileWriter fw = startHtmlOut(fileNumber, filename);

            //box summary
            fw.write("<img  id='imgHeader' src='" + CommonData.getWebportalServer() + "/area-report/header.jpg' width='100%' />");
            //fw.write("<div>AREA REPORT</div>");
            fw.write("<table id='dashboard' >");
            fw.write("<tr>");
            fw.write("<td>");
            fw.write("Area: " + String.format("%s", (counts.getString("Area (sq km)"))) + " sq km");
            fw.write("</td>");
            fw.write("<td>");
            fw.write("Species: " + String.format("%s", (counts.getString("Species"))));
            fw.write("</td>");
            fw.write("<td>");
            fw.write("Threatened Species: " + String.format("%s", counts.getString("Threatened Species")));
            fw.write("</td>");
            fw.write("</tr>");
            fw.write("<tr>");
            fw.write("<td>");
            fw.write("Species (spatially valid only): " + String.format("%s", counts.getString("Species (spatially valid only)")));
            fw.write("</td>");
            fw.write("<td>");
            fw.write("Endemic Species: " + String.format("%s", counts.getString("Endemic Species")));
            fw.write("</td>");
            fw.write("<td>");
            fw.write("Checklist Areas: " + String.format("%s", counts.getString("Checklist Areas")));
            fw.write("</td>");
            fw.write("</tr>");
            fw.write("<tr>");
            fw.write("<td>");
            fw.write("Occurrences: " + String.format("%s", counts.getString("Occurrences")));
            fw.write("</td>");
            fw.write("<td>");
            fw.write("Occurrences (spatially valid only): " + String.format("%s", counts.getString("Occurrences (spatially valid only)")));
            fw.write("</td>");
            fw.write("<td>");
            fw.write("Distribution Areas: " + String.format("%s", counts.getString("Distribution Areas")));
            fw.write("</td>");
            fw.write("</tr>");
            fw.write("<tr>");
            fw.write("<td>");
            fw.write("Checklist Species: " + String.format("%s", counts.getString("Checklist Species")));
            fw.write("</td>");
            fw.write("<td>");
            fw.write("");
            fw.write("</td>");
            fw.write("<td>");
            fw.write("");
            fw.write("</td>");
            fw.write("</tr>");

            fw.write("</table>");

            fw.write("</body></html>");
            fw.close();
            fileNumber++;
            fw = startHtmlOut(fileNumber, filename);
            //index page


            //map pages
            int figureNumber = 1;
            int tableNumber = 1;
            mapPage(fw, areaName, figureNumber, tableNumber, "base_area.png",
                    "Area: <b>" + String.format("%s", counts.getString("Area (sq km)")) + " sq km</b>",
                    null, null);
            fw.write("</body></html>");
            fw.close();
            fileNumber++;
            fw = startHtmlOut(fileNumber, filename);
            figureNumber++;
            mapPage(fw, "National Dynamic Land Cover", figureNumber, tableNumber, "dlcmv1.png",
                    "<br /><br />The Dynamic Land Cover Dataset is the first nationally consistent and thematically comprehensive land cover reference for Australia. It provides a base-line for reporting on change and trends in vegetation cover and extent. Information about land cover dynamics is essential to understanding and addressing a range of national challenges such as drought, salinity, water availability and ecosystem health. The data is a synopsis of land cover information for every 250m by 250m area of the country from April 2000 to April 2008. The classification scheme used to describe land cover categories in the Dataset conforms to the 2007 International Standards Organisation (ISO) land cover standard (19144-2). The Dataset shows Australian land covers clustered into 34 ISO classes. These reflect the structural character of vegetation, ranging from cultivated and managed land covers (crops and pastures) to natural land covers such as closed forest and open grasslands. [Ref1]" +
                            "<br /><br />Australia's Dynamic Land Cover: <a href='http://www.ga.gov.au/earth-observation/landcover.html'>http://www.ga.gov.au/earth-observation/landcover.html</a>" +
                            "<br /><br />National Dynamic Land Cover layer: Classification: Vegetation; Type: Contextual (polygonal); Metadata contact organisation: Geoscience Australia (GA). <a href='http://spatial.ala.org.au/ws/layers/view/more/dlcmv1'>http://spatial.ala.org.au/ws/layers/view/more/dlcmv1</a>",
                    tabulations.getJSONObject(CommonData.getLayerFacetName("dlcmv1"))
                    , "http://spatial.ala.org.au/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=dlcmv1");
            fw.write("</body></html>");
            fw.close();
            fileNumber++;
            fw = startHtmlOut(fileNumber, filename);
            figureNumber++;
            mapPage(fw, "Global Context Ecoregions", figureNumber, tableNumber, "teow.png",
                    "<br /><br />Terrestrial Ecoregions of the World (TEOW)" +
                            "<br /><br />Terrestrial Ecoregions of the World (TEOW) is a biogeographic regionalisation of the Earth's terrestrial biodiversity. Our biogeographic units are ecoregions, which are defined as relatively large units of land or water containing a distinct assemblage of natural communities sharing a large majority of species, dynamics, and environmental conditions. There are 867 terrestrial ecoregions, classified into 14 different biomes such as forests, grasslands, or deserts. Ecoregions represent the original distribution of distinct assemblages of species and communities. [Ref2]" +
                            "<br /><br />TEOW: <a href='http://worldwildlife.org/biome-categories/terrestrial-ecoregions'>http://worldwildlife.org/biome-categories/terrestrial-ecoregions</a>" +
                            "<br /><br />Terrestrial Ecoregional Boundaries layer: Classification: Biodiversity - Region; Type: Contextual (polygonal); Metadata contact organisation: The Nature Conservancy (TNC).  <a href='http://spatial.ala.org.au/ws/layers/view/more/1053'>http://spatial.ala.org.au/ws/layers/view/more/1053</a>",
                    tabulations.getJSONObject(CommonData.getLayerFacetName("teow")), null);
            fw.write("</body></html>");
            fw.close();
            fileNumber++;
//            fw = startHtmlOut(fileNumber, filename);
//            figureNumber++;
//            mapPage(fw, "Integrated Marine and Coastal Regionalisation of Australia (IMCRA)", figureNumber, "imcra4_pb.png",
//                    "<br /><br />The Integrated Marine and Coastal Regionalisation of Australia (IMCRA v4.0) is a spatial framework for classifying Australia's marine environment into bioregions that make sense ecologically and are at a scale useful for regional planning. These bioregions are the basis for the development of a National Representative System of Marine Protected Areas (NRSMPA). [Ref3]\n" +
//                            "<br /><br />IMCRA: <a href='http://www.environment.gov.au/node/18075'>http://www.environment.gov.au/node/18075</a>" +
//                            "<br /><br />IMCRA Regions layer: Classification: Biodiversity - Region; Type: Contextual (polygonal); Metadata contact organisation: Environmental Resources Information Network (ERIN).  <a href='http://spatial.ala.org.au/ws/layers/view/more/imcra4_pb'>http://spatial.ala.org.au/ws/layers/view/more/imcra4_pb</a>" +
//                            "<br /><br />Goals and Principles for the Establishment of the NRSMPA: <a href='http://www.environment.gov.au/resource/goals-and-principles-establishment-national-representative-system-marine-protected-areas'>http://www.environment.gov.au/resource/goals-and-principles-establishment-national-representative-system-marine-protected-areas</a>",
//                    tabulations.getJSONObject(CommonData.getLayerFacetName("imcra4_pb")));
//            fw.write("</body></html>");fw.close();
//            fileNumber++;
            fw = startHtmlOut(fileNumber, filename);
            figureNumber++;
            tableNumber++;
            mapPage(fw, "Freshwater Ecoregions of the World (FEOW)", figureNumber, tableNumber, "feow.png",
                    "<br /><br />Freshwater Ecoregions of the World (FEOW) is a collaborative project providing the first global biogeographic regionalization of the Earth's freshwater biodiversity, and synthesizing biodiversity and threat data for the resulting ecoregions. We define a freshwater ecoregion as a large area encompassing one or more freshwater systems that contains a distinct assemblage of natural freshwater communities and species. The freshwater species, dynamics, and environmental conditions within a given ecoregion are more similar to each other than to those of surrounding ecoregions and together form a conservation unit. [Ref5]" +
                            "<br /><br />FEOW: <a href='http://worldwildlife.org/biome-categories/freshwater-ecoregions'>http://worldwildlife.org/biome-categories/freshwater-ecoregions</a>" +
                            "<br /><br />Freshwater Ecoregions of the World layer: Classification: Biodiversity - Region; Type: Contextual (polygonal); Metadata contact organisation: TNC. <a href='http://spatial.ala.org.au/ws/layers/view/more/1052'>http://spatial.ala.org.au/ws/layers/view/more/1052</a>",
                    tabulations.getJSONObject(CommonData.getLayerFacetName("feow")), null);
            fw.write("</body></html>");
            fw.close();
            fileNumber++;
            fw = startHtmlOut(fileNumber, filename);
            figureNumber++;
            tableNumber++;

            //occurrences page
            int count = Integer.parseInt(counts.getString("Occurrences"));
            int countKosher = Integer.parseInt(counts.getString("Occurrences (spatially valid only)"));
            String imageUrl = StringConstants.OCCURRENCES + ".png";
            String notes = "Spatially valid records are considered those that do not have any type of flag questioning their location, for example a terrestrial species being recorded in the ocean. [Ref6]";
            speciesPage(true, fw, "My Area", "Occurrences", notes, tableNumber, count, countKosher, figureNumber, imageUrl,
                    null);
            figureNumber++;
            fw.write("</body></html>");
            fw.close();
            fileNumber++;
            fw = startHtmlOut(fileNumber, filename);

            //species pages
            count = Integer.parseInt(counts.getString("Species"));
            countKosher = Integer.parseInt(counts.getString("Species (spatially valid only)"));
            imageUrl = null;
            notes = "Spatially valid records are considered those that do not have any type of flag questioning their location, for example a terrestrial species being recorded in the ocean. [Ref6]";
            speciesPage(true, fw, "My Area", "Species", notes, tableNumber, count, countKosher, figureNumber, imageUrl,
                    csvs.getString("Species"));
            tableNumber++;
            fw.write("</body></html>");
            fw.close();
            fileNumber++;
            fw = startHtmlOut(fileNumber, filename);

            for (int i = 0; i < SPECIES_GROUPS.length; i++) {
                String s = SPECIES_GROUPS[i];
                count = Integer.parseInt(counts.getString(s));
                countKosher = Integer.parseInt(counts.getString(s + " (spatially valid only)"));
                speciesPage(true, fw, "My Area", "lifeform - " + s, notes, tableNumber, count, countKosher, figureNumber,
                        "lifeform - " + s + ".png", csvs.getString(s));
                tableNumber++;
                figureNumber++;
                fw.write("</body></html>");
                fw.close();
                fileNumber++;
                fw = startHtmlOut(fileNumber, filename);
            }

            //expert distributions
            count = Integer.parseInt(counts.getString("Distribution Areas"));
            speciesPage(false, fw, "My Area", "Expert Distributions", notes, tableNumber,
                    count, -1, figureNumber, null, csvs.getString(StringConstants.DISTRIBUTIONS));
            fw.write("</body></html>");
            fw.close();
            fileNumber++;
            fw = startHtmlOut(fileNumber, filename);
            count = Integer.parseInt(counts.getString("Checklist Areas"));
            speciesPage(false, fw, "My Area", "Checklist Areas", notes, tableNumber,
                    count, -1, figureNumber, null, csvs.getString(StringConstants.CHECKLISTS));
            fw.write("</body></html>");
            fw.close();
            fileNumber++;

        } catch (Exception e) {
            LOGGER.error("failed to produce report pdf", e);
        }
    }

    private void makePDF(String headerHtml, String[] inputHtmls, String outputPdf) {
        //generate pdf
        String[] cmdStart = new String[]{
                CommonData.getSettings().getProperty("wkhtmltopdf.path"),
            /* page margins (mm) */
                "-B", "10", "-L", "10", "-T", "10", "-R", "10",
            /* encoding */
                "--encoding", "UTF-8",
            /* footer settings */
                "--footer-font-size", "9",
                "--footer-line",
                "--footer-left", "    www.ala.org.au",
                "--footer-right", "Page [page] of [toPage]     "
        };

        String[] cmd = new String[cmdStart.length + 4 + inputHtmls.length + 2];
        System.arraycopy(cmdStart, 0, cmd, 0, cmdStart.length);
        cmd[cmdStart.length] = headerHtml;

        /* table of contents */
        cmd[cmdStart.length + 1] = "toc";
        cmd[cmdStart.length + 2] = "--xsl-style-sheet";
        cmd[cmdStart.length + 3] = filePath + "/toc.xsl";

        System.arraycopy(inputHtmls, 0, cmd, cmdStart.length + 4, inputHtmls.length);
        cmd[cmd.length - 2] = CommonData.getWebportalServer() + "/area-report/furtherLinks.html";

        cmd[cmd.length - 1] = outputPdf;

        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.environment().putAll(System.getenv());
        builder.redirectErrorStream(true);
        Process proc = null;
        try {
            proc = builder.start();

            proc.waitFor();
        } catch (Exception e) {
            LOGGER.error("error running wkhtmltopdf", e);
        }
    }

    private FileWriter startHtmlOut(int fileNumber, String filename) throws Exception {
        FileWriter fw = new FileWriter(filename.replace(".", "." + fileNumber + "."));
        fw.write("<html>");
        fw.write("<head><link rel='stylesheet' type='text/css' href='" + CommonData.getWebportalServer() + "/area-report/areaReport.css'></link></head>");
        fw.write("<body>");
        return fw;
    }

    private void speciesPage(boolean isSpecies, FileWriter fw, String areaName, String title, String notes, int tableNumber, int count, int countKosher, int figureNumber, String imageUrl, String csv) throws Exception {
        String imageUrlActual = imageUrl;
        if (imageUrlActual != null) {
            imageUrlActual = filePath + "/" + imageUrlActual;
        }
        fw.write("<table id='species'>");
        fw.write("<tr>");
        fw.write("<td id='title'><h1>");
        fw.write(title);
        fw.write("</h1></td>");
        fw.write("</tr><tr>");
        fw.write("<td>");
        fw.write("<br />Number of " + title.toLowerCase() + ": <b>" + count + "</b>");
        fw.write("</td>");
        fw.write("</tr><tr>");
        fw.write("<td><br />");
        fw.write(notes);
        fw.write("</td>");
        fw.write("</tr><tr>");
        if (countKosher >= 0) {
            fw.write("<td>");
            fw.write("<br />Number of " + title.toLowerCase() + " (spatially valid only): <b>" + countKosher + "</b>");
            fw.write("</td>");
            fw.write("</tr><tr>");
        }

        if ((count > 0 || countKosher > 0) && imageUrlActual != null) {
            fw.write("<td>");
            fw.write("<br /><img src='" + imageUrlActual + "' />");
            fw.write("</td>");
            fw.write("</tr><tr>");
            fw.write("<td id='figure'>");
            fw.write("<b>Figure " + figureNumber + ":</b> Map of " + title + " in " + areaName);
            fw.write("</td>");
            fw.write("</tr><tr>");
        }

        //tabulation table
        if ((count > 0 || countKosher > 0) && csv != null) {
            CSVReader r = new CSVReader(new StringReader(csv));

            fw.write("<td id='tableNumber'><br /><b>Table " + tableNumber + ":</b> " + title);
            if (speciesLinks.get(title.replace("lifeform - ", "")) != null) {
                fw.write("<a href='" + speciesLinks.get(title.replace("lifeform - ", "")) + "'>(Link to full list)</a>");
            }
            fw.write("</td></tr><tr><td>");

            fw.write("<table id='table'>");

            //reorder select columns
            int[] columnOrder;
            if (isSpecies) {
                columnOrder = new int[]{8, 1, 10, 11};
                fw.write("<tr><td>Family</td><td id='scientificName' >Scientific Name</td><td>Common Name</td><td>No. Occurrences</td></tr>");
            } else {
                columnOrder = new int[]{4, 1, 3, 7, 8, 11, 12};
                fw.write("<tr><td>Family</td><td id='scientificName' >Scientific Name</td><td>Common Name</td><td>Min Depth</td><td>Max Depth</td><td>Area Name</td><td>Area sq km</td></tr>");
            }
            //use fixed header
            r.readNext();

            String[] line;
            int row = 0;
            while ((line = r.readNext()) != null) {
                fw.write("<tr>");
                for (int i = 0; i < columnOrder.length && columnOrder[i] < line.length; i++) {
                    fw.write("<td><div>" + line[columnOrder[i]] + "</div></td>");
                }
                fw.write("</tr>");

                row++;
            }

            fw.write("</table>");
            fw.write("<td>");
        }

        fw.write("</tr>");
        fw.write("</table>");
    }

    private void mapPage(FileWriter fw, String areaName, int figureNumber, int tableNumber, String imageUrl, String notes, JSONObject tabulation, String legendUrl) throws Exception {
        String imageUrlActual = imageUrl;
        if (imageUrlActual != null) {
            imageUrlActual = filePath + "/" + imageUrlActual;
        }
        fw.write("<table id='mapPage'>");
        fw.write("<tr>");
        fw.write("<td id='title'><h1>");
        fw.write(areaName);
        fw.write("</h1></td>");
        fw.write("</tr><tr>");
        fw.write("<td><br />");
        fw.write(notes);
        fw.write("</td>");
        fw.write("</tr><tr>");
        fw.write("<td>");
        if (imageUrlActual.endsWith("base_area.png")) {
            fw.write("<br /><img src='" + filePath + "/base_area_zoomed_out.png' />");
        }
        fw.write("<br /><img " + (legendUrl != null ? "id='imgWithLegend' " : "") + " src='" + imageUrlActual + "' />");
        if (legendUrl != null) {
            fw.write("<img id='legend' src='" + legendUrl + "'/>");
        }
        fw.write("</td>");
        fw.write("</tr><tr>");
        fw.write("<td id='figure'>");
        fw.write("<b>Figure " + figureNumber + ":</b> Map of " + areaName);
        fw.write("</td>");
        fw.write("</tr><tr>");

        //tabulation table
        if (tabulation != null) {
            double totalArea = 0;
            for (Object o : tabulation.getJSONArray("tabulationList")) {
                JSONObject jo = (JSONObject) o;
                totalArea += Double.parseDouble(jo.getString("area")) / 1000000.0;
            }

            if (totalArea > 0) {
                fw.write("<td id='tableNumber'>");
                fw.write("<br /><b>Table " + tableNumber + ":</b> " + areaName);
                fw.write("</td></tr><tr><td>");
                fw.write("<br /><table id='table'><tr><td>Class/Region</td><td>Area (sq km)</td><td>% of total area</td></tr>");

                for (Object o : tabulation.getJSONArray("tabulationList")) {
                    JSONObject jo = (JSONObject) o;
                    fw.write("<tr><td>");
                    fw.write(jo.getString("name1"));
                    fw.write("</td><td>");
                    fw.write(String.format("%.2f", Double.parseDouble(jo.getString("area")) / 1000000.0));
                    fw.write("</td><td>");
                    fw.write(String.format("%.2f", Double.parseDouble(jo.getString("area")) / 1000000.0 / totalArea * 100));
                    fw.write("</td></tr>");
                }

                fw.write("</table>");
                fw.write("</td>");
            }
        }

        fw.write("</tr>");
        fw.write("</table>");
    }

    final void init() {
        counts = new HashMap<String, String>();
        csvs = new HashMap<String, String>();
        imageMap = new HashMap<String, byte[]>();
        tabulation = new HashMap();
        speciesLinks = new HashMap<String, String>();

        mlArea = createWKTLayer(wkt, 255, 0, 0, 0.6f);

        initTabulation();

        initImages();

        initCountArea();
        initCountSpecies();
        initCsvSpecies();
        initCountOccurrences();
        initCountEndemicSpecies();
        initCountThreatenedSpecies();
        initCountChecklistAreasAndSpecies();
        initCountDistributionAreas();

        initDistributionsCsv(StringConstants.DISTRIBUTIONS);
        initDistributionsCsv(StringConstants.CHECKLISTS);

        try {
            FileWriter fw = new FileWriter(filePath + File.separator + "counts.json");
            fw.write(JSONObject.fromObject(counts).toString());
            fw.close();

            fw = new FileWriter(filePath + File.separator + "tabulations.json");
            fw.write(JSONObject.fromObject(tabulation).toString());
            fw.close();

            fw = new FileWriter(filePath + File.separator + "csvs.json");
            fw.write(JSONObject.fromObject(csvs).toString());
            fw.close();

            FileUtils.copyURLToFile(new URL(CommonData.getWebportalServer() + "/area-report/toc.xsl"),
                    new File(filePath + "/toc.xsl"));
        } catch (Exception e) {
            LOGGER.error("failed to output area report information");
        }

    }

    private void initTabulation() {

        String fid = CommonData.getLayerFacetName("dlcmv1");
        tabulation.put(fid, JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/tabulation/" + fid + "/" + areaPid + ".json")));

        fid = CommonData.getLayerFacetName("teow");
        tabulation.put(fid, JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/tabulation/" + fid + "/" + areaPid + ".json")));

        //fid = CommonData.getLayerFacetName("meow_ecos");
        //tabulation.put(fid,JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/tabulation/" + fid + "/" + areaPid + ".json")));

        fid = CommonData.getLayerFacetName("feow");
        tabulation.put(fid, JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/tabulation/" + fid + "/" + areaPid + ".json")));

        fid = CommonData.getLayerFacetName("imcra4_pb");
        //tabulation.put(fid,JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/tabulation/" + fid + "/" + areaPid + ".json")));
    }

    private void initDistributionsCsv(String type) {
        StringBuilder sb = new StringBuilder();
        for (String line : Util.getDistributionsOrChecklists(type, wkt, null, null)) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        csvs.put(type, sb.toString());
    }


    private void initCsvSpecies() {
        csvs.put("Species", query.speciesList());
        speciesLinks.put("Species", query.getWS() + "/occurrences/search?q=" + query.getQ());

        for (int i = 0; i < SPECIES_GROUPS.length; i++) {
            String s = SPECIES_GROUPS[i];
            BiocacheQuery q = query.newFacet(new Facet("species_group", s, true), false);
            csvs.put(s, q.speciesList());
            speciesLinks.put(s, q.getWS() + "/occurrences/search?q=" + q.getQ());
            counts.put(s, String.valueOf(q.getSpeciesCount()));
            counts.put(s + " (spatially valid only)", String.valueOf(q.getSpeciesCountKosher()));
        }
    }

    private void initCountSpecies() {
        counts.put("Species", String.valueOf(query.getSpeciesCount()));
        counts.put("Species (spatially valid only)", String.valueOf(query.getSpeciesCountKosher()));
    }

    private void initCountOccurrences() {
        counts.put("Occurrences", String.valueOf(query.getOccurrenceCount()));
        counts.put("Occurrences (spatially valid only)", String.valueOf(query.getOccurrenceCountKosher()));
        speciesLinks.put("Occurrences", query.getWS() + "/occurrences/search?q=" + query.getQ());
    }

    private void initCountEndemicSpecies() {
        counts.put("Endemic Species", String.valueOf(query.getEndemicSpeciesCount()));
    }

    private void initCountThreatenedSpecies() {
        Facet f = new Facet("state_conservation", "Endangered", true);
        counts.put("Threatened Species", String.valueOf(query.newFacet(f, false).getSpeciesCount()));
    }

    private void initCountChecklistAreasAndSpecies() {
        checklists = Util.getDistributionsOrChecklists(StringConstants.CHECKLISTS, null, wkt, null);

        if (checklists.length <= 0) {
            counts.put("Checklist Areas", "0");
            counts.put("Checklist Species", "0");
        } else {
            String[] areaChecklistText = Util.getAreaChecklists(checklists);
            counts.put("Checklist Areas", String.valueOf(areaChecklistText.length - 1));
            counts.put("Checklist Species", String.valueOf(checklists.length - 1));
        }
    }

    private void initCountDistributionAreas() {
        String[] distributions = Util.getDistributionsOrChecklists(StringConstants.DISTRIBUTIONS, null, wkt, null);

        if (checklists.length <= 0) {
            counts.put("Distribution Areas", "0");
        } else {
            counts.put("Distribution Areas", String.valueOf(distributions.length - 1));
        }
    }

    private void initCountArea() {
        DecimalFormat df = new DecimalFormat("###,###.##");
        counts.put("Area (sq km)", df.format(Util.calculateArea(wkt) / 1000.0 / 1000.0));
    }

    private void initImages() {
        double aspectRatio = 1.6;
        String type = "png";
        int resolution = 0;

        String basemap = "Minimal";

        mlArea.setColourMode("hatching");

        List<Double> bbox = mlArea.getMapLayerMetadata().getBbox();
        //30% width buffer in decimal degrees
        double step = (bbox.get(2) - bbox.get(0)) * 0.3;
        double[] extents = new double[]{bbox.get(0) - step, bbox.get(1) - step, bbox.get(2) + step, bbox.get(3) + step};
        step = (bbox.get(2) - bbox.get(0)) * 0.05;
        double[] extentsSmall = new double[]{bbox.get(0) - step, bbox.get(1) - step, bbox.get(2) + step, bbox.get(3) + step};
        step = (bbox.get(2) - bbox.get(0)) * 10;
        double[] extentsLarge = new double[]{bbox.get(0) - step, bbox.get(1) - step, bbox.get(2) + step, bbox.get(3) + step};
        if (extentsLarge[2] > 180) {
            extentsLarge[2] = 180;
        }
        if (extentsLarge[0] < -180) {
            extentsLarge[0] = -180;
        }
        if (extentsLarge[1] < -85) {
            extentsLarge[1] = -85;
        }
        if (extentsLarge[3] > 85) {
            extentsLarge[3] = 85;
        }

        MapLayer mlSpecies = createSpeciesLayer(query, 0, 0, 255, .6f, false, 9, false);

        List<MapLayer> lifeforms = new ArrayList<MapLayer>();
        for (int i = 0; i < SPECIES_GROUPS.length; i++) {
            String s = SPECIES_GROUPS[i];
            lifeforms.add(createSpeciesLayer(query.newFacet(new Facet("species_group", s, true), false), 0, 0, 255, .6f, false, 9, false));
        }

        MapLayer mlDynamicLand = createLayer("dlcmv1", 1.0f);
        MapLayer mlTeow = createLayer("teow", 1.0f);
        mlTeow.setColourMode("&styles=teow&format_options=dpi:600");
        //MapLayer mlMeow = createLayer("meow_ecos", 1.0f);
        //mlMeow.setColourMode("&styles=meow_ecos");
        MapLayer mlFeow = createLayer("feow", 1.0f);
        mlFeow.setColourMode("&styles=feow&format_options=dpi:600");
        //MapLayer mlImcra = createLayer("imcra4_pb", 1.0f);
        //mlImcra.setColourMode("&styles=imcra4_pb&format_options=dpi:600");

        imageMap.put("base_area", new PrintMapComposer(extents, basemap, new MapLayer[]{mlArea}, aspectRatio, "", type, resolution).get());

        imageMap.put("base_area_zoomed_out", new PrintMapComposer(extentsLarge, basemap, new MapLayer[]{mlArea}, aspectRatio, "", type, resolution).get());

        imageMap.put(StringConstants.OCCURRENCES, new PrintMapComposer(extentsSmall, basemap, new MapLayer[]{mlArea, mlSpecies}, aspectRatio, "", type, resolution).get());

        for (int i = 0; i < SPECIES_GROUPS.length; i++) {
            imageMap.put("lifeform - " + SPECIES_GROUPS[i], new PrintMapComposer(extentsSmall, basemap, new MapLayer[]{mlArea, lifeforms.get(i)}, aspectRatio, "", type, resolution).get());
        }

        imageMap.put("dlcmv1", new PrintMapComposer(extents, basemap, new MapLayer[]{mlArea, mlDynamicLand}, aspectRatio, "", type, resolution).get());

        imageMap.put("teow", new PrintMapComposer(extents, basemap, new MapLayer[]{mlArea, mlTeow}, aspectRatio, "", type, resolution).get());

        //imageMap.put("meow_ecos", new PrintMapComposer(extents, basemap, new MapLayer[]{mlArea, mlMeow}, aspectRatio, "", type, resolution).get());

        imageMap.put("feow", new PrintMapComposer(extents, basemap, new MapLayer[]{mlArea, mlFeow}, aspectRatio, "", type, resolution).get());

        //imageMap.put("imcra4_pb", new PrintMapComposer(extents, basemap, new MapLayer[]{mlArea, mlImcra}, aspectRatio, "", type, resolution).get());

        //save images
        for (String key : imageMap.keySet()) {
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath + File.separator + key + ".png"));
                bos.write(imageMap.get(key));
                bos.close();
            } catch (Exception e) {
                LOGGER.error("failed to write image to: " + filePath, e);
            }
        }
    }

    MapLayer createLayer(String layerName, float opacity) {
        String uid;
        String type = "";
        String treeName = "";
        String treePath = "";
        String legendurl = "";
        String metadata = "";
        JSONArray layerlist = CommonData.getLayerListJSONArray();
        for (int j = 0; j < layerlist.size(); j++) {
            JSONObject jo = layerlist.getJSONObject(j);
            String name = jo.getString(StringConstants.NAME);
            if (name.equals(layerName)) {
                uid = jo.getString(StringConstants.ID);
                type = jo.getString(StringConstants.TYPE);
                treeName = StringUtils.capitalize(jo.getString(StringConstants.DISPLAYNAME));
                treePath = jo.getString("displaypath");
                legendurl = CommonData.getGeoServer() + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + layerName;
                metadata = CommonData.getLayersServer() + "/layers/view/more/" + uid;
                break;
            }
        }

        return addWMSLayer(layerName, treeName, treePath, opacity, metadata, legendurl,
                StringConstants.ENVIRONMENTAL.equalsIgnoreCase(type) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL, null, null, null);
    }

    MapLayer createWKTLayer(String wkt, int red, int green, int blue, float opacity) {

        String pid = UserShapes.upload(wkt, "", "", "area_report", CommonData.getSettings().getProperty("api_key"));

        areaPid = pid;
        if (pid != null) {
            //1. create new layer
            MapLayer newml = addObjectByPid(pid, red, green, blue, opacity);
            newml.setDisplayed(true);
            return newml;
        } else {
            LOGGER.error("failed to upload wkt to layers-service");
        }
        return null;
    }

    public MapLayer addObjectByPid(String pid, int red, int green, int blue, float opacity) {

        JSONObject obj = JSONObject.fromObject(Util.readUrl(CommonData.getLayersServer() + "/object/" + pid));
        //add feature to the map as a new layer
        MapLayer mapLayer = addWMSLayer("PID:" + pid, "", obj.getString(StringConstants.WMSURL), opacity, null, null, LayerUtilitiesImpl.WKT, null, null, null);

        mapLayer.setPolygonLayer(true);

        //if the layer is a point create a radius
        String bbox = obj.getString(StringConstants.BBOX);
        MapLayerMetadata md = mapLayer.getMapLayerMetadata();

        try {
            double[][] bb = SimpleShapeFile.parseWKT(bbox).getBoundingBox();
            List<Double> dbb = new ArrayList<Double>();
            dbb.add(bb[0][0]);
            dbb.add(bb[0][1]);
            dbb.add(bb[1][0]);
            dbb.add(bb[1][1]);
            md.setBbox(dbb);
        } catch (Exception e) {
            LOGGER.debug("failed to parse: " + bbox, e);
        }

        mapLayer.setRedVal(red);
        mapLayer.setGreenVal(green);
        mapLayer.setBlueVal(blue);
        mapLayer.setOpacity(opacity);

        mapLayer.setDynamicStyle(true);

        return mapLayer;
    }

    MapLayer createSpeciesLayer(Query query, int red, int green, int blue, float opacity, boolean grid, int size, boolean uncertainty) {
        Color c = new Color(red, green, blue);
        String hexColour = Integer.toHexString(c.getRGB() & 0x00ffffff);
        String envString = "";
        if (grid) {
            //colour mode is in 'filter' but need to move it to envString
            envString += "colormode:grid";
        } else {
            envString = "color:" + hexColour;
        }
        envString += ";name:circle;size:" + size + ";opacity:1";
        if (uncertainty) {
            envString += ";uncertainty:1";
        }

        String uri = query.getUrl();
        uri += "service=WMS&version=1.1.0&request=GetMap&format=image/png";
        uri += "&layers=ALA:occurrences";
        uri += "&transparent=true";
        uri += (query.getQc() == null ? "" : query.getQc());
        uri += "&CQL_FILTER=";

        MapLayer ml = addWMSLayer("", "", uri + query.getQ(), opacity, null, null, LayerUtilitiesImpl.SPECIES, "", envString, query);

        ml.setDynamicStyle(true);
        ml.setEnvParams(envString);

        // for the sizechooser
        ml.setGeometryType(LayerUtilitiesImpl.POINT);

        ml.setBlueVal(blue);
        ml.setGreenVal(green);
        ml.setRedVal(red);
        ml.setSizeVal(3);
        ml.setOpacity(opacity);

        ml.setClustered(false);

        ml.setSpeciesQuery(query);

        return ml;
    }

    public MapLayer addWMSLayer(String name, String displayName, String uri, float opacity, String metadata, String legendUri, int subType, String cqlfilter, String envParams, Query q) {
        MapLayer mapLayer = remoteMap.createAndTestWMSLayer(name, uri, opacity);
        mapLayer.setDisplayName(displayName);
        if (q != null) {
            mapLayer.setSpeciesQuery(q);
        }

        //ok
        mapLayer.setSubType(subType);
        mapLayer.setCql(cqlfilter);
        mapLayer.setEnvParams(envParams);
        String uriActual = CommonData.getGeoServer() + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + mapLayer.getLayer();
        mapLayer.setDefaultStyleLegendUri(uriActual);
        if (metadata != null) {
            if (metadata.startsWith("http")) {
                mapLayer.getMapLayerMetadata().setMoreInfo(metadata + "\n" + displayName);
            } else {
                mapLayer.getMapLayerMetadata().setMoreInfo(metadata);
            }
        }
        if (legendUri != null) {
            WMSStyle style = new WMSStyle();
            style.setName(StringConstants.DEFAULT);
            style.setDescription("Default style");
            style.setTitle(StringConstants.DEFAULT);
            style.setLegendUri(legendUri);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);
            LOGGER.debug("adding WMSStyle with legendUri: " + legendUri);
            mapLayer.setDefaultStyleLegendUriSet(true);
        }

        mapLayer.setDisplayed(true);

        return mapLayer;
    }

}
