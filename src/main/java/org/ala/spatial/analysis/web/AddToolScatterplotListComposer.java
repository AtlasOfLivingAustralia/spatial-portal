/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.util.LayerUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.data.*;
import org.ala.spatial.sampling.Sampling;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.ScatterplotData;
import org.ala.spatial.util.SelectedArea;
import org.ala.spatial.util.UserData;
import org.ala.spatial.wms.RecordsLookup;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleAnchor;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;

/**
 *
 * @author ajay
 */
public class AddToolScatterplotListComposer extends AddToolComposer {

    int generation_count = 1;
    ScatterplotData data;
    Checkbox chkShowEnvIntersection;
    DefaultXYZDataset xyzDataset;
    DefaultXYZDataset backgroundXyzDataset;
    JFreeChart jChart;
    XYPlot plot;
    ChartRenderingInfo chartRenderingInfo;
    Boolean missing_data = false;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot";
        this.totalSteps = 6;

        this.setIncludeAnalysisLayersForAnyQuery(true);
        //this.setIncludeAnalysisLayersForUploadQuery(true);

        this.loadAreaLayers("World");
        this.loadSpeciesLayers();
        this.loadAreaLayersHighlight();
        this.loadSpeciesLayersBk();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("My Scatterplot"));
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        this.updateName(getMapComposer().getNextAreaLayerName("My Scatterplot"));
    }

    @Override
    public boolean onFinish() {
        System.out.println("Area: " + getSelectedArea());
        System.out.println("Species: " + getSelectedSpecies());

        Query query = getSelectedSpecies();
        if (query == null) {
            getMapComposer().showMessage("There was a problem selecting the species.  Try to select the species again", this);
            return false;
        }

        Query lsid = getSelectedSpecies();
        String name = getSelectedSpeciesName();

//        JSONObject jo = (JSONObject) cbLayer1.getSelectedItem().getValue();
//        String lyr1name = cbLayer1.getText();
//        String lyr1value = jo.getString("name");
//
//        jo = (JSONObject) cbLayer2.getSelectedItem().getValue();
//        String lyr2name = cbLayer2.getText();
//        String lyr2value = jo.getString("name");        

        if (lsid instanceof BiocacheQuery) {
            //split layers into 'in biocache' and 'out of biocache'
            Set<String> biocacheLayers = CommonData.biocacheLayerList;

            {
                ArrayList<QueryField> f = new ArrayList<QueryField>();
                f.add(new QueryField(lsid.getRecordIdFieldName()));
                f.add(new QueryField(lsid.getRecordLongitudeFieldName()));
                f.add(new QueryField(lsid.getRecordLatitudeFieldName()));

                String results = lsid.sample(f);

                // Read a line in to check if it's a valid file
                // if it throw's an error, then it's not a valid csv file
                CSVReader reader = new CSVReader(new StringReader(results));

                List userPoints = null;
                try {
                    userPoints = reader.readAll();
                } catch (IOException ex) {
                    Logger.getLogger(AddToolScatterplotListComposer.class.getName()).log(Level.SEVERE, null, ex);
                }

                System.out.println("userPoints.size(): " + userPoints.size());
                //if only one column treat it as a list of LSID's
                if (userPoints.size() == 0) {
                    throw (new RuntimeException("no data in csv"));
                }

                boolean hasHeader = false;

                // check if it has a header
                String[] upHeader = (String[]) userPoints.get(0);
                try {
                    Double d1 = new Double(upHeader[1]);
                    Double d2 = new Double(upHeader[2]);
                } catch (Exception e) {
                    hasHeader = true;
                }

                System.out.println("hasHeader: " + hasHeader);

                UserData ud = new UserData("scatterplot");

                // check if the count of points goes over the threshold.
                int sizeToCheck = (hasHeader) ? userPoints.size() - 1 : userPoints.size();
                System.out.println("Checking user points size: " + sizeToCheck + " -> " + settingsSupplementary.getValueAsInt("max_record_count_upload"));

                ArrayList<QueryField> fields = new ArrayList<QueryField>();
                if (upHeader.length == 2) {
                    //only points upload, add 'id' column at the start
                    fields.add(new QueryField("id"));
                    fields.get(0).ensureCapacity(sizeToCheck);
                }
                String[] defaultHeader = {"id", "longitude", "latitude"};
                for (int i = 0; i < upHeader.length; i++) {
                    String n = upHeader[i];
                    if (upHeader.length == 2 && i < 2) {
                        n = defaultHeader[i + 1];
                    } else if (upHeader.length > 2 && i < 3) {
                        n = defaultHeader[i];
                    }
                    fields.add(new QueryField("f" + String.valueOf(i), n, QueryField.FieldType.AUTO));
                    fields.get(fields.size() - 1).ensureCapacity(sizeToCheck);
                }

                double[] points = new double[sizeToCheck * 2];
                int counter = 1;
                int hSize = hasHeader ? 1 : 0;
                for (int i = 0; i < userPoints.size() - hSize; i++) {
                    String[] up = (String[]) userPoints.get(i + hSize);
                    if (up.length > 2) {
                        for (int j = 0; j < up.length && j < fields.size(); j++) {
                            //replace anything that may interfere with webportal facet parsing
                            String s = up[j].replace("\"", "'").replace(" AND ", " and ").replace(" OR ", " or ");
                            if (s.length() > 0 && s.charAt(0) == '*') {
                                s = "_" + s;
                            }
                            fields.get(j).add(s);
                        }
                        try {
                            points[i * 2] = Double.parseDouble(up[1]);
                            points[i * 2 + 1] = Double.parseDouble(up[2]);
                        } catch (Exception e) {
                        }
                    } else if (up.length > 1) {
                        fields.get(0).add(ud.getName() + "-" + counter);
                        for (int j = 0; j < up.length && j < fields.size(); j++) {
                            fields.get(j + 1).add(up[j]);
                        }
                        try {
                            points[i * 2] = Double.parseDouble(up[0]);
                            points[i * 2 + 1] = Double.parseDouble(up[1]);
                        } catch (Exception e) {
                        }
                        counter++;
                    }
                }

                for (int i = 0; i < fields.size(); i++) {
                    fields.get(i).store();
                }

                String pid = String.valueOf(System.currentTimeMillis());

                ud.setFeatureCount(userPoints.size() - hSize);

                String metadata = "";
                metadata += "User uploaded points \n";
                metadata += "Name: " + ud.getName() + " <br />\n";
                metadata += "Description: " + ud.getDescription() + " <br />\n";
                metadata += "Date: " + ud.getDisplayTime() + " <br />\n";
                metadata += "Number of Points: " + ud.getFeatureCount() + " <br />\n";

                ud.setMetadata(metadata);
                ud.setSubType(LayerUtilities.SPECIES_UPLOAD);
                ud.setLsid(pid);

                lsid = new UploadQuery(pid, ud.getName(), points, fields, metadata);
                ud.setQuery(lsid);
                RecordsLookup.putData(pid, points, fields, metadata);
                try {
                    // close the reader and data streams
                    reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(AddToolScatterplotListComposer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        String pid = "";
        Rectangle2D.Double selection = null;
        boolean enabled = true;

        Query backgroundLsid = getSelectedSpeciesBk();
        if (bgSearchSpeciesAuto.getSelectedItem() != null
                && bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties() != null
                && bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() > 0) {
            backgroundLsid = QueryUtil.get((String) bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0), getMapComposer(), false, getGeospatialKosher());
        }

        SelectedArea filterSa = getSelectedArea();
        SelectedArea highlightSa = getSelectedAreaHighlight();

        boolean envGrid = chkShowEnvIntersection.isChecked();

        Query lsidQuery = QueryUtil.queryFromSelectedArea(lsid, filterSa, false, getGeospatialKosher());

        Query backgroundLsidQuery = QueryUtil.queryFromSelectedArea(backgroundLsid, filterSa, false, getGeospatialKosherBk());

        ArrayList<ScatterplotData> datas = new ArrayList<ScatterplotData>();

        String sbenvsel = getSelectedLayers();
        String[] layers = sbenvsel.split(":");
        if (layers.length > 20) {
            getMapComposer().showMessage(sbenvsel.split(":").length + " layers selected.  Please select fewer than 20 environmental layers in step 1.");
            return false;
        }

        //TODO: discover why the first resample works if done backwards first.
        if (data == null) {
            data = new ScatterplotData(lsidQuery, name, CommonData.getLayerDisplayName(layers[1]),
                    layers[1], CommonData.getLayerDisplayName(layers[0]), layers[0], pid, selection, enabled,
                    (backgroundLsid != null) ? backgroundLsidQuery : null,
                    filterSa, highlightSa, envGrid);
            resample();
        }

        ArrayList<String> imageUrls = new ArrayList<String>();
        for (int i = 0; i < layers.length - 1; i++) {
            for (int j = i + 1; j < layers.length; j++) {
                if (data == null) {
                    data = new ScatterplotData(lsidQuery, name, CommonData.getLayerDisplayName(layers[i]),
                            layers[i], CommonData.getLayerDisplayName(layers[j]), layers[j], pid, selection, enabled,
                            (backgroundLsid != null) ? backgroundLsidQuery : null,
                            filterSa, highlightSa, envGrid);
                } else {
                    data.setLayer1(layers[i]);
                    data.setLayer1Name(CommonData.getLayerDisplayName(layers[i]));
                    data.setLayer2(layers[j]);
                    data.setLayer2Name(CommonData.getLayerDisplayName(layers[j]));
                }

                resample();
                String imageUrl = null;
                try {
                    imageUrl = draw();
                    System.out.println("scatterplot image: " + layers[i] + "," + layers[j] + " > " + imageUrl);
                } catch (IOException ex) {
                    System.out.println("failed scatterplot image: " + layers[i] + "," + layers[j]);
                    ex.printStackTrace();                    
                }
                imageUrls.add(imageUrl);
            }
        }

        String htmlUrl = makeHtml(layers, imageUrls);
        Events.echoEvent("openUrl", getMapComposer(), htmlUrl);

        this.detach();

        return true;
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                if (rSpeciesSearch.isChecked()) {
                    searchSpeciesAuto.setFocus(true);
                } else {
                    rgSpecies.setFocus(true);
                }
                break;
            case 3:
                rgAreaHighlight.setFocus(true);
                break;
            case 4:
                //cbLayer2.setFocus(true);
                break;
            case 5:
                if (rSpeciesSearchBk.isChecked()) {
                    bgSearchSpeciesAuto.setFocus(true);
                } else {
                    rgSpeciesBk.setFocus(true);
                }
                break;
            case 6:
                tToolName.setFocus(true);
                break;
        }
    }

    String draw() throws IOException {
        if (missing_data) {
            return null;
        }
        //active area must be drawn first

        jChart = ChartFactory.createScatterPlot(data.getSpeciesName(), data.getLayer1Name(), data.getLayer2Name(), xyzDataset, PlotOrientation.HORIZONTAL, false, false, false);

        jChart.setBackgroundPaint(Color.white);
        plot = (XYPlot) jChart.getPlot();

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
        plot.setRenderer(getRenderer(legend, seriesColours, seriesNames, data.getSeriesValues()));

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

        int width = 320;
        int height = 320;
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

        try {
            FileOutputStream fos = new FileOutputStream(pth + uid + ".png");
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return htmlurl + uid + ".png";
    }
    String prevResampleData = null;
    String prevResampleLayers = null;
    String prevResampleHighlight = null;

    private void resample() {
        try {
            if (data != null && data.getQuery() != null
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {

                String thisResampleData = data.getQuery().getQ() + "*" + data.colourMode
                        + "*" + ((data.getFilterSa() != null) ? "Y" : "N");

                String thisResampleLayers = data.getLayer1() + "*" + data.getLayer2();
                String thisResampleHighlight = ((data.getHighlightSa() != null) ? String.valueOf(data.getHighlightSa().getWkt().hashCode()) : "N");

                if (prevResampleData == null || !prevResampleData.equals(thisResampleData)
                        || xyzDataset == null || xyzDataset.getSeriesCount() == 0) {
                    prevResampleData = thisResampleData;
                    prevResampleLayers = null;
                    prevResampleHighlight = null;

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

                    String results = data.getQuery().sample(fields);

                    if (results == null) {
                        //TODO: fail nicely
                    }

                    CSVReader csvreader = new CSVReader(new StringReader(results));
                    List<String[]> csv = csvreader.readAll();
                    csvreader.close();

                    int longitudeColumn = findInArray(data.getQuery().getRecordLongitudeFieldDisplayName(), csv.get(0));
                    int latitudeColumn = findInArray(data.getQuery().getRecordLatitudeFieldDisplayName(), csv.get(0));
                    int idColumn = findInArray(data.getQuery().getRecordIdFieldDisplayName(), csv.get(0));
                    int seriesColumn = findInArray(data.getQuery().getRecordFieldDisplayName(data.colourMode), csv.get(0));

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

                if (prevResampleLayers == null || !prevResampleLayers.equals(thisResampleLayers)
                        || xyzDataset == null || xyzDataset.getSeriesCount() == 0) {
                    prevResampleLayers = thisResampleLayers;

                    //TODO: put this somewhere where it makes more sense, Recordslookup
                    if (data.getQuery() instanceof UploadQuery) {
                        ArrayList<QueryField> fields = new ArrayList<QueryField>();
                        fields.add(new QueryField(CommonData.getLayerFacetName(data.getLayer1())));
                        fields.add(new QueryField(CommonData.getLayerFacetName(data.getLayer2())));
                        data.getQuery().sample(fields);
                    }

                    sample();

                    createDataset();
                }
            } else {
                //no resample available
                xyzDataset = null;
                if (data != null) {
                    data.setMissingCount(0);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    String prevResampleBackgroundData = null;
    String prevResampleBackgroundLayers = null;

    private void resampleBackground() {
        try {
            if (data.getBackgroundQuery() != null
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {

                String thisResampleBackgroundData = data.getBackgroundQuery().getQ();
                String thisResampleBackgroundLayers = data.getLayer1() + "*" + data.getLayer2();
                if (prevResampleBackgroundData == null || !prevResampleBackgroundData.equals(thisResampleBackgroundData)) {
                    prevResampleBackgroundData = thisResampleBackgroundData;
                    prevResampleBackgroundLayers = null;

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

                    int longitudeColumn = findInArray(data.getBackgroundQuery().getRecordLongitudeFieldDisplayName(), csv.get(0));
                    int latitudeColumn = findInArray(data.getBackgroundQuery().getRecordLatitudeFieldDisplayName(), csv.get(0));
                    int idColumn = findInArray(data.getBackgroundQuery().getRecordIdFieldDisplayName(), csv.get(0));

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

                if (prevResampleBackgroundLayers == null || !prevResampleBackgroundLayers.equals(thisResampleBackgroundLayers)) {
                    sampleBackground();

                    createBackgroundDataset();
                }
            } else {
                //invalid background
                backgroundXyzDataset = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
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

                int r = (c >> 16) & 0x000000FF;
                int g = (c >> 8) & 0x000000FF;
                int b = c & 0x000000FF;
                renderer.datasetColours[i] = new Color(r, g, b, alpha);
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

    private int findInArray(String lookFor, String[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(lookFor)) {
                return i;
            }
        }
        return -1;
    }
    String prevBlockPlot = null;
    DefaultXYZDataset blockXYZDataset = null;
    XYBlockRenderer xyBlockRenderer = null;

    void addBlockPlot(XYPlot scatterplot, String env1, String displayName1, double min1, double max1, String env2, String displayName2, double min2, double max2) {
        String thisBlockPlot = env1 + "*" + min1 + "*" + max1 + "*" + env2 + "*" + min2 + "*" + max2;
        if (prevBlockPlot == null || !prevBlockPlot.equals(thisBlockPlot)) {
            prevBlockPlot = thisBlockPlot;
            //get data
            double[][] d = null;
            double min = Double.MAX_VALUE, max = Double.MAX_VALUE * -1;
            int countNonZero = 0;
            try {
                int divisions = 20;
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(CommonData.satServer).append("/ws/chart?&divisions=").append(divisions);
                sbProcessUrl.append("&wkt=").append(URLEncoder.encode(data.getFilterSa().getWkt(), "UTF-8"));

                sbProcessUrl.append("&xaxis=").append(URLEncoder.encode(env1, "UTF-8")).append(",").append(min1).append(",").append(max1);
                sbProcessUrl.append("&yaxis=").append(URLEncoder.encode(env2, "UTF-8")).append(",").append(min2).append(",").append(max2);

                System.out.println(sbProcessUrl.toString());

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
                        if (d[i - 2][j - 1] > 0) {
                            countNonZero++;
                            if (d[i - 2][j - 1] < min) {
                                min = d[i - 2][j - 1];
                            }
                            if (d[i - 2][j - 1] > max) {
                                max = d[i - 2][j - 1];
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            double range = max - min;
            double xdiv = (max1 - min1) / d.length;
            double ydiv = (max2 - min2) / d[0].length;

            DefaultXYZDataset defaultXYZDataset = new DefaultXYZDataset();

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
            defaultXYZDataset.addSeries("Environmental area intersection", dat);
            blockXYZDataset = defaultXYZDataset;

            xyBlockRenderer = new XYBlockRenderer();

            GrayPaintScale ramp = new GrayPaintScale(Math.log10(min + 1), Math.log10(max + 1));
            xyBlockRenderer.setPaintScale(ramp);
            xyBlockRenderer.setBlockHeight(ydiv);   //backwards
            xyBlockRenderer.setBlockWidth(xdiv);    //backwards
            xyBlockRenderer.setBlockAnchor(RectangleAnchor.BOTTOM_LEFT);

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

    XYShapeRenderer getBackgroundRenderer() {
        XYShapeRenderer renderer = new XYShapeRenderer();
        Color c = new Color(255, 164, 96, 150);
        renderer.setSeriesPaint(0, c);

        double size = data.size + 10;
        double delta = size / 2;
        renderer.setSeriesShape(0, new Ellipse2D.Double(-delta, -delta, size, size));

        return renderer;
    }

    private void sample() {
        double[] points = data.getPoints();
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

        data.setData(d);
    }

    private void sampleBackground() {
        double[] points = data.getBackgroundPoints();
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

        data.setBackgroundData(d);
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

        missing_data = missingCount == data.getPoints().length / 2;
        data.setMissingCount(missingCount);
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

    String htmlHeader = "<html><body><table border=1><tr><th>Layer 1</th><th>Layer 2</th><th>Scatterplot</th></tr>";
    String htmlFooter = "</table></body></html>";
    private String makeHtml(String[] layers, ArrayList<String> imageUrls) {
        //linear
        StringBuilder sb = new StringBuilder();

        sb.append(htmlHeader);
        int pos = 0;
        for(int i=0;i<layers.length-1;i++) {
            for(int j=i+1;j<layers.length;j++) {
                sb.append("<tr><td>").append(CommonData.getLayerDisplayName(layers[i]));
                sb.append("</td><td>").append(CommonData.getLayerDisplayName(layers[j]));
                if(imageUrls.get(pos) == null) {
                    sb.append("</td><td>n/a</td></tr>");
                } else {
                    sb.append("</td><td><img src='").append(imageUrls.get(pos)).append("'/></td></tr>");
                }
                pos++;
            }
        }
        sb.append(htmlFooter);

        String uid = String.valueOf(System.currentTimeMillis());
        String pth = this.settingsSupplementary.getValue("print_output_path");
        String htmlurl = settingsSupplementary.getValue("print_output_url");
        try {
            FileWriter fw = new FileWriter(pth + uid + ".html");
            fw.write(sb.toString());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("ScatterplotList file: " + pth + uid + ".html");
        System.out.println("ScatterplotList output: " + sb.toString());
        return htmlurl + uid + ".html";
    }
}
