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

public class ALOCZK extends GenericForwardComposer {
	String pid = "";

	private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";
	private String satServer = "http://localhost:8080";// ws/aloc/processgeo?gc=20&envlist=Annual+Mean+Temperature%3AMean+Diurnal+Range&points=none

	List _layer_filters = new ArrayList();
	List _layer_filters_original = new ArrayList();
	List _layer_filters_selected = new ArrayList();

	/**
	 * for functions in popup box
	 */
	LayerFilter popup_filter;
	Listcell popup_cell;
	Listitem popup_item;
	Label lb_points;

	String temp_filename;

	String species_filter;
	String[] results = null;

	ArrayList<Integer> count_results = new ArrayList<Integer>();

	public Combobox cb;
	public Listbox lb;
	public Image results_image;

	public Textbox number_of_groups;
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

	public Button download;
	public Listbox popup_listbox_results;
	public Popup popup_results;

	public Button results_prev;
	public Button results_next;
	public Label results_label;
	int results_pos;
	public Div colours;
	public Slider sred;
	public Slider sgreen;
	public Slider sblue;
	public int colours_index;
	public Button applyColour;
	public Listcell legend_cell;
	public Label newcolour;
	public int legend_counter = 0;

        Textbox jobstate;
        Textbox jobstatus;
        Textbox jobprogress;
        Textbox joblog;
        Timer timer;
        Progressmeter progressbar;

	public Button run_button;
        public Button run_button2;

	public Listbox legend;
	ArrayList<String> legend_lines;

	String results_path;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		int i;
		TabulationSettings.load();
		geoServer = TabulationSettings.alaspatial_path;
		satServer = TabulationSettings.alaspatial_path;

		LayerFilter layer_filter;

