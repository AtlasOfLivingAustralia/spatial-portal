package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.layer.EnvLayersCombobox;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import net.sf.json.JSONObject;
import org.ala.layers.legend.Facet;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class AreaEnvironmentalEnvelope extends AreaToolComposer {

    public static final String LAYER_PREFIX = "working envelope: ";
    private static final Logger LOGGER = Logger.getLogger(AreaEnvironmentalEnvelope.class);
    private static final long serialVersionUID = -26560838825366347L;
    private static final String[] FILTER_COLOURS = {"0x0000FF", "0x000FF00", "0x00FFFF", "0xFF00FF" };
    private Div popupContinous;
    private Slider popupSliderMin;
    private Slider popupSliderMax;
    private Label popupRange;
    private Doublebox popupMinimum;
    private Doublebox popupMaximum;
    private Textbox popupIdx;
    private Button removeContinous;
    private Label labelContinous;
    private Button applyContinous;
    private Listbox lbSelLayers;
    private EnvLayersCombobox cbEnvLayers;
    private Button filterDone;
    /**
     * for functions in popup box
     */
    private SPLFilter popupFilter;
    private Listcell popupCell;
    private Listitem popupItem;
    private Textbox txtLayerName;
    private int speciescount = 0;
    private boolean isDirtyCount = true;
    //set to true and calling 'remove all layers' will end by closing the popup.
    private boolean cancelling = false;
    private List<JSONObject> selectedLayers;
    private Map<String, SPLFilter> selectedSPLFilterLayers;
    private MapComposer mc;
    private String activeAreaSize = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        //get the current MapComposer instance
        mc = getMapComposer();

        selectedLayers = new ArrayList<JSONObject>();
        selectedSPLFilterLayers = new HashMap<String, SPLFilter>();

        txtLayerName.setValue(getMapComposer().getNextAreaLayerName(CommonData.lang(StringConstants.DEFAULT_AREA_LAYER_NAME)));

        cbEnvLayers.setIncludeLayers(StringConstants.ENVIRONMENTAL);
    }

    public String getAreaSize() {
        if (activeAreaSize != null) {
            return activeAreaSize;
        } else {
            return null;
        }
    }

    public void onChange$cbEnvLayers(Event event) {
        applyFilter(true);
    }

    public void onChange$popupMinimum(Event event) {
        isDirtyCount = true;
        LOGGER.debug("popupMinimum=" + popupMinimum.getValue() + " " + event.getData());
        popupFilter.setMinimumValue(popupMinimum.getValue());
        serverFilter();

    }

    public void onChange$popupMaximum(Event event) {
        isDirtyCount = true;
        popupFilter.setMaximumValue(popupMaximum.getValue());
        serverFilter();
    }

    public void doAdd(String newValue) {
        String nv = newValue;
        try {
            JSONObject layer = null;
            if (nv.length() == 0
                    && cbEnvLayers.getItemCount() > 0
                    && cbEnvLayers.getSelectedItem() != null) {
                layer = cbEnvLayers.getSelectedItem().getValue();
                nv = layer.getString(StringConstants.NAME);
                cbEnvLayers.setValue("");
            }
            if (nv.isEmpty()) {
                return;
            }

            if (!selectedLayers.contains(layer)) {
                selectedLayers.add(layer);
            }

            /* apply something to line onclick in lb */
            lbSelLayers.setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data, int itemIdx) {
                    JSONObject layer = (JSONObject) data;
                    SPLFilter f = getSPLFilter(layer);

                    // Col 1: Add the layer name
                    Listcell lname = new Listcell(f.getLayer().getString(StringConstants.DISPLAYNAME));
                    lname.setStyle("white-space: normal;");
                    lname.setParent(li);

                    // Col 2: Add the filter string and set the onClick event
                    String filterString = f.getFilterString();
                    Listcell lc = new Listcell(filterString);
                    lc.setStyle("white-space: normal;");
                    lc.setParent(li);

                    // Col 3: Add the species count and set the onClick event
                    Listcell count = new Listcell(String.valueOf(f.getCount()));
                    count.setStyle("text-align: right; ");
                    count.setParent(li);

                }
            });

            lbSelLayers.setModel(new SimpleListModel(selectedLayers));
            Listitem li = lbSelLayers.getItemAtIndex(lbSelLayers.getItemCount() - 1);

            Listcell lc = (Listcell) li.getFirstChild();
            lc = (Listcell) lc.getNextSibling();

            listFix();

            showAdjustPopup(layer, lc, li);

            filterDone.setDisabled(false);
        } catch (Exception e) {
            LOGGER.error("error adding layer to EE : " + nv, e);
        }

        //reset active area size
        activeAreaSize = null;
    }

    private SPLFilter getSPLFilter(JSONObject layer) {
        SPLFilter splf = selectedSPLFilterLayers.get(layer.getString(StringConstants.NAME));

        // if splf is still null, then it must be new
        // so grab the details from the server
        if (splf == null) {
            splf = new SPLFilter();
            splf.setCount(0);
            splf.setLayername(layer.getString(StringConstants.NAME));
            splf.setLayer(layer);
            splf.setMinimumValue(layer.getDouble("environmentalvaluemin"));
            splf.setMaximumValue(layer.getDouble("environmentalvaluemax"));
            splf.setMinimumInitial(layer.getDouble("environmentalvaluemin"));
            splf.setMaximumInitial(layer.getDouble("environmentalvaluemax"));
            splf.setChanged(false);

            selectedSPLFilterLayers.put(layer.getString(StringConstants.NAME), splf);
        }

        return splf;
    }

    public void deleteSelectedFilters(Object o) {
        Listitem li;
        JSONObject layer;

        popupContinous.setVisible(false);

        if (o == null) {
            //change to get last item
            int count = lbSelLayers.getItemCount();
            li = lbSelLayers.getItemAtIndex(count - 1);
        } else {
            li = (Listitem) ((Listcell) o).getParent();
        }
        int idx = li.getIndex();

        layer = selectedLayers.get(idx);

        //not executing, echo
        Events.echoEvent("removeLayer", this, layer.getString(StringConstants.NAME));

        selectedLayers.remove(layer);

        li.detach();

        showAdjustPopup(null);

        filterDone.setDisabled(selectedLayers.isEmpty());

        listFix();

        //reset active area size
        activeAreaSize = null;
    }

    public void onClick$btnClearSelection(Event event) {
        Listitem li;
        JSONObject layer;

        popupContinous.setVisible(false);

        filterDone.setDisabled(true);

        //change to get last item
        int count = lbSelLayers.getItemCount();
        if (count == 0) {
            return;
        }
        li = lbSelLayers.getItemAtIndex(count - 1);

        int idx = li.getIndex();

        layer = selectedLayers.get(idx);

        //not executing, echo
        Events.echoEvent("removeLayerClearSelected", this, layer.getString(StringConstants.DISPLAYNAME));

        selectedLayers.remove(layer);

        li.detach();

        if (selectedLayers.isEmpty()) {
            showAdjustPopup(null);
            listFix();
        }
    }

    public void removeLayerClearSelected(Event event) {
        String layername = (String) event.getData();
        if (!selectedLayers.isEmpty()) {
            Events.echoEvent("onClick$btnClearSelection", this, null);
        }
        if (mc.getMapLayer(LAYER_PREFIX + layername) != null) {
            mc.removeLayer(LAYER_PREFIX + layername);
        }
    }

    public void removeLayers(Event event) {
        String all = (String) event.getData();
        String layername = all;
        int p = layername.indexOf('|');
        if (p > 0) {
            Events.echoEvent(StringConstants.REMOVE_LAYERS, this, layername.substring(p + 1));
            layername = layername.substring(0, p);
        }
        if (mc.getMapLayer(LAYER_PREFIX + layername) != null) {
            mc.removeLayer(LAYER_PREFIX + layername);
        }
        if (p <= 0) {
            detach();
        }
    }

    public void removeLayer(Event event) {
        String layername = (String) event.getData();
        if (mc.getMapLayer(LAYER_PREFIX + layername) != null) {
            mc.removeLayer(LAYER_PREFIX + layername);
        }
    }

    public void listFix() {
        int i;
        List<Listitem> list = lbSelLayers.getItems();
        for (i = 0; list != null && i < list.size() - 1; i++) {
            Listitem li = list.get(i);
            li.setDisabled(true);
        }
        if (list != null && !list.isEmpty()) {
            Listitem li = list.get(i);
            li.setDisabled(false);
        }
    }

    public void onScroll$popupSliderMin(Event event) {
        LOGGER.debug("Changing min slider");
        try {

            int curpos = popupSliderMin.getCurpos();

            double range = popupFilter.getMaximumInitial() - popupFilter.getMinimumInitial();

            popupMinimum.setValue((float) (curpos / 100.0 * range + popupFilter.getMinimumInitial()));

            popupFilter.setMinimumValue(popupMinimum.getValue());

            ((Listcell) popupItem.getChildren().get(1)).setLabel(popupFilter.getFilterString());

            serverFilter();

        } catch (Exception e) {
            LOGGER.error("slider change min", e);
        }
    }

    public void onScroll$popupSliderMax(Event event) {
        LOGGER.debug("Changing max slider");
        try {

            int curpos = popupSliderMax.getCurpos();

            double range = popupFilter.getMaximumInitial() - popupFilter.getMinimumInitial();

            popupMaximum.setValue((float) (curpos / 100.0 * range + popupFilter.getMinimumInitial()));

            popupFilter.setMaximumValue(popupMaximum.getValue());

            ((Listcell) popupItem.getChildren().get(1)).setLabel(popupFilter.getFilterString());

            serverFilter();
        } catch (Exception e) {
            LOGGER.error("slider change max", e);
        }
    }

    private void showAdjustPopup(Object obj) {
        Object o = obj;
        if (o == null) {

            //get the Count cell
            int i = lbSelLayers.getItemCount();
            if (i < 1) {
                popupContinous.setVisible(false);
                return;
            }

            Listitem li = lbSelLayers.getItemAtIndex(i - 1);
            List list = li.getChildren();

            o = list.get(list.size() - 2);
        }

        Listcell lc = (Listcell) o;
        Listitem li = (Listitem) lc.getParent();
        JSONObject layer = li.getValue();

        popupFilter = getSPLFilter(layer);
        popupIdx.setValue(layer.getString(StringConstants.DISPLAYNAME));

        popupCell = lc;
        popupItem = li;

        labelContinous.setValue("edit envelope for: " + layer.getString(StringConstants.DISPLAYNAME));
        String csv = String.format("%.4f", (float) popupFilter.getMinimumInitial()) + " - " + String.format("%.4f", (float) popupFilter.getMaximumInitial());
        popupRange.setValue(csv);

        popupMinimum.setValue((float) (popupFilter.getMinimumValue()));
        popupMaximum.setValue((float) (popupFilter.getMaximumValue()));

        double range = popupFilter.getMaximumInitial() - popupFilter.getMinimumInitial();
        int maxcursor = (int) ((popupFilter.getMinimumValue() - popupFilter.getMinimumInitial())
                / (range) * 100);
        int mincursor = (int) ((popupFilter.getMinimumValue() - popupFilter.getMinimumInitial())
                / (range) * 100);

        doApplyFilter(layer, Double.toString(popupFilter.getMinimumValue()), Double.toString(popupFilter.getMaximumValue()));

        popupSliderMin.setCurpos(mincursor);
        popupSliderMax.setCurpos(maxcursor);

        lc.focus();
        LOGGER.debug("attaching: " + lc + lc.getValue());
        popupContinous.setVisible(true);

    }

    private void showAdjustPopup(JSONObject layer, Listcell lc, Listitem li) {
        popupFilter = getSPLFilter(layer);
        popupIdx.setValue(layer.getString(StringConstants.DISPLAYNAME));

        popupCell = lc;
        popupItem = li;

        labelContinous.setValue("edit envelope for: " + layer.getString(StringConstants.DISPLAYNAME));

        String csv = String.format("%.4f", (float) popupFilter.getMinimumValue()) + " - " + String.format("%.4f", (float) popupFilter.getMaximumValue());
        popupRange.setValue(csv);

        popupMinimum.setValue((float) popupFilter.getMinimumValue());
        popupMaximum.setValue((float) popupFilter.getMaximumValue());

        double range = popupFilter.getMaximumValue() - popupFilter.getMinimumValue();
        int maxcursor = (int) ((popupFilter.getMaximumValue() - popupFilter.getMinimumValue())
                / (range) * 100);
        int mincursor = (int) ((popupFilter.getMinimumValue() - popupFilter.getMinimumValue())
                / (range) * 100);

        doApplyFilter(layer, Double.toString(popupFilter.getMinimumValue()), Double.toString(popupFilter.getMaximumValue()));

        popupSliderMin.setCurpos(mincursor);
        popupSliderMax.setCurpos(maxcursor);

        popupContinous.setVisible(true);
    }

    private void serverFilter() {
        doApplyFilter(popupFilter.getLayer(), Double.toString(popupFilter.getMinimumValue()), Double.toString(popupFilter.getMaximumValue()));
    }

    private void doApplyFilter(JSONObject layer, String val1, String val2) {
        if (layer == null) {
            LOGGER.error("error adding new filtered layer=null, min/max: " + val1 + "/" + val2);
        } else {
            loadMap(layer, lbSelLayers.getItemCount() - 1, Double.parseDouble(val1), Double.parseDouble(val2), false);
        }
    }

    public void onClick$filterDone(Event event) {
        ok = true;

        try {
            //create the layer
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.getSatServer()).append("/ws/envelope?area=").append(getWkt());
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(sbProcessUrl.toString());
            LOGGER.debug(sbProcessUrl.toString());

            post.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(post);
            String slist = post.getResponseBodyAsString();
            String[] list = slist.split("\n");
            String pid = list[0];
            String url = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:envelope_" + pid + "&FORMAT=image%2Fpng";
            String activeAreaExtent = list[1];
            activeAreaSize = list[2];

            //load the layer
            MapLayer ml = mc.addWMSLayer(pid, txtLayerName.getText(), url, 0.75f, null, null, LayerUtilitiesImpl.ENVIRONMENTAL_ENVELOPE, null, null);

            //add colour!
            ml.setRedVal(255);
            ml.setGreenVal(0);
            ml.setBlueVal(0);
            ml.setDynamicStyle(true);
            ml.setPolygonLayer(true);
            getMapComposer().updateLayerControls();

            //make the metadata?
            StringBuilder sb = new StringBuilder();
            StringBuilder sbLayerList = new StringBuilder();
            sb.append("Environmental Envelope<br>");
            for (int i = 0; i < selectedLayers.size(); i++) {
                JSONObject layer = selectedLayers.get(i);
                SPLFilter f = getSPLFilter(layer);
                sb.append(f.getLayername()).append(": ").append(f.getFilterString()).append("<br>");

                sbLayerList.append(f.getLayername());
                if (i < selectedLayers.size() - 1) {
                    sbLayerList.append(":");
                }
            }
            String activeAreaMetadata = LayersUtil.getMetadata(sb.toString());

            getMapComposer().setAttribute("activeLayerName", sbLayerList.toString());
            getMapComposer().setAttribute("mappolygonlayer", sb.toString());

            String finalWkt = null;
            try {
                finalWkt = getWkt();
            } catch (Exception e) {
                LOGGER.error("failed to get WKT", e);
            }

            this.layerName = ml.getName();
            List<Double> bb = new ArrayList<Double>(4);
            String[] bs = activeAreaExtent.split(",");
            for (int i = 0; i < 4; i++) {
                bb.add(Double.parseDouble(bs[i]));
            }
            ml.getMapLayerMetadata().setBbox(bb);
            ml.getMapLayerMetadata().setMoreInfo(activeAreaMetadata);

            ml.setWKT(StringConstants.ENVELOPE + "(" + finalWkt + ")");
            //not the actual WKT
            ml.setEnvelope(finalWkt);
            try {
                double area = Double.parseDouble(activeAreaSize.replace(",", ""));
                activeAreaSize = String.format("%,.2f", area);
            } catch (NumberFormatException e) {
                LOGGER.error("failed to parse environmental envelope area for: " + activeAreaSize);
            }
            ml.setAreaSqKm(activeAreaSize);
            ml.setFacets(getFacets());

            //this also shows active area
            removeAllSelectedLayers(true);

        } catch (Exception e) {
            LOGGER.error("unable to create envelope layer: ", e);
        }

        //do detach after adding the new layer
        mc.updateLayerControls();
    }

    public void onClick$applyContinous(Event event) {
        applyFilter();
        isDirtyCount = false;
    }

    public void onClick$removeContinous(Event event) {
        deleteSelectedFilters(null);
        isDirtyCount = false;
    }

    private int getSpeciesCount() {
        Query q = new BiocacheQuery(null, null, null, getFacets(), false, null);

        return q.getSpeciesCount();
    }

    public void onLater(Event event) throws Exception {
        applyFilterEvented();

        speciescount = getSpeciesCount();
        popupFilter.setCount(speciescount);
        ((Listcell) popupItem.getChildren().get(2)).setLabel(speciescount + "");

        Clients.clearBusy();
    }

    public void onLateron(Event event) throws Exception {
        applyFilterEvented();
        doAdd("");
    }

    public void applyFilter() {
        if (lbSelLayers.getItemCount() == 0) {
            return;
        }

        Clients.showBusy(CommonData.lang("updating_count"));
        Events.echoEvent("onLater", this, null);
    }

    public void applyFilter(boolean doAdd) {
        if (lbSelLayers.getItemCount() == 0) {

            if (doAdd) {
                doAdd("");
            }

            return;
        }

        if (doAdd) {
            Events.echoEvent("onLateron", this, null);
        } else {
            applyFilter();
        }
    }

    private void applyFilterEvented() {
        popupFilter.setMinimumValue(popupMinimum.getValue());
        popupFilter.setMaximumValue(popupMaximum.getValue());

        ((Listcell) popupItem.getChildren().get(1)).setLabel(popupFilter.getFilterString());

        serverFilter();

        String strCount = speciescount + "";
        if (isDirtyCount) {
            Clients.showBusy(CommonData.lang("updating_count"));
            strCount = getSpeciesCount() + "";
            Clients.clearBusy();
        }

        try {
            popupFilter.setCount(Integer.parseInt(strCount.split("\n")[0]));
            ((Listcell) popupItem.getChildren().get(2)).setLabel(strCount.split("\n")[0]);

        } catch (NumberFormatException e) {
            LOGGER.error("error parsing species count from:" + strCount, e);
        }
    }

    private MapLayer loadMap(JSONObject layer, int depth, double min, double max, boolean finalLayer) {

        String colour = finalLayer ? "0xFF0000" : FILTER_COLOURS[depth % FILTER_COLOURS.length];
        String filter
                = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\">"
                + "<NamedLayer><Name>ALA:" + layer.getString(StringConstants.NAME) + "</Name>"
                + "<UserStyle><FeatureTypeStyle><Rule><RasterSymbolizer><Geometry></Geometry>"
                + "<ColorMap>"
                + "<ColorMapEntry color=\"" + colour + "\" opacity=\"1\" quantity=\"" + (min - Math.abs(min * 0.0000001)) + "\"/>"
                + "<ColorMapEntry color=\"" + colour + "\" opacity=\"0\" quantity=\"" + min + "\"/>"
                + "<ColorMapEntry color=\"" + colour + "\" opacity=\"0\" quantity=\"" + max + "\"/>"
                + "<ColorMapEntry color=\"" + colour + "\" opacity=\"1\" quantity=\"" + (max + Math.abs(max * 0.0000001)) + "\"/>"
                + "</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";

        try {
            filter = URLEncoder.encode(filter, StringConstants.UTF_8);
        } catch (Exception e) {
            LOGGER.error("cannot encode filter sld: " + filter, e);
        }

        MapLayer ml = mc.getMapLayer(LAYER_PREFIX + layer.getString(StringConstants.NAME));
        if (ml == null) {
            ml = getMapComposer().addWMSLayer(LAYER_PREFIX + layer.getString(StringConstants.NAME),
                    LAYER_PREFIX + layer.getString(StringConstants.DISPLAYNAME),
                    layer.getString("displaypath").replace("/gwc/service", "") + "&sld_body=" + filter,
                    (float) 0.75,
                    CommonData.getLayersServer() + "/layers/view/more/" + layer.getString(StringConstants.ID),
                    CommonData.getGeoServer() + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + layer.getString(StringConstants.NAME),
                    LayerUtilitiesImpl.ENVIRONMENTAL_ENVELOPE,
                    null, null, null);
        } else {
            ml.setUri(layer.getString("displaypath").replace("/gwc/service", "") + "&sld_body=" + filter);
            mc.reloadMapLayerNowAndIndexes(ml);
        }

        return ml;
    }

    List<Facet> getFacets() {
        List<Facet> facets = new ArrayList<Facet>();
        for (int i = 0; i < selectedLayers.size(); i++) {
            SPLFilter splf = selectedSPLFilterLayers.get(selectedLayers.get(i).getString(StringConstants.NAME));
            Facet f = new Facet(CommonData.getLayerFacetName(splf.getLayername()), splf.getMinimumValue(), splf.getMaximumValue(), true);
            facets.add(f);
        }
        return facets;
    }

    String getWkt() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedLayers.size(); i++) {
            SPLFilter splf = selectedSPLFilterLayers.get(selectedLayers.get(i).getString(StringConstants.NAME));
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(CommonData.getLayerFacetName(splf.getLayername())).append(",").append(splf.getMinimumValue()).append(",").append(splf.getMaximumValue());
        }
        return sb.toString();
    }

    /**
     * Remove all selected layers from the map.
     * <p/>
     * For use when disabling Environmental Envelope in Active Layers.
     */
    public void removeAllSelectedLayers(boolean showActiveArea) {
        StringBuilder sb = new StringBuilder();
        if (showActiveArea) {
            sb.append("Active Area");
            if (!selectedLayers.isEmpty()) {
                sb.append("|");
            }
        }
        for (int i = 0; i < selectedLayers.size(); i++) {
            JSONObject layer = selectedLayers.get(i);
            sb.append(layer.getString(StringConstants.NAME));
            if (i < selectedLayers.size() - 1) {
                sb.append("|");
            }
        }
        if (!selectedLayers.isEmpty()) {
            Events.echoEvent(StringConstants.REMOVE_LAYERS, this, sb.toString());
        } else if (cancelling) {
            this.detach();
        }
    }

    public void onClick$btnCancel(Event event) {
        cancelling = true;
        removeAllSelectedLayers(false);
    }
}
