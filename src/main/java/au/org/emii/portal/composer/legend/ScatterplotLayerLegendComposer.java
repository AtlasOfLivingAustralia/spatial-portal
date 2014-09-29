/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer.legend;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.LegendMaker;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.UserDataQuery;
import au.org.emii.portal.composer.GenericAutowireAutoforwardComposer;
import au.org.emii.portal.menu.MapLayer;
import org.ala.layers.legend.QueryField;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Adam
 */
public class ScatterplotLayerLegendComposer extends GenericAutowireAutoforwardComposer {

    private static final Logger LOGGER = Logger.getLogger(ScatterplotLayerLegendComposer.class);

    private Slider opacitySlider;
    private Label opacityLabel;
    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;
    private Slider sizeSlider;
    private Slider plotSizeSlider;
    private Label redLabel;
    private Label greenLabel;
    private Label blueLabel;
    private Label sizeLabel;
    private Label plotSizeLabel;
    private Div sizeChooser;
    private Image legendImg;
    private Div legendHtml;
    private Div divUserColours;
    private Combobox cbColour;
    private Comboitem ciColourUser;
    private EventListener listener;
    private Query query;
    private MapLayer mapLayer;

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
        String backgroundColor = "background-color";
        int a = style.indexOf(backgroundColor);
        if (a >= 0) {
            String colour = style.substring(a + backgroundColor.length() + 2, a + backgroundColor.length() + 8);
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
        while (!legendHtml.getChildren().isEmpty()) {
            legendHtml.removeChild(legendHtml.getFirstChild());
        }

        //put any parameters into map
        Map map = new HashMap();
        map.put(StringConstants.QUERY, query);
        map.put(StringConstants.LAYER, mapLayer);
        map.put(StringConstants.READONLY, StringConstants.TRUE);
        map.put(StringConstants.COLOURMODE, cbColour.getSelectedItem().getValue());
        if (!StringConstants.GRID.equals(mapLayer.getColourMode())
                && query.getLegend((String) cbColour.getSelectedItem().getValue()).getCategoryNameOrder() != null) {
            map.put("checkmarks", StringConstants.TRUE);
        }
        map.put("disableselection", StringConstants.TRUE);

        try {
            Executions.createComponents(
                    "/WEB-INF/zul/legend/LayerLegendClassification.zul", legendHtml, map);
        } catch (Exception e) {
            LOGGER.error("error creating LayerLegendClassification.zul", e);
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

        //fill cbColour
        setupCBColour(query);
        for (Comboitem item : cbColour.getItems()) {
            if (item.getValue() != null && item.getValue().equals(colourMode)) {
                cbColour.setSelectedItem(item);
                break;
            }
        }
        this.listener = listener;

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
                listener.onEvent(null);
            } catch (Exception e) {
                LOGGER.error("Error updating legend in scatterplot window", e);
            }
        }
    }

    public void onClick$btnClose(Event event) {
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    private void setupCBColour(Query q) {
        for (int i = cbColour.getItemCount() - 1; i >= 0; i--) {
            if (cbColour.getItemAtIndex(i) != ciColourUser) {
                cbColour.removeItemAt(i);
            }
        }

        if (q != null) {

            List<QueryField> fields = query.getFacetFieldList();
            Collections.sort(fields, new QueryField.QueryFieldComparator());

            String lastGroup = null;

            for (QueryField field : fields) {
                if (field.getFieldType() == QueryField.FieldType.STRING
                        && (q instanceof UserDataQuery
                        || !(StringConstants.OCCURRENCE_YEAR.equalsIgnoreCase(field.getName())
                        || StringConstants.COORDINATE_UNCERTAINTY.equalsIgnoreCase(field.getName())
                        || StringConstants.MONTH.equalsIgnoreCase(field.getName())))) {
                    String newGroup = field.getGroup().getName();
                    if (!newGroup.equals(lastGroup)) {
                        Comboitem sep = new Comboitem(StringConstants.SEPERATOR);
                        sep.setLabel(StringUtils.center(newGroup, 19));
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
