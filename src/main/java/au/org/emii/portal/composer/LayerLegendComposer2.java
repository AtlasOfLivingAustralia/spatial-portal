/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.GeoJSONUtilities;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.data.UploadQuery;
import org.ala.spatial.util.LegendMaker;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Adam
 */
public class LayerLegendComposer2 extends GenericAutowireAutoforwardComposer {

    SettingsSupplementary settingsSupplementary = null;
    Slider opacitySlider;
    Label opacityLabel;
    Slider redSlider;
    Slider greenSlider;
    Slider blueSlider;
    Slider sizeSlider;
    Checkbox chkUncertaintySize;
    public Button btnPointsCluster;
    Label lblFupload;
    Label redLabel;
    Label greenLabel;
    Label blueLabel;
    Label sizeLabel;
    Listbox activeLayersList;
    Div layerControls;
    Div clusterpoints;
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
    public Radiogroup pointtype;
    public Radio rPoint, rCluster, rGrid;
    MapLayer mapLayer;
    boolean inInit = false;
    Textbox txtLayerName;
    String sLayerName;
    Button btnLayerName;

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
        refreshLayer();
    }

    public void updateLegendImage() {
        LegendMaker lm = new LegendMaker();
        int red = redSlider.getCurpos();
        int blue = blueSlider.getCurpos();
        int green = greenSlider.getCurpos();
        Color c = new Color(red, green, blue);

        legendImg.setContent(lm.singleCircleImage(c, 50, 50, 20.0));
//        sizeChooser.setVisible(true);

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
        refreshLayer();
    }

    public void onScroll$blueSlider() {
        int blue = blueSlider.getCurpos();
        blueLabel.setValue(String.valueOf(blue));
        updateLegendImage();
        refreshLayer();
    }

    public void onScroll$redSlider() {
        int red = redSlider.getCurpos();
        redLabel.setValue(String.valueOf(red));
        updateLegendImage();
        refreshLayer();
    }

    public void onScroll$greenSlider() {
        int green = greenSlider.getCurpos();
        greenLabel.setValue(String.valueOf(green));
        updateLegendImage();
        refreshLayer();
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

            refreshLayer();
        }
    }

    public void onChange$cbColour(Event event) {
        updateUserColourDiv();
        updateLegendImage();
        refreshLayer();
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

        //TODO: make work for query instead of lsid
        //1. register legend
        //String pid = registerPointsColourModeLegend(lsid, (String) cbColour.getSelectedItem().getValue());

        //put any parameters into map
        Map map = new HashMap();
        //map.put("pid", pid);
        map.put("query", query);
        map.put("layer", "points layer");
        map.put("readonly", "true");
        map.put("colourmode", (String) cbColour.getSelectedItem().getValue());

        try {
            Executions.createComponents(
                    "/WEB-INF/zul/AnalysisClassificationLegend.zul", legendHtml, map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(MapLayer ml, Query query, int red, int green, int blue, int size, int opacity, String colourMode, int type, boolean uncertainty, EventListener listener) {
        mapLayer = ml;
        inInit = true;

        txtLayerName.setValue(ml.getDisplayName());
        sLayerName = ml.getDisplayName();

        this.query = query;

        opacitySlider.setCurpos(opacity);
        onScroll$opacitySlider(null);

        redSlider.setCurpos(red);
        onScroll$redSlider();

        greenSlider.setCurpos(green);
        onScroll$greenSlider();

        blueSlider.setCurpos(blue);
        onScroll$blueSlider();

        sizeSlider.setCurpos(size);
        onScroll$sizeSlider();

        for (Comboitem item : (List<Comboitem>) cbColour.getItems()) {
            if (item.getValue().equals(colourMode)) {
                cbColour.setSelectedItem(item);
                break;
            }
        }
        this.listener = listener;

        if(type == 0) {
            pointtype.setSelectedItem(rGrid);
        } else if(type == 1) {
            pointtype.setSelectedItem(rCluster);
        } else if(type == 2) {
            pointtype.setSelectedItem(rPoint);
        }

        chkUncertaintySize.setChecked(uncertainty);        

        updateUserColourDiv();
        updateLegendImage();

        setupLayerControls(ml);

        inInit = false;
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

    public int getOpacity() {
        return opacitySlider.getCurpos();
    }

    public String getColourMode() {
        if(pointtype.getSelectedItem() == rGrid) {
            return "grid";
        } else {
            return (String) cbColour.getSelectedItem().getValue();
        }
    }

    public void onClick$btnApply(Event event) {
        if (listener != null) {
            try {
                listener.onEvent(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onClick$btnClose(Event event) {
        this.detach();
    }

    private void showPointsColourModeLegend(MapLayer m) {
        //remove all
        while (legendHtml.getChildren().size() > 0) {
            legendHtml.removeChild(legendHtml.getFirstChild());
        }

        //1. register legend
        String colourMode = (String) cbColour.getSelectedItem().getValue();
        if (pointtype.getSelectedItem() == rGrid) {
            colourMode = "grid";
        }

        //put any parameters into map
        Map map = new HashMap();
        map.put("query", m.getData("query"));
        map.put("layer", "points layer");
        map.put("readonly", "true");
        map.put("colourmode", colourMode);

        try {
            Executions.createComponents(
                    "/WEB-INF/zul/AnalysisClassificationLegend.zul", legendHtml, map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPointType() {
        if(pointtype.getSelectedItem() == rGrid) {
            return 0;
        } else if(pointtype.getSelectedItem() == rCluster) {
            return 1;
        }else {//if(pointtype.getSelectedItem() == rPoint) {
            return 2;
        }
    }

    public boolean getUncertainty() {
        return chkUncertaintySize.isChecked();
    }

    public void onCheck$chkUncertaintySize() {
         refreshLayer();
         uncertaintyLegend.setVisible(chkUncertaintySize.isChecked());
    }

    public void onCheck$pointtype(Event event) {
        Radio selectedItem = pointtype.getSelectedItem();
        try {
            selectedItem = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
            pointtype.setSelectedItem(selectedItem);
        } catch (Exception e) {
        }

        refreshLayer();

        setupLayerControls(mapLayer);
    }

    void refreshLayer() {
        sLayerName = txtLayerName.getValue();
        if (listener != null && !inInit) {
            try {
                listener.onEvent(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

     public void setupLayerControls(MapLayer m) {

        MapLayer currentSelection = m;

        if (currentSelection != null) {
            if (currentSelection.isDynamicStyle()) {
                if (m.getColourMode().equals("grid")) {
                    pointtype.setSelectedItem(rGrid);
                } else {
                    pointtype.setSelectedItem(rPoint);
                }

                //fill cbColour
                setupCBColour(m);

                updateComboBoxesColour(currentSelection);

                if (currentSelection.getColourMode().equals("-1")) {
                    divUserColours.setVisible(true);
                } else {
                    divUserColours.setVisible(false);
                }

                if (currentSelection.getGeometryType() != GeoJSONUtilities.POINT) {
                    sizeChooser.setVisible(false);
                    uncertainty.setVisible(false);
                } else {
                    sizeChooser.setVisible(pointtype.getSelectedItem() != rGrid);
                    if (m.getGeoJSON() != null && m.getGeoJSON().length() > 0) {
                        uncertainty.setVisible(false);
                    } else {
                        uncertainty.setVisible(!(query instanceof UploadQuery));
                    }
                }

                colourChooser.setVisible(pointtype.getSelectedItem() != rGrid);
                
                if ((cbColour.getSelectedItem() != ciColourUser || pointtype.getSelectedItem() == rGrid)
                        && m.isSpeciesLayer()
                        /*&& !m.isClustered()*/) {
                    legendHtml.setVisible(true);
                    legendImg.setVisible(false);

                    showPointsColourModeLegend(m);
                } else {
                    legendImg.setVisible(true);
                    legendHtml.setVisible(false);
                }
            } else if (currentSelection.getSelectedStyle() != null) {
                /* 1. classification legend has uri with ".zul" content
                 * 2. prediction legend works here
                 * TODO: do this nicely when implementing editable prediction layers
                 */
                String legendUri = currentSelection.getSelectedStyle().getLegendUri();
                if (legendUri != null && legendUri.indexOf(".zul") >= 0) {
                    //remove all
                    while (legendHtml.getChildren().size() > 0) {
                        legendHtml.removeChild(legendHtml.getFirstChild());
                    }

                    //put any parameters into map
                    Map map = null;
                    if (legendUri.indexOf("?") > 0) {
                        String[] parameters = legendUri.substring(legendUri.indexOf("?") + 1,
                                legendUri.length()).split("&");
                        if (parameters.length > 0) {
                            map = new HashMap();
                        }
                        for (String p : parameters) {
                            String[] parameter = p.split("=");
                            if (parameter.length == 2) {
                                map.put(parameter[0], parameter[1]);
                            }
                        }
                        legendUri = legendUri.substring(0, legendUri.indexOf("?"));
                    }

                    //open .zul with parameters
                    Executions.createComponents(
                            legendUri, legendHtml, map);

                    legendHtml.setVisible(true);
                    legendImgUri.setVisible(false);
                    legendLabel.setVisible(true);
                } else {
                    legendImgUri.setSrc(legendUri);
                    legendImgUri.setVisible(true);
                    legendHtml.setVisible(false);
                    legendLabel.setVisible(false);
                }
                legendImg.setVisible(false);
                colourChooser.setVisible(false);
                sizeChooser.setVisible(false);
            } else if (currentSelection.getCurrentLegendUri() != null) {
                // works for normal wms layers
                legendImgUri.setSrc(currentSelection.getCurrentLegendUri());
                legendImgUri.setVisible(true);
                legendHtml.setVisible(false);
                legendLabel.setVisible(false);
                legendImg.setVisible(false);
                colourChooser.setVisible(false);
                sizeChooser.setVisible(false);
            } else {
                //image layer?
                legendImgUri.setVisible(false);
                legendHtml.setVisible(false);
                legendLabel.setVisible(false);
                legendImg.setVisible(false);
                colourChooser.setVisible(false);
                sizeChooser.setVisible(false);
            }
            layerControls.setVisible(true);
            layerControls.setAttribute("activeLayerName", currentSelection.getName());
        }

        if (m != null && m.isSpeciesLayer()) {
            clusterpoints.setVisible(true);
            cbColour.setDisabled(m.isClustered());
        } else {
            clusterpoints.setVisible(false);
            cbColour.setDisabled(true);
        }

        uncertaintyLegend.setVisible(chkUncertaintySize.isChecked());
    }

    public String getDisplayName() {
        return txtLayerName.getValue();
    }

//    public void onChanging$txtLayerName(Event event) {
//        refreshLayer();
//    }
    
//    public void onChange$txtLayerName(Event event) {
//        refreshLayer();
//    }

    public void onOK$txtLayerName(Event event) {
        refreshLayer();
        btnLayerName.setDisabled(true);
    }

    public void onBlur$txtLayerName(Event event) {
        if(sLayerName.equals(txtLayerName.getValue())) {
            btnLayerName.setDisabled(true);
        }
    }

    private void setupCBColour(MapLayer m) {
        for(int i=0;i<cbColour.getItemCount();i++) {
            if(cbColour.getItemAtIndex(i) != ciColourUser) {
                cbColour.removeItemAt(i);
                i--;
            }
        }

        Query q = (Query) m.getData("query");
//        Object [] o = (Object []) RecordsLookup.getData(q.getQ());
//        ArrayList<QueryField> fields = (ArrayList<QueryField>) o[1];
        if(q != null) {
            ArrayList<QueryField> fields = q.getFacetFieldList();
            for(int i=0;i<fields.size();i++) {
                Comboitem ci = new Comboitem(fields.get(i).getDisplayName());
                ci.setValue(fields.get(i).getName());
                ci.setParent(cbColour);
            }
        }
    }
}
