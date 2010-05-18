package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import net.sf.json.JSONObject;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SPLFilter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class FilteringWCController extends UtilityComposer {

    private static final long serialVersionUID = -26560838825366347L;
    
    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String GEOSERVER_USERNAME = "geoserver_username";
    private static final String GEOSERVER_PASSWORD = "geoserver_password";
    private static final String SAT_URL = "sat_url";

    private Combobox cbEnvLayers;
    private Listbox lbSelLayers;
    public Popup popup_continous;
    public Slider popup_slider_min;
    public Slider popup_slider_max;
    public Label popup_range;
    public Textbox popup_minimum;
    public Textbox popup_maximum;
    public Textbox popup_idx;
    public Popup popup_catagorical;
    public Listbox popup_listbox;
    public Textbox popup_results_seek;
    public Button apply_continous;
    public Button apply_catagorical;
    private List<String> selectedLayers;
    private List _layer_filters = new ArrayList();
    //private SPLFilter[] selectedSPLFilterLayers;
    private Map<String, SPLFilter> selectedSPLFilterLayers;
    private String pid;

    private MapComposer mc;
    private String geoServer = "http://localhost:8080";
    private String satServer = "http://localhost:8080";

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
        if (mc == null) {
            System.out.println("mcobj is null");
        } else {
            System.out.println("mcobj is NOT null");
        }
        if (mc.getSettingsSupplementary() == null) {
            System.out.println("mc.ss is null");
        } else {
            System.out.println("mc.ss is NOT null"); 
        }
        if (mc.getSettings() == null) {
            System.out.println("mc.gs is null");
        } else {
            System.out.println("mc.gs is NOT null");
        }
        //geoServer = mc.getSettingsSupplementary().getValue(GEOSERVER_URL);
        //satServer = mc.getSettingsSupplementary().getValue(SAT_URL);


        selectedLayers = new Vector<String>();
        selectedSPLFilterLayers = new Hashtable<String, SPLFilter>();


        // init the session on the server and get a pid (process_id)
        pid = getInfo("/filtering/init");
        System.out.println("PID:  " + pid);

    }

    public void onChange$cbEnvLayers(Event event) {
        String new_value = "";

        try {

            /*
            System.out.println("Getting SPLFilter from WS");
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod("http://localhost:8080/alaspatial/ws/spatial/settings/layer/" + URLEncoder.encode(new_value, "UTF-8") + "/splfilter"); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, * /*");
            //get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from ALOCWSController: \n" + slist);

            //Map classMap = new HashMap();
            //classMap.put("layer", Layer.class);
            JSONObject jo = JSONObject.fromObject(slist);
            //SPLFilter splf = (SPLFilter) JSONObject.toBean(jo, SPLFilter.class, classMap);

            JSONObject lbean = jo.getJSONObject("layer");
            Layer rLayer = new Layer(lbean.getString("name"), lbean.getString("display_name"), lbean.getString("description"), lbean.getString("type"), null);
            SPLFilter splf = new SPLFilter();
            splf.setCount(jo.getInt("count"));
            splf.setLayername(jo.getString("layername"));
            //splf.setCatagories(null);
            splf.setLayer(rLayer);
            splf.setMinimum_value(jo.getDouble("minimum_value"));
            splf.setMaximum_value(jo.getDouble("maximum_value"));
            splf.setMinimum_initial(jo.getDouble("minimum_initial"));
            splf.setMaximum_initial(jo.getDouble("maximum_initial"));
            splf.setChanged(jo.getBoolean("changed"));
            splf.setFilterString(jo.getString("filterString"));

             */

            new_value = cbEnvLayers.getValue();
            if (new_value.equals("") || new_value.indexOf("(") < 0) {
                return;
            }
            new_value = (new_value.equals("")) ? "" : new_value.substring(0, new_value.indexOf("(")).trim();
            System.out.println("new value: " + new_value);


            if (selectedLayers.contains(new_value)) {
                System.out.println("is not new");
            } else {
                System.out.println("is new");
                selectedLayers.add(new_value);
            }


            /* apply something to line onclick in lb */

            lbSelLayers.setItemRenderer(new ListitemRenderer() {

                public void render(Listitem li, Object data) {
                    String layername = (String) data;

                    // Col 1: Add the layer name
                    new Listcell(layername + " (Terrestrial)").setParent(li);

                    // Col 2: Add the filter string and set the onClick event
                    String filterString = getFilterString(layername);
                    System.out.println("Filter String for " + layername + " is " + filterString);

                    Listcell lc = new Listcell(filterString); // f.getFilterString()
                    lc.setParent(li);

                    lc.addEventListener("onClick", new EventListener() {

                        public void onEvent(Event event) throws Exception {
                            if (!((Listcell) event.getTarget()).getLabel().equals("")
                                    && !((Listitem) event.getTarget().getParent()).isDisabled()) {
                                showAdjustPopup(event.getTarget());
                            }
                        }
                    });

                    // Col 3: Add the species count and set the onClick event
                    Listcell count = new Listcell("0"); // String.valueOf(f.count)
                    count.setParent(li);
                    count.addEventListener("onClick", new EventListener() {

                        public void onEvent(Event event) throws Exception {
                            //if(results != null && results.length() > 0){
                            if (!((Listcell) event.getTarget()).getLabel().equals("0")
                                    && !((Listitem) event.getTarget().getParent()).isDisabled()) {
                                /*
                                SPLFilter[] layer_filters = getSelectedFilters();
                                if (layer_filters != null) {
                                results = SpeciesListIndex.listArraySpeciesGeo(layer_filters);
                                java.util.Arrays.sort(results);

                                seekToResultsPosition(0);

                                popup_results.open(30, 30);//.open(event.getTarget());
                                }
                                 *
                                 */
                            }
                            //}
                        }
                    });

                    // Col 4: Add the action to remove and set onClick event
                    Listcell remove = new Listcell("remove");
                    remove.setParent(li);
                    remove.addEventListener("onClick", new EventListener() {

                        public void onEvent(Event event) throws Exception {
                            if (!((Listcell) event.getTarget()).getLabel().equals("")
                                    && !((Listitem) event.getTarget().getParent()).isDisabled()) {
                                deleteSelectedFilters(event.getTarget());
                            }
                        }
                    });

                }
            });

            lbSelLayers.setModel(new SimpleListModel(selectedLayers));
            Listitem li = lbSelLayers.getItemAtIndex(lbSelLayers.getItemCount() - 1);
            Listcell lc = (Listcell) li.getLastChild();
            System.out.println(lc);
            lc = (Listcell) lc.getPreviousSibling();

            listFix();


        } catch (Exception e) {
            e.printStackTrace(System.out);
        }


        ///showAdjustPopup(lc);
    }

    private String getFilterString(String layername) {
        return getInfo(layername, "filter");
    }

    private SPLFilter getSPLFilter(String layername) {
        SPLFilter splf = null;


        // First check if already present
        splf = selectedSPLFilterLayers.get(layername);

        // if splf is still null, then it must be new
        // so grab the details from the server
        if (splf == null) {

            System.out.println("new layers, generating splf ");

            //Map classMap = new HashMap();
            //classMap.put("layer", Layer.class);
            JSONObject jo = JSONObject.fromObject(getInfo(layername, "splfilter"));
            //SPLFilter splf = (SPLFilter) JSONObject.toBean(jo, SPLFilter.class, classMap);

            JSONObject lbean = jo.getJSONObject("layer");
            Layer rLayer = new Layer(lbean.getString("name"), lbean.getString("display_name"), lbean.getString("description"), lbean.getString("type"), null);
            splf = new SPLFilter();
            splf.setCount(jo.getInt("count"));
            splf.setLayername(jo.getString("layername"));
            //splf.setCatagories(null);
            splf.setLayer(rLayer);
            splf.setMinimum_value(jo.getDouble("minimum_value"));
            splf.setMaximum_value(jo.getDouble("maximum_value"));
            splf.setMinimum_initial(jo.getDouble("minimum_initial"));
            splf.setMaximum_initial(jo.getDouble("maximum_initial"));
            splf.setChanged(jo.getBoolean("changed"));
            splf.setFilterString(jo.getString("filterString"));

            selectedSPLFilterLayers.put(layername, splf);
        } else {
            System.out.println("splf already present. ");
        }

        return splf;

    }

    private String getInfo(String value, String type) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + URLEncoder.encode(value, "UTF-8") + "/" + type); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, * /*");
            //get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("Got response from ALOCWSController: \n" + slist);

            return slist;
        } catch (Exception ex) {
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
            //get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("Got response from ALOCWSController: \n" + slist);

            return slist;
        } catch (Exception ex) {
            //Logger.getLogger(FilteringWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    public void deleteSelectedFilters(Object o) {
        Listbox lb = null;
        Listitem li;
        String label;
        if (o == null) {
            li = (Listitem) lb.getSelectedItem();
        } else {
            li = (Listitem) ((Listcell) o).getParent();
        }
        int idx = li.getIndex();

        label = selectedLayers.get(idx);

        System.out.println("deleteSelectedFilters(" + label + ")");


        /*
        for (Object oi : _layer_filters_selected) {
        SPLFilter f = (SPLFilter) oi;

        if (f.layer.display_name.equals(label)) {
        ((SPLFilter) oi).count = 0;

        _layer_filters_selected.remove(oi);
        int i = 0;
        for (i = 0; i < _layer_filters_original.size(); i++) {
        if (((SPLFilter) _layer_filters_original.get(i)).layer.display_name.equals(label)) {
        if (((SPLFilter) _layer_filters_original.get(i)).layer.type.equals("environmental")) {
        Clients.evalJavaScript("applyFilter(" + i + ",-999,100000);");
        } else {
        String javascript = "applyFilterCtx(" + i + ",-2,true);";
        Clients.evalJavaScript(javascript);
        }
        System.out.println("done client applyFilter call for idx=" + i);
        break;
        }
        }

        System.out.println("deleting from seletion list success!");

        break;
        }

        }
         * 
         */

        selectedLayers.remove(label);

        li.detach();

        listFix();
    }

    public void listFix() {
        int i;
        List list = lbSelLayers.getItems();
        for (i = 0; list != null && i < list.size() - 1; i++) {
            Listitem li = (Listitem) list.get(i);
            ((Listcell) li.getFirstChild()).setLabel("");

            li.setDisabled(true);
        }
        if (list != null && list.size() > 0) {
            Listitem li = (Listitem) list.get(i);
            ((Listcell) li.getFirstChild()).setLabel("remove");

            li.setDisabled(false);
        }


    }

    public void onScroll$popup_slider_min(Event event) {
        System.out.println("Changing min slider");
        try {

            int curpos = popup_slider_min.getCurpos();

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

            popup_minimum.setValue(String.valueOf((float) (curpos / 100.0 * range + popup_filter.minimum_initial)));

            popup_filter.minimum_value = Double.parseDouble(popup_minimum.getValue());

            //((Listcell) popup_item.getLastChild().getPreviousSibling()).setLabel(popup_filter.getFilterString());
            ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

            serverFilter();

        } catch (Exception e) {
            System.out.println("slider change min:" + e.toString());
            e.printStackTrace(System.out);
        }
    }

    public void onScroll$popup_slider_max(Event event) {
        System.out.println("Changing min slider");
        try {

            int curpos = popup_slider_max.getCurpos();

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

            popup_maximum.setValue(String.valueOf((float) (curpos / 100.0 * range + popup_filter.minimum_initial)));

            popup_filter.maximum_value = Double.parseDouble(popup_maximum.getValue());

            //((Listcell) popup_item.getLastChild().getPreviousSibling()).setLabel(popup_filter.getFilterString());
            ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

            serverFilter();
        } catch (Exception e) {
            System.out.println("slider change max:" + e.toString());
            e.printStackTrace(System.out);
        }
    }

    private void showAdjustPopup(Object o) {
        System.out.println("showAdjustPopup");
        String layername = "";
        if (o == null) {

            //get the Count cell
            int i = lbSelLayers.getItemCount();
            System.out.println("items: " + i);
            Listitem li = (Listitem) lbSelLayers.getItemAtIndex(i - 1);
            System.out.println(li.getLabel());
            List list = li.getChildren();

            for (Object o2 : list) {
                Listcell m = (Listcell) o2;
                System.out.println("**" + o2 + ">" + m.getLabel());
            }
            //layername = ((Listcell) list.get(1)).getLabel();
            o = list.get(list.size() - 2);
            System.out.println(li);
            System.out.println(o);
        }

        Listcell lc = (Listcell) o;
        Listitem li = (Listitem) lc.getParent();
        layername = ((Listcell) li.getChildren().get(0)).getLabel();
        layername = (layername.equals("")) ? "" : layername.substring(0, layername.indexOf("(")).trim();

        System.out.println("sAP.layername: " + layername);
        popup_filter = getSPLFilter(layername);
        popup_idx.setValue(layername);

        popup_cell = lc;
        popup_item = li;

        System.out.println("layer type:" + popup_filter.layer.type);

        if (popup_filter.layer.type.equalsIgnoreCase("environmental")) {

            //String csv = getInfo(layername,"extents");
            String csv = popup_filter.maximum_value + " - " + popup_filter.maximum_value;
            popup_range.setValue(csv);

            popup_minimum.setValue(Double.toString(popup_filter.minimum_value));
            popup_maximum.setValue(Double.toString(popup_filter.maximum_value));

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
            int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                    / (range) * 100);
            int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                    / (range) * 100);

            System.out.println("range:" + range + " mincursor:" + mincursor + " maxcursor:" + maxcursor);

            //Clients.evalJavaScript("applyFilter(" + idx + "," + (mincursor / 100.0) + "," + (maxcursor / 100.0) + ");"); 	//should take out missing value
            ///filteringImage.applyFilter(idx, mincursor / 100.0, maxcursor / 100.0);
            //doApplyFilter(pid, layername, "environmental", Double.toString(mincursor / 100.0), Double.toString(maxcursor / 100.0));
            doApplyFilter(pid);

            popup_slider_min.setCurpos(mincursor);
            popup_slider_max.setCurpos(maxcursor);

            lc.focus();
            System.out.println("attaching: " + lc + lc.getValue());
            popup_continous.open(li); // .open(30, 30);
        }

    }

    private void serverFilter() {
        double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
        double maxcurpos = ((popup_filter.maximum_value - popup_filter.minimum_initial)
                / (range));
        double mincurpos = ((popup_filter.minimum_value - popup_filter.minimum_initial)
                / (range));

        System.out.println(popup_filter.maximum_initial + ", " + popup_filter.minimum_initial
                + ", " + popup_filter.maximum_value + "," + popup_filter.minimum_value);
        String idx = popup_idx.getValue();

        System.out.println("applying filter from idx:" + idx);

        if (popup_filter.layer.type.equalsIgnoreCase("environmental")) {
            //doApplyFilter(pid, idx, "environmental", Double.toString(mincurpos), Double.toString(maxcurpos));
            // SPLFilter sf =
            doApplyFilter(pid);
        } else {
            // write code for categorical 
        }





    }

    private void doApplyFilter(String pid) {
        try {

            String[] layers = new String[selectedSPLFilterLayers.size()];
            String[] types = new String[selectedSPLFilterLayers.size()];
            String[] mins = new String[selectedSPLFilterLayers.size()];
            String[] maxs = new String[selectedSPLFilterLayers.size()];
            //Iterator it = selectedSPLFilterLayers.iterator();

            Object[] keys = selectedSPLFilterLayers.keySet().toArray();

            for (int i = 0; i < selectedSPLFilterLayers.size(); i++) {
                SPLFilter sf = selectedSPLFilterLayers.get((String) keys[i]);
                // get the layers
                layers[i] = sf.layer.display_name;
                types[i] = sf.layer.type;
                mins[i] = Double.toString(sf.minimum_value);
                maxs[i] = Double.toString(sf.maximum_value);

            }

            // iterate thru' the list and build the url
            StringBuilder sbUrl = new StringBuilder();
            sbUrl.append("/filtering/apply2");
            sbUrl.append("/pid/" + pid);
            sbUrl.append("/layers/" + URLEncoder.encode(join(layers, ":"), "UTF-8"));
            sbUrl.append("/types/" + URLEncoder.encode(join(types, ":"), "UTF-8"));
            sbUrl.append("/val1s/" + URLEncoder.encode(join(mins, ":"), "UTF-8"));
            sbUrl.append("/val2s/" + URLEncoder.encode(join(maxs, ":"), "UTF-8"));


            System.out.println("Applying server side filter with only the pid");
            System.out.println(sbUrl.toString());
            getInfo(sbUrl.toString());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    private void doApplyFilter(String pid, int layerIndex, String val1, String val2) {
        String urlPart = "";
        urlPart += "/filtering/apply";
        urlPart += "/pid/" + pid;
        urlPart += "/layer/" + layerIndex;
        urlPart += "/val1/" + val1;
        urlPart += "/val2/" + val2;

        System.out.println("Applying server side filter");
        getInfo(urlPart);
    }

    private void doApplyFilter(String pid, String layername, String type, String val1, String val2) {
        try {

            String urlPart = "";
            urlPart += "/filtering/apply";
            urlPart += "/pid/" + pid;
            urlPart += "/layer/" + URLEncoder.encode(layername, "UTF-8");
            urlPart += "/type/" + type;
            urlPart += "/val1/" + val1;
            urlPart += "/val2/" + val2;

            System.out.println("Applying server side filter");
            getInfo(urlPart);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void onClick$apply_continous(Event event) {
        System.out.println("applycontinous" + event.toString());
        applyFilter();
    }

    public void onClick$apply_catagorical(Event event) {
        System.out.println("applycontinous" + event.toString());
        applyFilter();
    }

    private void applyFilter() {
        /* two cases: catagorical and continous */
        if (popup_filter.catagory_names == null) {
            try {
                popup_filter.minimum_value = Double.parseDouble(popup_minimum.getValue());
                popup_filter.maximum_value = Double.parseDouble(popup_maximum.getValue());
                System.out.println(popup_filter.minimum_value + "," + popup_filter.maximum_value);
            } catch (Exception e) {
                System.out.println("value conversion error");
            }
        } else {
            Set selected = popup_listbox.getSelectedItems();
            int[] items_selected = new int[selected.size()];
            int pos = 0;

            for (Object o : selected) {
                Listitem li = (Listitem) o;
                items_selected[pos++] = li.getIndex();
            }

            popup_filter.catagories = items_selected;
            System.out.println("selected catagories: " + popup_filter.catagories.length);
        }

        popup_catagorical.close();
        popup_continous.close();

        ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

        String strCount = getInfo("/filtering/apply/pid/" + pid + "/species/count");
        popup_filter.count = Integer.parseInt(strCount);
        ((Listcell) popup_item.getChildren().get(2)).setLabel(strCount);

        serverFilter();
    }

    private String join(String[] arr, String glue) {
        int s = arr.length;
        if (s == 0) {
            return null;
        }
        StringBuilder sbString = new StringBuilder();
        sbString.append(arr[0]);
        for (int i = 1; i < s; ++i) {
            sbString.append(glue).append(arr[i]);
        }
        return sbString.toString();
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

}
