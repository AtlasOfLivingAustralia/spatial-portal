/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.GeoJSONUtilities;
import au.org.emii.portal.util.LayerUtilities;
import java.awt.Color;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.LegendObject;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.data.UploadQuery;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.LegendMaker;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
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
    Hbox dAnimationStep;
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
    Label lInGroupCount;
    Button btnCreateGroupLayers;
    Div dGroupBox;
    Combobox cbClassificationGroup;
    Div divClassificationPicker;
    Div divAnimation;
    Combobox cbAnimationDenomination;
    Button btnAnimationStart;
    Button btnAnimationStop;
    Intbox intAnimationStep;
    Intbox intAnimationYearStart;
    Intbox intAnimationYearEnd;
    //Label lblAnimationLabel;
    Doublebox dblAnimationSeconds;

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

        legendImg.setContent(lm.singleCircleImage(c, 60, 60, 50.0));
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
        mapLayer.setHighlight(null);
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
                if (cbColour.getItemAtIndex(i).getValue() != null
                        && cbColour.getItemAtIndex(i).getValue().equals(currentSelection.getColourMode())) {
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
        map.put("layer", mapLayer);
        map.put("readonly", "true");

        String colourmode = (String) cbColour.getSelectedItem().getValue();
        if (!mapLayer.getColourMode().equals("grid")
                && query.getLegend(colourmode).getCategories() != null) {
            map.put("checkmarks", "true");
        }

        try {
            LegendObject lo = query.getLegend(colourmode);
            if (lo != null) {
                mapLayer.setData("legendobject", lo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        map.put("colourmode", colourmode);

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
            if (item.getValue() != null && item.getValue().equals(colourMode)) {
                cbColour.setSelectedItem(item);
                break;
            }
        }
        this.listener = listener;

        if (type == 0) {
            pointtype.setSelectedItem(rGrid);
        } else if (type == 1) {
            pointtype.setSelectedItem(rCluster);
        } else if (type == 2) {
            pointtype.setSelectedItem(rPoint);
        }

        chkUncertaintySize.setChecked(uncertainty);

        updateUserColourDiv();
        updateLegendImage();

        setupLayerControls(ml);
        
        updateAnimationDiv();

        String script = "mapFrame.stopAllAnimations();";
        getMapComposer().getOpenLayersJavascript().execute(script);

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
        if (pointtype.getSelectedItem() == rGrid) {
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
        map.put("layer", m);
        map.put("readonly", "true");
        map.put("colourmode", colourMode);

        String colourmode = (String) cbColour.getSelectedItem().getValue();
        if (!m.getColourMode().equals("grid")
                && query.getLegend(colourmode).getCategories() != null) {
            map.put("checkmarks", "true");
        }
        try {
            LegendObject lo = ((Query) m.getData("query")).getLegend(colourmode);
            if (lo != null) {
                m.setData("legendobject", lo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Executions.createComponents(
                    "/WEB-INF/zul/AnalysisClassificationLegend.zul", legendHtml, map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPointType() {
        if (pointtype.getSelectedItem() == rGrid) {
            return 0;
        } else if (pointtype.getSelectedItem() == rCluster) {
            return 1;
        } else {//if(pointtype.getSelectedItem() == rPoint) {
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
            mapLayer.setHighlight(null);
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

                updateAdhocGroupContols(m);


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
                        && m.isSpeciesLayer() /*&& !m.isClustered()*/) {
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
            setupForClassificationLayers();
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
        if (sLayerName.equals(txtLayerName.getValue())) {
            btnLayerName.setDisabled(true);
        }
    }

    private void setupCBColour(MapLayer m) {
        for (int i = 0; i < cbColour.getItemCount(); i++) {
            if (cbColour.getItemAtIndex(i) != ciColourUser) {
                cbColour.removeItemAt(i);
                i--;
            }
        }

        Query q = (Query) m.getData("query");
//        Object [] o = (Object []) RecordsLookup.getData(q.getQ());
//        ArrayList<QueryField> fields = (ArrayList<QueryField>) o[1];
        if (q != null) {
            ArrayList<QueryField> fields = q.getFacetFieldList();
            /*for (int i = 0; i < fields.size(); i++) {
            Comboitem ci = new Comboitem(fields.get(i).getDisplayName());
            ci.setValue(fields.get(i).getName());
            ci.setParent(cbColour);
            }
             *
             */
            Comboitem seperator = new Comboitem("seperator");
            seperator.setLabel("------------------Taxonomic------------------");
            seperator.setParent(cbColour);
            seperator.setDisabled(true);
            for (int i = 0; i < fields.size(); i++) {
                Comboitem ci = new Comboitem(fields.get(i).getDisplayName());
                ci.setValue(fields.get(i).getName());
                ci.setParent(cbColour);

                if (ci.getValue().equals("interaction")) {
                    Comboitem seperator1 = new Comboitem("seperator");
                    seperator1.setLabel("------------------Geospatial------------------");
                    seperator1.setParent(cbColour);
                    seperator1.setDisabled(true);
                }
                if (ci.getValue().equals("cl620")) {
                    Comboitem seperator2 = new Comboitem("seperator");
                    seperator2.setLabel("------------------Temporal------------------");
                    seperator2.setParent(cbColour);
                    seperator2.setDisabled(true);
                }
                if (ci.getValue().equals("occurrence_year_decade")) {
                    Comboitem seperator3 = new Comboitem("seperator");
                    seperator3.setLabel("------------------Record details------------------");
                    seperator3.setParent(cbColour);
                    seperator3.setDisabled(true);
                }
                if (ci.getValue().equals("collector")) {
                    Comboitem seperator4 = new Comboitem("seperator");
                    seperator4.setLabel("------------------Attribution------------------");
                    seperator4.setParent(cbColour);
                    seperator4.setDisabled(true);
                }
                if (ci.getValue().equals("institution_name")) {
                    Comboitem seperator5 = new Comboitem("seperator");
                    seperator5.setLabel("------------------Record assertions------------------");
                    seperator5.setParent(cbColour);
                    seperator5.setDisabled(true);
                }
            }
        }
    }

    public void onClick$btnCreateGroupLayers(Event event) {
        Query query;
        if (mapLayer != null
                && (query = (Query) mapLayer.getData("query")) != null
                && query.flagRecordCount() != 0) {

            Query inGroup = query.newFlaggedRecords(true);
            Query outGroup = query.newFlaggedRecords(false);

            getMapComposer().mapSpecies(inGroup, mapLayer.getDisplayName() + " in group", "species", -1, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, MapComposer.nextColour());
            getMapComposer().mapSpecies(outGroup, mapLayer.getDisplayName() + " out group", "species", -1, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, MapComposer.nextColour());
        }
    }

    private void updateAdhocGroupContols(MapLayer m) {
        if (m == null) {
            dGroupBox.setVisible(false);
            return;
        }
        Query query = (Query) m.getData("query");
        if (query == null || query.flagRecordCount() == 0) {
            dGroupBox.setVisible(false);
        } else {
            dGroupBox.setVisible(true);

            lInGroupCount.setValue(query.flagRecordCount() + (query.flagRecordCount() == 1 ? " record" : " records"));
        }
    }

    private void setupForClassificationLayers() {
        if (mapLayer != null && mapLayer.getSubType() == LayerUtilities.ALOC) {
            divClassificationPicker.setVisible(true);

            //reset content
            Integer groupCount = (Integer) mapLayer.getData("classificationGroupCount");
            if (groupCount == null) {
                mapLayer.setData("classificationGroupCount", getClassificationGroupCount(mapLayer.getName().replace("aloc_", "")));
                groupCount = 0;
            }
            for (int i = cbClassificationGroup.getItemCount() - 1; i >= 0; i--) {
                cbClassificationGroup.removeItemAt(i);
            }
            Comboitem ci = new Comboitem("none");
            ci.setParent(cbClassificationGroup);

            for (int i = 1; i <= groupCount; i++) {
                new Comboitem("Group " + i).setParent(cbClassificationGroup);
            }

            //is there a current selection?
            Integer groupSelection = (Integer) mapLayer.getData("classificationSelection");
            if (groupSelection == null) {
                groupSelection = 0;
                mapLayer.setData("classificationSelection", groupSelection);
            }
            cbClassificationGroup.setSelectedIndex(groupSelection);

        } else {
            divClassificationPicker.setVisible(false);
        }
    }

    //sld substitution strings
    private static final String SUB_LAYERNAME = "*layername*";
    private static final String SUB_COLOUR = "0xff0000"; //"*colour*";
    private static final String SUB_MIN_MINUS_ONE = "*min_minus_one*";
    private static final String SUB_MIN = "*min*";
    private static final String SUB_MAX_PLUS_ONE = "*max_plus_one*";
    String polygonSld =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\">"
            + "<NamedLayer><Name>ALA:" + SUB_LAYERNAME + "</Name>"
            + "<UserStyle><FeatureTypeStyle><Rule><RasterSymbolizer><Geometry></Geometry>"
            + "<ColorMap>"
            + "<ColorMapEntry color=\"" + SUB_COLOUR + "\" opacity=\"0\" quantity=\"" + SUB_MIN_MINUS_ONE + "\"/>"
            + "<ColorMapEntry color=\"" + SUB_COLOUR + "\" opacity=\"1\" quantity=\"" + SUB_MIN + "\"/>"
            + "<ColorMapEntry color=\"" + SUB_COLOUR + "\" opacity=\"0\" quantity=\"" + SUB_MAX_PLUS_ONE + "\"/>"
            + "</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";

    public void onChange$cbClassificationGroup(Event event) {
        if (mapLayer != null) {
            mapLayer.setData("classificationSelection", new Integer(cbClassificationGroup.getSelectedIndex()));

            String baseUri = mapLayer.getUri();
            int pos = baseUri.indexOf("&sld_body=");
            if (pos > 0) {
                baseUri = baseUri.substring(0, pos);
            }
            String layername = mapLayer.getName();
            int n = cbClassificationGroup.getSelectedIndex();
            if (n > 0) {
                try {
                    String sldBodyParam = "&sld_body=" + formatSld(URLEncoder.encode(polygonSld, "UTF-8"), layername, String.valueOf(n - 1), String.valueOf(n), String.valueOf(n), String.valueOf(n + 1));
                    mapLayer.setUri(baseUri + sldBodyParam);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                mapLayer.setUri(baseUri);
            }
            getMapComposer().reloadMapLayerNowAndIndexes(mapLayer);
        }
    }

    private String formatSld(String sld, String layername, String min_minus_one, String min, String max, String max_plus_one) {
        return sld.replace(SUB_LAYERNAME, layername).replace(SUB_MIN_MINUS_ONE, min_minus_one).replace(SUB_MIN, min).replace(SUB_MAX_PLUS_ONE, max_plus_one);
    }

    public Integer getClassificationGroupCount(String pid) {
        Integer i = 0;
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/output/aloc/" + pid + "/classification_means.csv");

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            String[] s = slist.split("\n");
            i = s.length - 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    public void onClick$btnAnimationStart(Event event) {
        try {

            Integer monthOrYear = 0; //0=month, 1=year

            if("1".equals(cbAnimationDenomination.getValue()) || "Year".equalsIgnoreCase(cbAnimationDenomination.getValue())){
                monthOrYear = 1;
            }

            System.out.println("Animation: " + monthOrYear);

            Integer step = 1;
            if(monthOrYear != 0){
                step = intAnimationStep.getValue();
                if(step < 1) {
                    step = 1;
                    intAnimationStep.setValue(1);
                }
            }
            
            Double interval = dblAnimationSeconds.getValue();
            if(interval < 0.2) {
                interval = 0.2;
                dblAnimationSeconds.setValue(0.2);
            }

            mapLayer.setData("animation_step", step);
            mapLayer.setData("animation_interval", interval);

            Integer start = (Integer) intAnimationYearStart.getValue();
            Integer end =  (Integer) intAnimationYearEnd.getValue();
            String script = "mapFrame.animateStart('" + StringEscapeUtils.escapeJavaScript(mapLayer.getNameJS()) + "',"
                        + monthOrYear + ","
                        + interval * 1000 + ","
                        + start + ","
                        + end + ","
                        + step + ");";
            System.out.println("Script: " + script );

            getMapComposer().getOpenLayersJavascript().execute(script);

            btnAnimationStop.setDisabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectYearOrMonth() {
        if("1".equals(cbAnimationDenomination.getValue()) || "Year".equals(cbAnimationDenomination.getValue())){
            dAnimationStep.setVisible(true);
            //lblAnimationLabel.setVisible(true);
        } else {
            dAnimationStep.setVisible(false);
            //lblAnimationLabel.setVisible(false);
        }
    }

    private void updateAnimationDiv() {
        if(dblAnimationSeconds == null) {
            return;
        }
        try {
            Query q = (Query) mapLayer.getData("query");
            if(q != null && q instanceof BiocacheQuery) {
                Integer firstYear = (Integer) mapLayer.getData("first_year");
                Integer lastYear =  (Integer) mapLayer.getData("last_year");
                if(firstYear == null) {
                    try {
                        LegendObject lo = ((BiocacheQuery)q).getLegend("occurrence_year");
                        if(lo != null && lo.getMinMax() != null) {
                            firstYear = (int) lo.getMinMax()[0];
                            lastYear = (int) lo.getMinMax()[1];
                            mapLayer.setData("first_year", firstYear);
                            mapLayer.setData("last_year", lastYear);
                        }
                    } catch (Exception e) {
                        //this will fail if there are no records
                    }
                }

                Integer step = (Integer) mapLayer.getData("animation_step");
                if(step != null) {
                    intAnimationStep.setValue(step);
                }

                Double interval = (Double) mapLayer.getData("animation_interval");
                if(interval != null) {
                    dblAnimationSeconds.setValue(interval);
                }

                if(firstYear < lastYear) {
                    //lblAnimationLabel.setValue("years " + firstYear + " to " + lastYear);
                    intAnimationYearStart.setValue(firstYear);
                    intAnimationYearEnd.setValue(lastYear);
                    divAnimation.setVisible(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$btnAnimationStop(Event event) {
        String script = "mapFrame.animateStop('" + StringEscapeUtils.escapeJavaScript(mapLayer.getNameJS()) + "');";
        getMapComposer().getOpenLayersJavascript().execute(script);
        btnAnimationStop.setDisabled(true);
    }
}
