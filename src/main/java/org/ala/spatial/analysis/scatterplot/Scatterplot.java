package org.ala.spatial.analysis.scatterplot;

import au.com.bytecode.opencsv.CSVReader;
import org.ala.layers.client.Client;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.layers.legend.Legend;
import org.ala.layers.legend.LegendObject;
import org.ala.layers.util.Occurrences;
import org.ala.layers.util.SpatialUtil;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.GridCutter;
import org.ala.spatial.util.Zipper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.util.FileCopyUtils;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

import org.slf4j.LoggerFactory;


/**
 * Created by a on 10/03/2014.
 */
public class Scatterplot {

    LoggerFactory lf;

    private static Logger logger = Logger.getLogger(Scatterplot.class);

    private static final String NUMBER_SERIES = "Number series";
    private static final String ACTIVE_AREA_SERIES = "In Active Area";

    private static final String[][] facetNameExceptions = {{"cl22", "state"}, {"cl20", "ibra"}, {"cl21", "imcra"}};

    JFreeChart jChart;
    XYPlot plot;
    ChartRenderingInfo chartRenderingInfo;
    XYBoxAnnotation annotation;
    XYZDataset xyzDataset;
    XYZDataset aaDataset;
    XYZDataset backgroundXyzDataset;
    XYZDataset blockXYZDataset = null;
    XYBlockRenderer xyBlockRenderer = null;

    LegendObject legend;

    ScatterplotDTO scatterplotDTO;
    ScatterplotStyleDTO scatterplotStyleDTO;
    ScatterplotDataDTO scatterplotDataDTO;

    //for scatterplot
    String imagePath;
    String imageURL;

    //for scatterplot list
    private String htmlURL;
    private String downloadURL;

    String cutDataPath = null;

    //Layers index for columns 1 and 2.  This is variable when constructing a scatterplot list.
    int col1 = 0;
    int col2 = 1;

    //remember colourMode related data queries
    HashMap<String, LegendObject> legends = new HashMap<String, LegendObject>();

    public Scatterplot(ScatterplotDTO scatterplotDTO, ScatterplotStyleDTO scatterplotStyleDTO, ScatterplotDataDTO scatterplotDataDTO) {
        this.scatterplotDTO = scatterplotDTO;

        if (scatterplotDTO.getId() == null) {
            scatterplotDTO.setId(String.valueOf(System.currentTimeMillis()));
        }

        if (scatterplotStyleDTO == null) {
            this.scatterplotStyleDTO = new ScatterplotStyleDTO();
        } else {
            this.scatterplotStyleDTO = scatterplotStyleDTO;
        }

        if (scatterplotDataDTO == null) {
            this.scatterplotDataDTO = new ScatterplotDataDTO();
        } else {
            this.scatterplotDataDTO = scatterplotDataDTO;
        }

        //if >2 layers, build all of them and html for displaying them
        int layercount = scatterplotDTO.getLayers().length;
        String[] layers = scatterplotDTO.getLayers();
        if (layercount > 2) {
            //scatterplot list, build all of them
            // this should to be put into ScatterplotStore
            for (int i = 0; i < layercount - 1; i++) {
                for (int j = i + 1; j < layercount; j++) {
                    col1 = i;
                    col2 = j;
                    buildScatterplot();

                    renderScatterplot("_" + layers[i] + "_" + layers[j]);
                }
            }

            makeHtml();

            makeZip();

            htmlURL = AlaspatialProperties.getBaseOutputURL() + "/output/scatterplot/" + scatterplotDTO.getId() + "/scatterplot.html";
            downloadURL = AlaspatialProperties.getBaseOutputURL() + "/output/scatterplot/" + scatterplotDTO.getId() + "/scatterplot.zip";
        } else {
            //build just this one
            // this can be added into ScatterplotStore
            buildScatterplot();

            imageURL = AlaspatialProperties.getAlaspatialUrl() + "ws/scatterplot/" + scatterplotDTO.getId() + ".png";

        }
    }

    private void makeZip() {
        String uid = scatterplotDTO.getId();
        String pth = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "scatterplot" + File.separator + uid + File.separator;
        String tmpFile = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "scatterplot" + File.separator + "scatterplot.zip";
        Zipper.zipDirectory(pth, tmpFile);
        try {
            FileUtils.moveFileToDirectory(new File(tmpFile),
                    new File(pth), false);
        } catch (IOException e) {
            logger.error("failed to zip scatterplot list", e);
        }
    }

