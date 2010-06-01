package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SPLFilter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.ArrayUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
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
    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String GEOSERVER_USERNAME = "geoserver_username";
    private static final String GEOSERVER_PASSWORD = "geoserver_password";
    private static final String SAT_URL = "sat_url";
    private EnvLayersCombobox cbEnvLayers;
    private Listbox lbSelLayers;
    public Div popup_continous;
    public Slider popup_slider_min;
    public Slider popup_slider_max;
    public Label popup_range;
    public Textbox popup_minimum;
    public Textbox popup_maximum;
    public Textbox popup_idx;
    public Div popup_catagorical;
    public Button remove_catagorical;
    public Button remove_continous;
    public Button preview_catagorical;
    public Button preview_continous;
    
    public Label label_catagorical;
    public Label label_continous;
    public Listbox popup_listbox;
   // public Textbox popup_results_seek;
    public Textbox popup_results_seek;
    public Textbox prs;
    public Button apply_continous;
    public Button apply_catagorical;
    public Button download;
    public Button downloadsamples;
   // public Listbox popup_listbox_results;
    //public Window popup_results;
   // public Button results_prev;
   // public Button results_next;
   // public Label results_label;
   // int results_pos;
    String[] results = null;
    private List<String> selectedLayers;
    private List _layer_filters = new ArrayList();
    //private SPLFilter[] selectedSPLFilterLayers;
    private Map<String, SPLFilter> selectedSPLFilterLayers;
    private String pid;
    private MapComposer mc;
    private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com"; // http://localhost:8080
    private String satServer = geoServer;
    private SettingsSupplementary settingsSupplementary = null;
    
    
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
            geoServer = settingsSupplementary.getValue(GEOSERVER_URL);
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        selectedLayers = new Vector<String>();
        selectedSPLFilterLayers = new Hashtable<String, SPLFilter>();


        // init the session on the server and get a pid (process_id)
        pid = getInfo("/filtering/init");
        System.out.println("PID:  " + pid);

    }

    public void onChange$cbEnvLayers(Event event) {
        String new_value = "";

        if(lbSelLayers.getItemCount() > 0){
        	applyFilter();
        }
        
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
                    SPLFilter f = getSPLFilter(layername);

                    // Col 1: Add the layer name
                    Listcell lname = new Listcell(f.layer.display_name + " (Terrestrial)");
                    lname.setStyle("white-space: normal;");
                    lname.setParent(li);

                    // Col 2: Add the filter string and set the onClick event
                    //String filterString = getFilterString(layername);
                    String filterString = f.getFilterString();
                    System.out.println("Filter String for " + layername + " is " + filterString);

                    Listcell lc = new Listcell(filterString); // f.getFilterString()
                    lc.setParent(li);

                    
                    /*always visible, this bit not required anymore
                     * lc.addEventListener("onClick", new EventListener() {

                        public void onEvent(Event event) throws Exception {
                            if (!((Listcell) event.getTarget()).getLabel().equals("")
                                    && !((Listitem) event.getTarget().getParent()).isDisabled()) {
                                showAdjustPopup(event.getTarget());
                            }
                        }
                    });*/

                    // Col 3: Add the species count and set the onClick event
                    Listcell count = new Listcell(String.valueOf(f.count));
                    count.setStyle("text-decoration: underline; text-align: right; ");
                    //count.setStyle("word-wrap: break-word");
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

                            	
                                if (lbSelLayers.getItemCount() > 0) {
                                	applyFilter();
                                	java.util.Map args = new java.util.HashMap();                                                     
                                	args.put("pid",pid);
                                	Window win = (Window) Executions.createComponents(
                                            "/WEB-INF/zul/AnalysisFilteringResults.zul", null, args);
                                	win.doModal();
                                
                                }
                            }
                            //}
                        }
                    });

                    // Col 4: Add the action to remove and set onClick event
                   /* Listcell remove = new Listcell("remove");
                    remove.setStyle("text-decoration: underline;");
                    remove.setStyle("width: 30px");
                    remove.setStyle("text-align: center");
                    remove.setParent(li);
                    remove.addEventListener("onClick", new EventListener() {

                        public void onEvent(Event event) throws Exception {
                            if (!((Listcell) event.getTarget()).getLabel().equals("")
                                    && !((Listitem) event.getTarget().getParent()).isDisabled()) {
                                deleteSelectedFilters(event.getTarget());
                            }
                        }
                    });*/

                }
            });

            lbSelLayers.setModel(new SimpleListModel(selectedLayers));
            System.out.println("total items: " + lbSelLayers.getItemCount());
            Listitem li = lbSelLayers.getItemAtIndex(lbSelLayers.getItemCount() - 1);
            List lich = li.getChildren();
            System.out.println("li: \n" + li + " - " + lich.size());
            for (int i = 0; i < lich.size(); i++) {
                Listcell tlc = (Listcell) lich.get(i);
                System.out.println(i + ": " + tlc.getLabel() + " -- " + tlc.getChildren().size());
            }
            Listcell lc = (Listcell) li.getFirstChild();
            System.out.print("first child: ");
            System.out.println(lc.getValue() + " - " + lc.getLabel());
            lc = (Listcell) lc.getNextSibling();
            System.out.println("lc: \n" + lc);

            listFix();

            showAdjustPopup(new_value,lc,li);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }


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
            JSONObject jo = JSONObject.fromObject(getInfo(layername, "layerfilter"));
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
            //splf.setFilterString(jo.getString("toString"));

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
         Listitem li;
        String label;
        
        popup_continous.setVisible(false);
        popup_catagorical.setVisible(false);
       
        if (o == null) {
            //li = (Listitem) lb.getSelectedItem();
        	
        	//change to get last item
        	int count = lbSelLayers.getItemCount();
        	li = (Listitem) lbSelLayers.getItemAtIndex(count-1);
        } else {
            li = (Listitem) ((Listcell) o).getParent();
        }
        int idx = li.getIndex();

        label = selectedLayers.get(idx);

        System.out.println("deleteSelectedFilters(" + label + ")");

        StringBuffer sbProcessUrl = new StringBuffer();
        sbProcessUrl.append("/filtering/apply4");
        sbProcessUrl.append("/pid/" + pid);
        sbProcessUrl.append("/layers/none");
        sbProcessUrl.append("/types/none");
        sbProcessUrl.append("/val1s/none");
        sbProcessUrl.append("/val2s/none");
        sbProcessUrl.append("/depth/" + lbSelLayers.getItemCount());

        String imagefilepath = getInfo(sbProcessUrl.toString());


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

        System.out.println("filtering.removing layer: " + "Filtering - " + pid + " - layer " + lbSelLayers.getItemCount());
        mc.removeLayer("Filtering - " + pid + " - layer " + lbSelLayers.getItemCount());

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
          //  ((Listcell) li.getLastChild()).setLabel("");

            li.setDisabled(true);
        }
       if (list != null && list.size() > 0) {
            Listitem li = (Listitem) list.get(i);
          //  ((Listcell) li.getLastChild()).setLabel("remove");

            li.setDisabled(false);
        }


    }

    public void onScroll$popup_slider_min(Event event) {
        System.out.println("Changing min slider");
        try {

            int curpos = popup_slider_min.getCurpos();

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

            popup_minimum.setValue(String.format("%.4f",((float) (curpos / 100.0 * range + popup_filter.minimum_initial))));

            popup_filter.minimum_value = Double.parseDouble(popup_minimum.getValue());

            //((Listcell) popup_item.getLastChild().getPreviousSibling()).setLabel(popup_filter.getFilterString());
            ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

            serverFilter(false);

        } catch (Exception e) {
            System.out.println("slider change min:" + e.toString());
            e.printStackTrace(System.out);
        }
    }

    public void onScroll$popup_slider_max(Event event) {
        System.out.println("Changing max slider");
        try {

            int curpos = popup_slider_max.getCurpos();

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

            popup_maximum.setValue(String.format("%.4f",((float) (curpos / 100.0 * range + popup_filter.minimum_initial))));

            popup_filter.maximum_value = Double.parseDouble(popup_maximum.getValue());

            //((Listcell) popup_item.getLastChild().getPreviousSibling()).setLabel(popup_filter.getFilterString());
            ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

            serverFilter(false);
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
            if(i < 1){
            	popup_continous.setVisible(false);
                popup_catagorical.setVisible(false);
                return;
            }
            System.out.println("items: " + i);
            Listitem li = (Listitem) lbSelLayers.getItemAtIndex(i - 1);
            System.out.println(li.getLabel());
            List list = li.getChildren();

            System.out.println("list has " + list.size() + " children");

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

        	label_continous.setValue("edit filter: " + layername);
            //String csv = getInfo(layername,"extents");
            String csv = String.format("%.4f",(float)popup_filter.minimum_value) + " - " + String.format("%.4f",(float)popup_filter.maximum_value);
            popup_range.setValue(csv);

            popup_minimum.setValue(String.format("%.4f", (float)(popup_filter.minimum_value)));
            popup_maximum.setValue(String.format("%.4f", (float)(popup_filter.maximum_value)));

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
            int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                    / (range) * 100);
            int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                    / (range) * 100);

            System.out.println("range:" + range + " mincursor:" + mincursor + " maxcursor:" + maxcursor);

            //Clients.evalJavaScript("applyFilter(" + idx + "," + (mincursor / 100.0) + "," + (maxcursor / 100.0) + ");"); 	//should take out missing value
            ///filteringImage.applyFilter(idx, mincursor / 100.0, maxcursor / 100.0);
            doApplyFilter(pid, layername, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), false);
            //doApplyFilter(pid);

            popup_slider_min.setCurpos(mincursor);
            popup_slider_max.setCurpos(maxcursor);

            lc.focus();
            System.out.println("attaching: " + lc + lc.getValue());
            popup_continous.setVisible(true);//.open(li); // .open(30, 30);
            popup_catagorical.setVisible(false);
        } else { //catagorical values

        	label_catagorical.setValue("edit filter: " + layername);
        	
            if (popup_filter.catagory_names != null) {
                System.out.println("popup_filter.catagory_names is NOT null: " + popup_filter.catagory_names.length);

            } else {
                System.out.println("popup_filter.catagory_names is null");
            }

            popup_listbox.setModel(new SimpleListModel(popup_filter.catagory_names));

            int idx = 0;
            for (idx = 0; idx < _layer_filters.size(); idx++) {
                if (((SPLFilter) _layer_filters.get(idx)).layer.name == popup_filter.layer.name) {
                    System.out.println("popup for " + popup_filter.layer.display_name);
                    break;
                }
            }
            popup_idx.setValue(String.valueOf(idx));

            String javascript = "applyFilterCtx(" + idx + ",-2,false);"; 	//should take out missing value
            //filteringImage.applyFilterCtx(idx,-2,false);

            /* set check boxes */
            for (int i : popup_filter.catagories) {
                Listitem listitem = popup_listbox.getItemAtIndex(i);
                popup_listbox.addItemToSelection(listitem);
            }
            for (int i = 0; i < popup_filter.catagory_names.length; i++) {
                int j = 0;
                for (j = 0; j < popup_filter.catagories.length; j++) {
                    if (i == popup_filter.catagories[j]) {
                        javascript += "applyFilterCtx(" + idx + "," + i + ",true);"; 	//should take out missing value
                        //filteringImage.applyFilterCtx(idx,i,true);
                        break;
                    }
                }
                if (j == popup_filter.catagories.length) {
                    //hide
                    //	javascript += "applyFilterCtx(" + idx + "," + i + ",false);"; 	//should take out missing value
                    //	filteringImage.applyFilterCtx(idx,i,false);
                }
            }
            lc.focus();
            //		System.out.println("attaching: " + lc + lc.getValue());
            popup_catagorical.setVisible(true);//.open(30, 30);//.open(li);
            popup_continous.setVisible(false);
            //Clients.evalJavaScript(javascript);

            Set selected = popup_listbox.getSelectedItems();
            int[] items_selected = new int[selected.size()];
            int pos = 0;

            for (Object obj : selected) {
                Listitem lisel = (Listitem) obj;
                items_selected[pos++] = lisel.getIndex();
            }

            popup_filter.catagories = items_selected;

            doApplyFilter(pid, popup_filter.layer.display_name, popup_filter.catagories, false);


        }

    }
    
    private void showAdjustPopup(String layername, Listcell lc, Listitem li) {
        System.out.println("sAP.layername: " + layername);
        
        popup_filter = getSPLFilter(layername);
        popup_idx.setValue(layername);

        popup_cell = lc;
        popup_item = li;

        System.out.println("layer type:" + popup_filter.layer.type);

        if (popup_filter.layer.type.equalsIgnoreCase("environmental")) {

        	label_continous.setValue("edit filter: " + layername);
        	
            //String csv = getInfo(layername,"extents");
        	String csv = String.format("%.4f",(float)popup_filter.minimum_value) + " - " + String.format("%.4f",(float)popup_filter.maximum_value);
            popup_range.setValue(csv);

            popup_minimum.setValue(String.format("%.4f", (float)(popup_filter.minimum_value)));
            popup_maximum.setValue(String.format("%.4f", (float)(popup_filter.maximum_value)));

            double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
            int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
                    / (range) * 100);
            int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
                    / (range) * 100);

            System.out.println("range:" + range + " mincursor:" + mincursor + " maxcursor:" + maxcursor);

            //Clients.evalJavaScript("applyFilter(" + idx + "," + (mincursor / 100.0) + "," + (maxcursor / 100.0) + ");"); 	//should take out missing value
            ///filteringImage.applyFilter(idx, mincursor / 100.0, maxcursor / 100.0);
            doApplyFilter(pid, layername, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), false);
            //doApplyFilter(pid);

            popup_slider_min.setCurpos(mincursor);
            popup_slider_max.setCurpos(maxcursor);

           // lc.focus();
           // System.out.println("attaching: " + lc + lc.getValue());
            popup_continous.setVisible(true);//.open(li); // .open(30, 30);
            popup_catagorical.setVisible(false);
        } else { //catagorical values

        	label_catagorical.setValue("edit filter: " + layername);
            if (popup_filter.catagory_names != null) {
                System.out.println("popup_filter.catagory_names is NOT null: " + popup_filter.catagory_names.length);

            } else {
                System.out.println("popup_filter.catagory_names is null");
            }

            popup_listbox.setModel(new SimpleListModel(popup_filter.catagory_names));

            int idx = 0;
            for (idx = 0; idx < _layer_filters.size(); idx++) {
                if (((SPLFilter) _layer_filters.get(idx)).layer.name == popup_filter.layer.name) {
                    System.out.println("popup for " + popup_filter.layer.display_name);
                    break;
                }
            }
            popup_idx.setValue(String.valueOf(idx));

            String javascript = "applyFilterCtx(" + idx + ",-2,false);"; 	//should take out missing value
            //filteringImage.applyFilterCtx(idx,-2,false);

            /* set check boxes */
            for (int i : popup_filter.catagories) {
                Listitem listitem = popup_listbox.getItemAtIndex(i);
                popup_listbox.addItemToSelection(listitem);
            }
            for (int i = 0; i < popup_filter.catagory_names.length; i++) {
                int j = 0;
                for (j = 0; j < popup_filter.catagories.length; j++) {
                    if (i == popup_filter.catagories[j]) {
                        javascript += "applyFilterCtx(" + idx + "," + i + ",true);"; 	//should take out missing value
                        //filteringImage.applyFilterCtx(idx,i,true);
                        break;
                    }
                }
                if (j == popup_filter.catagories.length) {
                    //hide
                    //	javascript += "applyFilterCtx(" + idx + "," + i + ",false);"; 	//should take out missing value
                    //	filteringImage.applyFilterCtx(idx,i,false);
                }
            }
            //lc.focus();
            //		System.out.println("attaching: " + lc + lc.getValue());
            popup_catagorical.setVisible(true);//.open(30, 30);//.open(li);
            popup_continous.setVisible(false);
            //Clients.evalJavaScript(javascript);

            Set selected = popup_listbox.getSelectedItems();
            int[] items_selected = new int[selected.size()];
            int pos = 0;

            for (Object obj : selected) {
                Listitem lisel = (Listitem) obj;
                items_selected[pos++] = lisel.getIndex();
            }

            popup_filter.catagories = items_selected;

            doApplyFilter(pid, popup_filter.layer.display_name, popup_filter.catagories, false);


        }

    }
