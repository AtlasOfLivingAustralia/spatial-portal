package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.composer.layer.EnvLayersCombobox;
import au.org.ala.spatial.data.BiocacheQuery;
import au.org.ala.spatial.data.Facet;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.LayersUtil;
import au.org.ala.spatial.util.SPLFilter;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

import java.net.URLEncoder;
import java.util.*;

/**
 * @author ajay
 */
public class AreaEnvironmentalEnvelope extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaEnvironmentalEnvelope.class);

    public static final String LAYER_PREFIX = "working envelope: ";
    private static final long serialVersionUID = -26560838825366347L;
    private static final String[] FILTER_COLOURS = {"0x0000FF", "0x000FF00", "0x00FFFF", "0xFF00FF"};
    private EnvLayersCombobox cbEnvLayers;
    private Listbox lbSelLayers;
    public Div popup_continous;
    public Slider popup_slider_min;
    public Slider popup_slider_max;
    public Label popup_range;
    public Doublebox popup_minimum;
    public Doublebox popup_maximum;
    public Textbox popup_idx;
    public Button remove_continous;
    public Button preview_continous;
    public Label label_continous;
    public Button apply_continous;
    String[] results = null;
    private List<JSONObject> selectedLayers;
    private Map<String, SPLFilter> selectedSPLFilterLayers;
    private MapComposer mc;
    LayersUtil layersUtil;
    Button filter_done;
    /**
     * for functions in popup box
     */
    SPLFilter popup_filter;
    Listcell popup_cell;
    Listitem popup_item;
    String activeAreaExtent = null;
    String activeAreaMetadata = null;
    private String activeAreaSize = null;
    Textbox txtLayerName;
    int speciescount = 0;
    boolean isDirtyCount = true;
    String final_wkt = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        //get the current MapComposer instance
        mc = getMapComposer();

        layersUtil = new LayersUtil(mc);

        selectedLayers = new Vector<JSONObject>();
        selectedSPLFilterLayers = new Hashtable<String, SPLFilter>();

        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));

        cbEnvLayers.setIncludeLayers("environmental");
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

    public void onChange$popup_minimum(Event event) {
        isDirtyCount = true;
        logger.debug("popup_minimum=" + popup_minimum.getValue() + " " + event.getData());
        popup_filter.minimum_value = popup_minimum.getValue();
        serverFilter();

    }

    public void onChange$popup_maximum(Event event) {
        isDirtyCount = true;
        popup_filter.maximum_value = popup_maximum.getValue();
        serverFilter();
    }

    public void doAdd(String new_value) {

        try {
            JSONObject layer = null;
            if (new_value.length() == 0) {
                if (cbEnvLayers.getItemCount() > 0 && cbEnvLayers.getSelectedItem() != null) {
                    layer = cbEnvLayers.getSelectedItem().getValue();
                    new_value = layer.getString("name");
                    cbEnvLayers.setValue("");
                }
            }
            if (new_value.equals("")) {
                return;
            }

            if (selectedLayers.contains(layer)) {
                //not a new value to add
            } else {
                selectedLayers.add(layer);
            }

            /* apply something to line onclick in lb */
            lbSelLayers.setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data, int item_idx) {
                    JSONObject layer = (JSONObject) data;
                    SPLFilter f = getSPLFilter(layer);

                    // Col 1: Add the layer name
                    Listcell lname = new Listcell(f.layer.getString("displayname"));
                    lname.setStyle("white-space: normal;");
                    lname.setParent(li);

                    // Col 2: Add the filter string and set the onClick event
                    String filterString = f.getFilterString();
                    Listcell lc = new Listcell(filterString);
                    lc.setStyle("white-space: normal;");
                    lc.setParent(li);

                    // Col 3: Add the species count and set the onClick event
                    Listcell count = new Listcell(String.valueOf(f.count));
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

            filter_done.setDisabled(false);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        //reset active area size
        activeAreaSize = null;
    }

    private SPLFilter getSPLFilter(JSONObject layer) {
        SPLFilter splf = null;

        // First check if already present
        splf = selectedSPLFilterLayers.get(layer.getString("name"));

        // if splf is still null, then it must be new
        // so grab the details from the server
        if (splf == null) {
            splf = new SPLFilter();
            splf.setCount(0);
            splf.setLayername(layer.getString("name"));
            splf.setLayer(layer);
            splf.setMinimum_value(layer.getDouble("environmentalvaluemin"));
            splf.setMaximum_value(layer.getDouble("environmentalvaluemax"));
            splf.setMinimum_initial(layer.getDouble("environmentalvaluemin"));
            splf.setMaximum_initial(layer.getDouble("environmentalvaluemax"));
            splf.setChanged(false);

            selectedSPLFilterLayers.put(layer.getString("name"), splf);
        }

        return splf;
    }

    public void deleteSelectedFilters(Object o) {
        Listitem li;
        JSONObject layer;

        popup_continous.setVisible(false);

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
        Events.echoEvent("removeLayer", this, layer.getString("name"));

        selectedLayers.remove(layer);

        li.detach();

        showAdjustPopup(null);

        filter_done.setDisabled(selectedLayers.size() == 0);

        listFix();

        //reset active area size
        activeAreaSize = null;
    }

    public void onClick$btnClearSelection(Event event) {
        Listitem li;
        JSONObject layer;

        popup_continous.setVisible(false);

        filter_done.setDisabled(true);

        //change to get last item
        int count = lbSelLayers.getItemCount();
        if (count == 0) {
            return;
        }
        li = lbSelLayers.getItemAtIndex(count - 1);

        int idx = li.getIndex();

        layer = selectedLayers.get(idx);

        //not executing, echo
        Events.echoEvent("removeLayerClearSelected", this, layer.getString("displayname"));

        selectedLayers.remove(layer);

        li.detach();

        if (selectedLayers.size() == 0) {
            showAdjustPopup(null);
            listFix();
        }
    }

    public void removeLayerClearSelected(Event event) {
        String layername = (String) event.getData();
        if (selectedLayers.size() > 0) {
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
            Events.echoEvent("removeLayers", this, layername.substring(p + 1));
            layername = layername.substring(0, p);
        }
        if (mc.getMapLayer(LAYER_PREFIX + layername) != null) {
            mc.removeLayer(LAYER_PREFIX + layername);
        } else if (layername.equalsIgnoreCase("Active Area")) {
            showActiveArea();
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
        if (list != null && list.size() > 0) {
            Listitem li = list.get(i);
            li.setDisabled(false);
        }
    }

    public void onScroll$popup_slider_min(Event event) {
        logger.debug("Changing min slider");
        try {

            int curpos = popup_slider_min.getCurpos();

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

            popup_minimum.setValue(((float) (curpos / 100.0 * range + popup_filter.minimum_initial)));

            popup_filter.minimum_value = popup_minimum.getValue();

            ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

            serverFilter();

        } catch (Exception e) {
            logger.debug("slider change min:" + e.toString());
            e.printStackTrace(System.out);
        }
    }

    public void onScroll$popup_slider_max(Event event) {
        logger.debug("Changing max slider");
        try {

            int curpos = popup_slider_max.getCurpos();

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

            popup_maximum.setValue(((float) (curpos / 100.0 * range + popup_filter.minimum_initial)));

            popup_filter.maximum_value = popup_maximum.getValue();

            ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

            serverFilter();
        } catch (Exception e) {
            logger.debug("slider change max:" + e.toString());
            e.printStackTrace(System.out);
        }
    }

    private void showAdjustPopup(Object o) {
        if (o == null) {

            //get the Count cell
            int i = lbSelLayers.getItemCount();
            if (i < 1) {
                popup_continous.setVisible(false);
                return;
            }

            Listitem li = lbSelLayers.getItemAtIndex(i - 1);
            List list = li.getChildren();

            o = list.get(list.size() - 2);
        }

        Listcell lc = (Listcell) o;
        Listitem li = (Listitem) lc.getParent();
        JSONObject layer = li.getValue();

        popup_filter = getSPLFilter(layer);
        popup_idx.setValue(layer.getString("displayname"));

        popup_cell = lc;
        popup_item = li;

        label_continous.setValue("edit envelope for: " + layer.getString("displayname"));
        String csv = String.format("%.4f", (float) popup_filter.minimum_initial) + " - " + String.format("%.4f", (float) popup_filter.maximum_initial);
        popup_range.setValue(csv);

        popup_minimum.setValue((float) (popup_filter.minimum_value));
        popup_maximum.setValue((float) (popup_filter.maximum_value));

        double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
        int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                / (range) * 100);
        int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                / (range) * 100);

        doApplyFilter(layer, Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value));

        popup_slider_min.setCurpos(mincursor);
        popup_slider_max.setCurpos(maxcursor);

        lc.focus();
        logger.debug("attaching: " + lc + lc.getValue());
        popup_continous.setVisible(true);

    }

    private void showAdjustPopup(JSONObject layer, Listcell lc, Listitem li) {
        popup_filter = getSPLFilter(layer);
        popup_idx.setValue(layer.getString("displayname"));

        popup_cell = lc;
        popup_item = li;

        label_continous.setValue("edit envelope for: " + layer.getString("displayname"));

        String csv = String.format("%.4f", (float) popup_filter.minimum_value) + " - " + String.format("%.4f", (float) popup_filter.maximum_value);
        popup_range.setValue(csv);

        popup_minimum.setValue(((float) (popup_filter.minimum_value)));
        popup_maximum.setValue(((float) (popup_filter.maximum_value)));

        double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
        int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                / (range) * 100);
        int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                / (range) * 100);

        doApplyFilter(layer, Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value));

        popup_slider_min.setCurpos(mincursor);
        popup_slider_max.setCurpos(maxcursor);

        popup_continous.setVisible(true);
    }

    private void serverFilter() {
        doApplyFilter(popup_filter.layer, Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value));
    }

    private void doApplyFilter(JSONObject layer, String val1, String val2) {
        try {
            loadMap(layer, lbSelLayers.getItemCount() - 1, Double.parseDouble(val1), Double.parseDouble(val2), false);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void onClick$filter_done(Event event) {
        ok = true;

        try {
            //create the layer
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.satServer).append("/ws/envelope?area=").append(getWkt());
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(sbProcessUrl.toString());
            logger.debug(sbProcessUrl.toString());

            post.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();
            String[] list = slist.split("\n");
            String pid = list[0];
            String url = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:envelope_" + pid + "&FORMAT=image%2Fpng";
            activeAreaExtent = list[1];
            activeAreaSize = list[2];

            //load the layer
            MapLayer ml = mc.addWMSLayer(pid, txtLayerName.getText(), url, 0.75f, null, null, LayerUtilities.ENVIRONMENTAL_ENVELOPE, null, null);

            //make the metadata?
            StringBuilder sb = new StringBuilder();
            StringBuilder sbLayerList = new StringBuilder();
            sb.append("Environmental Envelope<br>");
            for (int i = 0; i < selectedLayers.size(); i++) {
                JSONObject layer = selectedLayers.get(i);
                SPLFilter f = getSPLFilter(layer);
                sb.append(f.layername).append(": ").append(f.getFilterString()).append("<br>");

                sbLayerList.append(f.layername);
                if (i < selectedLayers.size() - 1) {
                    sbLayerList.append(":");
                }
            }
            activeAreaMetadata = LayersUtil.getMetadata(sb.toString());

            getMapComposer().setAttribute("activeLayerName", sbLayerList.toString());
            getMapComposer().setAttribute("mappolygonlayer", sb.toString());

            try {
                final_wkt = getWkt();
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.layerName = ml.getName();
            List<Double> bb = new ArrayList<Double>(4);
            String[] bs = activeAreaExtent.split(",");
            for (int i = 0; i < 4; i++) {
                bb.add(Double.parseDouble(bs[i]));
            }
            ml.getMapLayerMetadata().setBbox(bb);
            ml.getMapLayerMetadata().setMoreInfo(activeAreaMetadata);

            ml.setWKT("ENVELOPE(" + final_wkt + ")");
            ml.setEnvelope(final_wkt); //not the actual WKT
            try {
                double area = Double.parseDouble(activeAreaSize.replace(",", ""));
                activeAreaSize = String.format("%,.2f", area);
            } catch (Exception e) {
            }
            ml.setAreaSqKm(activeAreaSize);
            ml.setFacets(getFacets());

            removeAllSelectedLayers(true);  //this also shows active area

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        //do detach after adding the new layer
    }

    void showActiveArea() {
    }

    public void onClick$apply_continous(Event event) {
        applyFilter();
        isDirtyCount = false;
    }

    public void onClick$remove_continous(Event event) {
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
        popup_filter.setCount(speciescount);
        ((Listcell) popup_item.getChildren().get(2)).setLabel(speciescount + "");

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

        Clients.showBusy("Updating count...");
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
        popup_filter.minimum_value = (popup_minimum.getValue());
        popup_filter.maximum_value = (popup_maximum.getValue());

        ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

        serverFilter();

        String strCount = speciescount + "";
        if (isDirtyCount) {
            Clients.showBusy("Updating species count....");
            strCount = getSpeciesCount() + "";
            Clients.clearBusy();
        }

        //TODO: handle invalid counts/errors
        try {
            popup_filter.count = Integer.parseInt(strCount.split("\n")[0]);
            ((Listcell) popup_item.getChildren().get(2)).setLabel(strCount.split("\n")[0]);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MapLayer loadMap(JSONObject layer, int depth, double min, double max, boolean final_layer) {

        String colour = final_layer ? "0xFF0000" : FILTER_COLOURS[depth % FILTER_COLOURS.length];
        String filter
                = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\">"
                + "<NamedLayer><Name>ALA:" + layer.getString("name") + "</Name>"
                + "<UserStyle><FeatureTypeStyle><Rule><RasterSymbolizer><Geometry></Geometry>"
                + "<ColorMap>"
                + "<ColorMapEntry color=\"" + colour + "\" opacity=\"1\" quantity=\"" + (min - Math.abs(min * 0.0000001)) + "\"/>"
                + "<ColorMapEntry color=\"" + colour + "\" opacity=\"0\" quantity=\"" + min + "\"/>"
                + "<ColorMapEntry color=\"" + colour + "\" opacity=\"0\" quantity=\"" + max + "\"/>"
                + "<ColorMapEntry color=\"" + colour + "\" opacity=\"1\" quantity=\"" + (max + Math.abs(max * 0.0000001)) + "\"/>"
                + "</ColorMap></RasterSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";

        try {
            filter = URLEncoder.encode(filter, "UTF-8");
        } catch (Exception e) {
            logger.debug("invalid filter sld");
            e.printStackTrace();
        }

        MapLayer ml = mc.getMapLayer(LAYER_PREFIX + layer.getString("name"));
        if (ml == null) {
            ml = getMapComposer().addWMSLayer(LAYER_PREFIX + layer.getString("name"),
                    LAYER_PREFIX + layer.getString("displayname"),
                    layer.getString("displaypath").replace("/gwc/service", "") + "&sld_body=" + filter,
                    (float) 0.75,
                    CommonData.layersServer + "/layers/view/more/" + layer.getString("id"),
                    CommonData.geoServer + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + layer.getString("name"),
                    LayerUtilities.ENVIRONMENTAL_ENVELOPE,
                    null, null, null);
        } else {
            ml.setUri(layer.getString("displaypath").replace("/gwc/service", "") + "&sld_body=" + filter);
            mc.reloadMapLayerNowAndIndexes(ml);
        }

        return ml;
    }

    ArrayList<Facet> getFacets() {
        ArrayList<Facet> facets = new ArrayList<Facet>();
        for (int i = 0; i < selectedLayers.size(); i++) {
            SPLFilter splf = selectedSPLFilterLayers.get(selectedLayers.get(i).getString("name"));
            Facet f = new Facet(CommonData.getLayerFacetName(splf.layername), splf.minimum_value, splf.maximum_value, true);
            facets.add(f);
        }
        return facets;
    }

    String getWkt() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedLayers.size(); i++) {
            SPLFilter splf = selectedSPLFilterLayers.get(selectedLayers.get(i).getString("name"));
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(CommonData.getLayerFacetName(splf.layername)).append(",").append(splf.minimum_value).append(",").append(splf.maximum_value);
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
            if (selectedLayers.size() > 0) {
                sb.append("|");
            }
        }
        for (int i = 0; i < selectedLayers.size(); i++) {
            JSONObject layer = selectedLayers.get(i);
            sb.append(layer.getString("name"));
            if (i < selectedLayers.size() - 1) {
                sb.append("|");
            }
        }
        if (selectedLayers.size() > 0) {
            Events.echoEvent("removeLayers", this, sb.toString());
        }
    }

    public void onClick$btnCancel(Event event) {
        removeAllSelectedLayers(false);
    }
}
