package org.ala.spatial.analysis.web;

import java.awt.Color;
import java.io.BufferedReader;

import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.Component;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.ala.spatial.util.LayersUtil;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.awt.Color;
import org.ala.spatial.util.LegendMaker;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Image;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Comboitem;
import java.util.ArrayList;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
             
public class ClassificationLegend extends UtilityComposer {
	private static final String GEOSERVER_URL = "geoserver_url";
	private static final String GEOSERVER_USERNAME = "geoserver_username";
	private static final String GEOSERVER_PASSWORD = "geoserver_password";
	private static final String SAT_URL = "sat_url";
	private SettingsSupplementary settingsSupplementary = null;
	
	String pid = "";
	String layerLabel = "";
	String imagePath = "";
	
	MapComposer mc;

	private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";
	private String satServer = "http://localhost:8080";// ws/aloc/processgeo?gc=20&envlist=Annual+Mean+Temperature%3AMean+Diurnal+Range&points=none

	public Slider sred;
	public Slider sgreen;
	public Slider sblue;
	public int colours_index;
	public Button applyColour;
	public Listcell legend_cell;
	public Image newcolour;
	public int legend_counter = 0;
	public Div colours;

	public Listbox legend;
	
	ArrayList<String> legend_lines;

	@Override
	public void afterCompose() {
		// TODO Auto-generated method stub
		super.afterCompose();

		mc = getThisMapComposer();
        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(GEOSERVER_URL);
            satServer = settingsSupplementary.getValue(SAT_URL);
        }
		
        pid = (String)(Executions.getCurrent().getArg().get("pid"));
        System.out.println("PID:" + pid);
        layerLabel = (String)(Executions.getCurrent().getArg().get("layer"));
        System.out.println("layer:" + layerLabel);
        
