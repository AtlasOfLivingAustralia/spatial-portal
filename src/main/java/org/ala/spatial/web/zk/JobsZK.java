package org.ala.spatial.web.zk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.util.Clients;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.FilteringIndex;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;

public class JobsZK extends GenericForwardComposer {
	private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";
	private String satServer = "http://localhost:8080";// ws/aloc/processgeo?gc=20&envlist=Annual+Mean+Temperature%3AMean+Diurnal+Range&points=none

	Listbox lbwaiting;
        Listbox lbrunning;
        Listbox lbfinished;

        Textbox selectedJob;

        Textbox joblog;

        String pid;
        
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		TabulationSettings.load();

		geoServer = TabulationSettings.alaspatial_path;
		satServer = TabulationSettings.alaspatial_path;

                //timer.stop();

                onClick$refreshButton(null);

	}
        
        public void onClick$refreshButton(Event e){
            lbwaiting.setModel(new SimpleListModel(get("listwaiting").split("\n")));
            lbrunning.setModel(new SimpleListModel(get("listrunning").split("\n")));
            lbfinished.setModel(new SimpleListModel(get("listfinished").split("\n")));
        }

        public void onSelect$lbwaiting(Event e){
            String s = lbwaiting.getSelectedItem().getLabel();
            pid = s.substring(0,s.indexOf(";"));
            refreshInfo();
        }

        public void onSelect$lbrunning(Event e){
            String s = lbrunning.getSelectedItem().getLabel();
            pid = s.substring(0,s.indexOf(";"));
            refreshInfo();
        }

        public void onSelect$lbfinished(Event e){
            String s = lbfinished.getSelectedItem().getLabel();
            pid = s.substring(0,s.indexOf(";"));
            refreshInfo();
        }

        public void onClick$btnCancel(Event e){
            get("cancel");
        }

        void refreshInfo(){
            selectedJob.setValue(pid);
            joblog.setValue(get("log"));
        }

        String get(String type){
            try{
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(satServer + "ws/jobs/").append(type).append("?pid=").append(pid);

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
