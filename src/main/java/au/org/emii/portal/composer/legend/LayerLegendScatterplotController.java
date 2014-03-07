/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer.legend;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.data.*;
import au.org.ala.spatial.sampling.Sampling;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
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
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.*;
import org.zkoss.zul.Label;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Adam
 */
public class LayerLegendScatterplotController extends UtilityComposer implements HasMapLayer {

    private static Logger logger = Logger.getLogger(LayerLegendScatterplotController.class);

    private static final String NUMBER_SERIES = "Number series";
    private static final String ACTIVE_AREA_SERIES = "In Active Area";
    private SettingsSupplementary settingsSupplementary = null;

    Textbox tbxChartSelection;
    Label tbxSelectionCount;
    Label tbxRange;
    Label tbxDomain;
    Label tbxMissingCount;
    ScatterplotData data;
    JFreeChart jChart;
    XYPlot plot;
    ChartRenderingInfo chartRenderingInfo;
    LayersUtil layersUtil;
    Div scatterplotButtons;
    Div scatterplotDownloads;
    Div divHighlightArea;
    MapLayer mapLayer = null;
    private XYBoxAnnotation annotation;
    DefaultXYZDataset xyzDataset;
    DefaultXYZDataset aaDataset;
    Checkbox chkSelectMissingRecords;

    private DefaultXYZDataset backgroundXyzDataset;

    Label lblMissing;
    Button addNewLayers;
    Combobox cbHighlightArea;

    DefaultXYZDataset blockXYZDataset = null;
    XYBlockRenderer xyBlockRenderer = null;

