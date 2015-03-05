/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer.legend;

import au.org.ala.legend.Facet;
import au.org.ala.legend.LegendObject;
import au.org.ala.legend.QueryField;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.GenericAutowireAutoforwardComposer;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.*;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

import java.awt.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

/**
 * @author Adam
 */
public class LayerLegendGeneralComposer extends GenericAutowireAutoforwardComposer {

    private static final Logger LOGGER = Logger.getLogger(LayerLegendGeneralComposer.class);
    //sld substitution strings
    private static final String SUB_LAYERNAME = "*layername*";
    private static final String SUB_COLOUR = "0xff0000";
    private static final String SUB_MIN_MINUS_ONE = "*min_minus_one*";
    private static final String SUB_MIN = "*min*";
    private static final String SUB_MAX_PLUS_ONE = "*max_plus_one*";
    private static final String POLYGON_SLD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\">"
                    + "<NamedLayer><Name>ALA:" + SUB_LAYERNAME + "</Name>"
                    + "<UserStyle><FeatureTypeStyle><Rule><RasterSymbolizer><Geometry></Geometry>"
                    + "<ColorMap>"
                    + "<ColorMapEntry color=\"" + SUB_COLOUR + "\" opacity=\"0\" quantity=\"" + SUB_MIN_MINUS_ONE + "\"/>"
                    + "<ColorMapEntry color=\"" + SUB_COLOUR + "\" opacity=\"1\" quantity=\"" + SUB_MIN + "\"/>"
                    + "<ColorMapEntry color=\"" + SUB_COLOUR + "\" opacity=\"0\" quantity=\"" + SUB_MAX_PLUS_ONE + "\"/>"
                    + "</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";
    private Combobox cbColour;
    private Radiogroup pointtype;
    private Radio rPoint, rCluster, rGrid;
    private Textbox txtLayerName;
    private Slider opacitySlider;
    private Label opacityLabel;
    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;
    private Slider sizeSlider;
    private Checkbox chkUncertaintySize;
    private Label lblFupload;
    private Label redLabel;
    private Label greenLabel;
    private Label blueLabel;
    private Label sizeLabel;
    private Listbox activeLayersList;
    private Div layerControls;
    private Div clusterpoints;
    private Div uncertainty;
    private Hbox uncertaintyLegend;
    private Div colourChooser;
    private Div sizeChooser;
    private Image legendImg;
    private Image legendImgUri;
    private Div legendHtml;
    private Label legendLabel;
    private Div divUserColours;
    private Hbox dAnimationStep;
    private Comboitem ciColourUser;
    private Label layerName;
    private Query query;
    private MapLayer mapLayer;
    private boolean inInit = false;
    private String sLayerName;
    private Button btnLayerName;
    private Label lInGroupCount;
    private Button btnCreateGroupLayers;
    private Div dGroupBox;
    private Combobox cbClassificationGroup;
    private Div divClassificationPicker;
    private Div divAnimation;
    private Combobox cbAnimationDenomination;
    private Button btnAnimationStart;
    private Button btnAnimationStop;
    private Intbox intAnimationStep;
    private Intbox intAnimationYearStart;
    private Intbox intAnimationYearEnd;
    private Doublebox dblAnimationSeconds;