/*
    public void onClick$results_prev(Event event) {
        if (results_pos == 0) {
            return;
        }

        seekToResultsPosition(results_pos - 15);
    }

    public void onClick$results_next(Event event) {
        if (results_pos + 15 >= results.length) {
            return;
        }

        seekToResultsPosition(results_pos + 15);
    }

    public void onClick$download() {
        if (lbSelLayers.getItemCount() > 0) {
            StringBuffer sb = new StringBuffer();
            for (String s : results) {
                sb.append(s);
                sb.append("\r\n");
            }
            Filedownload.save(sb.toString(), "text/plain", "filter.csv");
        } else {
        }
    }

    public void onClick$downloadsamples() {
        if (lbSelLayers.getItemCount() > 0) {
            try {
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append("/filtering/apply");
                sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
                sbProcessUrl.append("/samples/list");
                //String point = lb_points.getValue();
                //if (point.length() == 0) {
                //    point = "none";
                //}
                //sbProcessUrl.append("/shape/" + URLEncoder.encode(point, "UTF-8"));
                sbProcessUrl.append("/shape/none");
                System.out.println("attempt to download: " + satServer + "/alaspatial/ws" + sbProcessUrl.toString());
                String samplesfile = getInfo(sbProcessUrl.toString());
                //org.zkoss.zul.Filedownload.save(new URL(outputfile), "application/zip");
                URL u = new URL(satServer + "/alaspatial/" + samplesfile);
                System.out.println("opening stream to " + samplesfile); 
                Filedownload.save(u.openStream(), "application/zip", "filter_samples_" + pid + ".zip");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
        }
    }*/

    /*public void onChange$popup_results_seek(InputEvent event) {
        if (!event.isChangingBySelectBack()) {
            System.out.println("onchange triggered");
    /*
    public void onChange$popup_results_seek(InputEvent event) {
    if (!event.isChangingBySelectBack()) {
    System.out.println("onchange triggered");
    }

    }
     *
     */

    private void serverFilter(boolean commit) {
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
            doApplyFilter(pid, popup_filter.layer.display_name, "environmental", Double.toString(popup_filter.minimum_value), Double.toString(popup_filter.maximum_value), commit);
            // SPLFilter sf =
            //doApplyFilter(pid);
        } else {
            // write code for categorical
            doApplyFilter(pid, popup_filter.layer.display_name, popup_filter.getCatagories(), commit);
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

    private void doApplyFilter(String pid, String layername, int[] catagories_to_show, boolean commit) {
        StringBuffer show = new StringBuffer();
        int i;
        for (i = 0; i < catagories_to_show.length; i++) {
            show.append(catagories_to_show[i]);
            if (i < catagories_to_show.length - 1) {
                show.append(",");
            }
        }

        doApplyFilter(pid, layername, "ctx", show.toString(), "none", commit);
    }

    private void doApplyFilter(String pid, String layername, String type, String val1, String val2, boolean commit) {
        try {

            System.out.print("Apply filter to layer: ");
            System.out.println(lbSelLayers.getSelectedItems().size());
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

            System.out.println("Applying server side filter");
            String imagefilepath = getInfo(urlPart);
            loadMap(imagefilepath);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void onClick$apply_continous(Event event) {
        System.out.println("applycontinous" + event.toString());
        applyFilter();
    }
    
    public void onClick$remove_continous(Event event) {
    	deleteSelectedFilters(null);
    }
    
    public void onClick$preview_continous(Event event) {
    	try{
    		applyFilter();
    	if (lbSelLayers.getItemCount() > 0) {
    		
    		java.util.Map args = new java.util.HashMap();                                                     
        	args.put("pid",pid);
    		Window win = (Window) Executions.createComponents(
                    "/WEB-INF/zul/AnalysisFilteringResults.zul", null, args);
        	win.doModal();
        }
    	}catch(Exception e){
    		
    	}
    }

    public void onClick$apply_catagorical(Event event) {
        System.out.println("applycontinous" + event.toString());
        applyFilter();
    }
    
    public void onClick$remove_catagorical(Event event) {
    	deleteSelectedFilters(null);
    }
    
    public void onClick$preview_catagorical(Event event) {
    	try{
    	if (lbSelLayers.getItemCount() > 0) {
    		applyFilter();
    		java.util.Map args = new java.util.HashMap();                                                     
        	args.put("pid",pid);
    		Window win = (Window) Executions.createComponents(
                    "/WEB-INF/zul/AnalysisFilteringResults.zul", null, args);
        	win.doModal();
        }
    	}catch(Exception e){
    		
    	}
    }

    public void onLater(Event event) throws Exception {
        applyFilterEvented();
        Clients.showBusy("", false);
    }

    public void applyFilter() {
        Clients.showBusy("Applying filter, please wait...", true);
        Events.echoEvent("onLater", this, null);
    }

    private void applyFilterEvented() {
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

        //popup_catagorical.setVisible(false);
        //popup_continous.setVisible(false);

        ((Listcell) popup_item.getChildren().get(1)).setLabel(popup_filter.getFilterString());

        serverFilter(true);

        String strCount = getInfo("/filtering/apply/pid/" + pid + "/species/count/shape/none");
        popup_filter.count = Integer.parseInt(strCount);
        ((Listcell) popup_item.getChildren().get(2)).setLabel(strCount);
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

    private void loadMap(String filename) {
        String label = "Filtering - " + pid + " - layer " + lbSelLayers.getItemCount();
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
}