    ScatterplotLayerLegendComposer layerWindow = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.addEventListener("onSize", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                redraw();
            }
        });
    }

    @Override
    public void doEmbedded() {
        super.doEmbedded();
        redraw();
    }

    @Override
    public void doOverlapped() {
        super.doOverlapped();
        redraw();
    }

    public ScatterplotData getScatterplotData() {
        if (data == null) {
            if (mapLayer == null) {
                data = new ScatterplotData();
            } else {
                data = mapLayer.getScatterplotData();
            }
        }
        return data;
    }

    public void onChange$tbxChartSelection(Event event) {
        try {
            //input order is [x, y, width, height]
            // + optional bounding box coordinates [x1,y1,x2,y2]
            logger.debug(event.getData());
            String[] coordsStr = ((String) event.getData()).replace("px", "").split(",");
            double[] coordsDbl = new double[coordsStr.length];
            for (int i = 0; i < coordsStr.length; i++) {
                coordsDbl[i] = Double.parseDouble(coordsStr[i]);
            }

            //chart area is wrong, but better than the above
            double tx1 = plot.getRangeAxis().java2DToValue(coordsDbl[0], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM);
            double tx2 = plot.getRangeAxis().java2DToValue(coordsDbl[2], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM);
            double ty1 = plot.getDomainAxis().java2DToValue(coordsDbl[1], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.LEFT);
            double ty2 = plot.getDomainAxis().java2DToValue(coordsDbl[3], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.LEFT);
            double x1 = Math.min(tx1, tx2);
            double x2 = Math.max(tx1, tx2);
            double y1 = Math.min(ty1, ty2);
            double y2 = Math.max(ty1, ty2);

            data.prevSelection = new double[4];
            data.prevSelection[0] = x1;
            data.prevSelection[1] = x2;
            data.prevSelection[2] = y1;
            data.prevSelection[3] = y2;

            registerScatterPlotSelection();

            ScatterplotData d = getScatterplotData();
            d.setSelection(new Rectangle2D.Double(x1, y1, x2, y2));
            d.setEnabled(true);

            Facet f = getFacetIn();
            if (f != null) {
                mapLayer.setHighlight(f.toString());
            } else {
                mapLayer.setHighlight(null);
            }

            getMapComposer().applyChange(mapLayer);

            tbxChartSelection.setText("");
            tbxDomain.setValue(String.format("%s: %g - %g", data.getLayer1Name(), y1, y2));
            tbxRange.setValue(String.format("%s: %g - %g", data.getLayer2Name(), x1, x2));

            annotation = new XYBoxAnnotation(y1, x1, y2, x2);

            data.imagePath = null;
            redraw();
        } catch (Exception e) {
            logger.error("failed to build scatterplot legend", e);
            clearSelection();
            getMapComposer().applyChange(mapLayer);
        }
    }

    void redraw() {
        getScatterplotData();

        resample();

        if (data != null /*&& data.getLsid() != null && data.getLsid().length() > 0*/
                && data.getLayer1() != null && data.getLayer1().length() > 0
                && data.getLayer2() != null && data.getLayer2().length() > 0
                && xyzDataset != null) {
            if (data != null && cbHighlightArea.getItemCount() == 0) {
                updateCbHighlightArea();
            }

            if (data.missing_data) {
                lblMissing.setVisible(true);
                divHighlightArea.setVisible(false);
            } else {
                lblMissing.setVisible(false);
                divHighlightArea.setVisible(true);
                try {
                    //only permits redrawing if imagePath has been defined
                    if (data.imagePath != null) {
                        int width = Integer.parseInt(this.getWidth().replace("px", "")) - 20;
                        int height = Integer.parseInt(this.getHeight().replace("px", "")) - Integer.parseInt(tbxChartSelection.getHeight().replace("px", ""));
                        if (height > width) {
                            height = width;
                        } else {
                            width = height;
                        }

                        //save to file
                        String pth = this.settingsSupplementary.getValue("print_output_path");
                        String htmlurl = settingsSupplementary.getValue("print_output_url");

                        String script = "updateScatterplot(" + width + "," + height + ",'url("
                                + data.imagePath.replace(pth, htmlurl) + ")')";
                        Clients.evalJavaScript(script);
                        scatterplotDownloads.setVisible(true);
                    } else {
                        //active area must be drawn first
                        if (data.getHighlightSa() != null && aaDataset != null) {
                            jChart = ChartFactory.createScatterPlot(data.getSpeciesName(), data.getLayer1Name(), data.getLayer2Name(), aaDataset, PlotOrientation.HORIZONTAL, false, false, false);
                        } else {
                            jChart = ChartFactory.createScatterPlot(data.getSpeciesName(), data.getLayer1Name(), data.getLayer2Name(), xyzDataset, PlotOrientation.HORIZONTAL, false, false, false);
                        }
                        jChart.setBackgroundPaint(Color.white);
                        plot = (XYPlot) jChart.getPlot();
                        if (annotation != null) {
                            plot.addAnnotation(annotation);
                        }

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
                        String[] seriesNames = {data.getQuery().getName()};
                        if (data.getLegend() != null && data.getLegend().getCategories() != null) {
                            seriesNames = data.getLegend().getCategories();
                        }
                        int[] seriesColours = new int[seriesNames.length];
                        LegendObject legend = data.getLegend();
                        for (int i = 0; i < seriesNames.length; i++) {
                            if (legend == null || data.getLegend().getCategories() == null) {
                                seriesColours[i] = data.red << 16 | data.green << 8 | data.blue | 0xff000000;
                            } else {
                                seriesColours[i] = legend.getColour(seriesNames[i]);
                            }
                        }
                        //active area must be drawn first
                        if (data.getHighlightSa() != null && aaDataset != null) {
                            plot.setRenderer(getActiveAreaRenderer());

                            int datasetCount = plot.getDatasetCount();
                            plot.setDataset(datasetCount, xyzDataset);
                            plot.setRenderer(datasetCount, getRenderer(legend, seriesColours, seriesNames, data.getSeriesValues()));
                        } else {
                            plot.setRenderer(getRenderer(legend, seriesColours, seriesNames, data.getSeriesValues()));
                        }

                        //add points background
                        if (data.getBackgroundQuery() != null) {
                            resampleBackground();

                            if (backgroundXyzDataset != null) {
                                int datasetCount = plot.getDatasetCount();
                                plot.setDataset(datasetCount, backgroundXyzDataset);
                                plot.setRenderer(datasetCount, getBackgroundRenderer());
                            }
                        }

                        //add block background
                        if (data.isEnvGrid()) {
                            NumberAxis x = (NumberAxis) plot.getDomainAxis();
                            NumberAxis y = (NumberAxis) plot.getRangeAxis();
                            addBlockPlot(plot,
                                    data.getLayer1(),
                                    data.getLayer1Name(),
                                    x.getLowerBound(),
                                    x.getUpperBound(),
                                    data.getLayer2(),
                                    data.getLayer2Name(),
                                    y.getLowerBound(),
                                    y.getUpperBound());
                        }

                        chartRenderingInfo = new ChartRenderingInfo();

                        int width = Integer.parseInt(this.getWidth().replace("px", "")) - 20;
                        int height = Integer.parseInt(this.getHeight().replace("px", "")) - Integer.parseInt(tbxChartSelection.getHeight().replace("px", ""));
                        if (height > width) {
                            height = width;
                        } else {
                            width = height;
                        }
                        BufferedImage bi = jChart.createBufferedImage(width, height, BufferedImage.TRANSLUCENT, chartRenderingInfo);
                        byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);

                        //save to file
                        String uid = String.valueOf(System.currentTimeMillis());
                        String pth = this.settingsSupplementary.getValue("print_output_path");
                        String htmlurl = settingsSupplementary.getValue("print_output_url");

                        data.imagePath = pth + uid + ".png";

                        try {
                            FileOutputStream fos = new FileOutputStream(pth + uid + ".png");
                            fos.write(bytes);
                            fos.close();
                        } catch (Exception e) {
                            logger.error("error writing scatterplot png", e);
                        }


                        String script = "updateScatterplot(" + width + "," + height + ",'url(" + htmlurl + uid + ".png)')";
                        Clients.evalJavaScript(script);

                        scatterplotDownloads.setVisible(true);

                        if (data.getMissingCount() > 0) {
                            tbxMissingCount.setValue("(" + data.getMissingCount() + ")");
                            chkSelectMissingRecords.setVisible(true);
                        } else {
                            tbxMissingCount.setValue("");
                            chkSelectMissingRecords.setVisible(false);
                        }
                        store();
                    }
                } catch (Exception e) {
                    logger.error("error producing scatterplot", e);
                    clearSelection();
                    getMapComposer().applyChange(mapLayer);
                }
            }
        } else {
            tbxMissingCount.setValue("");
        }
    }

    private void registerScatterPlotSelection() {
        try {
            double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
            if (data.prevSelection != null) {
                x1 = data.prevSelection[0];
                x2 = data.prevSelection[1];
                y1 = data.prevSelection[2];
                y2 = data.prevSelection[3];
                annotation = new XYBoxAnnotation(y1, x1, y2, x2);
            } else {
                annotation = null;
            }

            if (data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {
                Facet f = getFacetIn();

                int count = 0;
                if (f != null) {
                    Query q = data.getQuery().newFacet(f, false);
                    count = q.getOccurrenceCount();
                }
                updateCount(String.valueOf(count));
            }
        } catch (Exception e) {
            logger.error("error updating scatterplot selection", e);
            clearSelection();
            getMapComposer().applyChange(mapLayer);
        }
    }

    void updateCount(String txt) {
        try {
            data.selectionCount = Integer.parseInt(txt);
            tbxSelectionCount.setValue("Records selected: " + txt);
            if (data.selectionCount > 0) {
                addNewLayers.setVisible(true);
            } else {
                addNewLayers.setVisible(false);
            }
            scatterplotButtons.setVisible(true);
        } catch (Exception e) {
        }
    }

    void clearSelection() {
        tbxSelectionCount.setValue("");
        addNewLayers.setVisible(false);
        tbxRange.setValue("");
        tbxDomain.setValue("");

        data.prevSelection = null;

        chkSelectMissingRecords.setChecked(false);

        getScatterplotData().setEnabled(false);

        scatterplotDownloads.setVisible(false);

        annotation = null;

        scatterplotButtons.setVisible(false);
    }

    public void onClick$addSelectedRecords(Event event) {
        Facet f = getFacetIn();
        if (f != null) {
            addUserLayer(data.getQuery().newFacet(getFacetIn(), true), "IN " + data.getSpeciesName(), "from scatterplot in group", data.selectionCount);
        }
    }

    public void onClick$addUnSelectedRecords(Event event) {
        addUserLayer(data.getQuery().newFacet(getFacetOut(), true), "OUT " + data.getSpeciesName(), "from scatterplot out group", data.results.split("\n").length - data.selectionCount - 1);   //-1 for header
    }

    void addUserLayer(Query query, String layername, String description, int numRecords) {
        layername = StringUtils.capitalize(layername);

        getMapComposer().mapSpecies(query, layername, "species", -1, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE,
                MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
    }

    public void onClick$addNewLayers(Event event) {
        onClick$addUnSelectedRecords(null);
        onClick$addSelectedRecords(null);
    }

    public void onClick$scatterplotImageDownload(Event event) {
        try {
            Filedownload.save(new File(data.imagePath), "image/png");
        } catch (Exception e) {
            logger.error("error saving scatterplot image: " + data.imagePath, e);
        }
    }

    public void onClick$scatterplotDataDownload(Event event) {

        //make output csv; id, series, layer1, layer2, highlight
        StringBuilder sb = new StringBuilder();
        sb.append("id,series,").append(data.getLayer1Name()).append(",").append(data.getLayer2Name());
        if (data.gridData != null) {
            sb.append(",environmental envelope area (sq km)");
        }
        String[] series = data.getSeries();
        String[] ids = data.getIds();
        double[][] d = data.getData();
        for (int i = 0; i < series.length; i++) {
            sb.append("\n\"").append(ids[i].replace("\"", "\"\"")).append("\",\"").append(series[i].replace("\"", "\"\"")).append("\",").append(String.valueOf(d[i][0])).append(",").append(String.valueOf(d[i][1]));
            if (data.gridData != null) {
                int pos1 = java.util.Arrays.binarySearch(data.gridCutoffs[0], d[i][0]);
                if (pos1 < 0) {
                    pos1 = (pos1 * -1) - 1;
                }
                int pos2 = java.util.Arrays.binarySearch(data.gridCutoffs[1], d[i][1]);
                if (pos2 < 0) {
                    pos2 = (pos2 * -1) - 1;
                }
                if (pos1 >= 0 && pos1 < data.gridData.length
                        && pos2 >= 0 && pos2 < data.gridData[pos1].length) {
                    sb.append(",").append(data.gridData[pos1][pos2]);
                }
            }
        }

        Filedownload.save(sb.toString(), "text/plain", "scatterplot.csv");
    }

    public void onCheck$chkSelectMissingRecords(Event event) {
        try {
            registerScatterPlotSelection();

            ScatterplotData d = getScatterplotData();
            d.setEnabled(true);

            Facet f = getFacetIn();
            if (f == null) {
                mapLayer.setHighlight(null);
            } else {
                mapLayer.setHighlight(f.toString());
            }

            getMapComposer().applyChange(mapLayer);

            tbxChartSelection.setText("");

            data.imagePath = null;
            redraw();
        } catch (Exception e) {
            e.printStackTrace();
            clearSelection();
            getMapComposer().applyChange(mapLayer);
        }
    }

    void addBlockPlot(XYPlot scatterplot, String env1, String displayName1, double min1, double max1, String env2, String displayName2, double min2, double max2) {
        String thisBlockPlot = env1 + "*" + min1 + "*" + max1 + "*" + env2 + "*" + min2 + "*" + max2;
        if (data.prevBlockPlot == null || !data.prevBlockPlot.equals(thisBlockPlot)) {
            data.prevBlockPlot = thisBlockPlot;
            //get data
            double[][] d = null;
            try {
                int divisions = 20;
                StringBuilder sbProcessUrl = new StringBuilder();
                sbProcessUrl.append(CommonData.satServer).append("/ws/chart?&divisions=").append(divisions);
                sbProcessUrl.append("&wkt=").append(URLEncoder.encode(data.getFilterSa().getWkt(), "UTF-8"));

                sbProcessUrl.append("&xaxis=").append(URLEncoder.encode(env1, "UTF-8")).append(",").append(min1).append(",").append(max1);
                sbProcessUrl.append("&yaxis=").append(URLEncoder.encode(env2, "UTF-8")).append(",").append(min2).append(",").append(max2);

                logger.debug(sbProcessUrl.toString());

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(sbProcessUrl.toString());
                get.addRequestHeader("Accept", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                //get linear cutoffs
                double[][] cutoffs = new double[2][divisions];
                for (int j = 0; j < divisions; j++) {
                    cutoffs[0][j] = (float) (min1 + (max1 - min1) * ((j + 1) / (float) divisions));
                    cutoffs[1][j] = (float) (min2 + (max2 - min2) * ((j + 1) / (float) divisions));
                }
                cutoffs[0][divisions - 1] = max1;  //set max
                cutoffs[1][divisions - 1] = max2;  //set max
                data.gridCutoffs = cutoffs;

                d = new double[divisions][divisions];
                data.gridData = d;

                String[] rows = slist.split("\n");
                for (int i = 2; i < rows.length; i++) {
                    String[] column = rows[i].split(",");
                    for (int j = 1; j < column.length; j++) {
                        d[i - 2][j - 1] = Double.parseDouble(column[j]);
                    }
                }

                blockXYZDataset = null;
                xyBlockRenderer = null;
            } catch (Exception e) {
                logger.error("error getting env grid data for scatterplot", e);
            }
        }

        if (blockXYZDataset == null || xyBlockRenderer == null) {
            makeXYZdataset(min1, min2, max1, max2);
        }

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

        double[][] d = data.gridData;

        double xdiv = (max1 - min1) / d.length;
        double ydiv = (max2 - min2) / d[0].length;
        int countNonZero = 0;

        double min = Double.MAX_VALUE, max = Double.MAX_VALUE * -1;
        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                if (d[i][j] > 0) {
                    countNonZero++;
                    if (d[i][j] < min) {
                        min = d[i][j - 1];
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

    private void createDataset() {
        xyzDataset = new DefaultXYZDataset();

        String[] seriesNames = {data.getQuery().getName()};
        if (data.getLegend() != null && data.getLegend().getCategories() != null) {
            seriesNames = data.getLegend().getCategories();
        }

        int missingCount = 0;
        for (int i = 0; i < seriesNames.length; i++) {
            ArrayList<double[]> records = new ArrayList<double[]>(data.getPoints().length);
            double[][] d = data.getData();
            String[] series = data.getSeries();
            for (int j = 0; j < d.length; j++) {
                if (seriesNames.length == 1 || series[j].equals(seriesNames[i])) {
                    if (!Double.isNaN(d[j][0]) && !Double.isNaN(d[j][1])) {
                        double[] r = {d[j][0], d[j][1], i};
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
                xyzDataset.addSeries(seriesNames[i], sd);
            }
        }

        if (xyzDataset.getSeriesCount() == 0) {
            int i = 4;
            i++;
        }

        data.missing_data = missingCount == data.getPoints().length / 2;
        data.setMissingCount(missingCount);
    }

    private void createAADataset() {
        if (data.getHighlightSa() == null) {
            aaDataset = null;
            return;
        }

        aaDataset = new DefaultXYZDataset();

        SimpleRegion region = SimpleShapeFile.parseWKT(data.getHighlightSa().getWkt());

        ArrayList<double[]> records = new ArrayList<double[]>(data.getPoints().length);
        double[][] d = data.getData();
        double[] points = data.getPoints();
        for (int j = 0; j < d.length; j++) {
            if (!Double.isNaN(d[j][0]) && !Double.isNaN(d[j][1])
                    && region.isWithin(points[j * 2], points[j * 2 + 1])) {
                double[] r = {d[j][0], d[j][1], 0};
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
            aaDataset.addSeries(ACTIVE_AREA_SERIES, sd);
        }
    }

    private void createBackgroundDataset() {
        backgroundXyzDataset = new DefaultXYZDataset();

        String[] seriesNames = {data.getBackgroundQuery().getName()};

        for (int i = 0; i < seriesNames.length; i++) {
            ArrayList<double[]> records = new ArrayList<double[]>(data.getBackgroundPoints().length);
            double[][] d = data.getBackgroundData();
            String[] series = data.getBackgroundSeries();
            for (int j = 0; j < d.length; j++) {
                if (series[j].equals(seriesNames[i])) {
                    if (!Double.isNaN(d[j][0]) && !Double.isNaN(d[j][1])) {
                        double[] r = {d[j][0], d[j][1], i};
                        records.add(r);
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
                backgroundXyzDataset.addSeries(seriesNames[i], sd);
            }
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
                    return new Color(legend.getColour(seriesValues[item]) | (alpha << 24), true);
                } else if (datasetColours != null && datasetColours.length > series && datasetColours[series] != null) {
                    //colour series
                    return datasetColours[series];
                }

                return super.getPaint(dataset, series, item);
            }
        }

        int red = data.red;
        int green = data.green;
        int blue = data.blue;
        int alpha = data.opacity * 255 / 100;

        MyXYShapeRenderer renderer = new MyXYShapeRenderer();
        renderer.shapeSize = data.size;
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
                double[] d = legend.getMinMax();
                return d == null ? 0 : d[0];
            }

            @Override
            public double getUpperBound() {
                if (legend == null) {
                    return 1;
                }
                double[] d = legend.getMinMax();
                return d == null ? 0 : d[1];
            }

            @Override
            public Paint getPaint(double d) {
                if (legend == null) {
                    return defaultColour;
                }
                return new Color(legend.getColour(d));
            }
        }

        renderer.setPaintScale(new LegendFieldPaintScale(legend, new Color(red, green, blue, alpha)));

        return renderer;
    }

    public void onClick$btnEditAppearance1(Event event) {
        if (layerWindow != null) {
            boolean closing = layerWindow.getParent() != null;
            layerWindow.detach();
            layerWindow = null;

            if (closing) {
                return;
            }
        }

        getScatterplotData();

        if (data.getQuery() != null) {
            //preview species list
            layerWindow = (ScatterplotLayerLegendComposer) Executions.createComponents("WEB-INF/zul/legend/ScatterplotLayerLegend.zul", getRoot(), null);
            EventListener el = new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    updateFromLegend();
                }
            };
            layerWindow.init(data.getQuery(), mapLayer, data.red, data.green, data.blue, data.size, data.opacity, data.colourMode, el);

            try {
                layerWindow.doOverlapped();
                layerWindow.setPosition("right");
            } catch (Exception e) {
                logger.error("error opening ScatterplotLayerLegend.zul", e);
            }
        }
    }

    public void updateFromLegend() {
        updateFromLegend(
                layerWindow.getRed(),
                layerWindow.getGreen(),
                layerWindow.getBlue(),
                layerWindow.getOpacity(),
                layerWindow.getPlotSize(),
                layerWindow.getColourMode());

        if (mapLayer != null) {
            mapLayer.setColourMode(layerWindow.getColourMode());
            mapLayer.setRedVal(layerWindow.getRed());
            mapLayer.setGreenVal(layerWindow.getGreen());
            mapLayer.setBlueVal(layerWindow.getBlue());
            mapLayer.setOpacity(layerWindow.getOpacity() / 100.0f);
            mapLayer.setSizeVal(layerWindow.getSize());

            getMapComposer().applyChange(mapLayer);
        }
    }

    public void updateFromLegend(int red, int green, int blue, int opacity, int size, String colourMode) {
        data.red = red;
        data.green = green;
        data.blue = blue;
        data.opacity = opacity;
        data.size = size;
        data.colourMode = colourMode;

        data.imagePath = null;
        redraw();
    }

    private void resample() {
        try {
            if (data != null && data.getQuery() != null
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {

                String thisResampleData = data.getQuery().getQ() + "*" + data.colourMode
                        + "*" + ((data.getFilterSa() != null) ? "Y" : "N");

                String thisResampleLayers = data.getLayer1() + "*" + data.getLayer2();
                String thisResampleHighlight = ((data.getHighlightSa() != null) ? String.valueOf(data.getHighlightSa().getWkt().hashCode()) : "N");

                if (data.prevResampleData == null || !data.prevResampleData.equals(thisResampleData)
                        || xyzDataset == null || xyzDataset.getSeriesCount() == 0) {
                    data.prevResampleData = thisResampleData;
                    data.prevResampleLayers = null;
                    data.prevResampleHighlight = null;

                    ArrayList<QueryField> fields = new ArrayList<QueryField>();
                    fields.add(new QueryField(data.getQuery().getRecordIdFieldName()));
                    fields.add(new QueryField(data.getQuery().getRecordLongitudeFieldName()));
                    fields.add(new QueryField(data.getQuery().getRecordLatitudeFieldName()));

                    if (!data.colourMode.equals("-1")) {
                        fields.add(new QueryField(data.colourMode));

                        data.setLegend(data.getQuery().getLegend(data.colourMode));
                    } else {
                        data.setLegend(null);
                    }

                    data.results = data.getQuery().sample(fields);

                    if (data.results == null) {
                        //TODO: fail nicely
                    }

                    CSVReader csvreader = new CSVReader(new StringReader(data.results));
                    List<String[]> csv = csvreader.readAll();
                    csvreader.close();

                    int longitudeColumn = Util.findInArray(data.getQuery().getRecordLongitudeFieldDisplayName(), csv.get(0));
                    int latitudeColumn = Util.findInArray(data.getQuery().getRecordLatitudeFieldDisplayName(), csv.get(0));
                    int idColumn = Util.findInArray(data.getQuery().getRecordIdFieldDisplayName(), csv.get(0));
                    int seriesColumn = Util.findInArray(data.getQuery().getRecordFieldDisplayName(data.colourMode), csv.get(0));

                    double[] points = new double[(csv.size() - 1) * 2];
                    String[] series = new String[csv.size() - 1];
                    double[] seriesValues = new double[points.length];
                    String[] ids = new String[csv.size() - 1];
                    int pos = 0;
                    for (int i = 1; i < csv.size(); i++) {
                        try {
                            points[pos] = Double.parseDouble(csv.get(i)[longitudeColumn]);
                            points[pos + 1] = Double.parseDouble(csv.get(i)[latitudeColumn]);
                        } catch (Exception e) {
                            points[pos] = Double.NaN;
                            points[pos + 1] = Double.NaN;
                        }
                        if (seriesColumn < 0) {
                            series[pos / 2] = data.getQuery().getName();
                        } else {
                            series[pos / 2] = csv.get(i)[seriesColumn];

                            //why is this required only sometimes?
                            String s = series[pos / 2];
                            if (s != null && s.length() > 2
                                    && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
                                series[pos / 2] = s.substring(1, s.length() - 1);
                            }
                        }
                        try {
                            seriesValues[pos / 2] = Double.parseDouble(csv.get(i)[seriesColumn]);
                        } catch (Exception e) {
                        }
                        ids[pos / 2] = csv.get(i)[idColumn];
                        pos += 2;
                    }

                    data.setPoints(points);
                    data.setSeries(series);
                    data.setSeriesValues(seriesValues);
                    data.setIds(ids);
                }

                if (data.prevResampleLayers == null || !data.prevResampleLayers.equals(thisResampleLayers)
                        || xyzDataset == null || xyzDataset.getSeriesCount() == 0) {
                    data.prevResampleLayers = thisResampleLayers;

                    //TODO: put this somewhere where it makes more sense, Recordslookup
                    if (data.getQuery() instanceof UploadQuery) {
                        ArrayList<QueryField> fields = new ArrayList<QueryField>();
                        fields.add(new QueryField(CommonData.getLayerFacetName(data.getLayer1())));
                        fields.add(new QueryField(CommonData.getLayerFacetName(data.getLayer2())));
                        data.getQuery().sample(fields);
                    }

                    data.setData(sample(data.getPoints()));

                    createDataset();
                }

                if (data.prevResampleHighlight == null || !data.prevResampleHighlight.equals(thisResampleHighlight)
                        || aaDataset == null || aaDataset.getSeriesCount() == 0) {
                    data.prevResampleHighlight = thisResampleHighlight;
                    if (data.getHighlightSa() == null) {
                        aaDataset = null;
                    } else {
                        createAADataset();
                    }
                }
            } else {
                //no resample available
                aaDataset = null;
                xyzDataset = null;
                annotation = null;
                if (data != null) {
                    data.setMissingCount(0);
                }
            }

        } catch (Exception e) {
            logger.error("scatterplot resampling error", e);
        }
    }

    private void resampleBackground() {
        try {
            if (data.getBackgroundQuery() != null
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {

                String thisResampleBackgroundData = data.getBackgroundQuery().getQ();
                String thisResampleBackgroundLayers = data.getLayer1() + "*" + data.getLayer2();
                if (data.prevResampleBackgroundData == null || !data.prevResampleBackgroundData.equals(thisResampleBackgroundData)) {
                    data.prevResampleBackgroundData = thisResampleBackgroundData;
                    data.prevResampleBackgroundLayers = null;

                    ArrayList<QueryField> fields = new ArrayList<QueryField>();
                    fields.add(new QueryField(data.getBackgroundQuery().getRecordIdFieldName()));
                    fields.add(new QueryField(data.getBackgroundQuery().getRecordLongitudeFieldName()));
                    fields.add(new QueryField(data.getBackgroundQuery().getRecordLatitudeFieldName()));

                    String backgroundResults = data.getBackgroundQuery().sample(fields);

                    if (backgroundResults == null) {
                        //TODO: fail nicely
                    }

                    CSVReader csvReader = new CSVReader(new StringReader(backgroundResults));
                    List<String[]> csv = csvReader.readAll();
                    csvReader.close();

                    int longitudeColumn = Util.findInArray(data.getBackgroundQuery().getRecordLongitudeFieldDisplayName(), csv.get(0));
                    int latitudeColumn = Util.findInArray(data.getBackgroundQuery().getRecordLatitudeFieldDisplayName(), csv.get(0));
                    int idColumn = Util.findInArray(data.getBackgroundQuery().getRecordIdFieldDisplayName(), csv.get(0));

                    double[] points = new double[(csv.size() - 1) * 2];
                    String[] series = new String[csv.size() - 1];
                    String[] ids = new String[csv.size() - 1];
                    int pos = 0;
                    for (int i = 1; i < csv.size(); i++) {
                        try {
                            points[pos] = Double.parseDouble(csv.get(i)[longitudeColumn]);
                            points[pos + 1] = Double.parseDouble(csv.get(i)[latitudeColumn]);
                        } catch (Exception e) {
                            points[pos] = Double.NaN;
                            points[pos + 1] = Double.NaN;
                        }
                        series[pos / 2] = data.getBackgroundQuery().getName();
                        ids[pos / 2] = csv.get(i)[idColumn];
                        pos += 2;
                    }

                    data.setBackgroundPoints(points);
                    data.setBackgroundSeries(series);
                    data.setBackgroundIds(ids);
                }

                if (data.prevResampleBackgroundLayers == null || !data.prevResampleBackgroundLayers.equals(thisResampleBackgroundLayers)) {
                    data.setBackgroundData(sample(data.getBackgroundPoints()));

                    createBackgroundDataset();
                }
            } else {
                //invalid background
                backgroundXyzDataset = null;
            }
        } catch (Exception e) {
            logger.error("error preparing background data", e);
        }
    }

    public void onClick$btnClearSelection(Event event) {
        clearSelection();
    }

    XYShapeRenderer getBackgroundRenderer() {
        XYShapeRenderer renderer = new XYShapeRenderer();
        Color c = new Color(255, 164, 96, 150);
        renderer.setSeriesPaint(0, c);

        double size = data.size + 10;
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

        double size = data.size + 3;
        double delta = size / 2;
        renderer.setSeriesShape(0, new Ellipse2D.Double(-delta, -delta, size, size));

        return renderer;
    }

    @Override
    public void setMapLayer(MapLayer mapLayer) {
        this.mapLayer = mapLayer;
        retrieve();

        redraw();
    }

    private void retrieve() {
        try {
            if (mapLayer != null) {
                data = mapLayer.getScatterplotData();

                //data rebuilding required
                if (data.getBackgroundQuery() != null) {
                    data.setBackgroundData(sample(data.getBackgroundPoints()));
                    createBackgroundDataset();
                }

                //interface
                chkSelectMissingRecords.setChecked(data.missing_data_checked);

                if (data.prevSelection != null) {
                    annotation = new XYBoxAnnotation(data.prevSelection[2], data.prevSelection[0], data.prevSelection[3], data.prevSelection[1]);
                    tbxDomain.setValue(String.format("%s: %g - %g", data.getLayer1Name(), data.prevSelection[2], data.prevSelection[3]));
                    tbxRange.setValue(String.format("%s: %g - %g", data.getLayer2Name(), data.prevSelection[0], data.prevSelection[1]));

                    updateCount(String.valueOf(data.selectionCount));
                } else {
                    scatterplotButtons.setVisible(false);
                }

                if (data.getMissingCount() > 0) {
                    tbxMissingCount.setValue("(" + data.getMissingCount() + ")");
                    chkSelectMissingRecords.setVisible(true);
                } else {
                    tbxMissingCount.setValue("");
                    chkSelectMissingRecords.setVisible(false);
                }

            }
        } catch (Exception e) {
            logger.error("error restore scatterplot legend", e);
        }
    }

    private void store() {
        if (mapLayer != null) {
            mapLayer.setScatterplotData(data);
        }
    }

    private Facet getFacetIn() {
        String fq = null;
        String e1 = CommonData.getLayerFacetName(data.getLayer1());
        String e2 = CommonData.getLayerFacetName(data.getLayer2());

        if (chkSelectMissingRecords.isChecked() && data.prevSelection == null) {
            fq = "-(" + e1 + ":[* TO *] AND " + e2 + ":[* TO *])";
        } else if (data.prevSelection != null) {
            double x1 = data.prevSelection[0];
            double x2 = data.prevSelection[1];
            double y1 = data.prevSelection[2];
            double y2 = data.prevSelection[3];

            Facet f1 = new Facet(e1, y1, y2, true);
            Facet f2 = new Facet(e2, x1, x2, true);

            if (chkSelectMissingRecords.isChecked()) {
                fq = "-(-(" + f1.toString() + " AND " + f2.toString() + ") AND " + e1 + ":[* TO *] AND " + e2 + ":[* TO *])";
            } else {
                fq = f1.toString() + " AND " + f2.toString();
            }
        }

        return Facet.parseFacet(fq);
    }

    private Facet getFacetOut() {
        String fq = "*:*";
        String e1 = CommonData.getLayerFacetName(data.getLayer1());
        String e2 = CommonData.getLayerFacetName(data.getLayer2());
        if (chkSelectMissingRecords.isChecked() && data.prevSelection == null) {
            fq = e1 + ":[* TO *] AND " + e2 + ":[* TO *]";
        } else if (data.prevSelection != null) {
            double x1 = data.prevSelection[0];
            double x2 = data.prevSelection[1];
            double y1 = data.prevSelection[2];
            double y2 = data.prevSelection[3];

            Facet f1 = new Facet(e1, y1, y2, true);
            Facet f2 = new Facet(e2, x1, x2, true);
            if (chkSelectMissingRecords.isChecked()) {
                fq = "-(" + f1.toString() + " AND " + f2.toString() + ") AND " + e1 + ":[* TO *] AND " + e2 + ":[* TO *]";
            } else {
                fq = "-(" + f1.toString() + " AND " + f2.toString() + ")";
            }
        }

        return Facet.parseFacet(fq);
    }

    private double[][] sample(double[] points) {
        double[][] p = new double[points.length / 2][2];
        for (int i = 0; i < points.length; i += 2) {
            p[i / 2][0] = points[i];
            p[i / 2][1] = points[i + 1];
        }

        ArrayList<String> layers = new ArrayList<String>();
        layers.add(CommonData.getLayerFacetName(data.getLayer1()));
        layers.add(CommonData.getLayerFacetName(data.getLayer2()));
        List<String[]> sample = Sampling.sampling(layers, p);

        double[][] d = new double[p.length][2];
        for (int j = 0; j < p.length; j++) {
            for (int i = 0; i < sample.size(); i++) {
                try {
                    d[j][i] = Double.parseDouble(sample.get(i)[j]);
                } catch (Exception e) {
                    d[j][i] = Double.NaN;
                }
            }
        }

        return d;
    }

    private void updateCbHighlightArea() {
        for (int i = cbHighlightArea.getItemCount() - 1; i >= 0; i--) {
            cbHighlightArea.removeItemAt(i);
        }

        boolean selectionSuccessful = false;
        for (MapLayer ml : getMapComposer().getPolygonLayers()) {
            Comboitem ci = new Comboitem(ml.getDisplayName());
            ci.setValue(ml);
            ci.setParent(cbHighlightArea);
            if (data != null && data.getHighlightSa() != null
                    && data.getHighlightSa().getMapLayer().getName().equals(ml.getName())) {
                cbHighlightArea.setSelectedItem(ci);
                selectionSuccessful = true;
            }
        }

        //this may be a deleted layer or current view or au or world
        if (!selectionSuccessful && data != null
                && data.getHighlightSa() != null) {
            MapLayer ml = data.getHighlightSa().getMapLayer();
            if (ml != null) {
                Comboitem ci = new Comboitem(ml.getDisplayName() + " (DELETED LAYER)");
                ci.setValue(ml);
                ci.setParent(cbHighlightArea);
                cbHighlightArea.setSelectedItem(ci);
            } else {
                String name = "Previous area";
                if (data.getHighlightSa().getWkt() != null) {
                    if (data.getHighlightSa().getWkt().equals(CommonData.AUSTRALIA_WKT)) {
                        name = "Australia";
                    } else if (data.getHighlightSa().getWkt().equals(CommonData.WORLD_WKT)) {
                        name = "World";
                    }
                }
                Comboitem ci = new Comboitem(name);
                ci.setValue(data.getHighlightSa().getWkt());
                ci.setParent(cbHighlightArea);
                cbHighlightArea.setSelectedItem(ci);
            }
        }
    }

    public void onSelect$cbHighlightArea(Event event) {
        if (cbHighlightArea.getSelectedItem() != null) {
            if (cbHighlightArea.getSelectedItem().getValue() instanceof MapLayer) {
                MapLayer ml = ((MapLayer) cbHighlightArea.getSelectedItem().getValue());
                SelectedArea sa = new SelectedArea(ml, ml.getWKT());
                data.setHighlightSa(sa);
            } else {
                String wkt = (String) cbHighlightArea.getSelectedItem().getValue();
                SelectedArea sa = new SelectedArea(null, wkt);
                data.setHighlightSa(sa);
            }
        } else {
            data.setHighlightSa(null);
        }
        data.imagePath = null;
        redraw();
    }

    public void onClick$bClearHighlightArea(Event event) {
        cbHighlightArea.setSelectedIndex(-1);
        onSelect$cbHighlightArea(null);
    }
}
