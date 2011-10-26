/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.LayerLegendComposer;
import au.org.emii.portal.composer.MapComposer;
import java.awt.Shape;
import org.apache.commons.lang.StringUtils;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.event.Event;
import org.apache.commons.httpclient.HttpClient;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.zkoss.zul.Textbox;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.LegendObject;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.data.UploadQuery;
import org.ala.spatial.sampling.Sampling;
import org.ala.spatial.sampling.SimpleRegion;
import org.ala.spatial.sampling.SimpleShapeFile;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.ScatterplotData;
import org.ala.spatial.util.SelectedArea;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;

/**
 *
 * @author Adam
 */
public class ScatterplotWCController extends UtilityComposer implements HasMapLayer {

    private static final String NUMBER_SERIES = "Number series";
    private static final String ACTIVE_AREA_SERIES = "In Active Area";
    private SettingsSupplementary settingsSupplementary = null;
    SpeciesAutoComplete sac;
    SpeciesAutoComplete sacBackground;
    EnvLayersCombobox cbLayer1;
    EnvLayersCombobox cbLayer2;
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
    int selectionCount;
    String imagePath;
    String results;
    Checkbox chkSelectMissingRecords;
    double[] prevSelection = null;
    Checkbox chkRestrictOccurrencesToActiveArea;
    Checkbox chkHighlightActiveAreaOccurrences;
    Checkbox chkShowEnvIntersection;
    double zmin, zmax;
    private DefaultXYZDataset backgroundXyzDataset;
    //String backgroundLSID;
    //private String backgroundName;
    Div envLegend;
    Boolean missing_data = false;
    Label lblMissing;
    Button addNewLayers;
    Combobox cbHighlightArea;

    @Override
    public void afterCompose() {
        super.afterCompose();

        layersUtil = new LayersUtil(getMapComposer(), CommonData.satServer);

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

    public void onChange$sac(Event event) {
        getScatterplotData();

        //remove any previous layer highlighted now
//        if (data.getLsid() != null) {
//            getMapComposer().removeLayerHighlight(data, "species");
//        }
//
//        data.setLsid(null);
        data.setSpeciesName(null);

        if (sac.getSelectedItem() == null) {
            return;
        }

        cleanTaxon(sac);
        String taxon = sac.getValue();

        String rank = "";
        String spVal = sac.getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
            rank = spVal.trim().substring(0, spVal.trim().indexOf(":")); //"species";

            if (rank.equalsIgnoreCase("scientific name")) {
                rank = "taxon";
            }
        } else {
            rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
        }
        System.out.println("mapping rank and species: " + rank + " - " + taxon);

//        data.setLsid((String) sac.getSelectedItem().getAnnotatedProperties().get(0));
        data.setSpeciesName(taxon);

        //only add it to the map if this was signaled from an event
        if (event != null) {
            Events.echoEvent("doSpeciesChange", this, null);
        } else {
            Events.echoEvent("updateScatterplot", this, null);
        }
    }

    public void onChange$sacBackground(Event event) {
        data.setBackgroundQuery(null);

        if (sacBackground.getSelectedItem() == null) {
            return;
        }

        cleanTaxon(sacBackground);
        String taxon = sacBackground.getValue();

        String rank = "";
        String spVal = sacBackground.getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
            rank = spVal.trim().substring(0, spVal.trim().indexOf(":")); //"species";

            if (rank.equalsIgnoreCase("scientific name")) {
                rank = "taxon";
            }
        } else {
            rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
        }
        System.out.println("mapping rank and species: " + rank + " - " + taxon);

        Query query = QueryUtil.get((String) sacBackground.getSelectedItem().getAnnotatedProperties().get(0),
                getMapComposer(), false);

        Query q = QueryUtil.queryFromSelectedArea(query, data.getFilterSa(), false);

        data.setBackgroundQuery(query);

        //only add it to the map if this was signaled from an event
        //clearSelection();

