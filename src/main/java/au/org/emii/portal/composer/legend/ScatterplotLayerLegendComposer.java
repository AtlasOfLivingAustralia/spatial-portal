/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer.legend;

import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.UserDataQuery;
import au.org.ala.spatial.util.LegendMaker;
import au.org.emii.portal.composer.GenericAutowireAutoforwardComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.ala.layers.legend.QueryField;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.*;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adam
 */
public class ScatterplotLayerLegendComposer extends GenericAutowireAutoforwardComposer {

    private static Logger logger = Logger.getLogger(ScatterplotLayerLegendComposer.class);

    SettingsSupplementary settingsSupplementary = null;

    Slider opacitySlider;
    Label opacityLabel;
    Slider redSlider;
    Slider greenSlider;
    Slider blueSlider;
    Slider sizeSlider;
    Slider plotSizeSlider;
    Checkbox chkUncertaintySize;
    public Button btnPointsCluster;
    Label lblFupload;
    Label redLabel;
    Label greenLabel;
    Label blueLabel;
    Label sizeLabel;
    Label plotSizeLabel;
    Listbox activeLayersList;
    Div layerControls;
    Div uncertainty;
    Hbox uncertaintyLegend;
    Div colourChooser;
    Div sizeChooser;
    Image legendImg;
    Image legendImgUri;
    Div legendHtml;
    Label legendLabel;
    Div divUserColours;
    Combobox cbColour;
    Comboitem ciColourUser; //User selected colour
    Label layerName;
    EventListener listener;
    Query query;
    MapLayer mapLayer;

    @Override
    public void afterCompose() {
        super.afterCompose();

        cbColour.setSelectedIndex(0);
    }

    public void onScroll$opacitySlider(Event e) {
        float opacity = ((float) opacitySlider.getCurpos()) / 100;
        int percentage = (int) (opacity * 100);
        opacitySlider.setCurpos(percentage);
        opacityLabel.setValue(percentage + "%");
    }

    public void updateLegendImage() {
        LegendMaker lm = new LegendMaker();
        int red = redSlider.getCurpos();
        int blue = blueSlider.getCurpos();
        int green = greenSlider.getCurpos();
        Color c = new Color(red, green, blue);

        legendImg.setContent(lm.singleCircleImage(c, 120, 120, 50.0));
        sizeChooser.setVisible(true);

        if (cbColour.getSelectedItem() != ciColourUser) {
            legendHtml.setVisible(true);
            legendImg.setVisible(false);

            showPointsColourModeLegend();
        } else {
            legendImg.setVisible(true);
            legendHtml.setVisible(false);
        }
    }

    public void onScroll$sizeSlider() {
        int size = sizeSlider.getCurpos();
        sizeLabel.setValue(String.valueOf(size));
    }

    public void onScroll$plotSizeSlider() {
        int size = plotSizeSlider.getCurpos();
        plotSizeLabel.setValue(String.valueOf(size));
    }

    public void onScroll$blueSlider() {
        int blue = blueSlider.getCurpos();
        blueLabel.setValue(String.valueOf(blue));
        updateLegendImage();
    }

    public void onScroll$redSlider() {
        int red = redSlider.getCurpos();
        redLabel.setValue(String.valueOf(red));
        updateLegendImage();
    }

    public void onScroll$greenSlider() {
        int green = greenSlider.getCurpos();
        greenLabel.setValue(String.valueOf(green));
        updateLegendImage();
    }

    public void selectColour(Object obj) {
        Div div = (Div) obj;
        String style = div.getStyle();
        String background_color = "background-color";
        int a = style.indexOf(background_color);
        if (a >= 0) {
            String colour = style.substring(a + background_color.length() + 2, a + background_color.length() + 8);
            int r = Integer.parseInt(colour.substring(0, 2), 16);
            int g = Integer.parseInt(colour.substring(2, 4), 16);
            int b = Integer.parseInt(colour.substring(4, 6), 16);

            redSlider.setCurpos(r);
            greenSlider.setCurpos(g);
            blueSlider.setCurpos(b);
            redLabel.setValue(String.valueOf(r));
            greenLabel.setValue(String.valueOf(g));
            blueLabel.setValue(String.valueOf(b));

            updateLegendImage();
        }
    }

    public void onChange$cbColour(Event event) {
        updateUserColourDiv();
        updateLegendImage();
    }

    void updateUserColourDiv() {
        if (cbColour.getSelectedItem() == ciColourUser) {
            divUserColours.setVisible(true);
        } else {
            divUserColours.setVisible(false);
        }
    }

    void updateComboBoxesColour(MapLayer currentSelection) {
        if (currentSelection.isClustered()) {
            cbColour.setSelectedItem(ciColourUser);
            cbColour.setDisabled(true);
        } else {
            cbColour.setDisabled(false);
            for (int i = 0; i < cbColour.getItemCount(); i++) {
                if (cbColour.getItemAtIndex(i).getValue().equals(currentSelection.getColourMode())) {
                    cbColour.setSelectedIndex(i);
                }
            }
            updateUserColourDiv();
        }
    }

