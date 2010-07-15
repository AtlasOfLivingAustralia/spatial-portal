package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import org.ala.spatial.util.LayersUtil;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SPLFilter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.ArrayUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
public class FilteringWCController extends UtilityComposer {

    private static final long serialVersionUID = -26560838825366347L;
    private static final String SAT_URL = "sat_url";
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
    private Map<String, SPLFilter> selectedSPLFilterLayers;
    private String pid = "";
    private MapComposer mc;
    private String satServer = "";
    private SettingsSupplementary settingsSupplementary = null;
    LayersUtil layersUtil;
    /**
     * for functions in popup box
     */
    SPLFilter popup_filter;
    Listcell popup_cell;
    Listitem popup_item;

    @Override
    public void afterCompose() {
        super.afterCompose();

        //get the current MapComposer instance
        mc = getThisMapComposer();
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        } else {
            //TODO: error message
        }

        layersUtil = new LayersUtil(mc, satServer);

        selectedLayers = new Vector<String>();
        selectedSPLFilterLayers = new Hashtable<String, SPLFilter>();

        // init the session on the server and get a pid (process_id)
        pid = getInfo("/filtering/init");
    }

    public String getPid() {
        if (selectedLayers.size() > 0) {
            //TODO: 'apply' only if required  //applyFilter(true);
            return pid;
        } else {
            return "";
        }
    }

    public void onChange$cbEnvLayers(Event event) {
        applyFilter(true);
    }

    public void onChange$popup_minimum(Event event) {
        System.out.println("popup_minimum=" + popup_minimum.getValue() + " " + event.getData());
        serverFilter(false);

    }
    public void onChange$popup_maximum(Event event) {
        serverFilter(false);
    }

    public void doAdd(String new_value) {

        try {
            if (new_value.length() == 0) {
                new_value = cbEnvLayers.getValue();
            }
            if (new_value.equals("") || new_value.indexOf("(") < 0) {
                return;
            }
            new_value = (new_value.equals("")) ? "" : new_value.substring(0, new_value.indexOf("(")).trim();

            if (selectedLayers.contains(new_value)) {
                //not a new value to add
            } else {
                selectedLayers.add(new_value);
            }

            /* apply something to line onclick in lb */
            lbSelLayers.setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data) {
                    String layername = (String) data;
                    SPLFilter f = getSPLFilter(layername);

                    // Col 1: Add the layer name
                    Listcell lname = new Listcell(f.layer.display_name + " (Terrestrial)");
                    lname.setStyle("white-space: normal;");
                    lname.setParent(li);

                    // Col 2: Add the filter string and set the onClick event
                    String filterString = f.getFilterString();
                    Listcell lc = new Listcell(filterString);
                    lc.setStyle("white-space: normal;");
                    lc.setParent(li);

                    // Col 3: Add the species count and set the onClick event
                    Listcell count = new Listcell(String.valueOf(f.count));
                    count.setStyle("text-decoration: underline; text-align: right; ");
                    count.setParent(li);
                    /* species list tab exists
                    count.addEventListener("onClick", new EventListener() {

                    public void onEvent(Event event) throws Exception {
                    if (!((Listcell) event.getTarget()).getLabel().equals("0")
                    && !((Listitem) event.getTarget().getParent()).isDisabled()) {

                    if (lbSelLayers.getItemCount() > 0) {
                    applyFilterEvented();
                    java.util.Map args = new java.util.HashMap();
                    args.put("pid", pid);
                    Window win = (Window) Executions.createComponents(
                    "/WEB-INF/zul/AnalysisFilteringResults.zul", null, args);
                    win.doModal();
                    }
                    }
                    }
                    });*/
                }
            });

            lbSelLayers.setModel(new SimpleListModel(selectedLayers));
            Listitem li = lbSelLayers.getItemAtIndex(lbSelLayers.getItemCount() - 1);

            Listcell lc = (Listcell) li.getFirstChild();
            lc = (Listcell) lc.getNextSibling();

            listFix();

            showAdjustPopup(new_value, lc, li);
        } catch (Exception e) {
            //TODO: error message
            e.printStackTrace(System.out);
        }
    }

    private SPLFilter getSPLFilter(String layername) {
        SPLFilter splf = null;

        // First check if already present
        splf = selectedSPLFilterLayers.get(layername);

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

            selectedSPLFilterLayers.put(layername, splf);
        }

        return splf;

    }

    private String getInfo(String value, String type) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + URLEncoder.encode(value, "UTF-8") + "/" + type); // testurl
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
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws" + urlPart); // testurl
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

        StringBuffer sbProcessUrl = new StringBuffer();
        sbProcessUrl.append("/filtering/apply4");
        sbProcessUrl.append("/pid/" + pid);
        sbProcessUrl.append("/layers/none");
        sbProcessUrl.append("/types/none");
        sbProcessUrl.append("/val1s/none");
        sbProcessUrl.append("/val2s/none");
        sbProcessUrl.append("/depth/" + lbSelLayers.getItemCount());

        getInfo(sbProcessUrl.toString());

        mc.removeLayer(label);
        //mc.getOpenLayersJavascript().removeMapLayerNow(mc.getMapLayer(label));

        selectedLayers.remove(label);

        li.detach();

        showAdjustPopup(null);

        listFix();
    }

    public void listFix() {
        int i;
        List list = lbSelLayers.getItems();
        for (i = 0; list != null && i < list.size() - 1; i++) {
            Listitem li = (Listitem) list.get(i);
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
        layername = (layername.equals("")) ? "" : layername.substring(0, layername.indexOf("(")).trim();

        popup_filter = getSPLFilter(layername);
        popup_idx.setValue(layername);

        popup_cell = lc;
        popup_item = li;

        label_continous.setValue("edit filter: " + layername);
        String csv = String.format("%.4f", (float) popup_filter.minimum_value) + " - " + String.format("%.4f", (float) popup_filter.maximum_value);
        popup_range.setValue(csv);

        popup_minimum.setValue((float) (popup_filter.minimum_value));
        popup_maximum.setValue((float) (popup_filter.maximum_value));

        double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
        int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                / (range) * 100);
        int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                / (range) * 100);

        doApplyFilter(pid, layername, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), false);

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



        label_continous.setValue("edit filter: " + layername);

        String csv = String.format("%.4f", (float) popup_filter.minimum_value) + " - " + String.format("%.4f", (float) popup_filter.maximum_value);
        popup_range.setValue(csv);

        popup_minimum.setValue(((float) (popup_filter.minimum_value)));
        popup_maximum.setValue(((float) (popup_filter.maximum_value)));

        double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
        int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                / (range) * 100);
        int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                / (range) * 100);

        doApplyFilter(pid, layername, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), false);

        popup_slider_min.setCurpos(mincursor);
        popup_slider_max.setCurpos(maxcursor);

        popup_continous.setVisible(true);
    }

    private void serverFilter(boolean commit) {
        doApplyFilter(pid, popup_filter.layer.display_name, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), commit);
    }

    private void doApplyFilter(String pid, String layername, String type, String val1, String val2, boolean commit) {
        try {
            String urlPart = "";
            if (commit) {
                urlPart += "/filtering/apply4";
            } else {
                urlPart += "/filtering/apply3";
            }
            urlPart += "/pid/" + URLEncoder.encode(pid, "UTF-8");
            urlPart += "/layers/" + URLEncoder.encode(layername, "UTF-8");
            urlPart += "/types/" + URLEncoder.encode(type, "UTF-8");
            urlPart += "/val1s/" + URLEncoder.encode(val1, "UTF-8");
            urlPart += "/val2s/" + URLEncoder.encode(val2, "UTF-8");
            urlPart += "/depth/" + lbSelLayers.getItemCount();

            String imagefilepath = getInfo(urlPart);
            loadMap(imagefilepath);
        } catch (Exception e) {
            //TODO: error message
            e.printStackTrace(System.out);
        }
    }

    public void onClick$apply_continous(Event event) {
        applyFilter();
    }

    public void onClick$remove_continous(Event event) {
        deleteSelectedFilters(null);
    }

    public void onClick$preview_continous(Event event) {
        try {
            applyFilterEvented();
            if (lbSelLayers.getItemCount() > 0) {

                java.util.Map args = new java.util.HashMap();
                args.put("pid", pid);
                Window win = (Window) Executions.createComponents(
                        "/WEB-INF/zul/AnalysisFilteringResults.zul", null, args);
                win.doModal();
            }
        } catch (Exception e) {
            //TODO: error message
        }
    }

    public void onLater(Event event) throws Exception {
        applyFilterEvented();
        Clients.showBusy("", false);
    }

    public void onLateron(Event event) throws Exception {
        applyFilterEvented();
        doAdd("");
        Clients.showBusy("", false);
    }

    public void applyFilter() {
        Clients.showBusy("Applying filter...", true);
        if (lbSelLayers.getItemCount() == 0) {
            return;
        }

        Clients.showBusy("Applying filter...", true);
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
            Clients.showBusy("Applying filter...", true);
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

        String strCount = postInfo("/filtering/apply/pid/" + pid + "/species/count?area=none");

        //TODO: handle invalid counts/errors

        popup_filter.count = Integer.parseInt(strCount);
        ((Listcell) popup_item.getChildren().get(2)).setLabel(strCount);

        //update Species List analysis tab
        updateSpeciesList(popup_filter.count);
    }

     /**
     * updates species list analysis tab with refreshCount
     *
      * similar function in SelectionController.java
     */
    void updateSpeciesList(int newCount) {
        try {
            FilteringResultsWCController win =
                    (FilteringResultsWCController) getMapComposer()
                        .getFellow("leftMenuAnalysis")
                            .getFellow("analysiswindow")
                                .getFellow("speciesListForm")
                                    .getFellow("popup_results");
            win.refreshCount(newCount);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String postInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(satServer + "/alaspatial/ws" + urlPart); // testurl

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

    private void loadMap(String filename) {
        String label = "Filtering - " + pid + " - layer " + lbSelLayers.getItemCount();
        label = selectedLayers.get(selectedLayers.size() - 1);
        String uri = satServer + "/alaspatial/output/filtering/" + pid + "/" + filename;
        float opacity = Float.parseFloat("0.75");

        List<Double> bbox = new ArrayList<Double>();
        bbox.add(112.0);
        bbox.add(-44.0000000007);
        bbox.add(154.00000000084);
        bbox.add(-9.0);

        mc.addImageLayer(pid, label, uri, opacity, bbox);

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

    /**
     * populate sampling screen with values from active layers
     * 
     * only operates if no filter layers present
     * 
     * TODO: run this on 'tab' open
     */
    public void callPullFromActiveLayers() {
        //already has layers applied, do nothing
        if (lbSelLayers.getItemCount() != 0) {
            return;
        }

        //get top env layer
        String layer = layersUtil.getFirstEnvLayer();

        if (layer != null) {
            doAdd(layer + " (Terrestrial)");
        }
    }
}