        if (event != null) {
            Events.echoEvent("doSpeciesChange", this, null);
        } else {
            Events.echoEvent("updateScatterplot", this, null);
        }
    }

    public void doSpeciesChange(Event event) {
        //getMapComposer().activateLayerForScatterplot(getScatterplotData(), "species");

        Events.echoEvent("updateScatterplot", this, null);
    }

    public void onChange$cbLayer1(Event event) {
        getScatterplotData();

        //remove any previous layer highlighted now
//        if (data.getLsid() != null) {
//            getMapComposer().removeLayerHighlight(data, "species");
//        }

        if (cbLayer1.getItemCount() > 0 && cbLayer1.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) cbLayer1.getSelectedItem().getValue();
            data.setLayer1(jo.getString("name"));
            data.setLayer1Name(cbLayer1.getText());
        }

        clearSelection();

        Events.echoEvent("updateScatterplot", this, null);
    }

    public void onChange$cbLayer2(Event event) {
        getScatterplotData();

        //remove any previous layer highlighted now
//        if (data.getLsid() != null) {
//            getMapComposer().removeLayerHighlight(data, "species");
//        }

        if (cbLayer2.getItemCount() > 0 && cbLayer2.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) cbLayer2.getSelectedItem().getValue();
            data.setLayer2(jo.getString("name"));
            data.setLayer2Name(cbLayer2.getText());
        }

        clearSelection();

        Events.echoEvent("updateScatterplot", this, null);
    }

    public ScatterplotData getScatterplotData() {
        if (data == null) {
            if (mapLayer == null) {
                data = new ScatterplotData();
            } else {
                data = (ScatterplotData) mapLayer.getData("scatterplotData");
            }
        }
        return data;
    }

    public void setScatterplotData(ScatterplotData data) {
        this.data = data;

        cbLayer1.setText(data.getLayer1Name());
        cbLayer2.setText(data.getLayer2Name());

        updateScatterplot(null);

        if (!data.isEnabled()) {
            clearSelection();
        }
    }

    public void setSpecies(String lsid, String speciesName) {
        ScatterplotData d = getScatterplotData();
//        d.setLsid(lsid);
        d.setSpeciesName(speciesName);

        cbLayer1.setText(d.getLayer1Name());
        cbLayer2.setText(d.getLayer2Name());

        clearSelection();
    }

    public void onChange$tbxChartSelection(Event event) {
        try {
            //input order is [x, y, width, height]
            // + optional bounding box coordinates [x1,y1,x2,y2]
            System.out.println(((String) event.getData()));
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

            prevSelection = new double[4];
            prevSelection[0] = x1;
            prevSelection[1] = x2;
            prevSelection[2] = y1;
            prevSelection[3] = y2;

            registerScatterPlotSelection();

            ScatterplotData d = getScatterplotData();
            d.setSelection(new Rectangle2D.Double(x1, y1, x2, y2));
            d.setEnabled(true);

            //refresh mapLayer
            refreshMapLayer();

            Facet f = getFacetIn();
            if(f != null) {
                mapLayer.setHighlight(f.toString());
            } else {
                mapLayer.setHighlight(null);
            }

            getMapComposer().applyChange(mapLayer);

            tbxChartSelection.setText("");
            tbxDomain.setValue(String.format("%s: %g - %g", data.getLayer1Name(), y1, y2));
            tbxRange.setValue(String.format("%s: %g - %g", data.getLayer2Name(), x1, x2));

            annotation = new XYBoxAnnotation(y1, x1, y2, x2);

            imagePath = null;
            redraw();
        } catch (Exception e) {
            e.printStackTrace();
            clearSelection();
            getMapComposer().applyChange(mapLayer);
        }
    }

    public void updateScatterplot(Event event) {
        try {
            clearSelection();

            getScatterplotData();

            redraw();
        } catch (Exception e) {
            e.printStackTrace();
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
            if(data != null && cbHighlightArea.getItemCount() == 0) {
                updateCbHighlightArea();
            }

            if (missing_data) {
                lblMissing.setVisible(true);
                divHighlightArea.setVisible(false);
            } else {
                lblMissing.setVisible(false);
                divHighlightArea.setVisible(true);
                try {
                    //only permits redrawing if imagePath has been defined
                    if (imagePath != null) {
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
                                + imagePath.replace(pth, htmlurl) + ")')";
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

                        imagePath = pth + uid + ".png";

                        try {
                            FileOutputStream fos = new FileOutputStream(pth + uid + ".png");
                            fos.write(bytes);
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //chartImg.setWidth(width + "px");
                        //chartImg.setHeight(height + "px");
                        String script = "updateScatterplot(" + width + "," + height + ",'url(" + htmlurl + uid + ".png)')";
                        Clients.evalJavaScript(script);
                        //chartImg.setVisible(true);
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
                    e.printStackTrace();
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
            if (prevSelection != null) {
                x1 = prevSelection[0];
                x2 = prevSelection[1];
                y1 = prevSelection[2];
                y2 = prevSelection[3];
                annotation = new XYBoxAnnotation(y1, x1, y2, x2);
            } else {
                annotation = null;
            }

            if (/*data.getLsid() != null && data.getLsid().length() > 0
                    && */data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {
                ArrayList<Facet> facets = new ArrayList<Facet>();
                Facet f = getFacetIn();

                int count = 0;
                if(f != null) {
                    Query q = data.getQuery().newFacet(f, false);
                    count = q.getOccurrenceCount();
                } 
                updateCount(String.valueOf(count));
            }
        } catch (Exception e) {
            e.printStackTrace();
            clearSelection();
            getMapComposer().applyChange(mapLayer);
        }
    }

    void updateCount(String txt) {
        try {
            selectionCount = Integer.parseInt(txt);
            tbxSelectionCount.setValue("Records selected: " + txt);
            if (selectionCount > 0) {
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

        prevSelection = null;

        chkSelectMissingRecords.setChecked(false);

        getScatterplotData().setEnabled(false);

        //chartImg.setVisible(false);
        scatterplotDownloads.setVisible(false);

        annotation = null;

        scatterplotButtons.setVisible(false);
    }

    /**
     * populate sampling screen with values from active layers and area tab
     */
    public void callPullFromActiveLayers() {
        //get top species and list of env/ctx layers
        if (sac.getSelectedItem() == null || sac.getSelectedItem().getValue() == null) {
            //String species = layersUtil.getFirstSpeciesLayer();
            String speciesandlsid = layersUtil.getFirstSpeciesLayer();
            String species = null;
            if (StringUtils.isNotBlank(speciesandlsid)) {
                species = speciesandlsid.split(",")[0];
            }

            /* set species from layer selector */
            if (species != null) {
                String tmpSpecies = species;
                if (species.contains(" (")) {
                    tmpSpecies = StringUtils.substringBefore(species, " (");
                }
                sac.setValue(tmpSpecies);
                sac.refresh(tmpSpecies);

                onChange$sac(null);
            }
        }
    }

    /**
     * get rid of the common name if present
     * 2 conditions here
     *  1. either species is automatically filled in from the Layer selector
     *     and is a common name and is in format Scientific name (Common name)
     *
     *  2. or user has searched for a common name from the analysis tab itself
     *     in which case we need to grab the scientific name for analysis
     *
     *  * condition 1 should also parse the proper taxon if its a genus, for eg
     *
     * @param taxon
     * @return
     */
    private String cleanTaxon(SpeciesAutoComplete sac) {
        String taxon = null;

        if (sac.getSelectedItem() == null && sac.getValue() != null) {
            String taxValue = sac.getValue();
            if (taxValue.contains(" (")) {
                taxValue = StringUtils.substringBefore(taxValue, " (");
            }
            sac.refresh(taxValue);
        }

        //make the sac.getValue() a selected value if it appears in the list
        // - fix for common names entered but not selected
        if (sac.getSelectedItem() == null) {
            List list = sac.getItems();
            for (int i = 0; i < list.size(); i++) {
                Comboitem ci = (Comboitem) list.get(i);
                if (ci.getLabel().equalsIgnoreCase(sac.getValue())) {
                    System.out.println("cleanTaxon: set selected item");
                    sac.setSelectedItem(ci);
                    break;
                }
            }
        }

        if (sac.getSelectedItem() != null && sac.getSelectedItem().getAnnotatedProperties() != null) {
            taxon = (String) sac.getSelectedItem().getAnnotatedProperties().get(0);
        }

        return taxon;
    }

    public void onClick$addSelectedRecords(Event event) {
        Facet f = getFacetIn();
        if(f != null) {
            addUserLayer(data.getQuery().newFacet(getFacetIn(), true), "IN " + data.getSpeciesName(), "from scatterplot in group", selectionCount);
        }
    }

    public void onClick$addUnSelectedRecords(Event event) {
        addUserLayer(data.getQuery().newFacet(getFacetOut(), true), "OUT " + data.getSpeciesName(), "from scatterplot out group", results.split("\n").length - selectionCount - 1);   //-1 for header
    }

    void addUserLayer(Query query, String layername, String description, int numRecords) {
        layername = StringUtils.capitalize(layername);

        getMapComposer().mapSpecies(query, layername, "species", -1, LayerUtilities.SPECIES, null, -1);
    }

    public void onClick$addNewLayers(Event event) {
        onClick$addUnSelectedRecords(null);
        onClick$addSelectedRecords(null);
    }

    public void onClick$scatterplotImageDownload(Event event) {
        try {
            Filedownload.save(new File(imagePath), "image/png");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$scatterplotDataDownload(Event event) {
        //preview species list
//        ScatterplotResults window = (ScatterplotResults) Executions.createComponents("WEB-INF/zul/AnalysisScatterplotResults.zul", this, null);
//        window.populateList(data.getLayer1Name(), data.getLayer2Name(), results);
//        try {
//            window.doModal();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //make output csv; id, series, layer1, layer2, highlight
        StringBuilder sb = new StringBuilder();
        sb.append("id,series," + data.getLayer1Name() + "," + data.getLayer2Name());
        String[] series = data.getSeries();
        String[] ids = data.getIds();
        //double [][] points = data.getPoints();
        double[][] d = data.getData();
//        SimpleRegion r = null;
//        if(data.getHighlightWkt() != null) {
//            sb.append(",area highlight");
//            r = SimpleShapeFile.parseWKT(data.getHighlightWkt());
//        }
        for (int i = 0; i < series.length; i++) {
            sb.append("\n\"").append(ids[i].replace("\"", "\"\"")).append("\",\"").append(series[i].replace("\"", "\"\"")).append("\",").append(String.valueOf(d[i][0])).append(",").append(String.valueOf(d[i][1]));
//            if(r != null) {
//                String highlight = r.isWithin(points[i][0],points[i][1])?"true":"false";
//                sb.append(",").append(highlight).append(",")
//            }
        }

        Filedownload.save(sb.toString(), "text/plain", "scatterplot.csv");
    }

    void refreshMapLayer() {
        //mapLayer = getMapComposer().activateLayerForScatterplot(getScatterplotData(), "species");
    }

    public void onCheck$chkHighlightActiveAreaOccurrences(Event event) {
        imagePath = null;
        redraw();
    }

    public void onCheck$chkRestrictOccurrencesToActiveArea(Event event) {
        if (chkRestrictOccurrencesToActiveArea.isChecked()) {
            chkHighlightActiveAreaOccurrences.setChecked(false);
            chkHighlightActiveAreaOccurrences.setDisabled(true);
        } else {
            chkHighlightActiveAreaOccurrences.setDisabled(false);
        }

        imagePath = null;
        redraw();
    }

    public void onCheck$chkShowEnvIntersection(Event event) {
        imagePath = null;
        redraw();
    }

    public void onCheck$chkSelectMissingRecords(Event event) {
        try {
            registerScatterPlotSelection();

            ScatterplotData d = getScatterplotData();
            d.setEnabled(true);

            //refresh mapLayer
            refreshMapLayer();

            Facet f = getFacetIn();
            if(f == null) {
                mapLayer.setHighlight(null);
            } else {
                mapLayer.setHighlight(f.toString());
            }

            getMapComposer().applyChange(mapLayer);

            tbxChartSelection.setText("");

            imagePath = null;
            redraw();
        } catch (Exception e) {
            e.printStackTrace();
            clearSelection();
            getMapComposer().applyChange(mapLayer);
        }
    }
    String prevBlockPlot = null;
    DefaultXYZDataset blockXYZDataset = null;
    XYBlockRenderer xyBlockRenderer = null;

    void addBlockPlot(XYPlot scatterplot, String env1, String displayName1, double min1, double max1, String env2, String displayName2, double min2, double max2) {
        String thisBlockPlot = env1 + "*" + min1 + "*" + max1 + "*" + env2 + "*" + min2 + "*" + max2;
        if (prevBlockPlot == null || !prevBlockPlot.equals(thisBlockPlot)) {
            prevBlockPlot = thisBlockPlot;
            //get data
            double[][] data = null;
            double min = Double.MAX_VALUE, max = Double.MAX_VALUE * -1;
            int countNonZero = 0;
            try {
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(CommonData.satServer).append("/ws/sampling/chart?basis=layer&filter=lsid:none;area:none");

                sbProcessUrl.append("&xaxis=").append(URLEncoder.encode(env1, "UTF-8")).append(",").append(min1).append(",").append(max1);
                sbProcessUrl.append("&yaxis=").append(URLEncoder.encode(env2, "UTF-8")).append(",").append(min2).append(",").append(max2);

                System.out.println(sbProcessUrl.toString());

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(sbProcessUrl.toString());
                get.addRequestHeader("Accept", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                String[] rows = slist.split("\n");
                for (int i = 2; i < rows.length; i++) {
                    String[] column = rows[i].split(",");
                    if (data == null) {
                        data = new double[rows.length - 2][column.length - 1];
                    }
                    for (int j = 1; j < column.length; j++) {
                        data[i - 2][j - 1] = Double.parseDouble(column[j]);
                        if (data[i - 2][j - 1] > 0) {
                            countNonZero++;
                            if (data[i - 2][j - 1] < min) {
                                min = data[i - 2][j - 1];
                            }
                            if (data[i - 2][j - 1] > max) {
                                max = data[i - 2][j - 1];
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            double range = max - min;

            double xdiv = (max1 - min1) / data.length;
            double ydiv = (max2 - min2) / data[0].length;

            DefaultXYZDataset defaultXYZDataset = new DefaultXYZDataset();

            double[][] dat = new double[3][countNonZero];
            int pos = 0;

            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data.length; j++) {
                    if (data[i][j] > 0) {
                        dat[0][pos] = min1 + i * xdiv;
                        dat[1][pos] = min2 + j * ydiv;
                        dat[2][pos] = Math.log10(data[i][j] + 1);
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
        
        if(xyzDataset.getSeriesCount() == 0) {
            int i = 4;
            i++;
        }

        missing_data = missingCount == data.getPoints().length / 2;
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
    LayerLegendComposer layerWindow = null;

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
            layerWindow = (LayerLegendComposer) Executions.createComponents("WEB-INF/zul/LayerLegend.zul", getRoot(), null);
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
                e.printStackTrace();
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

        imagePath = null;
        redraw();
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

                    results = data.getQuery().sample(fields);

                    if (results == null) {
                        //TODO: fail nicely
                    }

                    CSVReader csvreader = new CSVReader(new StringReader(results));
                    List<String[]> csv = csvreader.readAll();
                    csvreader.close();

                    int longitudeColumn = findInArray(data.getQuery().getRecordLongitudeFieldDisplayName(), csv.get(0));
                    int latitudeColumn = findInArray(data.getQuery().getRecordLatitudeFieldDisplayName(), csv.get(0));
                    int idColumn = findInArray(data.getQuery().getRecordIdFieldDisplayName(), csv.get(0));
                    int seriesColumn = findInArray(data.colourMode, csv.get(0));

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
                            String s = series[pos/2];
                            if(s != null && s.length() > 2
                                    && s.charAt(0) == '[' && s.charAt(s.length()-1) == ']') {
                                series[pos/2] = s.substring(1,s.length()-1);
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

                if (prevResampleHighlight == null || !prevResampleHighlight.equals(thisResampleHighlight)
                        || aaDataset == null || aaDataset.getSeriesCount() == 0) {
                    prevResampleHighlight = thisResampleHighlight;
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

    public void onClick$btnClear(Event event) {
        data.setBackgroundQuery(null);

        sacBackground.setValue("");

        imagePath = null;
        redraw();
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
        data = (ScatterplotData) mapLayer.getData("scatterplotData");
        retrieve();

        redraw();
    }

    private void retrieve() {
        try {
            if (mapLayer != null) {
                //data = (ScatterplotData) mapLayer.getData("scatterplotData");
                jChart = (JFreeChart) mapLayer.getData("jChart");
                plot = (XYPlot) mapLayer.getData("plot");
                chartRenderingInfo = (ChartRenderingInfo) mapLayer.getData("chartRenderingInfo");
                //mapLayer = (MapLayer) mapLayer.getData("mapLayer");
                //mapLayer = mapLayer.getData("mapLayer");
                annotation = (XYBoxAnnotation) mapLayer.getData("annotation");
                xyzDataset = (DefaultXYZDataset) mapLayer.getData("xyzDataset");
                aaDataset = (DefaultXYZDataset) mapLayer.getData("aaDataset");
                selectionCount = (Integer) mapLayer.getData("selectionCount");

                if (selectionCount > 0) {
                    addNewLayers.setVisible(true);
                } else {
                    addNewLayers.setVisible(false);
                }
                results = (String) mapLayer.getData("results");
                prevSelection = (double[]) mapLayer.getData("prevSelection");
                zmin = (Double) mapLayer.getData("zmin");
                zmax = (Double) mapLayer.getData("zmax");
                backgroundXyzDataset = (DefaultXYZDataset) mapLayer.getData("backgroundXyzDataset");

                //interface
                tbxChartSelection.setValue((String) mapLayer.getData("tbxChartSelection"));
                tbxSelectionCount.setValue((String) mapLayer.getData("tbxSelectionCount"));
                tbxRange.setValue((String) mapLayer.getData("tbxRange"));
                tbxDomain.setValue((String) mapLayer.getData("tbxDomain"));
                tbxMissingCount.setValue((String) mapLayer.getData("tbxMissingCount"));

                scatterplotButtons.setVisible((Boolean) mapLayer.getData("scatterplotButtons"));
                scatterplotDownloads.setVisible((Boolean) mapLayer.getData("scatterplotDownloads"));
                envLegend.setVisible((Boolean) mapLayer.getData("envLegend"));

                chkSelectMissingRecords.setChecked((Boolean) mapLayer.getData("chkSelectMissingRecords"));
                chkRestrictOccurrencesToActiveArea.setChecked((Boolean) mapLayer.getData("chkRestrictOccurrencesToActiveArea"));
                chkHighlightActiveAreaOccurrences.setChecked((Boolean) mapLayer.getData("chkHighlightActiveAreaOccurrences"));
                chkShowEnvIntersection.setChecked((Boolean) mapLayer.getData("chkShowEnvIntersection"));

                imagePath = (String) mapLayer.getData("imagePath");
                missing_data = (Boolean) mapLayer.getData("missing_data");

                if (data.getMissingCount() > 0) {
                    tbxMissingCount.setValue("(" + data.getMissingCount() + ")");
                    chkSelectMissingRecords.setVisible(true);
                } else {
                    tbxMissingCount.setValue("");
                    chkSelectMissingRecords.setVisible(false);
                }

                prevResampleData = (String) mapLayer.getData("prevResampleData");
                prevResampleHighlight = (String) mapLayer.getData("prevResampleHighlight");
                prevResampleLayers = (String) mapLayer.getData("prevResampleLayers");
                prevResampleBackgroundData = (String) mapLayer.getData("prevResampleBackgroundData");
                prevResampleBackgroundLayers = (String) mapLayer.getData("prevResampleBackgroundLayers");
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void store() {
        if (mapLayer != null) {
            mapLayer.setData("scatterplotData", data);
            mapLayer.setData("jChart", jChart);
            mapLayer.setData("plot", plot);
            mapLayer.setData("chartRenderingInfo", chartRenderingInfo);
            mapLayer.setData("mapLayer", mapLayer);
            mapLayer.setData("mapLayer", mapLayer);
            mapLayer.setData("annotation", annotation);
            mapLayer.setData("xyzDataset", xyzDataset);
            mapLayer.setData("aaDataset", aaDataset);
            mapLayer.setData("selectionCount", new Integer(selectionCount));
            mapLayer.setData("results", results);
            mapLayer.setData("prevSelection", prevSelection);
            mapLayer.setData("zmin", new Double(zmin));
            mapLayer.setData("zmax", new Double(zmax));
            mapLayer.setData("backgroundXyzDataset", backgroundXyzDataset);

            mapLayer.setData("tbxChartSelection", tbxChartSelection.getValue());
            mapLayer.setData("tbxSelectionCount", tbxSelectionCount.getValue());
            mapLayer.setData("tbxRange", tbxRange.getValue());
            mapLayer.setData("tbxDomain", tbxDomain.getValue());
            mapLayer.setData("tbxMissingCount", tbxMissingCount.getValue());

            mapLayer.setData("scatterplotButtons", new Boolean(scatterplotButtons.isVisible()));
            mapLayer.setData("scatterplotDownloads", new Boolean(scatterplotDownloads.isVisible()));
            mapLayer.setData("envLegend", new Boolean(envLegend.isVisible()));

            mapLayer.setData("chkSelectMissingRecords", new Boolean(chkSelectMissingRecords.isChecked()));
            mapLayer.setData("chkRestrictOccurrencesToActiveArea", new Boolean(chkRestrictOccurrencesToActiveArea.isChecked()));
            mapLayer.setData("chkHighlightActiveAreaOccurrences", new Boolean(chkHighlightActiveAreaOccurrences.isChecked()));
            mapLayer.setData("chkShowEnvIntersection", new Boolean(chkShowEnvIntersection.isChecked()));

            mapLayer.setData("imagePath", imagePath);
            mapLayer.setData("missing_data", new Boolean(missing_data));

            mapLayer.setData("prevResampleData", prevResampleData);
            mapLayer.setData("prevResampleHighlight", prevResampleHighlight);
            mapLayer.setData("prevResampleLayers", prevResampleLayers);
            mapLayer.setData("prevResampleBackgroundData", prevResampleBackgroundData);
            mapLayer.setData("prevResampleBackgroundLayers", prevResampleBackgroundLayers);
        }
    }

    private Facet getFacetIn() {
        String fq = null;
        String e1 = CommonData.getLayerFacetName(data.getLayer1());
        String e2 = CommonData.getLayerFacetName(data.getLayer2());

         if (chkSelectMissingRecords.isChecked() && prevSelection == null) {
             fq = "-(" + e1 + ":[* TO *] AND " + e2 + ":[* TO *])";
         } else if(prevSelection != null) {
            double x1 = prevSelection[0];
            double x2 = prevSelection[1];
            double y1 = prevSelection[2];
            double y2 = prevSelection[3];

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
        if (chkSelectMissingRecords.isChecked() && prevSelection == null) {
             fq = e1 + ":[* TO *] AND " + e2 + ":[* TO *]";
        } else if(prevSelection !=  null) {
            double x1 = prevSelection[0];
            double x2 = prevSelection[1];
            double y1 = prevSelection[2];
            double y2 = prevSelection[3];

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

    private int findInArray(String lookFor, String[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(lookFor)) {
                return i;
            }
        }
        return -1;
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

    private void updateCbHighlightArea() {
        for(int i=cbHighlightArea.getItemCount()-1;i>=0;i--) {
            cbHighlightArea.removeItemAt(i);
        }

        boolean selectionSuccessful = false;
        for(MapLayer ml : getMapComposer().getPolygonLayers()) {
            Comboitem ci = new Comboitem(ml.getDisplayName());
            ci.setValue(ml);
            ci.setParent(cbHighlightArea);
            if(data != null && data.getHighlightSa() != null
                    && data.getHighlightSa().getMapLayer() == ml) {
                cbHighlightArea.setSelectedItem(ci);
                selectionSuccessful = true;
            }
        }

        //this may be a deleted layer or current view or au or world
        if(!selectionSuccessful && data != null
                && data.getHighlightSa() != null) {            
            MapLayer ml = data.getHighlightSa().getMapLayer();
            if(ml != null) {
                Comboitem ci = new Comboitem(ml.getDisplayName());
                ci.setValue(ml);
                ci.setParent(cbHighlightArea);
                cbHighlightArea.setSelectedItem(ci);
            } else {
                String name = "Previous area";
                if(data.getHighlightSa().getWkt() != null) {
                    if(data.getHighlightSa().getWkt().equals(CommonData.AUSTRALIA_WKT)) {
                        name = "Australia";
                    } else if(data.getHighlightSa().getWkt().equals(CommonData.WORLD_WKT)) {
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

    public void onChange$cbHighlightArea(Event event) {
        if(cbHighlightArea.getSelectedItem() != null) {
            if(cbHighlightArea.getSelectedItem().getValue() instanceof MapLayer) {
                MapLayer ml = ((MapLayer)cbHighlightArea.getSelectedItem().getValue());
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
        imagePath = null;
        redraw();
    }

    public void onClick$bClearHighlightArea(Event event) {
        cbHighlightArea.setSelectedIndex(-1);
        onChange$cbHighlightArea(null);
    }
}
