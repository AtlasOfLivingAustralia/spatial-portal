package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import org.ala.spatial.util.LayersUtil;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SPLFilter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.ArrayUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AreaEnvironmentalEnvelope extends AreaToolComposer {

    public static final String LAYER_PREFIX = "working envelope: ";
    private static final long serialVersionUID = -26560838825366347L;
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
    private List<String> selectedLayers;
    private List<String> selectedLayersUrl;
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
    String activeAreaUrl = null;
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
        mc = getThisMapComposer();

        layersUtil = new LayersUtil(mc);

        selectedLayers = new Vector<String>();
        selectedLayersUrl = new Vector<String>();
        selectedSPLFilterLayers = new Hashtable<String, SPLFilter>();

        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
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
        System.out.println("popup_minimum=" + popup_minimum.getValue() + " " + event.getData());
        serverFilter(false);

    }

    public void onChange$popup_maximum(Event event) {
        isDirtyCount = true;
        serverFilter(false);
    }

    public void doAdd(String new_value) {

        try {
            if (new_value.length() == 0) {
                //new_value = cbEnvLayers.getValue();

                if (cbEnvLayers.getItemCount() > 0 && cbEnvLayers.getSelectedItem() != null) {
                    JSONObject jo = (JSONObject) cbEnvLayers.getSelectedItem().getValue();
                    new_value = jo.getString("name");
                    cbEnvLayers.setValue("");
                }

            }
            if (new_value.equals("")) {
                return;
            }

            if (selectedLayers.contains(new_value)) {
                //not a new value to add
            } else {
                selectedLayers.add(new_value);
                selectedLayersUrl.add(null);
            }

            /* apply something to line onclick in lb */
            lbSelLayers.setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data) {
                    String layername = (String) data;
                    SPLFilter f = getSPLFilter(layername);

                    // Col 1: Add the layer name
                    Listcell lname = new Listcell(f.layer.display_name);
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

            showAdjustPopup(new_value, lc, li);

            filter_done.setDisabled(false);
        } catch (Exception e) {
            //TODO: error message
            e.printStackTrace(System.out);
        }

        //reset active area size
        activeAreaSize = null;
    }

    private SPLFilter getSPLFilter(String layername) {
        SPLFilter splf = null;

        // First check if already present
        splf = selectedSPLFilterLayers.get(layername.toLowerCase());

        // if splf is still null, then it must be new
        // so grab the details from the server
        if (splf == null) {
            JSONObject jo = JSONObject.fromObject(getInfo(layername, "layerfilter"));

            JSONObject lbean = jo.getJSONObject("layer");
            Layer rLayer = new Layer(lbean.getString("name"), lbean.getString("display_name"), lbean.getString("description"), lbean.getString("type"), null);
            splf = new SPLFilter();
            splf.setCount(jo.getInt("count"));
            splf.setLayername(jo.getString("layername"));
            splf.setLayer(rLayer);
            splf.setMinimum_value(jo.getDouble("minimum_value"));
            splf.setMaximum_value(jo.getDouble("maximum_value"));
            splf.setMinimum_initial(jo.getDouble("minimum_initial"));
            splf.setMaximum_initial(jo.getDouble("maximum_initial"));
            splf.setChanged(jo.getBoolean("changed"));

            if (lbean.getString("type").equalsIgnoreCase("contextual")) {
                JSONArray jaCatNames = jo.getJSONArray("catagory_names");
                if (jaCatNames != null) {
                    splf.setCatagory_names((String[]) jaCatNames.toArray(new String[0]));
                }

                JSONArray jaCatNums = jo.getJSONArray("catagories");
                if (jaCatNums != null) {
                    splf.setCatagories(ArrayUtils.toPrimitive((Integer[]) jaCatNums.toArray(new Integer[0])));
                }
            }

            selectedSPLFilterLayers.put(layername.toLowerCase(), splf);
        }

        if (!layername.equals(splf.layer.name)) {
            splf = getSPLFilter(splf.layer.name.toLowerCase());
        }

        return splf;

    }

    private String getInfo(String value, String type) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.satServer + "/ws/spatial/settings/layer/" + URLEncoder.encode(value, "UTF-8") + "/" + type); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, * /*");

            int result = client.executeMethod(get);

            //TODO: test results
            String slist = get.getResponseBodyAsString();


            return slist;
        } catch (Exception ex) {
            //TODO: error message
            //Logger.getLogger(FilteringWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    private String getInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.satServer + "/ws" + urlPart); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);

            //TODO: test results
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            //Logger.getLogger(FilteringWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    public void deleteSelectedFilters(Object o) {
        Listitem li;
        String label;

        popup_continous.setVisible(false);

        if (o == null) {
            //change to get last item
            int count = lbSelLayers.getItemCount();
            li = (Listitem) lbSelLayers.getItemAtIndex(count - 1);
        } else {
            li = (Listitem) ((Listcell) o).getParent();
        }
        int idx = li.getIndex();

        label = selectedLayers.get(idx);

        //not executing, echo
        //mc.removeLayer(getSPLFilter(label).layer.display_name);
        Events.echoEvent("removeLayer", this, getSPLFilter(label).layer.display_name);

        selectedLayers.remove(label);
        selectedLayersUrl.remove(idx);

        li.detach();

        showAdjustPopup(null);

        filter_done.setDisabled(selectedLayers.size() == 0);

        listFix();

        //reset active area size
        activeAreaSize = null;
    }

    public void onClick$btnClearSelection(Event event) {
        Listitem li;
        String label;

        popup_continous.setVisible(false);

        filter_done.setDisabled(true);

        //change to get last item
        int count = lbSelLayers.getItemCount();
        if (count == 0) {
            return;
        }
        li = (Listitem) lbSelLayers.getItemAtIndex(count - 1);

        int idx = li.getIndex();

        label = selectedLayers.get(idx);

        //not executing, echo
        //mc.removeLayer(getSPLFilter(label).layer.display_name);
        Events.echoEvent("removeLayerClearSelected", this, getSPLFilter(label).layer.display_name);

        selectedLayers.remove(label);
        selectedLayersUrl.remove(idx);

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

    public void removeLayer(Event event) {
        String all = (String) event.getData();
        String layername = all;
        int p = layername.indexOf('|');
        if (p > 0) {
            Events.echoEvent("removeLayer", this, layername.substring(p + 1));
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

    public void listFix() {
        int i;
        List<Listitem> list = lbSelLayers.getItems();
        for (i = 0; list != null && i < list.size() - 1; i++) {
            Listitem li = list.get(i);
            li.setDisabled(true);
        }
        if (list != null && list.size() > 0) {
            Listitem li = (Listitem) list.get(i);
            li.setDisabled(false);
        }
    }

    public void onScroll$popup_slider_min(Event event) {
        System.out.println("Changing min slider");
        try {

            int curpos = popup_slider_min.getCurpos();

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

            popup_minimum.setValue(((float) (curpos / 100.0 * range + popup_filter.minimum_initial)));

            popup_filter.minimum_value = popup_minimum.getValue();

            ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

            serverFilter(false);

        } catch (Exception e) {
            //TODO: error message
            System.out.println("slider change min:" + e.toString());
            e.printStackTrace(System.out);
        }
    }

    public void onScroll$popup_slider_max(Event event) {
        System.out.println("Changing max slider");
        try {

            int curpos = popup_slider_max.getCurpos();

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

            popup_maximum.setValue(((float) (curpos / 100.0 * range + popup_filter.minimum_initial)));

            popup_filter.maximum_value = popup_maximum.getValue();

            ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

            serverFilter(false);
        } catch (Exception e) {
            //TODO: error message

            System.out.println("slider change max:" + e.toString());
            e.printStackTrace(System.out);
        }
    }

    private void showAdjustPopup(Object o) {
        String layername = "";
        if (o == null) {

            //get the Count cell
            int i = lbSelLayers.getItemCount();
            if (i < 1) {
                popup_continous.setVisible(false);
                return;
            }

            Listitem li = (Listitem) lbSelLayers.getItemAtIndex(i - 1);
            List list = li.getChildren();

            o = list.get(list.size() - 2);
        }

        Listcell lc = (Listcell) o;
        Listitem li = (Listitem) lc.getParent();
        layername = ((Listcell) li.getChildren().get(0)).getLabel();
        //layername = (layername.equals("")) ? "" : layername.substring(0, layername.lastIndexOf("(")).trim();

        popup_filter = getSPLFilter(layername);
        popup_idx.setValue(layername);

        popup_cell = lc;
        popup_item = li;

        label_continous.setValue("edit envelope for: " + getSPLFilter(layername).layer.display_name);
        String csv = String.format("%.4f", (float) popup_filter.minimum_initial) + " - " + String.format("%.4f", (float) popup_filter.maximum_initial);
        popup_range.setValue(csv);

        popup_minimum.setValue((float) (popup_filter.minimum_value));
        popup_maximum.setValue((float) (popup_filter.maximum_value));

        double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
        int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                / (range) * 100);
        int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                / (range) * 100);

        doApplyFilter(getSPLFilter(layername).layer.display_name, layername, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), false);

        popup_slider_min.setCurpos(mincursor);
        popup_slider_max.setCurpos(maxcursor);

        lc.focus();
        System.out.println("attaching: " + lc + lc.getValue());
        popup_continous.setVisible(true);

    }

    private void showAdjustPopup(String layername, Listcell lc, Listitem li) {
        popup_filter = getSPLFilter(layername);
        popup_idx.setValue(layername);

        popup_cell = lc;
        popup_item = li;

        label_continous.setValue("edit envelope for: " + getSPLFilter(layername).layer.display_name);

        String csv = String.format("%.4f", (float) popup_filter.minimum_value) + " - " + String.format("%.4f", (float) popup_filter.maximum_value);
        popup_range.setValue(csv);

        popup_minimum.setValue(((float) (popup_filter.minimum_value)));
        popup_maximum.setValue(((float) (popup_filter.maximum_value)));

        double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
        int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                / (range) * 100);
        int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                / (range) * 100);

        doApplyFilter(getSPLFilter(layername).layer.display_name, layername, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), false);

        popup_slider_min.setCurpos(mincursor);
        popup_slider_max.setCurpos(maxcursor);

        popup_continous.setVisible(true);
    }

    private void serverFilter(boolean commit) {
        doApplyFilter(popup_filter.layer.display_name, popup_filter.layername, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), commit);
    }

    private void doApplyFilter(String layerdisplayname, String layername, String type, String val1, String val2, boolean commit) {
        try {
//            String urlPart = "";
//            if (commit) {
//                urlPart += "/filtering/apply4";
//            } else {
//                urlPart += "/filtering/apply3";
//            }
//            urlPart += "/pid/" + URLEncoder.encode(pid, "UTF-8");
//            urlPart += "/layers/" + URLEncoder.encode(layername, "UTF-8");
//            urlPart += "/types/" + URLEncoder.encode(type, "UTF-8");
//            urlPart += "/val1s/" + URLEncoder.encode(val1, "UTF-8");
//            urlPart += "/val2s/" + URLEncoder.encode(val2, "UTF-8");
//            urlPart += "/depth/" + lbSelLayers.getItemCount();
//
//            String imagefilepath = getInfo(urlPart);
//            MapLayer ml = loadMap(imagefilepath, LAYER_PREFIX + layerdisplayname);
//
//            selectedLayersUrl.set(lbSelLayers.getItemCount() - 1, imagefilepath);

        } catch (Exception e) {
            //TODO: error message
            e.printStackTrace(System.out);
        }
    }

    public void onClick$filter_done(Event event) {
        ok = true;

        applyFilterEvented();

        try {
            String urlPart = "/ws/envelope";

            String[] imagefilepath = getInfo(urlPart).split("\n");

            activeAreaUrl = imagefilepath[0];
            activeAreaExtent = imagefilepath[1];
            activeAreaSize = imagefilepath[2];

            int p = activeAreaSize.indexOf('.');
            if (p > 0) {
                activeAreaSize = activeAreaSize.substring(0, p);
            }

            //make the metadata?
            StringBuilder sb = new StringBuilder();
            StringBuilder sbLayerList = new StringBuilder();
            sb.append("Environmental Envelope<br>");
            for (int i = 0; i < selectedLayers.size(); i++) {
                String layername = (String) selectedLayers.get(i);
                SPLFilter f = getSPLFilter(layername);
                sb.append(f.layername).append(": ").append(f.getFilterString()).append("<br>");

                sbLayerList.append(f.layername);
                if (i < selectedLayers.size()-1) {
                    sbLayerList.append(":"); 
                }
            }
            activeAreaMetadata = LayersUtil.getMetadata(sb.toString());

            getMapComposer().setAttribute("activeLayerName", sbLayerList.toString());
            getMapComposer().setAttribute("mappolygonlayer", sb.toString());

            try {
//                final_wkt = getWkt(pid);
            } catch (Exception e) {
                e.printStackTrace();
            }

            removeAllSelectedLayers(true);  //this also shows active area

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        //do detach after adding the new layer
    }

    void showActiveArea() {
        if (activeAreaUrl != null) {
            loadMap(activeAreaUrl, txtLayerName.getValue(), true);
            MapLayer ml = mc.getMapLayer(txtLayerName.getValue());
            if (ml.getMapLayerMetadata() == null) {
                ml.setMapLayerMetadata(new MapLayerMetadata());
            }
            this.layerName = ml.getName();
            List<Double> bb = new ArrayList<Double>(4);
            String[] bs = activeAreaExtent.split(",");
            for (int i = 0; i < 4; i++) {
                bb.add(Double.parseDouble(bs[i]));
            }
            ml.getMapLayerMetadata().setBbox(bb);
            ml.getMapLayerMetadata().setMoreInfo(activeAreaMetadata);
        }
    }

    void updateActiveArea(boolean hide) {
        //trigger update to 'active area'
        Component c = getParent().getParent();
        while (c != null && !c.getId().equals("selectionwindow")) {
            c = c.getParent();
        }
        if (c != null) {
            //((SelectionController) c).onEnvelopeDone(hide);
        }
    }

    public void onClick$apply_continous(Event event) {
        applyFilter();
        isDirtyCount = false;
    }

    public void onClick$remove_continous(Event event) {
        deleteSelectedFilters(null);
        isDirtyCount = false;
    }

    public void onClick$preview_continous(Event event) {
        try {
            applyFilterEvented();
            if (lbSelLayers.getItemCount() > 0) {

                java.util.Map args = new java.util.HashMap();
//                args.put("pid", pid);
                Window win = (Window) Executions.createComponents(
                        "/WEB-INF/zul/AnalysisFilteringResults.zul", null, args);
                win.doModal();
            }
        } catch (Exception e) {
            //TODO: error message
        }
    }

    private int getSpeciesCount() {
        Query q = new BiocacheQuery(null, null, null, getFacets(), false);

        return q.getSpeciesCount();
    }

    public void onLater(Event event) throws Exception {
        applyFilterEvented();
        //Clients.showBusy("", false);
        updateActiveArea(false);

        //int spcount = q.getSpeciesCount();
        speciescount = getSpeciesCount();
        popup_filter.setCount(speciescount);
        ((Listcell) popup_item.getChildren().get(2)).setLabel(speciescount + "");

        Clients.clearBusy();
    }

    public void onLateron(Event event) throws Exception {
        applyFilterEvented();
        doAdd("");
        //Clients.clearBusy();
    }

    public void applyFilter() {
        //Clients.showBusy("Applying filter...", true);
        if (lbSelLayers.getItemCount() == 0) {
            return;
        }

        //Clients.showBusy("Applying filter...", true);
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
            //Clients.showBusy("Applying filter...");
            Events.echoEvent("onLateron", this, null);
        } else {
            applyFilter();
        }
    }

    private void applyFilterEvented() {
        popup_filter.minimum_value = (popup_minimum.getValue());
        popup_filter.maximum_value = (popup_maximum.getValue());

        ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

        serverFilter(true);

        //String strCount = postInfo("/filtering/apply/pid/" + pid + "/species/count?area=none");
        String strCount = speciescount + "";
        if (isDirtyCount) {
//            System.out.println("******************* is dirty count");
            Clients.showBusy("Updating species count....");
            strCount = getSpeciesCount() + "";
            Clients.clearBusy();
            //applyFilter();
//            System.out.println("**********************************");
//            System.out.println("**********************************");
//            System.out.println("strCount: " + strCount);
//            System.out.println("**********************************");
//            System.out.println("**********************************");

        } else {
//            System.out.println("******************* is not dirty count");
        }
        //strCount = speciescount + "";

        //TODO: handle invalid counts/errors
        try {
            popup_filter.count = Integer.parseInt(strCount.split("\n")[0]);
            ((Listcell) popup_item.getChildren().get(2)).setLabel(strCount.split("\n")[0]);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String postInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(CommonData.satServer + "/ws" + urlPart); // testurl

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            //get.addParameter("area", URLEncoder.encode("none", "UTF-8"));

            int result = client.executeMethod(get);

            //TODO: confirm result
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    private MapLayer loadMap(String filename, String layername) {
        return loadMap(filename, layername, false);
    }
    private MapLayer loadMap(String filename, String layername, boolean final_layer) {
        //String label = "Filtering - " + pid + " - layer " + lbSelLayers.getItemCount();
        //label = selectedLayers.get(selectedLayers.size() - 1);
        String uri = null;//CommonData.satServer + "/output/filtering/" + pid + "/" + filename;
        float opacity = Float.parseFloat("0.75");

        List<Double> bbox = new ArrayList<Double>();
        bbox.add(112.0);
        bbox.add(-44.0);
        bbox.add(154.0);
        bbox.add(-9.0);
        //bbox.add(12467782.96884664);
        //bbox.add(-5465442.183322753);
        //bbox.add(17143201.58216413);
        //bbox.add(-1006021.0627551343);

//        MapLayer ml = mc.addImageLayer(pid, layername, uri, opacity, bbox, LayerUtilities.ENVIRONMENTAL_ENVELOPE);
//        if(final_layer) {
//            ml.setWKT(final_wkt);
//            ml.setData("envelope", pid);
//            ml.setData("area", activeAreaSize);
//            ml.setData("facets", getFacets());
//        }

//        return ml;
        return null;

    }

    ArrayList<Facet> getFacets() {
        ArrayList<Facet> facets = new ArrayList<Facet>();
        for (int i = 0; i < selectedLayers.size(); i++) {
            SPLFilter splf = selectedSPLFilterLayers.get(selectedLayers.get(i).toLowerCase());
            Facet f = new Facet(CommonData.getLayerFacetName(splf.layername), splf.minimum_value, splf.maximum_value, true);
            facets.add(f);
        }
        return facets;
    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        //Page page = maxentWindow.getPage();
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }
    
    boolean state_visible = true;

    /**
     * Remove all selected layers from the map.
     *
     * For use when disabling Environmental Envelope in Active Layers.
     */
    public void removeAllSelectedLayers(boolean showActiveArea) {
        //if (state_visible) {
        state_visible = false;
        StringBuffer sb = new StringBuffer();
        if (activeAreaUrl != null && showActiveArea) {
            sb.append("Active Area");
            if (selectedLayers.size() > 0) {
                sb.append("|");
            }
        }
        for (int i = 0; i < selectedLayers.size(); i++) {
            String label = selectedLayers.get(i);
            sb.append(getSPLFilter(label).layer.display_name);
            if (i < selectedLayers.size() - 1) {
                sb.append("|");
            }
        }
        if (selectedLayers.size() > 0) {
            Events.echoEvent("removeLayer", this, sb.toString());
        }
        //}
    }

    public void showAllSelectedLayers() {
        state_visible = true;
        StringBuffer sb = new StringBuffer();
        if (selectedLayers.size() > 0) {
            sb.append("hideActive Area|");
        }
        for (int i = 0; i < selectedLayers.size(); i++) {
            String label = selectedLayers.get(i);
            sb.append("show");
            sb.append(String.valueOf(i));
            if (i < selectedLayers.size() - 1) {
                sb.append("|");
            }
        }
        if (selectedLayers.size() > 0) {
            Events.echoEvent("showLayer", this, sb.toString());
        }
    }

    public void showLayer(Event event) {
        String all = (String) event.getData();
        String idx = all;
        int p = idx.indexOf('|');
        if (p > 0) {
            Events.echoEvent("showLayer", this, idx.substring(p + 1));
            idx = idx.substring(0, p);
        }
        //is it show or hide?
        //show + idx to show
        //hide + layername to hide
        if (idx.startsWith("show")) {
            int i = Integer.parseInt(idx.substring(4));
            loadMap(selectedLayersUrl.get(i), LAYER_PREFIX + getSPLFilter(selectedLayers.get(i)).layer.display_name);
        } else { //"hide"
            if (mc.getMapLayer(LAYER_PREFIX + idx.substring(4)) != null) {
                mc.removeLayer(LAYER_PREFIX + idx.substring(4));
            }
        }
    }

    public void onClick$btnCancel(Event event) {
        removeAllSelectedLayers(false);
    }

    private String getWkt(String pid) {
        return getInfo("/filtering/wkt/" + activeAreaExtent + "/" + pid);
    }
}