		/* list of all layers */
		for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
			layer_filter = FilteringIndex
					.getLayerFilter(TabulationSettings.environmental_data_files[i].name);
			_layer_filters.add(layer_filter);
			layer_filter = FilteringIndex
					.getLayerFilter(TabulationSettings.environmental_data_files[i].name);
			_layer_filters_original.add(layer_filter);
		}

		onChanging$cb(null);

                timer.stop();

		System.out.println("done layer setup");
	}

	public void onChanging$cb(Event event) {

		// if(event == null || !event.isChangingBySelectBack()){
		Iterator it = cb.getItems().iterator();
		int i = 0;
		while (i < _layer_filters_original.size()) {
			if (it != null && it.hasNext()) {
				((Comboitem) it.next())
						.setLabel(((LayerFilter) _layer_filters_original.get(i)).layer.display_name
								+ " (Terrestrial)");
			} else {
				it = null;
				new Comboitem(
						((LayerFilter) _layer_filters_original.get(i)).layer.display_name
								+ " (Terrestrial)").setParent(cb);
			}
			System.out
					.println("*x*"
							+ ((LayerFilter) _layer_filters_original.get(i)).layer.display_name);
			i++;
		}
		while (it != null && it.hasNext()) {
			((Comboitem) it.next()).detach();
		}
		// }

	}

	public void onChange$cb(Event event) {
		String new_value = "";

		new_value = cb.getValue();
		if (new_value.equals("")) {
			return;
		}
		new_value = (new_value.equals("")) ? "" : new_value.substring(0,
				new_value.indexOf("(")).trim();
		System.out.println("new value: " + new_value);

		for (Object o : _layer_filters_selected) {
			LayerFilter f = (LayerFilter) o;
			if (f.layer.display_name.equals(new_value)) {
				System.out.println("already added");
				return; // already added
			}
		}
		System.out.println("not already added");
		for (Object o : _layer_filters) {
			LayerFilter f = (LayerFilter) o;
			System.out.println(f.layer.display_name);
			if (f.layer.display_name.equals(new_value)) {
				System.out.println("found");
				_layer_filters_selected.add(f);
			}
		}

		/* apply something to line onclick in lb */
		lb.setItemRenderer(new ListitemRenderer() {
			public void render(Listitem li, Object data) {
				LayerFilter f = (LayerFilter) data;
				Listcell remove = new Listcell("remove");
				remove.setParent(li);
				remove.addEventListener("onClick", new EventListener() {
					public void onEvent(Event event) throws Exception {
						if (!((Listcell) event.getTarget()).getLabel().equals(
								"")
								&& !((Listitem) event.getTarget().getParent())
										.isDisabled()) {
							deleteSelectedFilters(event.getTarget());
						}
					}
				});

				new Listcell(f.layer.display_name + " (Terrestrial)")
						.setParent(li);
			}
		});

		lb.setModel(new SimpleListModel(_layer_filters_selected));
		Listitem li = lb.getItemAtIndex(lb.getItemCount() - 1);
		Listcell lc = (Listcell) li.getLastChild();
		System.out.println(lc);
		lc = (Listcell) lc.getPreviousSibling();

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

		label = ((LayerFilter) _layer_filters_selected.get(idx)).layer.display_name;

		System.out.println("deleteSelectedFilters(" + label + ")");

		for (Object oi : _layer_filters_selected) {
			LayerFilter f = (LayerFilter) oi;

			if (f.layer.display_name.equals(label)) {
				_layer_filters_selected.remove(oi);

				System.out.println("deleting from seletion list success!");

				break;
			}
		}
		li.detach();
	}

	public LayerFilter[] getSelectedFilters() {
		LayerFilter[] f = new LayerFilter[_layer_filters_selected.size()];
		_layer_filters_selected.toArray(f);
		return f;
	}

	public void onClick$download() {
		// org.zkoss.zhtml.Filedownload.save(temp_filename,"text/plain","ALOC.png");
		// java.net.URL url = new java.net.URL(results_path);

		// org.zkoss.zhtml.Filedownload.save(url,"image/png");
	}

	public void onClick$run_button() {
		try {
			File temporary_file0 = java.io.File.createTempFile("ALOC_", ".png");
			LayerFilter[] filters = getSelectedFilters();

			if (filters == null || filters.length == 0) {
				return;
			}

			Layer[] layers = new Layer[filters.length];
			int i;
			for (i = 0; i < layers.length; i++) {
				layers[i] = filters[i].layer;
			}

			SimpleRegion sr = null;
			if (lb_points.getValue().length() > 0) {
				sr = getSimpleRegion(lb_points.getValue());
			}

			// ALOC.run(temporary_file0.getPath(),layers,Integer.parseInt(number_of_groups.getValue()));
			/*
			 * ALOC.run(temporary_file0.getPath(),layers,Integer.parseInt(number_of_groups
			 * .getValue()),sr);
			 * 
			 * temp_filename = temporary_file0.getPath();
			 * 
			 * org.zkoss.image.Image image = new
			 * org.zkoss.image.AImage(temporary_file0.getPath());
			 * 
			 * results_image.setContent(image); popup_results.open(cb);
			 */

			btnGenerate(layers, number_of_groups.getValue(), lb_points
					.getValue());
			if (download != null) {
				download.setVisible(true);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

        public void onClick$run_button2() {
		try {
			File temporary_file0 = java.io.File.createTempFile("ALOC_", ".png");
			LayerFilter[] filters = getSelectedFilters();

			if (filters == null || filters.length == 0) {
				return;
			}

			Layer[] layers = new Layer[filters.length];
			int i;
			for (i = 0; i < layers.length; i++) {
				layers[i] = filters[i].layer;
			}

			SimpleRegion sr = null;
			if (lb_points.getValue().length() > 0) {
				sr = getSimpleRegion(lb_points.getValue());
			}

			btnGenerate2(layers, number_of_groups.getValue(), lb_points
					.getValue());
			if (download != null) {
				download.setVisible(true);
			}

                        onTimer$timer(null);

                        timer.start();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
        public void onTimer$timer(Event e) {
            //get status
            
            jobstatus.setValue(get("status"));
            String s = get("state");
            jobstate.setValue(get("state"));
            String p = get("progress");
            jobprogress.setValue(p);
            //joblog.setValue(get("log"));

            double d = Double.parseDouble(p);
            progressbar.setValue((int)(d*100));

            if(s.equals("SUCCESSFUL") || s.equals("FAILED")){
                timer.stop();
            }
        }

        String get(String type){
            try{
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append(satServer + "ws/jobs/").append(type).append("?pid=").append(pid2);

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

	public void btnGenerate(Layer[] layers, String number_of_groups,
			String points) {
		try {
			StringBuffer sbenvsel = new StringBuffer();
			for (int i = 0; i < layers.length; i++) {
				sbenvsel.append(layers[i].display_name);
				if (i < layers.length - 1) {
					sbenvsel.append(":");
				}
			}

			StringBuffer sbProcessUrl = new StringBuffer();
			sbProcessUrl.append(satServer + "ws/aloc/processgeo?");
			sbProcessUrl.append("gc="
					+ URLEncoder.encode(number_of_groups, "UTF-8"));
			sbProcessUrl.append("&envlist="
					+ URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
			if (points.length() > 0) {
				sbProcessUrl.append("&area="
						+ URLEncoder.encode(points, "UTF-8"));
			} else {
				sbProcessUrl.append("&area="
						+ URLEncoder.encode("none", "UTF-8"));
			}

			HttpClient client = new HttpClient();
			PostMethod get = new PostMethod(sbProcessUrl.toString());

			get.addRequestHeader("Accept", "text/plain");

			int result = client.executeMethod(get);
			String slist = get.getResponseBodyAsString();

			System.out.println("Got response from ALOCWSController: \n" + slist);
			
			pid = slist;

			String img = satServer + "output/aloc/" + slist + "/aloc.png";
                        double [] extents = getExtents(slist);
			String client_request = "getALOCimage('" + img + "'," 
                                + extents[2] + "," + extents[5] + ","
                                + extents[4] + "," + extents[3] + ","
                                + extents[0] + "," + extents[1] + ");";

			System.out.println("evaljavascript: " + client_request);
			Clients.evalJavaScript(client_request);

			results_path = img;

			buildLegend();

		} catch (Exception ex) {
			System.out.println("Opps!: ");
			ex.printStackTrace(System.out);
		}

	}
        String pid2;
        public void btnGenerate2(Layer[] layers, String number_of_groups,
			String points) {
		try {
			StringBuffer sbenvsel = new StringBuffer();
			for (int i = 0; i < layers.length; i++) {
				sbenvsel.append(layers[i].display_name);
				if (i < layers.length - 1) {
					sbenvsel.append(":");
				}
			}

			StringBuffer sbProcessUrl = new StringBuffer();
			sbProcessUrl.append(satServer + "ws/aloc/processgeoq?");
			sbProcessUrl.append("gc="
					+ URLEncoder.encode(number_of_groups, "UTF-8"));
			sbProcessUrl.append("&envlist="
					+ URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
			if (points.length() > 0) {
				sbProcessUrl.append("&area="
						+ URLEncoder.encode(points, "UTF-8"));
			} else {
				sbProcessUrl.append("&area="
						+ URLEncoder.encode("none", "UTF-8"));
			}

			HttpClient client = new HttpClient();
			PostMethod get = new PostMethod(sbProcessUrl.toString());

			get.addRequestHeader("Accept", "text/plain");

			int result = client.executeMethod(get);
			String slist = get.getResponseBodyAsString();

			System.out
					.println("Got response from ALOCWSController: \n" + slist);

                        pid2 = slist;

		} catch (Exception ex) {
			System.out.println("Opps!: ");
			ex.printStackTrace(System.out);
		}

	}

        public void onClick$addbutton(Event e){

            String img = satServer + "output/aloc/" + pid2 + "/aloc.png";
            double [] extents = getExtents(pid2);
            String client_request = "getALOCimage('" + img + "',"
                    + extents[2] + "," + extents[5] + ","
                    + extents[4] + "," + extents[3] + ","
                    + extents[0] + "," + extents[1] + ");";

                            //+ "',112,-9,154,-44,252,210);";


            System.out.println("evaljavascript: " + client_request);
            Clients.evalJavaScript(client_request);

            results_path = img;

            buildLegend();

        }

	SimpleRegion getSimpleRegion(String pointsString) {
		SimpleRegion simpleregion = new SimpleRegion();
		String[] pairs = pointsString.split(",");

		double[][] points = new double[pairs.length][2];
		for (int i = 0; i < pairs.length; i++) {
			String[] longlat = pairs[i].split(":");
			if (longlat.length == 2) {
				try {
					points[i][0] = Double.parseDouble(longlat[0]);
					points[i][1] = Double.parseDouble(longlat[1]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.print("(" + points[i][0] + "," + points[i][1] + ")");
			} else {
				System.out.print("err:" + pairs[i]);
			}
		}

		simpleregion.setPolygon(points);

		return simpleregion;
	}

	void buildLegend() {
		try {
			// call get
			StringBuffer sbProcessUrl = new StringBuffer();
			sbProcessUrl.append(satServer + "ws/layer/get?");
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

			System.out.println(satServer + slista[1]);
			client = new HttpClient();
			get = new GetMethod(satServer + slista[1]);
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
					Listcell lc = new Listcell(ss[0]);
					lc.setParent(li);				

					int red = Integer.parseInt(ss[1]);
					int green = Integer.parseInt(ss[2]);
					int blue = Integer.parseInt(ss[3]);

					lc = new Listcell(ss[0]);
					lc.setStyle("background-color: rgb(" + red + "," + green
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
		int blue = Integer.parseInt(la[2]);
		int green = Integer.parseInt(la[3]);

		colours_index = i;
		colours.setVisible(true);
		sred.setCurpos(red * 100 / 255);		
		sgreen.setCurpos(green * 100 / 255);
		sblue.setCurpos(blue * 100 / 255);
		
		newcolour.setStyle("background-color: rgb(" + red + "," + green + ","
				+ blue + ")");
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
		legend_cell.setStyle("background-color: rgb(" + red + "," + green + ","
				+ blue + ")");		

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
			sbProcessUrl.append(satServer + "ws/layer/set?");
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

			// redraw image
			String img = satServer + "output/layers/" + pid + "/img.png";
			String client_request = "getALOCimage('" + img + "?" + legend_counter
					+ "',112,-9,154,-44,252,210);";
			legend_counter++;
			System.out.println("evaljavascript: " + client_request);
			Clients.evalJavaScript(client_request);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public void onScroll$sred(Event event) {
		System.out.println("Changing red slider: " + sred.getCurpos() + " " + sgreen.getCurpos() + " " + sblue.getCurpos());
		int red = sred.getCurpos() * 255 / 100;
		int green = sgreen.getCurpos() * 255 / 100;
		int blue = sblue.getCurpos() * 255 / 100;
		newcolour.setStyle("background-color: rgb(" + red + "," + green + ","
				+ blue + ")");		
	}
	
	public void onScroll$sgreen(Event event) {
		System.out.println("Changing green slider: " + sred.getCurpos() + " " + sgreen.getCurpos() + " " + sblue.getCurpos());
		int red = sred.getCurpos() * 255 / 100;
		int green = sgreen.getCurpos() * 255 / 100;
		int blue = sblue.getCurpos() * 255 / 100;
		newcolour.setStyle("background-color: rgb(" + red + "," + green + ","
				+ blue + ")");		
	}
	
	public void onScroll$sblue(Event event) {
		System.out.println("Changing blue slider: " + sred.getCurpos() + " " + sgreen.getCurpos() + " " + sblue.getCurpos());
		int red = sred.getCurpos() * 255 / 100;
		int green = sgreen.getCurpos() * 255 / 100;
		int blue = sblue.getCurpos() * 255 / 100;
		newcolour.setStyle("background-color: rgb(" + red + "," + green + ","
				+ blue + ")");		
	}


        double [] getExtents(String path){
            double [] d = new double[6];
            try {
                StringBuffer sbProcessUrl = new StringBuffer();
		sbProcessUrl.append(satServer + "output/aloc/" + path + "/aloc.pngextents.txt");

			HttpClient client = new HttpClient();
			GetMethod get = new GetMethod(sbProcessUrl.toString());

			get.addRequestHeader("Accept", "text/plain");

			int result = client.executeMethod(get);
			String slist = get.getResponseBodyAsString();
			System.out.println("getExtents:" + slist);

                        String [] s = slist.split("\n");
                        for(int i=0;i<6 && i<s.length;i++){
                            d[i] = Double.parseDouble(s[i]);
                        }
            }catch (Exception e){
                e.printStackTrace();
            }
            return d;
        }
	
}
