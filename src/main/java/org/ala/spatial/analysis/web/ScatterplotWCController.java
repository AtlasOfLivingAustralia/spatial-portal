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
import java.util.List;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.ScatterplotData;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jfree.ui.RectangleEdge;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;

/**
 *
 * @author Adam
 */
public class ScatterplotWCController extends UtilityComposer {

    private SettingsSupplementary settingsSupplementary = null;
    String satServer;
    Chart chart;
    Image chartImg;
    SpeciesAutoComplete sac;
    EnvLayersCombobox cbLayer1;
    EnvLayersCombobox cbLayer2;
    Textbox tbxChartSelection;
    Label tbxSelectionCount;
    Label tbxRange;
    Label tbxDomain;
    ScatterplotData data;
    JFreeChart jChart;
    XYPlot plot;
    ChartRenderingInfo chartRenderingInfo;
    LayersUtil layersUtil;
    Button addSelectedRecords;
    Button addUnSelectedRecords;
    MapLayer mapLayer = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        layersUtil = new LayersUtil(getMapComposer(), satServer);
    }

    public void onChange$sac(Event event) {
        getScatterplotData();

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
        if(event != null) {
            getMapComposer().activateLayerForScatterplot(getScatterplotData(), "species");
        }

        updateScatterplot();
    }

    public void onChange$cbLayer1(Event event) {
        getScatterplotData();

        if (cbLayer1.getItemCount() > 0 && cbLayer1.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) cbLayer1.getSelectedItem().getValue();
            data.setLayer1(jo.getString("name"));
            data.setLayer1Name(cbLayer1.getText());
        }

        updateScatterplot();
    }

    public void onChange$cbLayer2(Event event) {
        getScatterplotData();

        if (cbLayer2.getItemCount() > 0 && cbLayer2.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) cbLayer2.getSelectedItem().getValue();
            data.setLayer2(jo.getString("name"));
            data.setLayer2Name(cbLayer2.getText());
        }

        updateScatterplot();
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

        updateScatterplot();

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
            double x1 = plot.getRangeAxis().java2DToValue(coordsDbl[0], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM);
            double x2 = plot.getRangeAxis().java2DToValue(coordsDbl[2] + coordsDbl[0], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.BOTTOM);
            double y1 = plot.getDomainAxis().java2DToValue(coordsDbl[1], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.LEFT);
            double y2 = plot.getDomainAxis().java2DToValue(coordsDbl[3] + coordsDbl[1], chartRenderingInfo.getPlotInfo().getDataArea(), RectangleEdge.LEFT);

            registerScatterPlotSelection(x1, x2, y1, y2);

            ScatterplotData d = getScatterplotData();
            d.setSelection(new Rectangle2D.Double(x1, y1, x2, y2));
            d.setEnabled(true);

            //refresh mapLayer
            refreshMapLayer();

            mapLayer.setHighlight(data.getPid());

            getMapComposer().applyChange(mapLayer);

            tbxChartSelection.setText("");
            tbxDomain.setValue(String.format("%s: %g - %g", data.getLayer1Name(), x1, x2));
            tbxRange.setValue(String.format("%s: %g - %g", data.getLayer2Name(), y2, y1));
        } catch (Exception e) {
            e.printStackTrace();
            clearSelection();
            getMapComposer().applyChange();
        }
    }

    private void updateScatterplot() {
        try {
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

                String[] lines = slist.split("\n");

                DefaultXYDataset xyDataset = new DefaultXYDataset();
                //XYModel xymodel = new SimpleXYModel();
                double[][] dbl = new double[2][lines.length - 1];
                for (int i = 1; i < lines.length; i++) {   //skip header
                    String[] words = lines[i].split(",");

                    try {
                        dbl[0][i - 1] = Double.parseDouble(words[2]);
                        dbl[1][i - 1] = Double.parseDouble(words[3]);
                        //      xymodel.addValue(sac.getText(), Double.parseDouble(words[2]), Double.parseDouble(words[3]));
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
                xyDataset.addSeries("lsid", dbl);

                jChart = ChartFactory.createScatterPlot(data.getSpeciesName(), data.getLayer1Name(), data.getLayer1Name(), xyDataset, PlotOrientation.HORIZONTAL, false, false, false);
                plot = (XYPlot) jChart.getPlot();
                plot.setForegroundAlpha(0.5f);
                chartRenderingInfo = new ChartRenderingInfo();

                int width = Integer.parseInt(chartImg.getWidth().replace("px", ""));
                int height = Integer.parseInt(chartImg.getHeight().replace("px", ""));
                BufferedImage bi = jChart.createBufferedImage(width, height, BufferedImage.TRANSLUCENT, chartRenderingInfo);
                byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);

                AImage image = new AImage("scatterplot", bytes);
                chartImg.setContent(image);
                chartImg.setVisible(true);

                //chart.setModel((ChartModel) xymodel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            clearSelection();
            getMapComposer().applyChange();
        }
    }

    private void registerScatterPlotSelection(double x1, double x2, double y1, double y2) {
        try {
            if (data.getLsid() != null && data.getLsid().length() > 0
                    && data.getLayer1() != null && data.getLayer1().length() > 0
                    && data.getLayer2() != null && data.getLayer2().length() > 0) {
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(satServer).append("/alaspatial/ws/sampling/scatterplot/register?");
                sbProcessUrl.append("taxonid=").append(URLEncoder.encode(data.getLsid().replace(".", "__"), "UTF-8"));
                String sbenvsel = data.getLayer1() + ":" + data.getLayer2();
                sbProcessUrl.append("&envlist=").append(URLEncoder.encode(sbenvsel, "UTF-8"));
                if (data.getPid() != null && data.getPid().length() > 0) {
                    sbProcessUrl.append("&pid=").append(URLEncoder.encode(data.getPid(), "UTF-8"));
                }
                sbProcessUrl.append("&bounds=").append(y1).append(",").append(y2).append(",").append(x1).append(",").append(x2);

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
        tbxSelectionCount.setValue("Records selected: " + txt);

        try {
            if (Integer.parseInt(txt) > 0) {
                addSelectedRecords.setVisible(true);
                addUnSelectedRecords.setVisible(true);
            }
        } catch (Exception e) {
        }
    }

    void clearSelection() {
        tbxSelectionCount.setValue("");
        tbxRange.setValue("");
        tbxDomain.setValue("");

        getScatterplotData().setEnabled(false);

        chartImg.setVisible(false);

        mapLayer = null;

        getMapComposer().getOpenLayersJavascript().execute("clearSelection()");

        addUnSelectedRecords.setVisible(false);
        addSelectedRecords.setVisible(false);
    }

    /**
     * populate sampling screen with values from active layers and area tab
     */
    public void callPullFromActiveLayers() {
        //get top species and list of env/ctx layers
        if (sac.getSelectedItem() == null) {
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
        String id = addSelectedRecords(true);
        getMapComposer().mapSpeciesByLsid(id, "Scatterplot Selected " + data.getSpeciesName());
    }

    public void onClick$addUnSelectedRecords(Event event) {
        String id = addSelectedRecords(false);
        getMapComposer().mapSpeciesByLsid(id, "Scatterplot Unselected " + data.getSpeciesName());
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