    void showPointsColourModeLegend() {
        //remove all
        while (legendHtml.getChildren().size() > 0) {
            legendHtml.removeChild(legendHtml.getFirstChild());
        }

        //1. register legend
        //String pid = registerPointsColourModeLegend(lsid, (String) cbColour.getSelectedItem().getValue());

        //put any parameters into map
        Map map = new HashMap();
        //map.put("pid", pid);
        map.put("query", query);
        map.put("layer", mapLayer);
        map.put("readonly", "true");
        map.put("colourmode", cbColour.getSelectedItem().getValue());
        if (!mapLayer.getColourMode().equals("grid")
                && query.getLegend((String) cbColour.getSelectedItem().getValue()).getCategoryNameOrder() != null) {
            map.put("checkmarks", "true");
        }
        map.put("disableselection", "true");

        try {
            Executions.createComponents(
                    "/WEB-INF/zul/legend/LayerLegendClassification.zul", legendHtml, map);
        } catch (Exception e) {
            logger.error("error creating LayerLegendClassification.zul", e);
        }
    }

    public void init(Query query, MapLayer mapLayer, int red, int green, int blue, int size, int opacity, String colourMode, EventListener listener) {
        this.query = query;
        this.mapLayer = mapLayer;

        opacitySlider.setCurpos(opacity);
        onScroll$opacitySlider(null);

        redSlider.setCurpos(red);
        onScroll$redSlider();

        greenSlider.setCurpos(green);
        onScroll$greenSlider();

        blueSlider.setCurpos(blue);
        onScroll$blueSlider();

        sizeSlider.setCurpos(mapLayer.getSizeVal());
        onScroll$sizeSlider();

        plotSizeSlider.setCurpos(size);
        onScroll$plotSizeSlider();

        for (Comboitem item : cbColour.getItems()) {
            if (item.getValue().equals(colourMode)) {
                cbColour.setSelectedItem(item);
                break;
            }
        }
        this.listener = listener;

        //fill cbColour
        setupCBColour(query);

        updateUserColourDiv();
        updateLegendImage();
    }

    public int getRed() {
        return redSlider.getCurpos();
    }

    public int getGreen() {
        return greenSlider.getCurpos();
    }

    public int getBlue() {
        return blueSlider.getCurpos();
    }

    public int getSize() {
        return sizeSlider.getCurpos();
    }

    public int getPlotSize() {
        return plotSizeSlider.getCurpos();
    }

    public int getOpacity() {
        return opacitySlider.getCurpos();
    }

    public String getColourMode() {
        return (String) cbColour.getSelectedItem().getValue();
    }

    public void onClick$btnApply(Event event) {
        if (listener != null) {
            try {
                //listener.onEvent(null);
                Component c = getParent().getFellowIfAny("scatterplotwindow");
                if (c != null && c instanceof LayerLegendScatterplotController) {
                    //((LayerLegendScatterplotController) c).updateFromLegend();
                }
            } catch (Exception e) {
                logger.error("Error updating legend in scatterplot window", e);
            }
        }
        //this.detach();
    }

    public void onClick$btnClose(Event event) {
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    private void setupCBColour(Query q) {
        for (int i = 0; i < cbColour.getItemCount(); i++) {
            if (cbColour.getItemAtIndex(i) != ciColourUser) {
                cbColour.removeItemAt(i);
                i--;
            }
        }

        //Query q = (Query) m.getSpeciesQuery();
//        Object [] o = (Object []) RecordsLookup.getData(q.getQ());
//        ArrayList<QueryField> fields = (ArrayList<QueryField>) o[1];


        if (q != null) {

            ArrayList<QueryField> fields = query.getFacetFieldList();
            Collections.sort(fields, new QueryField.QueryFieldComparator());
            Comboitem seperator = new Comboitem("seperator");
            String lastGroup = null;


            for (QueryField field : fields) {
                if (q != null
                        && field.getFieldType() == QueryField.FieldType.STRING
                        && (q instanceof UserDataQuery
                        || !(field.getName().equalsIgnoreCase("occurrence_year")
                        || field.getName().equalsIgnoreCase("coordinate_uncertainty")
                        || field.getName().equalsIgnoreCase("month")))) {
                    String newGroup = field.getGroup().getName();
                    if (!newGroup.equals(lastGroup)) {
                        Comboitem sep = new Comboitem("seperator");
                        sep.setLabel("---------------" + StringUtils.center(newGroup, 19) + "---------------");
                        sep.setParent(cbColour);
                        sep.setDisabled(true);
                        lastGroup = newGroup;
                    }
                    Comboitem ci = new Comboitem(field.getDisplayName());
                    ci.setValue(field.getName());
                    ci.setParent(cbColour);
                }
            }


        }
    }
}
