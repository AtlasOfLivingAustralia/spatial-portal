/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.wms.WMSStyle;
import org.ala.spatial.util.LayersUtil;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbarbutton;

/**
 *
 * @author ajay
 */
public class ALOCProgressWCController extends UtilityComposer {

    private static final String SAT_URL = "sat_url";

    Label jobstatus;
    Progressmeter jobprogress;
    Timer timer;
    public String pid = null;

    private String satServer = "";
    private SettingsSupplementary settingsSupplementary = null;

    public ALOCWCController parent = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        timer.stop();
    }

    public void start(String pid_){
        pid = pid_;
        
        timer.start();

        onTimer$timer(null);
    }

     public void onTimer$timer(Event e) {
            //get status

            jobstatus.setValue(get("status"));

            String s = get("state");

            String p = get("progress");
            System.out.println("prog:" + p);
            try{
                double d = Double.parseDouble(p);
                jobprogress.setValue((int)(d*100));
            }catch(Exception ex){

            }

            if(s.equals("SUCCESSFUL") || s.equals("FAILED")){
                timer.stop();
                parent.loadMap();

                this.detach();
            }
        }

        String get(String type){
            try{
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(satServer + "/alaspatial/ws/jobs/").append(type).append("?pid=").append(pid);

                System.out.println(sbProcessUrl.toString());
                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(sbProcessUrl.toString());

                get.addRequestHeader("Accept", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();
                System.out.println(slist);
                return slist;
            }catch(Exception e){
                e.printStackTrace();
            }
            return "";
        }
}