    void buildScatterplot() {

        resample(true);
        createDataset(true);

        if (scatterplotDTO.getBackgroundOccurrencesBs() != null) {
            resample(false);
            createDataset(false);
        }

        createAADataset();

        //Annotation changes are applied in isolation to other style changes.
        //Identify annotation changes so as little as possible needs to be redone.
        boolean annotation_change;
        double[] old_annotation = (scatterplotDataDTO != null
                && scatterplotStyleDTO.getPrevSelection() != null) ?
                scatterplotStyleDTO.getPrevSelection() : null;
        if (scatterplotStyleDTO.getSelection() != null && old_annotation != null) {
            annotation_change = scatterplotStyleDTO.getSelection()[0] != old_annotation[0]
                    || scatterplotStyleDTO.getSelection()[1] != old_annotation[1]
                    || scatterplotStyleDTO.getSelection()[2] != old_annotation[2]
                    || scatterplotStyleDTO.getSelection()[3] != old_annotation[3];
        } else if (old_annotation == null && scatterplotStyleDTO.getSelection() == null) {
            annotation_change = false;
        } else {
            annotation_change = true;
        }

        //identify layer changes
        boolean layer_change = scatterplotDataDTO.getLayer1() == null || !scatterplotDataDTO.getLayer1().equals(scatterplotDTO.getLayers()[col1])
                || scatterplotDataDTO.getLayer2() == null || !scatterplotDataDTO.getLayer2().equals(scatterplotDTO.getLayers()[col2]);

        //active area must be drawn first
        boolean chart_rebuild = false;
        if (jChart == null || !annotation_change || layer_change) {
            chart_rebuild = true;
            if (scatterplotStyleDTO.getHighlightWkt() != null && aaDataset != null) {
                jChart = ChartFactory.createScatterPlot(
                        scatterplotDTO.getForegroundName()
                        , scatterplotDTO.getLayernames()[col1]
                        , scatterplotDTO.getLayernames()[col2]
                        , aaDataset
                        , PlotOrientation.HORIZONTAL, false, false, false);
            } else {
                jChart = ChartFactory.createScatterPlot(
                        scatterplotDTO.getForegroundName()
                        , scatterplotDTO.getLayernames()[col1]
                        , scatterplotDTO.getLayernames()[col2]
                        , xyzDataset
                        , PlotOrientation.HORIZONTAL, false, false, false);
            }

            jChart.setBackgroundPaint(Color.white);

            plot = (XYPlot) jChart.getPlot();
        }

        if (annotation != null && !chart_rebuild) {
            plot.removeAnnotation(annotation);
        }

        if (scatterplotStyleDTO.getSelection() != null) {
            annotation = new XYBoxAnnotation(
                    scatterplotStyleDTO.getSelection()[1]
                    , scatterplotStyleDTO.getSelection()[0]
                    , scatterplotStyleDTO.getSelection()[3]
                    , scatterplotStyleDTO.getSelection()[2]);
            plot.addAnnotation(annotation);
        }

        if (chart_rebuild) {
            Font axisfont = new Font("Arial", Font.PLAIN, 10);
            Font titlefont = new Font("Arial", Font.BOLD, 11);
            plot.getDomainAxis().setLabelFont(axisfont);
            plot.getDomainAxis().setTickLabelFont(axisfont);
            plot.getRangeAxis().setLabelFont(axisfont);
            plot.getRangeAxis().setTickLabelFont(axisfont);
            plot.setBackgroundPaint(new Color(160, 220, 220));
            plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
            jChart.getTitle().setFont(titlefont);

            //colours
            String[] seriesNames = {scatterplotDTO.getForegroundName()};
            if (legend != null) {
                seriesNames = legend.getCategoryNameOrder();
            }
            int[] seriesColours = new int[scatterplotDataDTO.getSeries().length];
            for (int i = 0; i < seriesNames.length; i++) {
                if (legend == null) {
                    seriesColours[i] = scatterplotStyleDTO.getRed() << 16 | scatterplotStyleDTO.getGreen() << 8 | scatterplotStyleDTO.getBlue() | 0xff000000;
                } else {
                    seriesColours[i] = legend.getColour(seriesNames[i]);
                }
            }

            if (aaDataset != null) {
                plot.setRenderer(getActiveAreaRenderer());

                int datasetCount = plot.getDatasetCount();
                plot.setDataset(datasetCount, xyzDataset);
                plot.setRenderer(datasetCount, getRenderer(legend, seriesColours, seriesNames, scatterplotDataDTO.getSeriesValues()));
            } else {
                XYShapeRenderer sr = getRenderer(legend, seriesColours, seriesNames, scatterplotDataDTO.getSeriesValues());
                plot.setRenderer(sr);
            }

            //add points background
            if (backgroundXyzDataset != null) {
                int datasetCount = plot.getDatasetCount();
                plot.setDataset(datasetCount, backgroundXyzDataset);
                plot.setRenderer(datasetCount, getBackgroundRenderer());
            }

            //add block background
            if (scatterplotDTO.isEnvGrid()) {
                NumberAxis x = (NumberAxis) plot.getDomainAxis();
                NumberAxis y = (NumberAxis) plot.getRangeAxis();

                //does data already exist?
                createBlockPlot((float) x.getLowerBound(), (float) x.getUpperBound(), (float) y.getLowerBound(), (float) y.getUpperBound());

                addBlockPlot(plot,
                        scatterplotDTO.getLayers()[col1],
                        scatterplotDTO.getLayernames()[col1],
                        x.getLowerBound(),
                        x.getUpperBound(),
                        scatterplotDTO.getLayers()[col2],
                        scatterplotDTO.getLayernames()[col2],
                        y.getLowerBound(),
                        y.getUpperBound());
            }

            chartRenderingInfo = new ChartRenderingInfo();
        }

        //save stuff for later comparisons
        storeSettingsToDetectChanges();
    }

    void storeSettingsToDetectChanges() {
        scatterplotStyleDTO.setPrevSelection(scatterplotStyleDTO.getSelection());
        scatterplotStyleDTO.setPrevHighlightWKT(scatterplotStyleDTO.getHighlightWkt());

        scatterplotDataDTO.setColourMode(scatterplotStyleDTO.getColourMode());
        scatterplotDataDTO.setLayer1(scatterplotDataDTO.getLayer1());
        scatterplotDataDTO.setLayer2(scatterplotDataDTO.getLayer2());
    }