        buildLegend();
	}

	void buildLegend() {
		try {
			// call get
			StringBuffer sbProcessUrl = new StringBuffer();
			sbProcessUrl.append(satServer + "/alaspatial/ws/layer/get?");
			sbProcessUrl.append("pid=" + URLEncoder.encode(pid, "UTF-8"));

			HttpClient client = new HttpClient();
			GetMethod get = new GetMethod(sbProcessUrl.toString());

			get.addRequestHeader("Accept", "text/plain");

			int result = client.executeMethod(get);
			String slist = get.getResponseBodyAsString();

			// retrieve legend file
			String[] slista = slist.split("\r\n");

			/*
			 * URL url = new URL(satServer + slista[1]);
			 * 
			 * BufferedReader br = new BufferedReader (new
			 * InputStreamReader(url.openStream()));
			 */

			System.out.println(satServer + "/alaspatial/" + slista[1]);
			client = new HttpClient();
			get = new GetMethod(satServer + "/alaspatial/" + slista[1]);
			get.addRequestHeader("Accept", "text/plain");
			result = client.executeMethod(get);
			slist = get.getResponseBodyAsString();
			System.out.print("!!=" + slist);

			String[] lines = slist.split("\r\n");
			;
			legend_lines = new ArrayList<String>();
			int i = 0;
			for (i = 1; i < lines.length; i++) {
				legend_lines.add(lines[i]);
			}
			System.out.println("len=" + legend_lines.size());

			/* apply something to line onclick in lb */
			legend.setItemRenderer(new ListitemRenderer() {

				public void render(Listitem li, Object data) {
					String s = (String) data;
					String[] ss = s.split(",");
					Listcell lc = new Listcell("group " + ss[0]);
					lc.setParent(li);				

					int red = Integer.parseInt(ss[1]);
					int green = Integer.parseInt(ss[2]);
					int blue = Integer.parseInt(ss[3]);

					lc = new Listcell(ss[0]);
					lc.setStyle("background-color: rgb(" + red + "," + green
							+ "," + blue + "); color: rgb(" + red + "," + green
							+ "," + blue + ")");
					lc.setParent(li);
					lc.addEventListener("onClick", new EventListener() {
						public void onEvent(Event event) throws Exception {
							// open colours selector
							openColours((Listcell) event.getTarget());
						}
					});
				}
			});

			legend.setModel(new SimpleListModel(legend_lines));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void openColours(Listcell lc) {
		String name = lc.getLabel();
		legend_cell = lc;

		// find in legend list
		int i = 0;
		for (i = 0; i < legend_lines.size(); i++) {
			if (legend_lines.get(i).startsWith(name + ",")) {
				break;
			}
		}
		String[] la = legend_lines.get(i).split(",");
		int red = Integer.parseInt(la[1]);
		int green = Integer.parseInt(la[2]);
		int blue = Integer.parseInt(la[3]);

		colours_index = i;
		
		sred.setCurpos(red * 100 / 255);		
		sgreen.setCurpos(green * 100 / 255);
		sblue.setCurpos(blue * 100 / 255);
		
		LegendMaker lm = new LegendMaker();
		Color c = new Color(red, green, blue);		
		newcolour.setContent(lm.singleRectImage(c, 50, 50, 45, 45));
		
		colours.setVisible(true);
	}

	public void onClick$applyColour() {
		System.out.println("applyColour");
		int red = sred.getCurpos() * 255 / 100;
		int green = sgreen.getCurpos() * 255 / 100;
		int blue = sblue.getCurpos() * 255 / 100;
		String[] la = legend_lines.get(colours_index).split(",");
		la[1] = "" + red;
		la[2] = "" + green;
		la[3] = "" + blue;
		String las = la[0];
		for(int i=1;i<la.length;i++){
			las += "," + la[i];
		}
		legend_lines.set(colours_index, las);
		legend_cell.setValue(las);
		legend_cell.setStyle("background-color: rgb(" + red + "," + green
				+ "," + blue + "); color: rgb(" + red + "," + green
				+ "," + blue + ")");

		// service call to change map layer
		// pid
		// colours_index
		// red, green blue
		//
		System.out.println("changing colours: " + red + " " + green + " "
				+ blue + " @" + colours_index);
		try {
			// call get
			StringBuffer sbProcessUrl = new StringBuffer();
			sbProcessUrl.append(satServer + "/alaspatial/ws/layer/set?");
			sbProcessUrl.append("pid=" + URLEncoder.encode(pid, "UTF-8"));
			sbProcessUrl.append("&idx="
					+ URLEncoder.encode("" + colours_index, "UTF-8"));
			sbProcessUrl.append("&red=" + URLEncoder.encode("" + red, "UTF-8"));
			sbProcessUrl.append("&green="
					+ URLEncoder.encode("" + green, "UTF-8"));
			sbProcessUrl.append("&blue="
					+ URLEncoder.encode("" + blue, "UTF-8"));

			HttpClient client = new HttpClient();
			GetMethod get = new GetMethod(sbProcessUrl.toString());

			get.addRequestHeader("Accept", "text/plain");

			int result = client.executeMethod(get);
			String slist = get.getResponseBodyAsString();
			System.out.println("updated layer image:" + slist);		
			imagePath = satServer + "/alaspatial/" + slist.split("\r\n")[0];
			loadMap();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		colours.setVisible(false);
	}	

	public void onScroll$sred(Event event) {
		System.out.println("Changing red slider: " + sred.getCurpos() + " " + sgreen.getCurpos() + " " + sblue.getCurpos());
		int red = sred.getCurpos() * 255 / 100;
		int green = sgreen.getCurpos() * 255 / 100;
		int blue = sblue.getCurpos() * 255 / 100;
		LegendMaker lm = new LegendMaker();
		Color c = new Color(red, green, blue);		
		newcolour.setContent(lm.singleRectImage(c, 50, 50, 45, 45));
	}
	
	public void onScroll$sgreen(Event event) {
		System.out.println("Changing green slider: " + sred.getCurpos() + " " + sgreen.getCurpos() + " " + sblue.getCurpos());
		int red = sred.getCurpos() * 255 / 100;
		int green = sgreen.getCurpos() * 255 / 100;
		int blue = sblue.getCurpos() * 255 / 100;
		LegendMaker lm = new LegendMaker();
		Color c = new Color(red, green, blue);		
		newcolour.setContent(lm.singleRectImage(c, 50, 50, 45, 45));		
	}
	
	public void onScroll$sblue(Event event) {
		System.out.println("Changing blue slider: " + sred.getCurpos() + " " + sgreen.getCurpos() + " " + sblue.getCurpos());
		int red = sred.getCurpos() * 255 / 100;
		int green = sgreen.getCurpos() * 255 / 100;
		int blue = sblue.getCurpos() * 255 / 100;
		LegendMaker lm = new LegendMaker();
		Color c = new Color(red, green, blue);		
		newcolour.setContent(lm.singleRectImage(c, 50, 50, 45, 45));		
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
    
    private void loadMap() {     
        
        String uri = satServer + "/alaspatial/output/layers/" + pid + "/img.png";
        float opacity = Float.parseFloat("0.75");

        List<Double> bbox = new ArrayList<Double>();
        bbox.add(112.0);
        bbox.add(-44.0000000007);
        bbox.add(154.00000000084);
        bbox.add(-9.0);

        mc.addImageLayer(pid, layerLabel, imagePath, opacity, bbox);

    }
	
}
