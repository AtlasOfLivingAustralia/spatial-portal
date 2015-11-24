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
import au.org.emii.portal.value.BoundingBox;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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
    private Listbox lbClassificationGroup;
    private Hbox hboxClassificationGroup;
    private Div divClassificationPicker;
    private Div divAnimation;
    private Combobox cbAnimationDenomination;
    private Button btnAnimationStart;
    private Button btnAnimationStop;
    private Intbox intAnimationStep;
    private Intbox intAnimationYearStart;
    private Intbox intAnimationYearEnd;
    private Doublebox dblAnimationSeconds;
    private Div legendImgUriDiv;
    private Button clearSelection;
    private Button createInGroup;
    private Label lblSelectedCount;
    private Set selectedList = new HashSet();

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

        getFellow("btnSearch").addEventListener(StringConstants.ONCLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                List<String[]> legendLinesFiltered = new ArrayList<String[]>();
                String txt = ((Textbox) getFellow("txtSearch")).getValue().toLowerCase();
                if (txt.length() > 0) {
                    Integer groupCount = mapLayer.getClassificationGroupCount();
                    JSONArray groupObjects = mapLayer.getClassificationObjects();
                    List<JSONObject> model = new ArrayList<JSONObject>();
                    for (int i = 0; i < groupCount; i++) {
                        if (((JSONObject) groupObjects.get(i)).get("name").toString().toLowerCase().contains(txt)) {
                            model.add((JSONObject) groupObjects.get(i));
                        }
                    }
                    lbClassificationGroup.setModel(new SimpleListModel(model));
                    lbClassificationGroup.setActivePage(0);
                    ((Button) getFellow("btnClear")).setDisabled(false);
                }
            }
        });

        getFellow("btnClear").addEventListener(StringConstants.ONCLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                ((Textbox) getFellow("txtSearch")).setValue("");
                ((Button) getFellow("btnClear")).setDisabled(true);
                Integer groupCount = mapLayer.getClassificationGroupCount();
                JSONArray groupObjects = mapLayer.getClassificationObjects();
                List<JSONObject> model = new ArrayList<JSONObject>();
                for (int i = 0; i < groupCount; i++) {
                    model.add((JSONObject) groupObjects.get(i));
                }
                lbClassificationGroup.setModel(new SimpleListModel(model));
                lbClassificationGroup.setActivePage(0);
            }
        });

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
                    legendImgUriDiv.setVisible(false);
                    legendLabel.setVisible(true);
                } else {
                    legendImgUri.setSrc(legendUri);
                    legendImgUriDiv.setVisible(true);
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
                legendImgUriDiv.setVisible(true);
                legendHtml.setVisible(false);
                legendLabel.setVisible(false);
                legendImg.setVisible(false);

                //if it is an envelope or layerdb object table layer (i.e. polygon layer)
                //the colour choose must be visible, wms legend off and colour combobox not visible
                colourChooser.setVisible(mapLayer.isPolygonLayer());
                if (mapLayer.isPolygonLayer()) {
                    cbColour.setVisible(false);
                    legendImgUriDiv.setVisible(false);
                }

                sizeChooser.setVisible(false);
                uncertainty.setVisible(false);
            } else {
                //image layer?
                legendImgUriDiv.setVisible(false);
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
        if (mapLayer.isPolygonLayer()) return;

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

            cbClassificationGroup.setVisible(true);
            lbClassificationGroup.setVisible(false);
            hboxClassificationGroup.setVisible(false);

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

            lbClassificationGroup.setItemRenderer(new ListitemRenderer<JSONObject>() {
                @Override
                public void render(Listitem item, JSONObject data, int index) throws Exception {
                    Checkbox cb = new Checkbox();
                    final int idx = index;
                    cb.addEventListener("onCheck", new EventListener<Event>() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            if (mapLayer != null) {
                                lbClassificationGroup.setMultiple(true);

                                String v = ((Listcell) event.getTarget().getParent().getParent().getChildren().get(1)).getLabel();
                                if (((CheckEvent) event).isChecked()) {
                                    selectedList.add(v);
                                } else {
                                    selectedList.remove(v);
                                }

                                lblSelectedCount.setValue(selectedList.size() + " checked");

                                getFellow("clearSelection").setVisible(selectedList.size() > 0);
                                getFellow("createInGroup").setVisible(selectedList.size() > 0);

                                highlightSelect(idx);
                            }
                        }
                    });
                    determineCheckboxState(cb, data.get("name").toString());

                    Listcell lc;
                    lc = new Listcell();
                    cb.setParent(lc);
                    lc.setParent(item);

                    lc = new Listcell(data.get("name").toString());
                    lc.setParent(item);

                    lc = new Listcell();
                    Image img = new Image();
                    img.setTooltip("Create as an area layer");
                    img.setClass("icon-plus-sign");
                    img.setParent(lc);
                    lc.setParent(item);
                    final JSONObject j = data;
                    img.addEventListener("onClick", new EventListener<Event>() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            createAreaEcho(j.get("pid").toString());
                        }
                    });

                    lc = new Listcell();
                    img = new Image();
                    img.setTooltip("Zoom to area");
                    img.setClass("icon-zoom-in");
                    img.setParent(lc);
                    lc.setParent(item);
                    img.addEventListener("onClick", new EventListener<Event>() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            List<Double> b = Util.getBoundingBox(j.get("bbox").toString());
                            BoundingBox bbox = new BoundingBox();
                            bbox.setMinLongitude(b.get(0).floatValue());
                            bbox.setMinLatitude(b.get(1).floatValue());
                            bbox.setMaxLongitude(b.get(2).floatValue());
                            bbox.setMaxLatitude(b.get(3).floatValue());
                            getMapComposer().getOpenLayersJavascript().execute(getMapComposer().getOpenLayersJavascript().zoomToBoundingBox(
                                    bbox, false
                            ));
                        }
                    });
                }
            });

            lbClassificationGroup.addEventListener("onSelect", new EventListener<Event>() {
                @Override
                public void onEvent(Event event) throws Exception {
                    if (mapLayer != null) {
                        highlightSelect(lbClassificationGroup.getSelectedIndex());
                    }
                }
            });

            List<JSONObject> model = new ArrayList<JSONObject>();
            for (int i = 0; i < groupCount; i++) {
                model.add((JSONObject) groupObjects.get(i));
            }
            lbClassificationGroup.setModel(new SimpleListModel(model));

            //is there a current selection?
            Integer groupSelection = mapLayer.getClassificationSelection();
            if (groupSelection == null) {
                groupSelection = 0;
                mapLayer.setClassificationSelection(groupSelection);
            }

            getFellow("btnCreateArea").setVisible(true);

            getFellow("btnCreateArea").setVisible(false);
            cbClassificationGroup.setVisible(false);
            lbClassificationGroup.setVisible(true);
            hboxClassificationGroup.setVisible(true);
        } else {
            getFellow("btnCreateArea").setVisible(false);
            divClassificationPicker.setVisible(false);
        }
    }

    private void determineCheckboxState(Checkbox cb, String name) {
//        Integer groupCount = mapLayer.getClassificationGroupCount();
//        JSONArray groupObjects = mapLayer.getClassificationObjects();
//        for (int i = 0; i < groupCount; i++) {
//            if (name.equals(((JSONObject) groupObjects.get(i)).get("name"))) {
//                highlightSelect(i);
//                break;
//            }
//        }
        cb.setChecked(selectedList.contains(name));
    }

    private void createAreaEcho(String pid) {
        Events.echoEvent("createArea", this, pid);
    }

    private void highlightSelect(int idx) {
        //translate list idx to pid
        if (idx == -1) {
            //no selection
            idx = 0;
        } else {
            JSONObject jo = (JSONObject) lbClassificationGroup.getModel().getElementAt(idx);
            for (int i = 0; i < mapLayer.getClassificationObjects().size(); i++) {
                if (((JSONObject) mapLayer.getClassificationObjects().get(i)).get("id").equals(jo.get("id").toString())) {
                    idx = i + 1;
                    break;
                }
            }
        }

        mapLayer.setClassificationSelection(idx);

        String baseUri = mapLayer.getBaseUri();
        if (baseUri == null) {
            mapLayer.setBaseUri(mapLayer.getUri());
            int pos = mapLayer.getUri().indexOf("&sld_body=");
            if (pos > 0) {
                int pos2 = mapLayer.getUri().indexOf("&", pos + 1);
                if (pos2 > 0) {
                    baseUri = mapLayer.getUri().substring(0, pos) + mapLayer.getUri().substring(pos2);
                } else {
                    baseUri = mapLayer.getUri().substring(0, pos);
                }
            }
        }
        String layername = mapLayer.getName();
        int n = idx;
        if (n > 0) {
            try {
                String sldBodyParam;
                if (mapLayer.getClassificationObjects() == null) {
                    sldBodyParam = "&sld_body=" + formatSld(URLEncoder.encode(POLYGON_SLD, StringConstants.UTF_8), layername, String.valueOf(n - 1), String.valueOf(n), String.valueOf(n + 1));
                    mapLayer.setUri(baseUri + sldBodyParam);
                } else {
                    //use a child layer
                    if (mapLayer.getChildCount() > 0) {
                        MapLayer child = mapLayer.getChild(0);
                        getMapComposer().deactiveLayer(child, false, true);
                        mapLayer.getChildren().remove(0);
                    }

                    MapLayer mlHighlight = (MapLayer) mapLayer.clone();
                    mlHighlight.setName(mapLayer.getName() + "_selection");

                    JSONParser jp = new JSONParser();
                    JSONObject obj = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/object/" + ((JSONObject) mapLayer.getClassificationObjects().get(n - 1)).get("pid")));
                    String url = obj.get(StringConstants.WMSURL).toString();
                    mlHighlight.setUri(url);

                    mapLayer.addChild(mlHighlight);
                }

            } catch (Exception e) {
                LOGGER.error("error encoding this to UTF-8: " + POLYGON_SLD, e);
            }
        } else {
            if (mapLayer.getChildCount() > 0) {
                MapLayer child = mapLayer.getChild(0);
                child.setUri(mapLayer.getUri());
                getMapComposer().deactiveLayer(child, false, true);
                getMapComposer().reloadMapLayerNowAndIndexes(mapLayer);
                mapLayer.getChildren().remove(0);
            }
        }
        getMapComposer().reloadMapLayerNowAndIndexes(mapLayer);
    }

    public void onSelect$cbClassificationGroup(Event event) {
        if (mapLayer != null) {
            highlightSelect(cbClassificationGroup.getSelectedIndex());
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

    public void onClick$clearSelection(Event event) {
        for (Listitem li : lbClassificationGroup.getItems()) {
            if (!li.getFirstChild().getChildren().isEmpty()
                    && ((Checkbox) li.getFirstChild().getFirstChild()).isChecked()) {
                ((Checkbox) li.getFirstChild().getFirstChild()).setChecked(false);
            }
        }

        selectedList = new HashSet();
        lblSelectedCount.setValue(selectedList.size() + " checked");
    }

    public void onClick$createInGroup(Event e) {

        StringBuilder sb = new StringBuilder();

        String layer = ((JSONObject) mapLayer.getClassificationObjects().get(0)).get("fid").toString();
        //build facet
        for (Object n : selectedList) {
            if (sb.length() > 0) {
                sb.append(" OR ");
            }
            sb.append(layer).append(":\"").append(n).append("\"");
        }

        //get pids
        String pids = "";
        String anyPid = "";
        Integer groupCount = mapLayer.getClassificationGroupCount();
        JSONArray groupObjects = mapLayer.getClassificationObjects();
        for (Object name : selectedList) {
            for (int i = 0; i < groupCount; i++) {
                if (name.equals(((JSONObject) groupObjects.get(i)).get("name"))) {
                    if (!pids.isEmpty()) {
                        pids += "~";
                    }
                    anyPid = ((JSONObject) groupObjects.get(i)).get("pid").toString();
                    pids += anyPid;
                    break;
                }
            }
        }

        JSONParser jp = new JSONParser();
        JSONObject obj = null;

        Double[] bbox = new Double[4];
        boolean firstPid = true;
        Map<Integer, String> urlParts = new HashMap<Integer, String>();
        try {
            obj = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/object/" + anyPid));

            for (String p : pids.split("~")) {
                JSONObject jo = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/object/" + p));

                String bbString = jo.get(StringConstants.BBOX).toString();
                bbString = bbString.replace(StringConstants.POLYGON + "((", "").replace("))", "").replace(",", " ");
                String[] split = bbString.split(" ");

                String u = jo.get(StringConstants.WMSURL).toString();
                if (!u.contains("viewparams")) {
                    //grid as contextual

                    //extract colour map entries
                    String s = u.split("ColorMap%3E")[1];
                    int pos = 0;
                    for (String c : s.split("%3CColorMapEntry")) {
                        int start = c.indexOf("quantity") + 14;
                        if (start > 14) {
                            Integer qty = Integer.parseInt(c.substring(start, c.indexOf("%", start)));
                            //do no overwrite existing quantities if this is the first or last entry
                            if (pos == 1 || pos == 2 || !urlParts.containsKey(qty))
                                urlParts.put(qty, c.replace("%3C%2F", ""));
                        }
                    }
                }

                if (firstPid || Double.parseDouble(split[0]) < bbox[0]) bbox[0] = Double.parseDouble(split[0]);
                if (firstPid || Double.parseDouble(split[1]) < bbox[1]) bbox[1] = Double.parseDouble(split[1]);
                if (firstPid || Double.parseDouble(split[4]) > bbox[2]) bbox[2] = Double.parseDouble(split[4]);
                if (firstPid || Double.parseDouble(split[5]) > bbox[3]) bbox[3] = Double.parseDouble(split[5]);
                firstPid = false;
            }
        } catch (ParseException er) {
            LOGGER.error("failed to parse for object: " + anyPid);
        }
        String bboxString = "POLYGON((" + bbox[0] + " " + bbox[1] + "," + bbox[0] + " " + bbox[3] + ","
                + bbox[2] + " " + bbox[3] + "," + bbox[2] + " " + bbox[1] + ","
                + bbox[0] + " " + bbox[1] + "))";

        String url = obj.get(StringConstants.WMSURL).toString();
        if (!url.contains("s:" + anyPid)) {
            //grid as contextual layer
            String[] split = url.split("ColorMap%3E");
            String colours = "";
            List<Integer> sorted = new ArrayList<Integer>(urlParts.keySet());
            Collections.sort(sorted);
            for (int i = 0; i < sorted.size(); i++) {
                colours += "%3CColorMapEntry" + urlParts.get(sorted.get(i));
            }
            colours += "%3C%2F";

            url = split[0] + "ColorMap%3E" + colours + "ColorMap%3E" + split[2];
        } else {
            url = url.replace("s:" + anyPid, "s:" + pids);
        }

        String name = selectedList.size() + " areas: " + mapLayer.getDisplayName();

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
        ml.setPolygonLayer(true);

        Facet facet = Facet.parseFacet(sb.toString());
        //only get field data if it is an intersected layer (to exclude layers containing points)
        JSONObject layerObj = CommonData.getLayer((String) obj.get(StringConstants.FID));

        List<Facet> facets = new ArrayList<Facet>();
        facets.add(facet);
        ml.setFacets(facets);

        ml.setWKT(bboxString);

        MapLayerMetadata md = ml.getMapLayerMetadata();
        md.setBbox(Arrays.asList(bbox));

        try {
            md.setMoreInfo(CommonData.getLayersServer() + "/layers/view/more/" + layerObj.get("id").toString());
        } catch (Exception er) {
            LOGGER.error("error setting map layer moreInfo: " + (layerObj != null ? layerObj.toString() : "layerObj is null"), er);
        }

        getMapComposer().applyChange(ml);
        getMapComposer().updateLayerControls();
        getMapComposer().reloadMapLayerNowAndIndexes(ml);
    }

    public void onClick$btnClearAreaSelection(Event event) {
        lbClassificationGroup.clearSelection();
        highlightSelect(-1);
    }

    public void createArea(Event event) {
        String pid = event.getData().toString();

        getMapComposer().addObjectByPid(pid, null, 1);
    }

    public void onClick$btnCreateArea(Event event) {
        int n = cbClassificationGroup.getSelectedIndex();
        if (n > 0) {
            String pid = ((JSONObject) mapLayer.getClassificationObjects().get(n - 1)).get("pid").toString();
            createAreaEcho(pid);
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

    public void onClick$btnPopupLegend(Event event) {
        //return if already open
        List<Component> list = getRoot().getChildren();
        for (Component c : getRoot().getChildren()) {
            if (c instanceof Popup) {
                Image img = (Image) ((Popup) c).getFellowIfAny("img", true);
                if (img != null && img.getSrc().equalsIgnoreCase(mapLayer.getCurrentLegendUri())) {
                    return;
                }
            }
        }
        
        Popup layerWindow = (Popup) Executions.createComponents("WEB-INF/zul/legend/Popup.zul", getRoot(), null);

        try {
            layerWindow.doOverlapped();
            layerWindow.setPosition("right,center");

            layerWindow.init(mapLayer.getDisplayName(), mapLayer.getCurrentLegendUri());
        } catch (Exception e) {
            LOGGER.error("failed to open popup layer legend", e);
        }
    }
}