    void renderScatterplot(String filename_part) {
        if (filename_part == null) {
            filename_part = "";
        }

        int width = scatterplotStyleDTO.getWidth();
        int height = scatterplotStyleDTO.getHeight();

        try {

            BufferedImage bi = jChart.createBufferedImage(width, height, BufferedImage.TRANSLUCENT, chartRenderingInfo);
            byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);

            //save to file
            String uid = scatterplotDTO.getId();
            String pth = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "scatterplot" + File.separator + uid + File.separator;

            File sessfolder = new File(pth);
            if (!sessfolder.exists()) {
                sessfolder.mkdirs();
            }

            imagePath = pth + uid + filename_part + ".png";
            imageURL = AlaspatialProperties.getBaseOutputURL() + "/output/scatterplot/" + uid + "/" + uid + filename_part + ".png";

            try {
                FileOutputStream fos = new FileOutputStream(imagePath);
                fos.write(bytes);
                fos.close();
            } catch (Exception e) {
                logger.error("error writing scatterplot png", e);
            }

        } catch (Exception e) {
            logger.error("error producing scatterplot", e);
        }
    }

    public String getCSV() {
        //make output csv; id, series, layer1, layer2, highlight
        StringBuilder sb = new StringBuilder();
        String[] series = scatterplotDataDTO.getSeries();
        String[] ids = scatterplotDataDTO.getIds();
        double[][] d = scatterplotDataDTO.getData();

        sb.append("id,series");
        for (int j = 0; j < d[0].length; j++) {
            sb.append(",\"").append(scatterplotDTO.getLayers()[j].replace("\"", "\"\"")).append("\"");
        }
        if (scatterplotDTO.isEnvGrid()) {
            sb.append(",environmental envelope area (sq km)");
        }
        for (int i = 0; i < series.length; i++) {
            sb.append("\n\"").append(ids[i].replace("\"", "\"\"")).append("\",\"")
                    .append(series[i].replace("\"", "\"\"")).append("\"");
            for (int j = 0; j < d[0].length; j++) {
                sb.append(",").append(String.valueOf(d[i][j]));
            }
            if (scatterplotDataDTO.getGridData() != null) {
                int pos1 = java.util.Arrays.binarySearch(scatterplotDataDTO.getGridCutoffs()[0], (float) d[i][0]);
                if (pos1 < 0) {
                    pos1 = (pos1 * -1) - 1;
                }
                int pos2 = java.util.Arrays.binarySearch(scatterplotDataDTO.getGridCutoffs()[1], (float) d[i][1]);
                if (pos2 < 0) {
                    pos2 = (pos2 * -1) - 1;
                }
                if (pos1 >= 0 && pos1 < scatterplotDataDTO.getGridData().length
                        && pos2 >= 0 && pos2 < scatterplotDataDTO.getGridData()[pos1].length) {
                    sb.append(",").append(scatterplotDataDTO.getGridData()[pos1][pos2]);
                }
            }
        }

        return sb.toString();
    }

    void createBlockPlot(float min1, float max1, float min2, float max2) {
        int divisions = scatterplotDTO.getGridDivisions();

        //get linear cutoffs
        float[][] cutoffs = new float[2][divisions];
        for (int j = 0; j < divisions; j++) {
            cutoffs[0][j] = (float) (min1 + (max1 - min1) * ((j + 1) / (float) divisions));
            cutoffs[1][j] = (float) (min2 + (max2 - min2) * ((j + 1) / (float) divisions));
        }
        cutoffs[0][divisions - 1] = max1;  //set max
        cutoffs[1][divisions - 1] = max2;  //set max
        scatterplotDataDTO.setGridCutoffs(cutoffs);

        double[][] d = new double[divisions][divisions];
        scatterplotDataDTO.setGridData(d);

        divisions = scatterplotDTO.getGridDivisions();

        LayerFilter[] envelope = null;
        SimpleRegion region = null;

        if (scatterplotDTO.getFilterWkt() != null && scatterplotDTO.getFilterWkt().startsWith("ENVELOPE")) {
            envelope = LayerFilter.parseLayerFilters(scatterplotDTO.getFilterWkt());
        } else {
            region = SimpleShapeFile.parseWKT(scatterplotDTO.getFilterWkt());
        }

        //get grid data
        float[][] data = new float[2][];
        String[] layers = scatterplotDTO.getLayers();

        if (cutDataPath == null) {  //only need to cut once if rebuilding
            cutDataPath = GridCutter.cut2(layers, AlaspatialProperties.getLayerResolutionDefault(), region, envelope, null);
        }

        Grid g = new Grid(cutDataPath + File.separator + layers[col1]);
        data[col1] = g.getGrid();
        data[col2] = new Grid(cutDataPath + File.separator + layers[col2]).getGrid();

        //TODO: delete cut grid files

        int len = data[0].length;
        int divs = 20;  //same as number of cutpoints in Legend
        double[][] area = new double[divs][divs];

        for (int i = 0; i < len; i++) {
            if (Float.isNaN(data[0][i]) || Float.isNaN(data[1][i])) {
                continue;
            }
            int x = getPos(data[0][i], cutoffs[0]);
            int y = getPos(data[1][i], cutoffs[1]);

            if (x >= 0 && x < area.length
                    && y >= 0 && y < area[x].length) {
                area[x][y] += SpatialUtil.cellArea(Double.parseDouble(AlaspatialProperties.getLayerResolutionDefault()), g.ymin + (i / g.ncols) * g.yres);
            }
        }

        //to csv
        StringBuilder sb = new StringBuilder();
        sb.append(",").append(layers[1]).append("\n").append(layers[0]);
        for (int j = 0; j < divs; j++) {
            sb.append(",").append(cutoffs[1][j]);
        }
        for (int i = 0; i < divs; i++) {
            sb.append("\n").append(cutoffs[0][i]);
            for (int j = 0; j < divs; j++) {
                sb.append(",").append(area[i][j]);
            }
        }

        String[] rows = sb.toString().split("\n");
        for (int i = 2; i < rows.length; i++) {
            String[] column = rows[i].split(",");
            for (int j = 1; j < column.length; j++) {
                d[i - 2][j - 1] = Double.parseDouble(column[j]);
            }
        }

        makeXYZdataset(min1, min2, max1, max2);
    }

    int getPos(float d, float[] cutoffs) {
        int pos = java.util.Arrays.binarySearch(cutoffs, d);
        if (pos < 0) {
            pos = (pos * -1) - 1;
        }

        return pos;
    }

    void addBlockPlot(XYPlot scatterplot, String env1, String displayName1, double min1, double max1, String env2, String displayName2, double min2, double max2) {
        //add to the plot
        int datasetCount = scatterplot.getDatasetCount();
        scatterplot.setDataset(datasetCount, blockXYZDataset);
        scatterplot.setRenderer(datasetCount, xyBlockRenderer);

        plot.getDomainAxis().setLowerBound(min1);
        plot.getDomainAxis().setUpperBound(max1);
        plot.getDomainAxis().setLowerMargin(0);
        plot.getDomainAxis().setUpperMargin(0);
        plot.getRangeAxis().setLowerBound(min2);
        plot.getRangeAxis().setUpperBound(max2);
        plot.getRangeAxis().setLowerMargin(0);
        plot.getRangeAxis().setUpperMargin(0);
    }

    private void makeXYZdataset(double min1, double min2, double max1, double max2) {

        double[][] d = scatterplotDataDTO.getGridData();

        double xdiv = (max1 - min1) / d.length;
        double ydiv = (max2 - min2) / d[0].length;
        int countNonZero = 0;

        double min = Double.MAX_VALUE, max = Double.MAX_VALUE * -1;
        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                if (d[i][j] > 0) {
                    countNonZero++;
                    if (d[i][j] < min) {
                        min = d[i][j];
                    }
                    if (d[i][j] > max) {
                        max = d[i][j];
                    }
                }
            }
        }

        double[][] dat = new double[3][countNonZero];
        int pos = 0;

        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d.length; j++) {
                if (d[i][j] > 0) {
                    dat[0][pos] = min1 + i * xdiv;
                    dat[1][pos] = min2 + j * ydiv;
                    dat[2][pos] = Math.log10(d[i][j] + 1);
                    pos++;
                }
            }
        }

        DefaultXYZDataset defaultXYZDataset = new DefaultXYZDataset();
        defaultXYZDataset.addSeries("Environmental area intersection", dat);

        blockXYZDataset = defaultXYZDataset;

        xyBlockRenderer = new XYBlockRenderer();

        GrayPaintScale ramp = new GrayPaintScale(Math.log10(min + 1), Math.log10(max + 1));
        xyBlockRenderer.setPaintScale(ramp);
        xyBlockRenderer.setBlockHeight(ydiv);   //backwards
        xyBlockRenderer.setBlockWidth(xdiv);    //backwards
        xyBlockRenderer.setBlockAnchor(RectangleAnchor.BOTTOM_LEFT);
    }

    private void createDataset(boolean foreground) {
        if (scatterplotDTO.getLayers()[col1] != scatterplotDataDTO.getLayer1()
                || scatterplotDTO.getLayers()[col2] != scatterplotDataDTO.getLayer2()) {
            if (foreground) {
                xyzDataset = null;
            } else {
                backgroundXyzDataset = null;
            }
        }
        //only rebuild if required
        if ((foreground && xyzDataset != null)
                || (!foreground && backgroundXyzDataset != null)) {
            return;
        } else if (!foreground && (scatterplotDTO.getBackgroundOccurrencesQs() == null || scatterplotDTO.getForegroundOccurrencesBs() == null)) {
            return;
        }

        DefaultXYZDataset dataset = new DefaultXYZDataset();

        String[] seriesNames = new String[1];
        double[] points;
        double[][] d;
        String[] series;

        if (foreground) {
            points = scatterplotDataDTO.getPoints();
            d = scatterplotDataDTO.getData();
            series = scatterplotDataDTO.getSeries();
            seriesNames[0] = scatterplotDTO.getForegroundName();
        } else {
            points = scatterplotDataDTO.getBackgroundPoints();
            d = scatterplotDataDTO.getBackgroundData();
            series = scatterplotDataDTO.getBackgroundSeries();
            seriesNames[0] = scatterplotDTO.getBackgroundName();
        }

        if (foreground && legend != null) {
            seriesNames = legend.getCategoryNameOrder();
        }

        int missingCount = 0;
        for (int i = 0; i < seriesNames.length; i++) {
            ArrayList<double[]> records = new ArrayList<double[]>(points.length);
            for (int j = 0; j < d.length; j++) {
                if (seriesNames.length == 1 || series[j].equals(seriesNames[i])) {
                    if (!Double.isNaN(d[j][col1]) && !Double.isNaN(d[j][col2])) {
                        double[] r = {d[j][col1], d[j][col2], i};
                        records.add(r);
                    } else {
                        missingCount++;
                    }
                }
            }
            if (records.size() > 0) {
                //flip data into an array
                double[][] sd = new double[3][records.size()];
                for (int j = 0; j < records.size(); j++) {
                    sd[0][j] = records.get(j)[0];
                    sd[1][j] = records.get(j)[1];
                    sd[2][j] = records.get(j)[2];
                }
                dataset.addSeries(seriesNames[i], sd);
            }
        }

        if (foreground) {
            scatterplotDataDTO.setMissingCount(missingCount);

            xyzDataset = dataset;
        } else {
            backgroundXyzDataset = dataset;
        }
    }

    private void createAADataset() {
        if (scatterplotStyleDTO.getHighlightWkt() == null) {
            aaDataset = null;
            return;
        }

        //rebuild only if not built or changed
        if (aaDataset != null
                && (scatterplotStyleDTO.getPrevHighlightWkt() != null
                && !scatterplotStyleDTO.getPrevHighlightWkt().equals(scatterplotStyleDTO.getHighlightWkt()))) {
            return;
        }

        aaDataset = new DefaultXYZDataset();

        SimpleRegion region = SimpleShapeFile.parseWKT(scatterplotStyleDTO.getHighlightWkt());

        double[] points = scatterplotDataDTO.getPoints();
        ArrayList<double[]> records = new ArrayList<double[]>(points.length);
        double[][] d = scatterplotDataDTO.getData();
        for (int j = 0; j < d.length; j++) {
            if (!Double.isNaN(d[j][col1]) && !Double.isNaN(d[j][col2])
                    && region.isWithin(points[j * 2], points[j * 2 + 1])) {
                double[] r = {d[j][col1], d[j][col2], 0};
                records.add(r);
            }
        }
        if (records.size() > 0) {
            //flip data into an array
            double[][] sd = new double[3][records.size()];
            for (int j = 0; j < records.size(); j++) {
                sd[0][j] = records.get(j)[0];
                sd[1][j] = records.get(j)[1];
                sd[2][j] = records.get(j)[2];
            }
            ((DefaultXYZDataset) aaDataset).addSeries(ACTIVE_AREA_SERIES, sd);
        }
    }

    private XYShapeRenderer getRenderer(LegendObject legend, int[] datasetColours, String[] seriesNames, double[] seriesValues) {
        class MyXYShapeRenderer extends XYShapeRenderer {

            public int alpha = 255;
            public Paint[] datasetColours;
            public int shapeSize = 4;
            public double[] seriesValues;
            public LegendObject legend;
            Shape shape = null;

            @Override
            public Shape getItemShape(int row, int column) {
                //return super.getItemShape(row, column);
                if (shape == null) {
                    double delta = shapeSize / 2.0;
                    shape = new Ellipse2D.Double(-delta, -delta, shapeSize, shapeSize);
                }
                return shape;
            }

            @Override
            public Paint getPaint(XYDataset dataset, int series, int item) {
                if (legend != null && legend.getCategories() == null) {
                    //colour item
                    return new Color(legend.getColour((float) seriesValues[item]) | (alpha << 24), true);
                } else if (datasetColours != null && datasetColours.length > series && datasetColours[series] != null) {
                    //colour series
                    return datasetColours[series];
                }

                return super.getPaint(dataset, series, item);
            }
        }

        int red = scatterplotStyleDTO.getRed();
        int green = scatterplotStyleDTO.getGreen();
        int blue = scatterplotStyleDTO.getBlue();
        int alpha = scatterplotStyleDTO.getOpacity() * 255 / 100;

        MyXYShapeRenderer renderer = new MyXYShapeRenderer();
        renderer.shapeSize = scatterplotStyleDTO.getSize();
        if (datasetColours != null) {
            renderer.datasetColours = new Color[datasetColours.length];
            for (int i = 0; i < datasetColours.length; i++) {
                int c = datasetColours[i];
                if (seriesNames.equals(NUMBER_SERIES)) {
                    renderer.datasetColours[i] = null;
                } else {
                    int r = (c >> 16) & 0x000000FF;
                    int g = (c >> 8) & 0x000000FF;
                    int b = c & 0x000000FF;
                    renderer.datasetColours[i] = new Color(r, g, b, alpha);
                }
            }
        }
        renderer.alpha = alpha;
        renderer.seriesValues = seriesValues;
        renderer.legend = legend;

        class LegendFieldPaintScale implements PaintScale, Serializable {

            LegendObject legend;
            Color defaultColour;

            public LegendFieldPaintScale(LegendObject legend, Color defaultColour) {
                this.legend = legend;
                this.defaultColour = defaultColour;
            }

            @Override
            public double getLowerBound() {
                if (legend == null) {
                    return 0;
                }
                float[] d = legend.getMinMax();
                return d == null ? 0 : d[0];
            }

            @Override
            public double getUpperBound() {
                if (legend == null) {
                    return 1;
                }
                float[] d = legend.getMinMax();
                return d == null ? 0 : d[1];
            }

            @Override
            public Paint getPaint(double d) {
                if (legend == null) {
                    return defaultColour;
                }
                return new Color(legend.getColour((float) d));
            }
        }

        renderer.setPaintScale(new LegendFieldPaintScale(legend, new Color(red, green, blue, alpha)));

        return renderer;
    }

    private void resample(boolean foreground) {
        //no change = no data refresh

        if (foreground) {
            if (scatterplotDataDTO.getPoints() != null
                    && scatterplotDataDTO.getColourMode() != null && scatterplotDataDTO.getColourMode().equals(scatterplotStyleDTO.getColourMode())) {
                return;
            } else {
                xyzDataset = null; //set to null so it is rebuilt later
            }
        } else {
            if (scatterplotDTO.getBackgroundOccurrencesQs() == null
                    || scatterplotDTO.getBackgroundOccurrencesBs() == null
                    || (scatterplotDataDTO.getBackgroundPoints() != null)) {
                return;
            } else {
                backgroundXyzDataset = null; //set to null so it is rebuilt later
            }
        }

        //get foreground id, longitude, latitude

        String fields = "id,longitude,latitude";

        //append all layers
        int[] layerColIdxs = new int[scatterplotDTO.getLayers().length];
        for (int i = 0; i < scatterplotDTO.getLayers().length; i++) {
            IntersectionFile f = Client.getLayerIntersectDao().getConfig().getIntersectionFile(scatterplotDTO.getLayers()[i]);
            if (f != null) {
                fields += "," + f.getFieldId();
            }
        }

        ArrayList<String> layersNotInBiocache = new ArrayList<String>();
        ArrayList<Integer> layersNotInBiocacheIdx = new ArrayList<Integer>();

        boolean resampleColourMode = !scatterplotStyleDTO.getColourMode().equals(scatterplotDataDTO.getColourMode());
        boolean colourModeAttemptedFromBiocache = false;
        boolean colourModeHasData = false;

        try {

            //species and layers cannot change but colourmode can
            double[] points;
            String[] ids;
            double[][] data;
            if (foreground) {
                points = scatterplotDataDTO.getPoints();
                ids = scatterplotDataDTO.getIds();
                data = scatterplotDataDTO.getData();
            } else {
                points = scatterplotDataDTO.getBackgroundPoints();
                ids = scatterplotDataDTO.getBackgroundIds();
                data = scatterplotDataDTO.getBackgroundData();
            }

            String[] series = null;
            if (points == null) {   //get points
                String string;
                if (foreground) {
                    //append colourmode if != -1 (user colour mode), get the data from data query
                    if (scatterplotStyleDTO.getColourMode() != "-1") {
                        fields += "," + translateFieldForSolr(scatterplotStyleDTO.getColourMode());
                        colourModeAttemptedFromBiocache = true;
                    }
                    string = Occurrences.getOccurrences(scatterplotDTO.getForegroundOccurrencesQs(), scatterplotDTO.getForegroundOccurrencesBs(), fields);
                } else {
                    string = Occurrences.getOccurrences(scatterplotDTO.getBackgroundOccurrencesQs(), scatterplotDTO.getBackgroundOccurrencesBs(), fields);
                }

                CSVReader csvreader = new CSVReader(new StringReader(string));
                java.util.List<String[]> csv = csvreader.readAll();
                csvreader.close();

                int longitudeColumn = findInArray("longitude", csv.get(0));
                int latitudeColumn = findInArray("latitude", csv.get(0));
                int idColumn = findInArray("id", csv.get(0));
                int colourColumn = -1;
                if (!scatterplotStyleDTO.getColourMode().equalsIgnoreCase("-1")) {
                    colourColumn = findInArray(translateFieldForSolr(scatterplotStyleDTO.getColourMode()), csv.get(0));
                    if (colourColumn == -1) {
                        resampleColourMode = true;
                    } else {
                        resampleColourMode = false;
                    }
                }
                for (int i = 0; i < scatterplotDTO.getLayers().length; i++) {
                    IntersectionFile f = Client.getLayerIntersectDao().getConfig().getIntersectionFile(scatterplotDTO.getLayers()[i]);
                    if (f == null) {
                        layerColIdxs[i] = -1;
                    } else {
                        layerColIdxs[i] = findInArray(f.getFieldId(), csv.get(0));
                    }
                    if (layerColIdxs[i] == -1) {
                        layersNotInBiocache.add(scatterplotDTO.getLayers()[i]);
                        layersNotInBiocacheIdx.add(i);
                    }
                }

                points = new double[(csv.size() - 1) * 2];
                ids = new String[csv.size() - 1];
                series = new String[csv.size() - 1];
                data = new double[csv.size() - 1][layerColIdxs.length];
                int pos = 0;
                for (int i = 1; i < csv.size(); i++) {
                    try {
                        points[pos] = Double.parseDouble(csv.get(i)[longitudeColumn]);
                        points[pos + 1] = Double.parseDouble(csv.get(i)[latitudeColumn]);

                        if (colourColumn >= 0) {
                            series[i - 1] = csv.get(i)[colourColumn];
                        }

                        for (int j = 0; j < layerColIdxs.length; j++) {
                            if (layerColIdxs[j] >= 0) {
                                try {
                                    data[i - 1][j] = Double.parseDouble(csv.get(i)[layerColIdxs[j]]);
                                } catch (Exception e) {
                                    data[i - 1][j] = Double.NaN;
                                }
                            } else {
                                data[i - 1][j] = Double.NaN;
                            }
                        }
                    } catch (Exception e) {
                        points[pos] = Double.NaN;
                        points[pos + 1] = Double.NaN;
                    }
                    ids[pos / 2] = csv.get(i)[idColumn];
                    pos += 2;
                }

                //double check series and data to make sure the columns were not empty
                for (int i = 0; i < series.length; i++) {
                    if (series[i] != null && series[i].length() > 0) {
                        colourModeHasData = true;
                        break;
                    }
                }
                if (!colourModeHasData) {
                    //didn't get colour mode so need to try to get the data later.
                    resampleColourMode = true;
                }
                for (int i = 0; i < layerColIdxs.length; i++) {
                    //if -1 it is already added to 'layersNotInBiocache'
                    if (layerColIdxs[i] != -1) {
                        boolean found = false;
                        for (int j = 0; j < data.length; j++) {
                            if (!Double.isNaN(data[j][i])) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            //didn't have any data for this layer so record it as not in biocache
                            layersNotInBiocacheIdx.add(i);
                            layersNotInBiocache.add(scatterplotDTO.getLayers()[i]);
                        }
                    }
                }

            }

            double[][] p = new double[points.length / 2][2];
            for (int i = 0; i < points.length; i += 2) {
                p[i / 2][0] = points[i];
                p[i / 2][1] = points[i + 1];
            }

            if (layersNotInBiocache.size() > 0) {
                double[][] dataSample = samplePoints(p, layersNotInBiocache);

                //copy to data
                for (int i = 0; i < layersNotInBiocache.size(); i++) {
                    int dest = layersNotInBiocacheIdx.get(i);
                    for (int j = 0; j < data.length; j++) {
                        data[j][dest] = dataSample[j][i];
                    }
                }
            }

            if (scatterplotStyleDTO.getColourMode().equals("-1") || !foreground) {
                series = new String[p.length];
                if (foreground) {
                    for (int i = 0; i < series.length; i++) {
                        series[i] = scatterplotDTO.getForegroundName();
                    }
                } else {
                    for (int i = 0; i < series.length; i++) {
                        series[i] = scatterplotDTO.getBackgroundName();
                    }
                }
                legend = null;
            } else {// if (foreground) {
                //do we still need to fetch colourmode data?
                if (resampleColourMode) {
                    //have we attempted to get it from biocache?
                    if (!colourModeAttemptedFromBiocache) {
                        series = sampleBiocacheSeries();
                    }

                    //if we don't have series info yet, try to get it from local sampling
                    boolean found = false;
                    for (int i = 0; i < series.length; i++) {
                        if (series[i] != null && series[i].length() > 0) {
                            found = true;
                            break;
                        }
                    }
                    if (!found && Client.getLayerIntersectDao().getConfig().getIntersectionFile(scatterplotStyleDTO.getColourMode()) != null) {
                        //valid for intersections
                        series = sampleSeries(p, scatterplotStyleDTO.getColourMode());
                    }
                }

                //get the legend
                legend = getLegend(translateFieldForSolr(scatterplotStyleDTO.getColourMode()));
            }

            if (foreground) {
                scatterplotDataDTO.setPoints(points);
                scatterplotDataDTO.setSeries(series);

                scatterplotDataDTO.setIds(ids);
                scatterplotDataDTO.setData(data);

                double[] seriesValues = new double[points.length];
                for (int i = 0; i < series.length; i++) {
                    try {
                        seriesValues[i] = Double.parseDouble(series[i]);
                    } catch (Exception e) {
                        //don't care
                    }
                }
                scatterplotDataDTO.setSeriesValues(seriesValues);

            } else {
                scatterplotDataDTO.setBackgroundPoints(points);
                scatterplotDataDTO.setBackgroundSeries(series);
                scatterplotDataDTO.setBackgroundIds(ids);
                scatterplotDataDTO.setBackgroundData(data);
            }

        } catch (Exception e) {
            if (foreground) {
                logger.error("failed to sample points: q=" + scatterplotDTO.getForegroundOccurrencesQs()
                        + ", bs=" + scatterplotDTO.getForegroundOccurrencesBs()
                        + ", fields=" + fields, e);
            } else {
                logger.error("failed to sample points: q=" + scatterplotDTO.getBackgroundOccurrencesQs()
                        + ", bs=" + scatterplotDTO.getBackgroundOccurrencesBs()
                        + ", fields=" + fields, e);
            }

        }
    }

    private String[] sampleBiocacheSeries() {
        try {
            String[] ids = scatterplotDataDTO.getIds();
            String[] series = new String[ids.length];
            HashMap<String, String> seriesMap = new HashMap<String, String>();

            String string = Occurrences.getOccurrences(scatterplotDTO.getForegroundOccurrencesQs()
                    , scatterplotDTO.getForegroundOccurrencesBs()
                    , "id," + translateFieldForSolr(scatterplotStyleDTO.getColourMode()));
            CSVReader csv = new CSVReader(new StringReader(string));
            String[] line;
            csv.readNext(); //read header
            while ((line = csv.readNext()) != null) {
                seriesMap.put(line[0], line[1]); //put occurrence id (as key) and series (as value) into a map
            }

            //read out of the map into the array
            for (int i = 0; i < ids.length; i++) {
                series[i] = seriesMap.get(ids[i]);
            }

            return series;
        } catch (IOException e) {
            logger.error("cannot get colourmode for biocache series", e);
        }

        return null;
    }

    XYShapeRenderer getBackgroundRenderer() {
        XYShapeRenderer renderer = new XYShapeRenderer();
        Color c = new Color(255, 164, 96, 150);
        renderer.setSeriesPaint(0, c);

        double size = scatterplotStyleDTO.getSize() + 10;
        double delta = size / 2;
        renderer.setSeriesShape(0, new Ellipse2D.Double(-delta, -delta, size, size));

        return renderer;
    }

    XYShapeRenderer getActiveAreaRenderer() {
        XYShapeRenderer renderer = new XYShapeRenderer();

        LookupPaintScale paint = new LookupPaintScale(0, 1, new Color(255, 255, 255, 0)) {

            @Override
            public Paint getPaint(double value) {
                return this.getDefaultPaint();
            }
        };

        renderer.setPaintScale(paint);
        renderer.setSeriesOutlinePaint(0, new Color(255, 0, 0, 255));
        renderer.setDrawOutlines(true);

        double size = scatterplotStyleDTO.getSize() + 3;
        double delta = size / 2;
        renderer.setSeriesShape(0, new Ellipse2D.Double(-delta, -delta, size, size));

        return renderer;
    }

    private double[][] samplePoints(double[][] p, List<String> layersToSample) {

        String[] layers = new String[layersToSample.size()];
        layersToSample.toArray(layers);

        java.util.List<String> sample = Client.getLayerIntersectDao().sampling(layers, p);

        double[][] d = new double[p.length][layers.length];

        Scanner[] scanners = new Scanner[layers.length];
        for (int i = 0; i < layers.length; i++) {
            scanners[i] = new Scanner(sample.get(i));
        }
        for (int j = 0; j < p.length; j++) {
            for (int i = 0; i < scanners.length; i++) {
                try {
                    d[j][i] = Double.parseDouble(scanners[i].nextLine());
                } catch (Exception e) {
                    d[j][i] = Double.NaN;
                }
            }
        }
        for (int i = 0; i < scanners.length; i++) {
            scanners[i].close();
        }

        return d;
    }

    private String[] sampleSeries(double[][] p, String seriesName) {

        String[] layers = {seriesName};
        java.util.List<String> sample = Client.getLayerIntersectDao().sampling(layers, p);

        String[] series = new String[p.length];

        Scanner s = new Scanner(sample.get(0));
        for (int j = 0; j < p.length; j++) {
            series[j] = s.nextLine();
        }
        s.close();

        return series;
    }

    private int findInArray(String lookFor, String[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(lookFor)) {
                return i;
            }
        }
        return -1;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getImageURL() {
        return imageURL;
    }

    public ScatterplotDTO getScatterplotDTO() {
        return scatterplotDTO;
    }

    public ScatterplotStyleDTO getScatterplotStyleDTO() {
        return scatterplotStyleDTO;
    }

    public ScatterplotDataDTO getScatterplotDataDTO() {
        return scatterplotDataDTO;
    }

    public void annotatePixelBox(Integer minx, Integer miny, Integer maxx, Integer maxy) {
        try {
            //make sure chartRenderingInfo has something in it
            getAsBytes();

            //chart area is wrong, but better than the above
            double tx1 = plot.getRangeAxis().java2DToValue(minx, chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM);
            double tx2 = plot.getRangeAxis().java2DToValue(maxx, chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM);
            double ty1 = plot.getDomainAxis().java2DToValue(miny, chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.LEFT);
            double ty2 = plot.getDomainAxis().java2DToValue(maxy, chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.LEFT);
            double x1 = Math.min(tx1, tx2);
            double x2 = Math.max(tx1, tx2);
            double y1 = Math.min(ty1, ty2);
            double y2 = Math.max(ty1, ty2);

            scatterplotStyleDTO.setSelection(new double[]{x1, y1, x2, y2});

            buildScatterplot();

            //does not re-render.  Use getAsStream() to get updated screen render or renderScatterplot() to update url image.
        } catch (Exception e) {
            logger.error("failed to apply annotation");
        }
    }

    public void reStyle(ScatterplotStyleDTO style, boolean colourModeUpdate, boolean redUpdate, boolean blueUpdate
            , boolean greenUpdate, boolean opacityUpdate, boolean sizeUpdate, boolean highlightWktUpdate) {

        //cannot restyle everything, only the basics
        if (colourModeUpdate) {
            scatterplotStyleDTO.setColourMode(style.getColourMode());
        }
        if (redUpdate) {
            scatterplotStyleDTO.setRed(style.getRed());
        }
        if (greenUpdate) {
            scatterplotStyleDTO.setGreen(style.getGreen());
        }
        if (blueUpdate) {
            scatterplotStyleDTO.setBlue(style.getBlue());
        }
        if (opacityUpdate) {
            scatterplotStyleDTO.setOpacity(style.getOpacity());
        }
        if (sizeUpdate) {
            scatterplotStyleDTO.setSize(style.getSize());
        }
        if (highlightWktUpdate) {
            scatterplotStyleDTO.setHighlightWkt(style.getHighlightWkt());
        }

        buildScatterplot();
        renderScatterplot(null);
    }

    public byte[] getAsBytes() {
        int width = scatterplotStyleDTO.getWidth();
        int height = scatterplotStyleDTO.getHeight();

        try {

            BufferedImage bi = jChart.createBufferedImage(width, height, BufferedImage.TRANSLUCENT, chartRenderingInfo);
            byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);


            return bytes;
        } catch (Exception e) {
            logger.error("error producing scatterplot", e);
        }

        return null;
    }

    /*
    scatterplot list specific functions
     */

    String htmlHeader = "<html><body><table border=1>";
    String htmlFooter = "</table></body></html>";

    private void makeHtml() {
        String pth = AlaspatialProperties.getBaseOutputDir() + "output" + File.separator + "scatterplot" + File.separator + scatterplotDTO.getId() + File.separator;
        String url = AlaspatialProperties.getBaseOutputURL() + "/output/scatterplot/" + scatterplotDTO.getId() + "/";
        String pid = scatterplotDTO.getId();

        String[] layers = scatterplotDTO.getLayers();

        //linear
        StringBuilder sb = new StringBuilder();

        //confused, map it
        HashMap<String, String> map = new HashMap<String, String>();
        int pos = 0;
        for (int i = 0; i < layers.length - 1; i++) {
            for (int j = i + 1; j < layers.length; j++) {
                map.put(i + " " + j, url + pid + "_" + layers[i] + "_" + layers[j] + ".png");
                pos++;
            }
        }

        sb.append(htmlHeader);
        for (int i = 1; i < layers.length; i++) {
            sb.append("<tr>");
            for (int j = 0; j < i; j++) {
                int a = (layers.length - i - 1);
                int b = (layers.length - j - 1);
                String key = Math.min(a, b) + " " + Math.max(a, b);
                if (map.get(key) == null) {
                    sb.append("<td>n/a</td>");
                } else {
                    sb.append("<td><img src='").append(map.get(key)).append("'/></td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append(htmlFooter);

        try {
            FileWriter fw = new FileWriter(pth + "scatterplot.html");
            fw.write(sb.toString());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("ScatterplotList file: " + pth + "scatterplot.html");
        logger.debug("ScatterplotList output: " + sb.toString());
    }

    private void makeReadme(String pth) {
        try {
            FileWriter fw = new FileWriter(pth + "readme.txt");
            fw.write("readme.txt               This file.\n"
                    + "scatterplot.html         Web page with all scatterplots.\n"
                    + "*.png                    Scatterplot images.");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getHtmlURL() {
        return htmlURL;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public LegendObject getLegend(String colourmode) {
        if (legends.get(colourmode) != null) {
            return legends.get(colourmode);
        } else {
            HttpClient client = new HttpClient();
            String facetToColourBy = colourmode.equals("occurrence_year_decade") ? "occurrence_year" : translateFieldForSolr(colourmode);

            try {
                String url = scatterplotDTO.getForegroundOccurrencesBs()
                        + "/webportal/legend?"
                        + "&q=" + scatterplotDTO.getForegroundOccurrencesQs()
                        + "&cm=" + URLEncoder.encode(facetToColourBy, "UTF-8");
                logger.debug(url);
                GetMethod get = new GetMethod(url);
                //NQ: Set the header type to JSON so that we can parse JSON instead of CSV (CSV has issue with quoted field where a quote is the escape character)
                get.addRequestHeader("Accept", "application/json");

                int result = client.executeMethod(get);
                String s = get.getResponseBodyAsString();
                //in the first line do field name replacement
                String t = translateFieldForSolr(colourmode);
                if (!colourmode.equals(t)) {
                    s = s.replaceFirst(t, colourmode);
                }

                LegendObject lo = new BiocacheLegendObject(colourmode, s);

                //test for exceptions
                if (!colourmode.contains(",") && (colourmode.equals("uncertainty") || colourmode.equals("decade") || colourmode.equals("occurrence_year") || colourmode.equals("coordinate_uncertainty"))) {
                    lo = ((BiocacheLegendObject) lo).getAsIntegerLegend();

                    //apply cutpoints to colourMode string
                    Legend l = lo.getNumericLegend();
                    float[] minmax = l.getMinMax();
                    float[] cutpoints = l.getCutoffFloats();
                    float[] cutpointmins = l.getCutoffMinFloats();
                    StringBuilder sb = new StringBuilder();
                    //NQ 20140109: use the translated SOLR field as the colour mode so that "decade" does not cause an issue
                    String newFacet = colourmode.equals("decade") ? "occurrence_year" : colourmode;
                    sb.append(newFacet);
                    int i = 0;
                    int lasti = 0;
                    while (i < cutpoints.length) {
                        if (i == cutpoints.length - 1 || cutpoints[i] != cutpoints[i + 1]) {
                            if (i > 0) {
                                sb.append(",").append(cutpointmins[i]);
                                if (colourmode.equals("occurrence_year") || colourmode.equals("decade"))
                                    sb.append("-01-01T00:00:00Z");
                            } else {
                                sb.append(",*");
                            }
                            sb.append(",").append(cutpoints[i]);
                            if (colourmode.equals("occurrence_year") || colourmode.equals("decade"))
                                sb.append("-12-31T00:00:00Z");
                            lasti = i;
                        }
                        i++;
                    }
                    String newColourMode = sb.toString();
                    if (colourmode.equals("occurrence_year") || colourmode.equals("decade")) {
                        newColourMode = newColourMode.replace(".0", "");
                    }

                    lo.setColourMode(newColourMode);
                    legends.put(colourmode, lo);

                    LegendObject newlo = getLegend(newColourMode);
                    newlo.setColourMode(newColourMode);
                    newlo.setNumericLegend(lo.getNumericLegend());
                    legends.put(newColourMode, newlo);

                    lo = newlo;
                } else if (colourmode.equals("month")) {
                    String newColourMode = "month,00,00,01,01,02,02,03,03,04,04,05,05,06,06,07,07,08,08,09,09,10,10,11,11,12,12";

                    lo.setColourMode(newColourMode);
                    legends.put(colourmode, lo);

                    LegendObject newlo = getLegend(newColourMode);
                    newlo.setColourMode(newColourMode);
                    newlo.setNumericLegend(lo.getNumericLegend());
                    legends.put(newColourMode, newlo);

                    lo = newlo;
                } else if (!colourmode.contains(",") && (colourmode.equals("occurrence_year_decade") || colourmode.equals("decade"))) {
                    TreeSet<Integer> decades = new TreeSet<Integer>();
                    for (double d : ((BiocacheLegendObject) lo).categoriesNumeric.keySet()) {
                        decades.add((int) (d / 10));
                    }
                    ArrayList<Integer> d = new ArrayList<Integer>(decades);

                    StringBuilder sb = new StringBuilder();
                    sb.append("occurrence_year");
                    for (int i = (d.size() > 0 && d.get(0) > 0 ? 0 : 1); i < d.size(); i++) {
                        if (i > 0) {
                            sb.append(",").append(d.get(i));
                            sb.append("0-01-01T00:00:00Z");
                        } else {
                            sb.append(",*");
                        }
                        sb.append(",").append(d.get(i));
                        sb.append("9-12-31T00:00:00Z");
                    }
                    String newColourMode = sb.toString();

                    lo.setColourMode(newColourMode);
                    legends.put(colourmode, lo);

                    LegendObject newlo = getLegend(newColourMode);
                    newlo.setColourMode(newColourMode);
                    newlo.setNumericLegend(lo.getNumericLegend());
                    legends.put(newColourMode, newlo);

                    lo = newlo;
                } else {
                    legends.put(colourmode, lo);
                }

                return lo;
            } catch (Exception e) {
                logger.error("error getting legend for : " + colourmode, e);
            }
        }
        return null;
    }

    static String translateFieldForSolr(String facetName) {
        if (facetName == null) {
            return facetName;
        }
        for (String[] s : facetNameExceptions) {
            if (facetName.equals(s[0])) {
                facetName = s[1];
                break;
            }
        }
        if ("occurrence_year_individual".equals(facetName)) {
            facetName = "occurrence_year";
        }
        if ("occurrence_year_decade".equals(facetName)) {
            facetName = "occurrence_year";
        }
        return facetName;
    }

    static String translateSolrForField(String facetName) {
        for (String[] s : facetNameExceptions) {
            if (facetName.equals(s[1])) {
                facetName = s[0];
                break;
            }
        }
        return facetName;
    }
}
