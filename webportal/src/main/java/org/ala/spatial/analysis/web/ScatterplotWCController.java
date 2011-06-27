/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.LayerLegendComposer;
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
import org.zkoss.zul.Chart;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
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
import java.io.FilterWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.ScatterplotData;
import org.ala.spatial.util.UserData;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
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
    MapLayer mapLayer = null;
    private XYBoxAnnotation annotation;
    DefaultXYZDataset xyzDataset;
    DefaultXYZDataset aaDataset;
    int selectionCount;
    String imagePath;
    String results;
    int missingCount;
    Checkbox chkSelectMissingRecords;
    double[] prevSelection = null;
    Checkbox chkRestrictOccurrencesToActiveArea;
    Checkbox chkHighlightActiveAreaOccurrences;
    Checkbox chkShowEnvIntersection;
    double zmin, zmax;
    int[] seriesColours;
    String[] seriesNames;
    private DefaultXYZDataset backgroundXyzDataset;
    //String backgroundLSID;
    //private String backgroundName;
    Div envLegend;
    Boolean missing_data = false;
    Label lblMissing;
    Button addNewLayers;

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
        if (data.getLsid() != null) {
            getMapComposer().removeLayerHighlight(data, "species");
        }

        data.setLsid(null);
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

        data.setLsid((String) sac.getSelectedItem().getAnnotatedProperties().get(0));
        data.setSpeciesName(taxon);

        //only add it to the map if this was signaled from an event
        if (event != null) {
            Events.echoEvent("doSpeciesChange", this, null);
        } else {
            Events.echoEvent("updateScatterplot", this, null);
        }
    }

    public void onChange$sacBackground(Event event) {
        data.setBackgroundLsid(null);

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

        data.setBackgroundLsid((String) sacBackground.getSelectedItem().getAnnotatedProperties().get(0));

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
        if (data.getLsid() != null) {
            getMapComposer().removeLayerHighlight(data, "species");
        }

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
        if (data.getLsid() != null) {
            getMapComposer().removeLayerHighlight(data, "species");
        }

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
        d.setLsid(lsid);
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

            mapLayer.setHighlight(data.getPid());

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

        if (data != null && data.getLsid() != null && data.getLsid().length() > 0
                && data.getLayer1() != null && data.getLayer1().length() > 0
                && data.getLayer2() != null && data.getLayer2().length() > 0
                && xyzDataset != null) {
            if (missing_data) {
                lblMissing.setVisible(true);
            } else {
                lblMissing.setVisible(false);
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
                        if (data.getHighlightWkt() != null && aaDataset != null) {
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

                        //active area must be drawn first
                        if (data.getHighlightWkt() != null && aaDataset != null) {
                            plot.setRenderer(getActiveAreaRenderer());

                            int datasetCount = plot.getDatasetCount();
                            plot.setDataset(datasetCount, xyzDataset);
                            plot.setRenderer(datasetCount, getRenderer(zmin, zmax, seriesColours, seriesNames));
                        } else {
                            plot.setRenderer(getRenderer(zmin, zmax, seriesColours, seriesNames));
                        }

                        //add points background
                        if (data.getBackgroundLsid() != null) {
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

                        if (missingCount > 0) {
                            tbxMissingCount.setValue("(" + missingCount + ")");
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

            if (data.getLsid() != null && data.getLsid().length() > 0
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(CommonData.satServer).append("/alaspatial/ws/sampling/scatterplot/register?");
                sbProcessUrl.append("lsid=").append(URLEncoder.encode(data.getLsid().replace(".", "__"), "UTF-8"));

                int pos = 0;
                if (prevSelection != null) {
                    sbProcessUrl.append("&param").append(pos).append("=and,double,").append(URLEncoder.encode(data.getLayer1(), "UTF-8")).append(",").append(y1).append(",").append(y2);
                    pos++;
                    sbProcessUrl.append("&param").append(pos).append("=and,double,").append(URLEncoder.encode(data.getLayer2(), "UTF-8")).append(",").append(x1).append(",").append(x2);
                    pos++;
                }

                if (chkSelectMissingRecords.isChecked()) {
                    sbProcessUrl.append("&param").append(pos).append("=or,double,").append(URLEncoder.encode(data.getLayer1(), "UTF-8")).append(",").append("NaN").append(",").append("NaN");
                    pos++;
                    sbProcessUrl.append("&param").append(pos).append("=or,double,").append(URLEncoder.encode(data.getLayer2(), "UTF-8")).append(",").append("NaN").append(",").append("NaN");
                    pos++;
                }

                System.out.println(sbProcessUrl.toString());

                HttpClient client = new HttpClient();
                PostMethod get = new PostMethod(sbProcessUrl.toString());
                get.addRequestHeader("Accept", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();
                String[] alist = slist.split("\n");
                data.setPid(alist[0]);

                updateCount(alist[1]);

                System.out.println("selection pid:" + alist[0] + " count:" + alist[1]);
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
            String speciesandlsid = layersUtil.getFirstSpeciesLsidLayer();
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
        String id = addSelectedRecords(false);
        addUserLayer(id, "IN " + data.getSpeciesName(), "from scatterplot in group", selectionCount);
    }

    public void onClick$addUnSelectedRecords(Event event) {
        String id = addSelectedRecords(true);
        addUserLayer(id, "OUT " + data.getSpeciesName(), "from scatterplot out group", results.split("\n").length - selectionCount - 1);   //-1 for header
    }

    void addUserLayer(String id, String layername, String description, int numRecords) {
        layername = StringUtils.capitalize(layername);

        getMapComposer().mapSpeciesByLsid(id, layername, LayerUtilities.SPECIES);

        UserData ud = new UserData(layername, description, "scatterplot");
        ud.setFeatureCount(numRecords);
        ud.setLsid(id);
        ud.setMetadata(id + ", " + layername + ", " + description);
        ud.setSubType(LayerUtilities.SPECIES);

        // add it to the user session
        Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
        if (htUserSpecies == null) {
            htUserSpecies = new Hashtable<String, UserData>();
        }
        htUserSpecies.put(id, ud);
        getMapComposer().getSession().setAttribute("userpoints", htUserSpecies);
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

        Filedownload.save(results, "text/plain", data.getSpeciesName() + "_" + data.getLayer1Name() + "_" + data.getLayer2Name() + ".csv");
    }

    String addSelectedRecords(boolean include) {
        //register with alaspatial using data.getPid();
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("species/highlight/register");
            sbProcessUrl.append("?lsid=").append(URLEncoder.encode(data.getLsid().replace(".", "__"), "UTF-8"));
            sbProcessUrl.append("&pid=").append(data.getPid());
            if (include) {
                sbProcessUrl.append("&include=1");
            }

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString());

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        //envLegend.setVisible(chkShowEnvIntersection.isChecked());
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

            mapLayer.setHighlight(data.getPid());

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
                sbProcessUrl.append(CommonData.satServer).append("/alaspatial/ws/sampling/chart?basis=layer&filter=lsid:none;area:none");

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
            //        LookupPaintScale ramp = new LookupPaintScale();
            //        for(int i=0;i<=10;i++) {
            //            //yellow - red
            //            int colour = 0xFFFFFF00 - (0x0000FF00 & (i * 0x0000FF00 / 25));
            //            ramp.add(i/10.0,new Color(colour));
            //        }
            GrayPaintScale ramp = new GrayPaintScale(Math.log10(min + 1), Math.log10(max + 1));
            xyBlockRenderer.setPaintScale(ramp);
            xyBlockRenderer.setBlockHeight(ydiv);   //backwards
            xyBlockRenderer.setBlockWidth(xdiv);    //backwards
            xyBlockRenderer.setBlockAnchor(RectangleAnchor.BOTTOM_LEFT);
            //XYPlot plot = new XYPlot(defaultXYZDataset,x,y, xyBlockRenderer);
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

    private HashMap<String, Integer> getSeriesSummary(String[] series, double[] seriesDbl) {
        HashMap<String, Integer> ts = new HashMap<String, Integer>();

        int pos = series.length;
        int count = 0;
        for (int i = 0; i < pos; i++) {
            count++;
            if (i == pos - 1 || !series[i].equals(series[i + 1])) {
                String key = series[i];
                if (!Double.isNaN(seriesDbl[i])) {
                    key = NUMBER_SERIES;
                }
                Integer c = ts.get(key);
                if (c == null) {
                    c = new Integer(0);
                }
                c += count;
                ts.put(key, c);

                count = 0;
            }
        }

        return ts;
    }

    private double[] convertStringsToDoubles(String[] series) {
        double[] seriesDbl = new double[series.length];
        zmin = Double.MAX_VALUE;
        zmax = -1 * Double.MAX_VALUE;
        for (int i = 0; i < series.length; i++) {
            try {
                if (series[i] == null || series[i].length() == 0 || series[i].equals("In Active Area")) {
                    seriesDbl[i] = Double.NaN;
                } else {
                    seriesDbl[i] = Double.parseDouble(series[i]);
                    series[i] = NUMBER_SERIES;
                    if (seriesDbl[i] < zmin) {
                        zmin = seriesDbl[i];
                    }
                    if (seriesDbl[i] > zmax) {
                        zmax = seriesDbl[i];
                    }
                }
            } catch (Exception e) {
                seriesDbl[i] = Double.NaN;
            }
        }
        return seriesDbl;
    }

    private DefaultXYZDataset createDataset(int pos, double[][] dblTmp, HashMap<String, Integer> ts, String[] series, double[] seriesDbl, String[] seriesNames) {
        DefaultXYZDataset xyzDataset = new DefaultXYZDataset();
        double[][] dbl = {{0.0}, {0.0}, {0.0}};
        missing_data = false;
        if (dblTmp != null && dblTmp.length > 0 && dblTmp[0].length > 0
                && series != null && series.length > 0) {
            //add series
            if (ts.size() > 1) {
                if (seriesNames == null) {
                    seriesNames = new String[ts.size()];
                    ts.keySet().toArray(seriesNames);
                }
                for (int j = 0; j <= seriesNames.length; j++) {
                    String key;
                    if (j < seriesNames.length) {
                        key = seriesNames[j];
                    } else if (ts.containsKey(ACTIVE_AREA_SERIES) && ts.size() > seriesNames.length) {
                        key = ACTIVE_AREA_SERIES;
                    } else {
                        continue;
                    }
                    int size = ts.get(key);
                    dbl = new double[3][size];
                    int p = 0;
                    for (int i = 0; i < pos; i++) {
                        if (key.equals(series[i])) {
                            dbl[0][p] = dblTmp[0][i];
                            dbl[1][p] = dblTmp[1][i];
                            dbl[2][p] = seriesDbl[i];
                            p++;
                        }
                    }
                    if (key.length() == 0) {
                        xyzDataset.addSeries("unknown", dbl);
                    } else if (key.equalsIgnoreCase(ACTIVE_AREA_SERIES)) {
                        aaDataset = new DefaultXYZDataset();
                        aaDataset.addSeries(key, dbl);
                    } else {
                        xyzDataset.addSeries(key, dbl);
                    }
                }
            } else {
                dbl = new double[3][pos];
                for (int i = 0; i < pos; i++) {
                    dbl[0][i] = dblTmp[0][i];
                    dbl[1][i] = dblTmp[1][i];
                    dbl[2][i] = seriesDbl[i];
                }
                xyzDataset.addSeries(series[0], dbl);
            }
        } else {
            xyzDataset.addSeries("lsid", dbl);
            missing_data = true;
        }

        return xyzDataset;
    }

    private XYShapeRenderer getRenderer(double min, double max, int[] datasetColours, String[] seriesNames) {
        class MyXYShapeRenderer extends XYShapeRenderer {

            public int alpha = 255;
            public Paint[] datasetColours;
            public int shapeSize = 8;
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
                if (datasetColours != null && datasetColours.length > series && datasetColours[series] != null) {
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

        if (!(min < max)) {
            min = 0;
            max = 1;
        }

        LookupPaintScale paint = new LookupPaintScale(min, max, new Color(red, green, blue, alpha)) {

            @Override
            public Paint getPaint(double value) {
                if (Double.isNaN(value)) {
                    return this.getDefaultPaint();
                }

                //yellow - red
                return new Color(255, 255 - (int) (((value - getLowerBound()) * 255) / (getUpperBound() - getLowerBound())), 0, (int) (0x000000FF * data.opacity / 100));
            }
        };

        renderer.setPaintScale(paint);

        return renderer;
    }
    LayerLegendComposer layerWindow = null;

    public void onClick$btnEditAppearance1(Event event) {
        getScatterplotData();

        if (data.getLsid() != null && data.getLsid().length() > 0) {
            //preview species list
            layerWindow = (LayerLegendComposer) Executions.createComponents("WEB-INF/zul/LayerLegend.zul", this, null);
            EventListener el = new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    updateFromLegend();
                }
            };
            layerWindow.init(data.getLsid(), data.red, data.green, data.blue, data.size, data.opacity, data.colourMode, el);

            try {
                layerWindow.doModal();
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
                layerWindow.getSize(),
                layerWindow.getColourMode());
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

    String registerPointsColourModeLegend(String speciesLsid, String colourmode) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.satServer + "/alaspatial/species/colourlegend?lsid="
                    + URLEncoder.encode(speciesLsid.replace(".", "__"), "UTF-8")
                    + "&colourmode="
                    + URLEncoder.encode(colourmode, "UTF-8")); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");


            int result = client.executeMethod(get);

            if (result == 200) {
                String slist = get.getResponseBodyAsString();
                return slist;
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }

        return null;
    }
    String prevLegendColours = null;

    void buildSeriesColours(HashMap<String, Integer> ts, String lsid, String colourMode) {
        try {
            String thisLegendColours = lsid + "*" + colourMode;
            if (prevLegendColours == null || !prevLegendColours.equals(thisLegendColours)) {
                prevLegendColours = thisLegendColours;

                //reset
                seriesColours = null;
                seriesNames = null;

                String pid = registerPointsColourModeLegend(lsid, colourMode);

                if (pid == null) {
                    return;
                }

                // call get
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/layer/get?");
                sbProcessUrl.append("pid=" + URLEncoder.encode(pid, "UTF-8"));

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(sbProcessUrl.toString());

                get.addRequestHeader("Accept", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                // retrieve legend file
                String[] slista = slist.split("\n");

                client = new HttpClient();
                get = new GetMethod(CommonData.satServer + "/alaspatial/" + slista[1]);
                get.addRequestHeader("Accept", "text/plain");
                result = client.executeMethod(get);
                slist = get.getResponseBodyAsString();

                String[] lines = slist.split("\r\n");
                if (lines.length == 1) {
                    lines = slist.split("\n");
                }

                ts = (HashMap<String, Integer>) ts.clone();
                if (ts.containsKey(ACTIVE_AREA_SERIES)) {
                    ts.remove(ACTIVE_AREA_SERIES);
                }

                int[] colours = new int[ts.size()];
                String[] keys = new String[ts.size()];
                ts.keySet().toArray(keys);

                int i = 0;
                for (i = 1; i < lines.length; i++) {
                    if (lines[i].split(",").length > 3) {
                        String[] ss = lines[i].split(",");
                        int red = Integer.parseInt(ss[1]);
                        int green = Integer.parseInt(ss[2]);
                        int blue = Integer.parseInt(ss[3]);

                        for (int j = 0; j < keys.length; j++) {
                            if (keys[j].equalsIgnoreCase(ss[0])) {
                                colours[j] = (red << 16) | (green << 8) | (blue);
                                break;
                            }
                        }
                    }
                }
                seriesColours = colours;
                seriesNames = keys;
            }
        } catch (Exception e) {
            e.printStackTrace();
            seriesColours = null;
        }
    }
    String prevResample = null;

    private void resample() {
        try {
            if (data != null && data.getLsid() != null && data.getLsid().length() > 0
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {

                String thisResample = data.getLsid() + "*" + data.getLayer1() + "*" + data.getLayer2() + "*" + data.colourMode
                        + "*" + ((data.getHighlightWkt() != null) ? "Y" : "N")
                        + "*" + ((data.getFilterWkt() != null) ? "Y" : "N");

                if (prevResample == null || !prevResample.equals(thisResample)) {
                    prevResample = thisResample;

                    HttpClient client = new HttpClient();

                    StringBuffer sbProcessUrl = new StringBuffer();
                    sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/sampling/scatterplot?");
                    sbProcessUrl.append("taxonid=" + URLEncoder.encode(data.getLsid().replace(".", "__"), "UTF-8"));
                    String sbenvsel = data.getLayer1() + ":" + data.getLayer2();
                    sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel, "UTF-8"));

                    sbProcessUrl.append("&colourmode=").append(data.colourMode);

                    PostMethod get = new PostMethod(sbProcessUrl.toString());

                    if (data.getHighlightWkt() != null) {
                        get.addParameter("areahighlight", URLEncoder.encode(data.getHighlightWkt(), "UTF-8"));
                    }
                    
                    if (data.getFilterWkt() != null) {
                        //Now done in AddToolScatterplotComposer
                        //get.addParameter("arearestrict", URLEncoder.encode(data.getFilterWkt(), "UTF-8"));
                    }
                    
                    get.addRequestHeader("Accept", "text/plain");

                    int result = client.executeMethod(get);
                    String slist = get.getResponseBodyAsString();
                    results = slist;

                    String[] lines = slist.split("\n");

                    double[][] dblTmp = new double[2][lines.length - 1];
                    HashMap<String, Integer> ts = new HashMap<String, Integer>();
                    String[] series = new String[lines.length - 1];
                    int pos = 0;
                    for (int i = 1; i < lines.length; i++) {   //skip header
                        String[] words = lines[i].split(",");

                        try {
                            if (words.length > 2) {
                                dblTmp[1][pos] = Double.parseDouble(words[words.length - 1]);
                                dblTmp[0][pos] = Double.parseDouble(words[words.length - 2]);
                                series[pos] = words[1];
                                if (series[pos] == null) {
                                    series[pos] = "";
                                }
                                pos++;
                            }
                        } catch (Exception e) {
                        }
                    }

                    series = java.util.Arrays.copyOf(series, pos);
                    double[] seriesDbl = convertStringsToDoubles(series);
                    ts = getSeriesSummary(series, seriesDbl);

                    buildSeriesColours(ts, data.getLsid(), data.colourMode);

                    missingCount = lines.length - 1 - pos;

                    aaDataset = null;
                    xyzDataset = createDataset(pos, dblTmp, ts, series, seriesDbl, seriesNames);
                }
            } else {
                //no resample available
                xyzDataset = null;
                annotation = null;
                missingCount = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    String prevResampleBackground = null;

    private void resampleBackground() {
        try {
            if (data.getBackgroundLsid() != null && data.getBackgroundLsid().length() > 0
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {

                String thisResampleBackground = data.getBackgroundLsid() + "*" + data.getLayer1() + "*" + data.getLayer2();
                if (prevResampleBackground == null || !prevResampleBackground.equals(thisResampleBackground)) {
                    StringBuffer sbProcessUrl = new StringBuffer();
                    sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/sampling/scatterplot?");
                    sbProcessUrl.append("taxonid=" + URLEncoder.encode(data.getBackgroundLsid().replace(".", "__"), "UTF-8"));
                    String sbenvsel = data.getLayer1() + ":" + data.getLayer2();
                    sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel, "UTF-8"));

//                    if (data.getHighlightWkt() != null) {
//                        sbProcessUrl.append("&areahighlight=").append(URLEncoder.encode(data.getHighlightWkt(), "UTF-8"));
//                    }
                    if (data.getFilterWkt() != null) {
                        //Now done in AddToolScatterplotComposer
                        //sbProcessUrl.append("&arearestrict=").append(URLEncoder.encode(data.getFilterWkt(), "UTF-8"));
                    }

                    //sbProcessUrl.append("&colourmode=").append(data.colourMode);
                    sbProcessUrl.append("&colourmode=").append("-1");   //default

                    HttpClient client = new HttpClient();
                    PostMethod get = new PostMethod(sbProcessUrl.toString());
                    get.addRequestHeader("Accept", "text/plain");

                    int result = client.executeMethod(get);
                    String slist = get.getResponseBodyAsString();

                    String[] lines = slist.split("\n");

                    double[][] dblTmp = new double[2][lines.length - 1];
                    HashMap<String, Integer> ts = new HashMap<String, Integer>();
                    String[] series = new String[lines.length - 1];
                    int pos = 0;
                    for (int i = 1; i < lines.length; i++) {   //skip header
                        String[] words = lines[i].split(",");

                        try {
                            if (words.length > 2) {
                                dblTmp[1][pos] = Double.parseDouble(words[words.length - 1]);
                                dblTmp[0][pos] = Double.parseDouble(words[words.length - 2]);
                                series[pos] = words[1];
                                if (series[pos] == null) {
                                    series[pos] = "";
                                }
                                pos++;
                            }
                        } catch (Exception e) {
                        }
                    }

                    series = java.util.Arrays.copyOf(series, pos);
                    double[] seriesDbl = convertStringsToDoubles(series);
                    ts = getSeriesSummary(series, seriesDbl);

                    //buildSeriesColours(ts, backgroundLSID, data.colourMode);

                    //missingCount = lines.length - 1 - pos;
                    String[] snames = new String[ts.size()];
                    ts.keySet().toArray(snames);
                    backgroundXyzDataset = createDataset(pos, dblTmp, ts, series, seriesDbl, snames);

                    //annotation = null;
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
        data.setBackgroundLsid(null);

        sacBackground.setValue("");

        imagePath = null;
        redraw();
    }

    public void onClick$btnClearSelection(Event event) {
        clearSelection();
    }

    XYShapeRenderer getBackgroundRenderer() {
        XYShapeRenderer renderer = new XYShapeRenderer();
        LookupPaintScale paint = new LookupPaintScale(0, 1, new Color(255, 164, 96, 150)) {

            @Override
            public Paint getPaint(double value) {
                return this.getDefaultPaint();
            }
        };

        renderer.setPaintScale(paint);

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
        renderer.setOutlinePaint(new Color(255, 0, 0, 255));
        renderer.setDrawOutlines(true);

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
                missingCount = (Integer) mapLayer.getData("missingCount");
                prevSelection = (double[]) mapLayer.getData("prevSelection");
                zmin = (Double) mapLayer.getData("zmin");
                zmax = (Double) mapLayer.getData("zmax");
                seriesColours = (int[]) mapLayer.getData("seriesColours");
                seriesNames = (String[]) mapLayer.getData("seriesNames");
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

                if (missingCount > 0) {
                    tbxMissingCount.setValue("(" + missingCount + ")");
                    chkSelectMissingRecords.setVisible(true);
                } else {
                    tbxMissingCount.setValue("");
                    chkSelectMissingRecords.setVisible(false);
                }

                prevResample = (String) mapLayer.getData("prevResample");
                prevResampleBackground = (String) mapLayer.getData("prevResampleBackground");
                prevLegendColours = (String) mapLayer.getData("prevLegendColours");
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
            mapLayer.setData("missingCount", new Integer(missingCount));
            mapLayer.setData("prevSelection", prevSelection);
            mapLayer.setData("zmin", new Double(zmin));
            mapLayer.setData("zmax", new Double(zmax));
            mapLayer.setData("seriesColours", seriesColours);
            mapLayer.setData("seriesNames", seriesNames);
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

            mapLayer.setData("prevResample", prevResample);
            mapLayer.setData("prevResampleBackground", prevResampleBackground);
            mapLayer.setData("prevLegendColours", prevLegendColours);
        }
    }
}
