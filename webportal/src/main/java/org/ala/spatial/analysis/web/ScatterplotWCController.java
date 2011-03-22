/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import org.apache.commons.lang.StringUtils;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
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
import org.jfree.data.xy.DefaultXYDataset;
import org.zkoss.image.AImage;
import org.zkoss.zul.Image;
import org.zkoss.zul.Textbox;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.List;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.ScatterplotData;
import org.ala.spatial.util.UserData;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.ui.RectangleEdge;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;

/**
 *
 * @author Adam
 */
public class ScatterplotWCController extends UtilityComposer {

    private SettingsSupplementary settingsSupplementary = null;
    String satServer;
    Chart chart;
    Div chartImg;
    SpeciesAutoComplete sac;
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
    DefaultXYDataset xyDataset;
    int selectionCount;
    String imagePath;
    String results;
    int missingCount;

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        layersUtil = new LayersUtil(getMapComposer(), satServer);

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

        cleanTaxon();
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
            Events.echoEvent("doSpeciesChange",this, null);
        } else {
            Events.echoEvent("updateScatterplot",this, null);
        }
    }

    public void doSpeciesChange(Event event) {
        getMapComposer().activateLayerForScatterplot(getScatterplotData(), "species");

        Events.echoEvent("updateScatterplot",this, null);
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

        updateScatterplot(null);
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

        updateScatterplot(null);
    }

    public ScatterplotData getScatterplotData() {
        if (data == null) {
            data = new ScatterplotData();
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

            /*
            //allow offset fix from onChange msg
            int x1off = 56;            
            int y1off = 30;
            int x2off = 500 - 56 - 4;
            int y2off = 300 - 30 - 56;
            if(coordsDbl.length > 7) {
            x1off = (int) coordsDbl[4];
            y1off = (int) coordsDbl[5];
            x2off = (int) coordsDbl[6];
            y2off = (int) coordsDbl[7];
            }

            //common values
            double domainRange = plot.getDomainAxis().getUpperBound() - plot.getDomainAxis().getLowerBound();
            double domainMax = plot.getDomainAxis().getUpperBound();
            double rangeRange = plot.getRangeAxis().getUpperBound() - plot.getRangeAxis().getLowerBound();
            double rangeMin = plot.getRangeAxis().getLowerBound();

            //scale coords into axis values
            double x1 = (coordsDbl[0] - x1off) / (double) (x2off - x1off) * rangeRange + rangeMin;
            double x2 = (coordsDbl[2] + coordsDbl[0] - x1off) / (double) (x2off - x1off) * rangeRange + rangeMin;
            //y is desc order scale
            double y1 = domainMax - (coordsDbl[1] - y1off) / (double) (y2off - y1off) * domainRange;
            double y2 = domainMax - (coordsDbl[3] + coordsDbl[3] - y1off) / (double) (y2off - y1off) * domainRange;
             *
             */

            //chart area is wrong, but better than the above
            double tx1 = plot.getRangeAxis().java2DToValue(coordsDbl[0], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM);
            double tx2 = plot.getRangeAxis().java2DToValue(coordsDbl[2], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM);
            double ty1 = plot.getDomainAxis().java2DToValue(coordsDbl[1], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.LEFT);
            double ty2 = plot.getDomainAxis().java2DToValue(coordsDbl[3], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.LEFT);
            double x1 = Math.min(tx1, tx2);
            double x2 = Math.max(tx1, tx2);
            double y1 = Math.min(ty1, ty2);
            double y2 = Math.max(ty1, ty2);


            registerScatterPlotSelection(x1, x2, y1, y2);

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

            redraw();
        } catch (Exception e) {
            e.printStackTrace();
            clearSelection();
            getMapComposer().applyChange();
        }
    }

    public void updateScatterplot(Event event) {
        try {
            clearSelection();

            getScatterplotData();

            if (data.getLsid() != null && data.getLsid().length() > 0
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(satServer + "/alaspatial/ws/sampling/scatterplot?");
                sbProcessUrl.append("taxonid=" + URLEncoder.encode(data.getLsid().replace(".", "__"), "UTF-8"));
                String sbenvsel = data.getLayer1() + ":" + data.getLayer2();
                sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel, "UTF-8"));

                HttpClient client = new HttpClient();
                PostMethod get = new PostMethod(sbProcessUrl.toString());
                get.addRequestHeader("Accept", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();
                results = slist;

                String[] lines = slist.split("\n");

                xyDataset = new DefaultXYDataset();
                double[][] dblTmp = new double[2][lines.length - 1];
                int pos = 0;
                for (int i = 1; i < lines.length; i++) {   //skip header
                    String[] words = lines[i].split(",");

                    try {
                        if(words.length > 2) {
                            dblTmp[1][pos] = Double.parseDouble(words[words.length-1]);
                            dblTmp[0][pos] = Double.parseDouble(words[words.length-2]);
                            pos++;
                        }
                    } catch (Exception e) {
                    }                   
                }
                missingCount = lines.length - 1 - pos;
                double[][] dbl = {{0.0},{0.0}};
                if(pos > 0) {
                    dbl = new double[2][pos];
                    for(int i=0;i<pos;i++) {
                        dbl[0][i] = dblTmp[0][i];
                        dbl[1][i] = dblTmp[1][i];
                    }
                    xyDataset.addSeries("lsid", dbl);
                } 
                xyDataset.addSeries("lsid", dbl);
                annotation = null;

                redraw();
            }
        } catch (Exception e) {
            e.printStackTrace();
            clearSelection();
            getMapComposer().applyChange();
        }
    }

    void redraw() {
        getScatterplotData();

        if (data.getLsid() != null && data.getLsid().length() > 0
                && data.getLayer1() != null && data.getLayer1().length() > 0
                && data.getLayer2() != null && data.getLayer2().length() > 0) {
            try {
                jChart = ChartFactory.createScatterPlot(data.getSpeciesName(), data.getLayer1Name(), data.getLayer2Name(), xyDataset, PlotOrientation.HORIZONTAL, false, false, false);
                jChart.setBackgroundPaint(Color.white);
                plot = (XYPlot) jChart.getPlot();
                if (annotation != null) {
                    plot.addAnnotation(annotation);
                }
                plot.setForegroundAlpha(0.5f);
                Font axisfont = new Font("Arial", Font.PLAIN, 10);
                Font titlefont = new Font("Arial", Font.BOLD, 11);
                plot.getDomainAxis().setLabelFont(axisfont);
                plot.getDomainAxis().setTickLabelFont(axisfont);
                plot.getRangeAxis().setLabelFont(axisfont);
                plot.getRangeAxis().setTickLabelFont(axisfont);
                jChart.getTitle().setFont(titlefont);

                chartRenderingInfo = new ChartRenderingInfo();

                int width = Integer.parseInt(this.getWidth().replace("px", "")) - 20;
                int height = Integer.parseInt(this.getHeight().replace("px", "")) - Integer.parseInt(tbxChartSelection.getHeight().replace("px", ""));
                if(height > width) {
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

                chartImg.setWidth(width + "px");
                chartImg.setHeight(height + "px");
                String script = "updateScatterplot(" + width + "," + height + ",'url(" + htmlurl + uid + ".png)')";
                Clients.evalJavaScript(script);
                chartImg.setVisible(true);
                scatterplotDownloads.setVisible(true);

                if(missingCount > 0) {
                    tbxMissingCount.setValue("Records with missing values: " + missingCount);
                } else {
                    tbxMissingCount.setValue("");
                }
            } catch (Exception e) {
                e.printStackTrace();
                clearSelection();
                getMapComposer().applyChange();
            }
        } else {
            tbxMissingCount.setValue("");
        }
    }

    private void registerScatterPlotSelection(double x1, double x2, double y1, double y2) {
        try {
            if (data.getLsid() != null && data.getLsid().length() > 0
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(satServer).append("/alaspatial/ws/sampling/scatterplot/register?");
                sbProcessUrl.append("lsid=").append(URLEncoder.encode(data.getLsid().replace(".", "__"), "UTF-8"));
                //String sbenvsel = data.getLayer1() + ":" + data.getLayer2();
                sbProcessUrl.append("&param1=double,").append(URLEncoder.encode(data.getLayer1(), "UTF-8")).append(",").append(y1).append(",").append(y2);
                sbProcessUrl.append("&param2=double,").append(URLEncoder.encode(data.getLayer2(), "UTF-8")).append(",").append(x1).append(",").append(x2);
                /*
                if (data.getPid() != null && data.getPid().length() > 0) {
                sbProcessUrl.append("&pid=").append(URLEncoder.encode(data.getPid(), "UTF-8"));
                }
                sbProcessUrl.append("&bounds=").append(y1).append(",").append(y2).append(",").append(x1).append(",").append(x2);*/

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
            getMapComposer().applyChange();
        }
    }

    void updateCount(String txt) {
        selectionCount = Integer.parseInt(txt);
        tbxSelectionCount.setValue("Records selected: " + txt);

        try {
            scatterplotButtons.setVisible(true);
        } catch (Exception e) {
        }
    }

    void clearSelection() {
        tbxSelectionCount.setValue("");
        tbxRange.setValue("");
        tbxDomain.setValue("");

        getScatterplotData().setEnabled(false);

        chartImg.setVisible(false);
        scatterplotDownloads.setVisible(false);

        mapLayer = null;

        getMapComposer().getOpenLayersJavascript().execute("clearSelection()");

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
    private String cleanTaxon() {
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
        addUserLayer(id, "Scatterplot Selected " + data.getSpeciesName(), "from scatterplot in group", selectionCount);
    }

    public void onClick$addUnSelectedRecords(Event event) {
        String id = addSelectedRecords(true);
        addUserLayer(id, "Scatterplot Unselected " + data.getSpeciesName(), "from scatterplot out group", results.split("\n").length - selectionCount - 1);   //-1 for header
    }

    void addUserLayer(String id, String layername, String description, int numRecords) {
        layername = StringUtils.capitalize(layername);

        getMapComposer().mapSpeciesByLsid(id, layername);

        UserData ud = new UserData(layername, description, "scatterplot");
        ud.setFeatureCount(numRecords);

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
        ScatterplotResults window = (ScatterplotResults) Executions.createComponents("WEB-INF/zul/AnalysisScatterplotResults.zul", this, null);
        window.populateList(data.getLayer1Name(), data.getLayer2Name(), results);
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Filedownload.save(results, "text/plain", data.getSpeciesName() + "_" + data.getLayer1Name() + "_" + data.getLayer2Name() + ".csv");
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
            GetMethod get = new GetMethod(satServer + "/alaspatial/" + sbProcessUrl.toString());

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
        mapLayer = getMapComposer().activateLayerForScatterplot(getScatterplotData(), "species");
    }
}