    @Override
    public void afterCompose() {
        super.afterCompose();

        MapLayer llc2MapLayer = null;

        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object o : m.entrySet()) {
                if (((Map.Entry) o).getKey() instanceof String
                        && "map_layer".equals(((Map.Entry) o).getKey())) {
                    llc2MapLayer = (MapLayer) ((Map.Entry) o).getValue();
                }
            }
        }

        cbColour.setSelectedIndex(0);

        getMapComposer().setFacetsOpenListener(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                cbColour.open();
            }
        });

        getMapComposer().setLayerLegendNameRefresh(new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                txtLayerName.setValue((String) event.getData());
            }
        });

        init(
                llc2MapLayer,
                llc2MapLayer.getSpeciesQuery(),
                llc2MapLayer.getRedVal(),
                llc2MapLayer.getGreenVal(),
                llc2MapLayer.getBlueVal(),
                llc2MapLayer.getSizeVal(),
                (int) (llc2MapLayer.getOpacity() * 100),
                llc2MapLayer.getColourMode(),
                (StringConstants.GRID.equals(llc2MapLayer.getColourMode())) ? 0 : ((llc2MapLayer.isClustered()) ? 1 : 2),
                llc2MapLayer.getSizeUncertain());

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
        while (!legendHtml.getChildren().isEmpty()) {
            legendHtml.removeChild(legendHtml.getFirstChild());
        }

        //put any parameters into map
        Map map = new HashMap();
        map.put(StringConstants.QUERY, query);
        map.put(StringConstants.LAYER, mapLayer);
        map.put(StringConstants.READONLY, StringConstants.TRUE);

        String colourmode = cbColour.getSelectedItem().getValue();
        if (!StringConstants.GRID.equals(mapLayer.getColourMode())
                && query.getLegend(colourmode) != null
                && query.getLegend(colourmode).getCategoryNameOrder() != null) {
            map.put("checkmarks", StringConstants.TRUE);
        }

        try {
            LegendObject lo = query.getLegend(colourmode);
            if (lo != null) {
                mapLayer.setLegendObject(lo);
            }
        } catch (Exception e) {
            LOGGER.error("querying layer legend for: " + query.getFullQ(false), e);
        }

        map.put(StringConstants.COLOURMODE, colourmode);

        try {
            Executions.createComponents(
                    "/WEB-INF/zul/legend/LayerLegendClassification.zul", legendHtml, map);
        } catch (Exception e) {
            LOGGER.error("attempting to open classification legend: " + legendHtml, e);
        }
    }

    public void init(MapLayer ml, Query query, int red, int green, int blue, int size, int opacity, String colourMode, int type, boolean uncertainty) {
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

        for (Comboitem item : cbColour.getItems()) {
            if (item.getValue() != null && item.getValue().equals(colourMode)) {
                cbColour.setSelectedItem(item);
                break;
            }
        }

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
            return StringConstants.GRID;
        } else {
            return (String) cbColour.getSelectedItem().getValue();
        }
    }

    public void onClick$btnApply(Event event) {
        MapComposer mc = getMapComposer();
        MapLayer ml = mapLayer;

        //layer on map settings
        if (getRed() != ml.getRedVal()
                || getGreen() != ml.getGreenVal()
                || getBlue() != ml.getBlueVal()
                || getSize() != ml.getSizeVal()
                || getOpacity() != (int) (ml.getOpacity() * 100)
                || (ml.getColourMode() != null && !ml.getColourMode().equals(getColourMode()))
                || (ml.isClustered() && getPointType() != 1)
                || ml.getSizeUncertain() != getUncertainty()) {

            ml.setRedVal(getRed());
            ml.setGreenVal(getGreen());
            ml.setBlueVal(getBlue());
            ml.setSizeVal(getSize());
            ml.setOpacity(getOpacity() / 100.0f);
            ml.setColourMode(getColourMode());
            ml.setClustered(getPointType() == 0);
            ml.setSizeUncertain(getUncertainty());

            mc.applyChange(ml);
        }

        //layer in menu settings
        if (!ml.getDisplayName().equals(getDisplayName())) {
            ml.setDisplayName(getDisplayName());

            //selection label
            mc.setLabelSelectedLayer(getDisplayName());

            mc.redrawLayersList();
        }
    }

    public void onClick$btnClose(Event event) {
        this.detach();
    }

    private void showPointsColourModeLegend(MapLayer m) {
        //remove all
        while (!legendHtml.getChildren().isEmpty()) {
            legendHtml.removeChild(legendHtml.getFirstChild());
        }

        //1. register legend
        String colourMode = cbColour.getSelectedItem().getValue();
        if (pointtype.getSelectedItem() == rGrid) {
            colourMode = StringConstants.GRID;
        }

        //put any parameters into map
        Map map = new HashMap();
        map.put(StringConstants.QUERY, m.getSpeciesQuery());
        map.put(StringConstants.LAYER, m);
        map.put(StringConstants.READONLY, StringConstants.TRUE);
        map.put(StringConstants.COLOURMODE, colourMode);

        String colourmode = cbColour.getSelectedItem().getValue();
        if (!StringConstants.GRID.equals(m.getColourMode())
                && query.getLegend(colourmode).getCategoryNameOrder() != null) {
            map.put("checkmarks", StringConstants.TRUE);
        }
        try {
            LegendObject lo = m.getSpeciesQuery().getLegend(colourmode);
            if (lo != null) {
                m.setLegendObject(lo);
            }
        } catch (Exception e) {
            LOGGER.error("error getting legend for map layer: " + m.getName(), e);
        }

        try {
            Executions.createComponents(
                    "/WEB-INF/zul/legend/LayerLegendClassification.zul", legendHtml, map);
        } catch (Exception e) {
            LOGGER.error("error opening classification legend: " + legendHtml, e);
        }
    }

    public int getPointType() {
        if (pointtype.getSelectedItem() == rGrid) {
            return 0;
        } else if (pointtype.getSelectedItem() == rCluster) {
            return 1;
        } else {
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
        Radio selectedItem = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        pointtype.setSelectedItem(selectedItem);
        mapLayer.setHighlight(null);

        refreshLayer();

        setupLayerControls(mapLayer);
    }

    void refreshLayer() {
        if (!inInit) {
            sLayerName = txtLayerName.getValue();
            onClick$btnApply(null);
        }
    }

    public void setupLayerControls(MapLayer m) {
        MapLayer currentSelection = m;

        if (currentSelection != null) {
            if (currentSelection.isDynamicStyle()) {
                if (StringConstants.GRID.equals(m.getColourMode())) {
                    pointtype.setSelectedItem(rGrid);
                    uncertainty.setVisible(false);
                } else {
                    pointtype.setSelectedItem(rPoint);

                    //uncertainty circles are only applicable to biocache point data
                    uncertainty.setVisible(currentSelection.getSpeciesQuery() != null
                            && currentSelection.getSpeciesQuery() instanceof BiocacheQuery);
                }

                //fill cbColour
                setupCBColour(currentSelection);

                updateComboBoxesColour(currentSelection);

                updateAdhocGroupContols(currentSelection);

                if ("-1".equals(currentSelection.getColourMode())) {
                    divUserColours.setVisible(true);
                } else {
                    divUserColours.setVisible(false);
                }

                if (currentSelection.getGeometryType() != LayerUtilitiesImpl.POINT) {
                    sizeChooser.setVisible(false);
                    uncertainty.setVisible(false);
                } else {
                    sizeChooser.setVisible(pointtype.getSelectedItem() != rGrid);
                    if (m.getGeoJSON() != null && m.getGeoJSON().length() > 0) {
                        uncertainty.setVisible(false);
                    } else {
                        uncertainty.setVisible(!(query instanceof UserDataQuery));
                    }
                }

                colourChooser.setVisible(pointtype.getSelectedItem() != rGrid);
                uncertainty.setVisible(pointtype.getSelectedItem() != rGrid);

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
                if (legendUri != null && legendUri.contains(".zul")) {
                    //remove all
                    while (!legendHtml.getChildren().isEmpty()) {
                        legendHtml.removeChild(legendHtml.getFirstChild());
                    }

                    //put any parameters into map
                    Map map = null;
                    if (legendUri.indexOf('?') > 0) {
                        String[] parameters = legendUri.substring(legendUri.indexOf('?') + 1,
                                legendUri.length()).split("&");
                        if (parameters.length > 0) {
                            map = new HashMap();

                            for (String p : parameters) {
                                String[] parameter = p.split("=");
                                if (parameter.length == 2) {
                                    map.put(parameter[0], parameter[1]);
                                }
                            }
                        }
                        legendUri = legendUri.substring(0, legendUri.indexOf('?'));
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
                cbColour.setVisible(false);
                uncertainty.setVisible(false);
            } else if (currentSelection.getCurrentLegendUri() != null) {
                // works for normal wms layers
                legendImgUri.setSrc(currentSelection.getCurrentLegendUri());
                legendImgUri.setVisible(true);
                legendHtml.setVisible(false);
                legendLabel.setVisible(false);
                legendImg.setVisible(false);

                //if it is an envelope or layerdb object table layer (i.e. polygon layer)
                //the colour choose must be visible, wms legend off and colour combobox not visible
                colourChooser.setVisible(mapLayer.isPolygonLayer());
                if (mapLayer.isPolygonLayer()) {
                    cbColour.setVisible(false);
                    legendImgUri.setVisible(false);
                }

                sizeChooser.setVisible(false);
                uncertainty.setVisible(false);
            } else {
                //image layer?
                legendImgUri.setVisible(false);
                legendHtml.setVisible(false);
                legendLabel.setVisible(false);
                legendImg.setVisible(false);
                colourChooser.setVisible(false);
                sizeChooser.setVisible(false);
                cbColour.setVisible(false);
                uncertainty.setVisible(false);
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
        for (int i = cbColour.getItemCount() - 1; i >= 0; i--) {
            if (cbColour.getItemAtIndex(i) != ciColourUser) {
                cbColour.removeItemAt(i);
            }
        }

        Query q = m.getSpeciesQuery();
        if (q != null) {
            List<QueryField> fields = q.getFacetFieldList();
            for (int i = fields.size() - 1; i >= 0; i--) {
                if (fields.get(i) == null) {
                    fields.remove(i);
                }
            }
            Collections.sort(fields, new QueryField.QueryFieldComparator());

            String lastGroup = null;


            for (QueryField field : fields) {
                String newGroup = field.getGroup().getName();
                if (!newGroup.equals(lastGroup)) {
                    Comboitem sep = new Comboitem(StringConstants.SEPERATOR);
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

    public void onClick$btnCreateGroupLayers(Event event) {
        Query q;
        if (mapLayer != null
                && (q = mapLayer.getSpeciesQuery()) != null
                && q.flagRecordCount() != 0) {

            Query inGroup = q.newFlaggedRecords(true);
            Query outGroup = q.newFlaggedRecords(false);

            getMapComposer().mapSpecies(inGroup, mapLayer.getDisplayName() + " in group", StringConstants.SPECIES, -1, LayerUtilitiesImpl.SPECIES, null, -1,
                    MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
            getMapComposer().mapSpecies(outGroup, mapLayer.getDisplayName() + " out group", StringConstants.SPECIES, -1, LayerUtilitiesImpl.SPECIES, null, -1,
                    MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
        }
    }

    private void updateAdhocGroupContols(MapLayer m) {
        if (m == null) {
            dGroupBox.setVisible(false);
            return;
        }
        Query q = m.getSpeciesQuery();
        if (q == null || q.flagRecordCount() == 0) {
            dGroupBox.setVisible(false);
        } else {
            dGroupBox.setVisible(true);

            lInGroupCount.setValue(q.flagRecordCount() + (q.flagRecordCount() == 1 ? " record" : " records"));
        }
    }

    private void setupForClassificationLayers() {

        String activeLayerName = StringConstants.NONE;
        JSONObject layer = null;
        if (mapLayer != null && mapLayer.getUri() != null) {
            if (mapLayer.getBaseUri() != null) {
                activeLayerName = mapLayer.getBaseUri().replaceAll("^.*ALA:", "").replaceAll("&.*", "");
            } else {
                activeLayerName = mapLayer.getUri().replaceAll("^.*ALA:", "").replaceAll("&.*", "");
            }
            layer = CommonData.getLayer(activeLayerName);
        }
        LOGGER.debug("ACTIVE LAYER: " + activeLayerName);

        if (mapLayer != null && mapLayer.getSubType() == LayerUtilitiesImpl.ALOC) {
            divClassificationPicker.setVisible(true);

            //reset content
            Integer groupCount = mapLayer.getClassificationGroupCount();
            if (groupCount == null) {
                mapLayer.setClassificationGroupCount(getClassificationGroupCount(mapLayer.getName().replace("aloc_", "")));
                groupCount = 0;
            }
            for (int i = cbClassificationGroup.getItemCount() - 1; i >= 0; i--) {
                cbClassificationGroup.removeItemAt(i);
            }
            Comboitem ci = new Comboitem(StringConstants.NONE);
            ci.setParent(cbClassificationGroup);

            for (int i = 1; i <= groupCount; i++) {
                new Comboitem("Group " + i).setParent(cbClassificationGroup);
            }

            //is there a current selection?
            Integer groupSelection = mapLayer.getClassificationSelection();
            if (groupSelection == null) {
                groupSelection = 0;
                mapLayer.setClassificationSelection(groupSelection);
            }
            cbClassificationGroup.setSelectedIndex(groupSelection);

            getFellow("btnCreateArea").setVisible(false);

        } else if (layer != null && layer.containsKey("type") && layer.get("type").toString().equalsIgnoreCase("contextual")
                && layer.containsKey("fields")) {
            divClassificationPicker.setVisible(true);

            if (mapLayer.getClassificationGroupCount() == null || mapLayer.getClassificationGroupCount() == 0) {
                //build
                String fieldId = null;
                JSONArray ja = (JSONArray) layer.get("fields");
                for (int i = 0; i < ja.size(); i++) {
                    if (((JSONObject) ja.get(i)).get("defaultlayer").toString().equalsIgnoreCase("true")) {
                        fieldId = ((JSONObject) ja.get(i)).get("id").toString();
                    }
                }

                JSONParser jp = new JSONParser();
                JSONObject objJson = null;
                try {
                    objJson = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/field/" + fieldId));
                } catch (ParseException e) {
                    LOGGER.error("failed to parse for: " + fieldId);
                }
                JSONArray objects = (JSONArray) objJson.get("objects");

                //sort
                List<JSONObject> list = objects.subList(0, objects.size());
                Collections.sort(list, new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject o1, JSONObject o2) {
                        String s1 = (o1 == null || !o1.containsKey("name")) ? "" : o1.get("name").toString();
                        String s2 = (o2 == null || !o2.containsKey("name")) ? "" : o2.get("name").toString();
                        return s1.compareTo(s2);
                    }
                });
                JSONArray obj = new JSONArray();
                obj.addAll(list);

                mapLayer.setClassificationGroupCount(obj.size());
                mapLayer.setClassificationObjects(obj);
            }
            //reset content
            Integer groupCount = mapLayer.getClassificationGroupCount();
            JSONArray groupObjects = mapLayer.getClassificationObjects();

            for (int i = cbClassificationGroup.getItemCount() - 1; i >= 0; i--) {
                cbClassificationGroup.removeItemAt(i);
            }
            Comboitem ci = new Comboitem(StringConstants.NONE);
            ci.setParent(cbClassificationGroup);

            for (int i = 0; i < groupCount; i++) {
                new Comboitem(((JSONObject) groupObjects.get(i)).get("name").toString()).setParent(cbClassificationGroup);
            }

            //is there a current selection?
            Integer groupSelection = mapLayer.getClassificationSelection();
            if (groupSelection == null) {
                groupSelection = 0;
                mapLayer.setClassificationSelection(groupSelection);
            }
            cbClassificationGroup.setSelectedIndex(groupSelection);

            getFellow("btnCreateArea").setVisible(true);

        } else {
            getFellow("btnCreateArea").setVisible(false);
            divClassificationPicker.setVisible(false);
        }
    }

    public void onChange$cbClassificationGroup(Event event) {
        if (mapLayer != null) {
            mapLayer.setClassificationSelection(cbClassificationGroup.getSelectedIndex());

            String baseUri = mapLayer.getBaseUri();
            if (baseUri == null) {
                mapLayer.setBaseUri(mapLayer.getUri());
                int pos = mapLayer.getUri().indexOf("&sld_body=");
                if (pos > 0) {
                    baseUri = mapLayer.getUri().substring(0, pos);
                }
            }
            String layername = mapLayer.getName();
            int n = cbClassificationGroup.getSelectedIndex();
            if (n > 0) {
                try {
                    String sldBodyParam;
                    if (mapLayer.getClassificationObjects() == null) {
                        sldBodyParam = "&sld_body=" + formatSld(URLEncoder.encode(POLYGON_SLD, StringConstants.UTF_8), layername, String.valueOf(n - 1), String.valueOf(n), String.valueOf(n + 1));
                        mapLayer.setUri(baseUri + sldBodyParam);
                    } else {
                        JSONObject obj = null;
                        JSONParser jp = new JSONParser();
                        obj = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/object/" + ((JSONObject) mapLayer.getClassificationObjects().get(n - 1)).get("pid")));
                        String url = obj.get(StringConstants.WMSURL).toString();

                        mapLayer.setUri(url);
                    }

                } catch (Exception e) {
                    LOGGER.error("error encoding this to UTF-8: " + POLYGON_SLD, e);
                }
            } else {
                mapLayer.setUri(baseUri);
            }
            getMapComposer().reloadMapLayerNowAndIndexes(mapLayer);
        }
    }

    private String formatSld(String sld, String layername, String minMinusOne, String min, String maxPlusOne) {
        return sld.replace(SUB_LAYERNAME, layername).replace(SUB_MIN_MINUS_ONE, minMinusOne).replace(SUB_MIN, min).replace(SUB_MAX_PLUS_ONE, maxPlusOne);
    }

    public Integer getClassificationGroupCount(String pid) {
        Integer i = 0;
        String url = CommonData.getSatServer() + "/output/aloc/" + pid + "/classification_means.csv";
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            String[] s = slist.split("\n");
            i = s.length - 1;
        } catch (Exception e) {
            LOGGER.error("error getting classification group counts:" + url, e);
        }
        return i;
    }

    public void onClick$btnCreateArea(Event event) {
        int n = cbClassificationGroup.getSelectedIndex();
        if (n > 0) {
            JSONParser jp = new JSONParser();
            JSONObject obj = null;
            String pid = ((JSONObject) mapLayer.getClassificationObjects().get(n - 1)).get("pid").toString();
            try {
                obj = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/object/" + pid));
            } catch (ParseException e) {
                LOGGER.error("failed to parse for object: " + pid);
            }

            String url = obj.get(StringConstants.WMSURL).toString();
            String name = obj.get(StringConstants.NAME).toString();

            MapLayer ml = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(name), name, url, 0.6f, /*metadata url*/ null,
                    null, LayerUtilitiesImpl.WKT, null, null);

            String lname = ml.getName();

            //add colour!
            int colour = Util.nextColour();
            int r = (colour >> 16) & 0x000000ff;
            int g = (colour >> 8) & 0x000000ff;
            int b = (colour) & 0x000000ff;

            ml.setRedVal(r);
            ml.setGreenVal(g);
            ml.setBlueVal(b);
            ml.setDynamicStyle(true);
            getMapComposer().applyChange(ml);
            getMapComposer().updateLayerControls();

            ml.setPolygonLayer(true);

            Facet facet = null;
            //only get field data if it is an intersected layer (to exclude layers containing points)
            JSONObject layerObj = CommonData.getLayer((String) obj.get(StringConstants.FID));
            if (layerObj != null) {
                facet = Util.getFacetForObject(obj.get(StringConstants.NAME).toString(), (String) obj.get(StringConstants.FID));
            }

            if (facet != null) {
                List<Facet> facets = new ArrayList<Facet>();
                facets.add(facet);
                ml.setFacets(facets);

                ml.setWKT(Util.readUrl(CommonData.getLayersServer() + "/shape/wkt/" + pid));
            } else {
                //no facet = not in Biocache, must use WKT
                ml.setWKT(Util.readUrl(CommonData.getLayersServer() + "/shape/wkt/" + pid));
            }
            MapLayerMetadata md = ml.getMapLayerMetadata();
            String bbString = "";
            try {
                bbString = obj.get(StringConstants.BBOX).toString();
                bbString = bbString.replace(StringConstants.POLYGON + "((", "").replace("))", "").replace(",", " ");
                String[] split = bbString.split(" ");
                List<Double> bbox = new ArrayList<Double>();

                bbox.add(Double.parseDouble(split[0]));
                bbox.add(Double.parseDouble(split[1]));
                bbox.add(Double.parseDouble(split[2]));
                bbox.add(Double.parseDouble(split[3]));

                md.setBbox(bbox);
            } catch (NumberFormatException e) {
                LOGGER.debug("failed to parse: " + bbString, e);
            }
            try {
                md.setMoreInfo(CommonData.getLayersServer() + "/layers/view/more/" + layerObj.get("id").toString());
            } catch (Exception e) {
                LOGGER.error("error setting map layer moreInfo: " + (layerObj != null ? layerObj.toString() : "layerObj is null"), e);
            }
        }
    }

    public void onClick$btnAnimationStart(Event event) {
        //0=month, 1=year
        Integer monthOrYear = 0;

        if ("1".equals(cbAnimationDenomination.getValue()) || "Year".equalsIgnoreCase(cbAnimationDenomination.getValue())) {
            monthOrYear = 1;
        }

        LOGGER.debug("Animation: " + monthOrYear);

        Integer step = 1;
        if (monthOrYear != 0) {
            step = intAnimationStep.getValue();
            if (step < 1) {
                step = 1;
                intAnimationStep.setValue(1);
            }
        }

        Double interval = dblAnimationSeconds.getValue();
        if (interval < 0.2) {
            interval = 0.2;
            dblAnimationSeconds.setValue(0.2);
        }

        mapLayer.setAnimationStep(step);
        mapLayer.setAnimationInterval(interval);

        Integer start = intAnimationYearStart.getValue();
        Integer end = intAnimationYearEnd.getValue();
        String script = "mapFrame.animateStart('" + mapLayer.getNameJS() + "',"
                + monthOrYear + ","
                + interval * 1000 + ","
                + start + ","
                + end + ","
                + step + ");";
        LOGGER.debug("Script: " + script);

        getMapComposer().getOpenLayersJavascript().execute(script);

        btnAnimationStop.setDisabled(false);

    }

    public void selectYearOrMonth() {
        if ("1".equals(cbAnimationDenomination.getValue()) || "Year".equals(cbAnimationDenomination.getValue())) {
            dAnimationStep.setVisible(true);

        } else {
            dAnimationStep.setVisible(false);

        }
    }

    private void updateAnimationDiv() {
        if (dblAnimationSeconds == null) {
            return;
        }

        Query q = mapLayer.getSpeciesQuery();
        if (q instanceof BiocacheQuery) {
            Integer firstYear = mapLayer.getFirstYear();
            Integer lastYear = mapLayer.getLastYear();
            if (firstYear == null) {
                try {
                    LegendObject lo = q.getLegend(StringConstants.OCCURRENCE_YEAR);
                    if (lo != null && lo.getMinMax().length > 0) {
                        firstYear = (int) lo.getMinMax()[0];
                        lastYear = (int) lo.getMinMax()[1];
                        mapLayer.setFirstYear(firstYear);
                        mapLayer.setLastYear(lastYear);
                    }
                } catch (Exception e) {
                    //this will fail if there are no records
                }
            }

            Integer step = mapLayer.getAnimationStep();
            if (step != null) {
                intAnimationStep.setValue(step);
            }

            Double interval = mapLayer.getAnimationInterval();
            if (interval != null) {
                dblAnimationSeconds.setValue(interval);
            }

            if (firstYear != null && firstYear < lastYear) {
                intAnimationYearStart.setValue(firstYear);
                intAnimationYearEnd.setValue(lastYear);
                divAnimation.setVisible(true);
            }
        }
    }

    public void onClick$btnAnimationStop(Event event) {
        String script = "mapFrame.animateStop('" + mapLayer.getNameJS() + "');";
        getMapComposer().getOpenLayersJavascript().execute(script);
        btnAnimationStop.setDisabled(true);
    }
}
