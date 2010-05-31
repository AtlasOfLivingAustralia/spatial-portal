package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelArray;
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
 * @author 
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String GEOSERVER_USERNAME = "geoserver_username";
    private static final String GEOSERVER_PASSWORD = "geoserver_password";
    private static final String SAT_URL = "sat_url";
  
    public Textbox popup_results_seek;
    public Button download;
    public Button downloadsamples;
    public Listbox popup_listbox_results;
    public Popup popup_results;
    public Button results_prev;
    public Button results_next;
    public Label results_label;
   
    public int results_pos;
    public String[] results = null;    
    public String pid;
    
    private MapComposer mc;
    private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com"; // http://localhost:8080
    private String satServer = "http://localhost:8080";
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
        
        pid = (String)(Executions.getCurrent().getArg().get("pid"));
        System.out.println("PID:" + pid);
    	
    	try{
    	 StringBuffer sbProcessUrl = new StringBuffer();
         sbProcessUrl.append("/filtering/apply");
         sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
         sbProcessUrl.append("/species/list");        
         sbProcessUrl.append("/shape/none");
         
         results = getInfo(sbProcessUrl.toString()).split("\r\n");
         Arrays.sort(results);
         
         seekToResultsPosition(0);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
        //get the current MapComposer instance
        mc = getThisMapComposer();
        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(GEOSERVER_URL);
            satServer = settingsSupplementary.getValue(SAT_URL);
        }
    }
           
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
        
            StringBuffer sb = new StringBuffer();
            for (String s : results) {
                sb.append(s);
                sb.append("\r\n");
            }
            Filedownload.save(sb.toString(), "text/plain", "filter.csv");
      
    }

    public void onClick$downloadsamples() {
        	
            try {
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append("/filtering/apply");
                sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
                sbProcessUrl.append("/samples/list");
              
                sbProcessUrl.append("/shape/none");
                System.out.println("attempt to download: " + satServer + "/alaspatial/ws" + sbProcessUrl.toString());
                String samplesfile = getInfo(sbProcessUrl.toString());
           
                URL u = new URL(satServer + "/alaspatial/" + samplesfile);
                System.out.println("opening stream to " + samplesfile); 
                Filedownload.save(u.openStream(), "application/zip", "filter_samples_" + pid + ".zip");
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    /*public void onChange$popup_results_seek(InputEvent event) {
        //seek results list
        System.out.print("Searching for ");
        System.out.println(event.getValue());
        String search_for = event.getValue();
        if (search_for.length() > 0) {
            search_for = search_for.toLowerCase();
        }

        int pos = java.util.Arrays.binarySearch(results, search_for);
        System.out.println("seek to: " + pos + " " + search_for);
        if (pos < 0) {
            pos = (pos * -1) - 1;
        }
        seekToResultsPosition(pos);
    }*/

    public void seekToResultsPosition(int newpos) {
        results_pos = newpos;

        if (results_pos < 0) {
            results_pos = 0;
        }
        if (results_pos >= results.length) {
            results_pos = results.length - 1;
        }

        int sz = results_pos + 15;
        if (results.length < sz) {
            sz = results.length;
        }

        String[] list = new String[sz - results_pos];
        int i;
        for (i = results_pos; i < sz; i++) {
            list[i - results_pos] = results[i].trim();
        }

        ListModelArray slm = new ListModelArray(list, true);

        popup_listbox_results.setModel(slm);
        results_label.setValue(results_pos + " to " + (sz) + " of " + results.length);
    }
    
    private String getInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();
            System.out.println(satServer + "/alaspatial/ws" + urlPart);
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws" + urlPart); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            //get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            //System.out.println("Got response from ALOCWSController: \n" + slist);

            return slist;
        } catch (Exception ex) {
            //Logger.getLogger(FilteringWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
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
