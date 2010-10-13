package org.ala.spatial.web.zk;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.ala.spatial.analysis.index.*;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import org.zkoss.zul.Window;
import org.zkoss.zul.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.util.Clients;


public class SpeciesListZK2 extends GenericForwardComposer {
	List _layer_filters = new ArrayList();
	List _layer_filters_original = new ArrayList();
	List _layer_filters_selected = new ArrayList();

	String service_pid;
	
	private String geoServer = "http://localhost:8080"; //"http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";  
    private String satServer = geoServer;  
    
	
	/**
	 * for functions in popup box
	 */
	LayerFilter popup_filter;
	Listcell popup_cell;
	Listitem popup_item;


	String species_filter;
	String [] results = null;

	ArrayList<Integer> count_results = new ArrayList<Integer>();

	public Combobox cb;
	public Listbox lb;
	public Label lb_points;
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
	public Button downloadsamples;
	public Listbox popup_listbox_results;
	public Popup popup_results;

	public Button results_prev;
	public Button results_next;
	public Label results_label;
	int results_pos;

	//FilteringImage filteringImage;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		int i;
		TabulationSettings.load();		
		geoServer = TabulationSettings.alaspatial_path;
		satServer = TabulationSettings.alaspatial_path;

		LayerFilter layer_filter;

		/* list of all layers */
		for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
			layer_filter =
				FilteringIndex.getLayerFilter(
						TabulationSettings.environmental_data_files[i].name);
			_layer_filters.add(layer_filter);
			layer_filter =
				FilteringIndex.getLayerFilter(
						TabulationSettings.environmental_data_files[i].name);
			_layer_filters_original.add(layer_filter);
		}
		for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
			layer_filter =
				FilteringIndex.getLayerFilter(
						TabulationSettings.geo_tables[i].name);
			_layer_filters.add(layer_filter);
			layer_filter =
				FilteringIndex.getLayerFilter(
						TabulationSettings.geo_tables[i].name);
			_layer_filters_original.add(layer_filter);
		}

		File file = java.io.File.createTempFile("spl",".png");
		//filteringImage = new FilteringImage(file.getPath());

		onChanging$cb(null);
		
		/*service init bit*/
		StringBuffer sbProcessUrl = new StringBuffer();
        sbProcessUrl.append(satServer + "ws/filtering/init");
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(sbProcessUrl.toString()); 
        get.addRequestHeader("Accept", "text/plain");
        int result = client.executeMethod(get);
        String slist = get.getResponseBodyAsString();
        service_pid = slist;

		System.out.println("done layer setup: " + slist);
	}

	public void onChanging$cb(Event event){

		//if(event == null || !event.isChangingBySelectBack()){
			Iterator it = cb.getItems().iterator();
			int i=0;
			while(i < _layer_filters_original.size()){
				if(it != null && it.hasNext()){
					((Comboitem) it.next()).setLabel(((LayerFilter)_layer_filters_original.get(i)).layer.display_name + " (Terrestrial)");
				}else{
					it = null;
					new Comboitem(((LayerFilter)_layer_filters_original.get(i)).layer.display_name + " (Terrestrial)").setParent(cb);
				}
				//System.out.println("*x*" + ((LayerFilter)_layer_filters_original.get(i)).layer.display_name);
				i++;
			}
			while(it != null && it.hasNext()){
				((Comboitem) it.next()).detach();
			}
		//}

	}

	public void onSelect$cb(SelectEvent event){
		String new_value = "";

		Set set = event.getSelectedItems();
		//System.out.println(set.size());
		if(set.size() > 0){
			Object [] os = set.toArray();

			//System.out.println(os[0]);

			new_value = ((Comboitem)os[0]).getLabel();
		}
		if(new_value.equals("") || new_value.indexOf("(") < 0){
			return;
		}
		new_value = (new_value.equals("")) ? "" : new_value.substring(0,new_value.indexOf("(")).trim();
		//System.out.println("new value: " + new_value);


		for(Object o : _layer_filters_selected){
			LayerFilter f = (LayerFilter) o;
			if(f.layer.display_name.equals(new_value)){
				//System.out.println("already added");
				return; //already added
			}
		}
		//System.out.println("not already added");
		for(Object o : _layer_filters){
			LayerFilter f = (LayerFilter) o;
			System.out.println(f.layer.display_name);
			if(f.layer.display_name.equals(new_value)){
				System.out.println("found");
				_layer_filters_selected.add(f);
			}
		}

		/* apply something to line onclick in lb */

		lb.setItemRenderer(new ListitemRenderer(){
			public void render(Listitem li, Object data) {
                LayerFilter f = (LayerFilter)data;
                Listcell remove = new Listcell("remove");
                remove.setParent(li);
                remove.addEventListener("onClick",new EventListener(){
                	public void onEvent(Event event) throws Exception {
                		if(!((Listcell)event.getTarget()).getLabel().equals("")
                				&& !((Listitem)event.getTarget().getParent()).isDisabled()){
                			deleteSelectedFilters(event.getTarget());
                		}
                	}
                });

                new Listcell(f.layer.display_name + " (Terrestrial)").setParent(li);
                Listcell lc = new Listcell(f.toString());
                lc.setParent(li);

                lc.addEventListener("onClick",new EventListener() {
        			public void onEvent(Event event) throws Exception {
        				if(!((Listcell)event.getTarget()).getLabel().equals("")
                				&& !((Listitem)event.getTarget().getParent()).isDisabled()){
        					showAdjustPopup(event.getTarget());
        				}
        			}
        		});

                Listcell count = new Listcell(String.valueOf(f.count));
                count.setStyle("text-decoration: underline;");
                count.setParent(li);
                count.addEventListener("onClick",new EventListener(){
                	public void onEvent(Event event) throws Exception {
                		//if(results != null && results.length() > 0){
                		if(!((Listcell)event.getTarget()).getLabel().equals("0")
                				 && !((Listitem)event.getTarget().getParent()).isDisabled()){
                			LayerFilter [] layer_filters = getSelectedFilters();
                			if(layer_filters != null){
                				/*results = FilteringIndex.listArraySpeciesGeo(layer_filters);*/
                				results = serviceSpeciesList().split("\r\n");
                				
                				java.util.Arrays.sort(results);

                				seekToResultsPosition(0);

	                			popup_results.open(30,30);//.open(event.getTarget());
                			}
                		}
                		//}
                	}
                });
            }
		});

		lb.setModel(new SimpleListModel(_layer_filters_selected));
		Listitem li = lb.getItemAtIndex(lb.getItemCount()-1);
		Listcell lc = (Listcell) li.getLastChild();
		//System.out.println(lc);
		lc = (Listcell) lc.getPreviousSibling();

		listFix();

		showAdjustPopup(lc);
	}

	public void onScroll$popup_slider_min(Event event) {
		System.out.println("Changing min slider");
		onChangeSliderMin();
	}
	public void onScroll$popup_slider_max(Event event) {
		System.out.println("Changing min slider");
		onChangeSliderMax();
	}

	public void showAdjustPopup(Object o){
		System.out.println("showAdjustPopup");
		if(o == null){
			//get end cell

			int i = lb.getItemCount();
		//	System.out.println("items: " + i);
			Listitem li = (Listitem) lb.getItemAtIndex(i-1);
		//	System.out.println(li.getLabel());
			List list = li.getChildren();

			for(Object o2 : list){
				Listcell m = (Listcell)o2;
				System.out.println("**"+o2 + ">" + m.getLabel());
			}
			o = list.get(list.size()-1);
		//	System.out.println(li);
		//	System.out.println(o);
		}

		Listcell lc = (Listcell)o;
		Listitem li = (Listitem) lc.getParent();
		LayerFilter lf = (LayerFilter) _layer_filters_selected.get(li.getIndex());

		popup_filter = lf;
		popup_cell = lc;
		popup_item = li;

/*		System.out.println("lf=" + lf);
		System.out.println(lf.getClass().toString());
		System.out.println("lflayer=" + lf.layer);
		System.out.println("lflayer name=" + lf.layer.name);
		System.out.println("lflayer display name=" + lf.layer.display_name);
		System.out.println("lflayer type =" + lf.layer.type);
*/
		if(popup_filter.layer.type == "environmental"){

			String csv = FilteringIndex.getLayerExtents(lf.layer.name);
			popup_range.setValue(csv);
			int idx = 0;
			try{
				popup_minimum.setValue(String.valueOf((float)popup_filter.minimum_value));
				popup_maximum.setValue(String.valueOf((float)popup_filter.maximum_value));

				for(idx=0;idx<_layer_filters.size();idx++){
					if(((LayerFilter)_layer_filters.get(idx)).layer.name == lf.layer.name){
						System.out.println("popup for " + lf.layer.display_name);
						break;
					}
				}
				popup_idx.setValue(String.valueOf(idx));


			}catch(Exception e){
				System.out.println("value conversion error");
			}

			double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
			int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
					/ (range) * 100);
			int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial)
					/ (range) * 100);

	//		System.out.println("range:" + range + " mincursor:" + mincursor + " maxcursor:" + maxcursor);

			//Clients.evalJavaScript("applyFilter(" + idx + "," + (mincursor/100.0) + "," + (maxcursor/100.0) + ");"); 	//should take out missing value
			//filteringImage.applyFilter(idx,mincursor/100.0,maxcursor/100.0);

			popup_slider_min.setCurpos(mincursor);
			popup_slider_max.setCurpos(maxcursor);

			lc.focus();
//			System.out.println("attaching: " + lc + lc.getValue());
			popup_continous.open(30,30);//.open(li);
		}else{ //catagorical values

			popup_listbox.setModel(new SimpleListModel(lf.catagory_names));

			int idx = 0;
			for(idx=0;idx<_layer_filters.size();idx++){
				if(((LayerFilter)_layer_filters.get(idx)).layer.name == lf.layer.name){
					System.out.println("popup for " + lf.layer.display_name);
					break;
				}
			}
			popup_idx.setValue(String.valueOf(idx));

			String javascript = "applyFilterCtx(" + idx + ",-2,false);"; 	//should take out missing value
			//filteringImage.applyFilterCtx(idx,-2,false);

			/* set check boxes */
			for(int i : lf.catagories){
				Listitem listitem = popup_listbox.getItemAtIndex(i);
				popup_listbox.addItemToSelection(listitem);
			}
			for(int i=0;i<lf.catagory_names.length;i++){
				int j = 0;
				for(j=0;j<lf.catagories.length;j++){
					if(i == lf.catagories[j]){
						javascript += "applyFilterCtx(" + idx + "," + i + ",true);"; 	//should take out missing value
						//filteringImage.applyFilterCtx(idx,i,true);
						break;
					}
				}
				if(j == lf.catagories.length){
					//hide
				//	javascript += "applyFilterCtx(" + idx + "," + i + ",false);"; 	//should take out missing value
				//	filteringImage.applyFilterCtx(idx,i,false);
				}
			}
			lc.focus();
	//		System.out.println("attaching: " + lc + lc.getValue());
			popup_catagorical.open(30,30);//.open(li);
			//Clients.evalJavaScript(javascript);
			
			
		}
		this.clientFilter();
	}
	
	public void serviceRemoveTopFilter(){
		System.out.println("serviceRemoveTopFilter()");
		try{
			///apply2/pid/{pid}/layers/{layers}/types/{types}/val1s/{val1s}/val2s/{val2s}
			StringBuffer sbProcessUrl = new StringBuffer();
	        sbProcessUrl.append(satServer + "ws/filtering/apply4");
	        sbProcessUrl.append("/pid/" + URLEncoder.encode(service_pid, "UTF-8"));
	        sbProcessUrl.append("/layers/" + URLEncoder.encode("none", "UTF-8"));
	        sbProcessUrl.append("/types/" + URLEncoder.encode("none", "UTF-8"));
	        sbProcessUrl.append("/val1s/" + URLEncoder.encode("none", "UTF-8"));
	        sbProcessUrl.append("/val2s/" + URLEncoder.encode("none", "UTF-8"));        
	        sbProcessUrl.append("/depth/" + URLEncoder.encode("" + (_layer_filters_selected.size()) , "UTF-8"));
	        
	
	        HttpClient client = new HttpClient();
	        GetMethod get = new GetMethod(sbProcessUrl.toString()); 
	
	        get.addRequestHeader("Accept", "text/plain");
	
	        int result = client.executeMethod(get);
	        String slist = get.getResponseBodyAsString();
		}catch(Exception e){
			e.printStackTrace();
		}

		String client_request = "removeImageLayer('filterlayer_" + getSelectedFilters().length + "');";		
        System.out.println("evaljavascript: " + client_request);                      
        Clients.evalJavaScript(client_request);
	}
	
	public void serviceUpdateTopFilter(String layername, double min, double max,boolean commit_changes){
		System.out.println("serviceUpdateTopFilter(): " + min + "," + max);
		try{
			///apply2/pid/{pid}/layers/{layers}/types/{types}/val1s/{val1s}/val2s/{val2s}
			StringBuffer sbProcessUrl = new StringBuffer();
			if(commit_changes){
				sbProcessUrl.append(satServer + "ws/filtering/apply4");
			}else{
				sbProcessUrl.append(satServer + "ws/filtering/apply3");
			}
	        sbProcessUrl.append("/pid/" + URLEncoder.encode(service_pid, "UTF-8"));
	        sbProcessUrl.append("/layers/" + URLEncoder.encode(layername, "UTF-8"));
	        sbProcessUrl.append("/types/" + URLEncoder.encode("environmental", "UTF-8"));
	        sbProcessUrl.append("/val1s/" + URLEncoder.encode(String.valueOf(min), "UTF-8"));
	        sbProcessUrl.append("/val2s/" + URLEncoder.encode(String.valueOf(max), "UTF-8"));   
	        sbProcessUrl.append("/depth/" + URLEncoder.encode("" + (_layer_filters_selected.size()) , "UTF-8"));
	        
	        HttpClient client = new HttpClient();
	        GetMethod get = new GetMethod(sbProcessUrl.toString()); 
	
	        get.addRequestHeader("Accept", "text/plain");
	
	        int result = client.executeMethod(get);
	        String slist = get.getResponseBodyAsString();
	        
	        String req1 = "removeImageLayer('filterlayer_" + getSelectedFilters().length + "');";			
			String img = satServer + "output/filtering/" + service_pid + "/" + slist;			
	        String client_request = "addImageLayer('" + img 
	        		+ "','filterlayer_" + getSelectedFilters().length + "',112,-9,154,-44,1008,840);";// + req1;
	       
	        System.out.println("evaljavascript: " + client_request);                      
	        Clients.evalJavaScript(client_request);	
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public int serviceSpeciesCount(){
		try{
			StringBuffer sbProcessUrl = new StringBuffer();
	        sbProcessUrl.append(satServer + "ws/filtering/apply");
	        sbProcessUrl.append("/pid/" + URLEncoder.encode(service_pid, "UTF-8"));
	        sbProcessUrl.append("/species/count"); 
	        String point = lb_points.getValue();
	        if(point.length() == 0){
	        	point = "none";
	        }
	        sbProcessUrl.append("/shape/" + URLEncoder.encode(point, "UTF-8"));
	        
	        HttpClient client = new HttpClient();
	        GetMethod get = new GetMethod(sbProcessUrl.toString()); 
	
	        get.addRequestHeader("Accept", "text/plain");
	
	        int result = client.executeMethod(get);
	        String slist = get.getResponseBodyAsString();
	        return Integer.parseInt(slist);
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	
	public String serviceSpeciesList(){
		try{
			StringBuffer sbProcessUrl = new StringBuffer();
	        sbProcessUrl.append(satServer + "ws/filtering/apply");
	        sbProcessUrl.append("/pid/" + URLEncoder.encode(service_pid, "UTF-8"));
	        sbProcessUrl.append("/species/list");   
	        String point = lb_points.getValue();
	        if(point.length() == 0){
	        	point = "none";
	        }
	        sbProcessUrl.append("/shape/" + URLEncoder.encode(point, "UTF-8"));
	        
	        HttpClient client = new HttpClient();
	        GetMethod get = new GetMethod(sbProcessUrl.toString()); 
	
	        get.addRequestHeader("Accept", "text/plain");
	
	        int result = client.executeMethod(get);
	        String slist = get.getResponseBodyAsString();
	        return slist;
		}catch(Exception e){
			e.printStackTrace();
		}
		return "";
	}
	
	public String serviceSamplesList(){
		try{
			StringBuffer sbProcessUrl = new StringBuffer();
	        sbProcessUrl.append(satServer + "ws/filtering/apply");
	        sbProcessUrl.append("/pid/" + URLEncoder.encode(service_pid, "UTF-8"));
	        sbProcessUrl.append("/samples/list");   
	        String point = lb_points.getValue();
	        if(point.length() == 0){
	        	point = "none";
	        }
	        sbProcessUrl.append("/shape/" + URLEncoder.encode(point, "UTF-8"));
	        
	        HttpClient client = new HttpClient();
	        GetMethod get = new GetMethod(sbProcessUrl.toString()); 
	
	        get.addRequestHeader("Accept", "text/plain");
	
	        int result = client.executeMethod(get);
	        String slist = get.getResponseBodyAsString();
	        return slist;
		}catch(Exception e){
			e.printStackTrace();
		}
		return "";
	}
	
	public void serviceUpdateTopFilter(String layername, int [] catagories_to_show, boolean commit_changes){
		System.out.println("serviceUpdateTopFilter");
		try{
			///apply2/pid/{pid}/layers/{layers}/types/{types}/val1s/{val1s}/val2s/{val2s}
			StringBuffer show = new StringBuffer();
			int i;
			for(i=0;i<catagories_to_show.length;i++){
				show.append(catagories_to_show[i]);
				if(i < catagories_to_show.length-1){
					show.append(",");
				}
			}
			
			StringBuffer sbProcessUrl = new StringBuffer();
			if(commit_changes){
				sbProcessUrl.append(satServer + "ws/filtering/apply4");
			}else{
				sbProcessUrl.append(satServer + "ws/filtering/apply3");
			}
	        sbProcessUrl.append("/pid/" + URLEncoder.encode(service_pid, "UTF-8"));
	        sbProcessUrl.append("/layers/" + URLEncoder.encode(layername, "UTF-8"));
	        sbProcessUrl.append("/types/" + URLEncoder.encode("ctx", "UTF-8"));
	        sbProcessUrl.append("/val1s/" + URLEncoder.encode(show.toString(), "UTF-8"));
	        sbProcessUrl.append("/val2s/" + URLEncoder.encode("none", "UTF-8"));  
	        sbProcessUrl.append("/depth/" + URLEncoder.encode("" + (_layer_filters_selected.size()) , "UTF-8"));
	        
	        HttpClient client = new HttpClient();
	        GetMethod get = new GetMethod(sbProcessUrl.toString()); 
	
	        get.addRequestHeader("Accept", "text/plain");
	
	        int result = client.executeMethod(get);
	        String slist = get.getResponseBodyAsString();
	        
	        String req1 = "removeImageLayer('filterlayer_" + getSelectedFilters().length + "');";			
			String img = satServer + "output/filtering/" + service_pid + "/" + slist;			
	        String client_request = "addImageLayer('" + img 
	        		+ "','filterlayer_" + getSelectedFilters().length + "',112,-9,154,-44,1008,840);";// + req1;
	        System.out.println("evaljavascript: " + client_request);                      
	        Clients.evalJavaScript(client_request);	
	        
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	public void deleteSelectedFilters(Object o){
		System.out.println("deleteSelectedFilters()");
		serviceRemoveTopFilter();
		
		Listbox lb = null;
		Listitem li;
		String label;
		if(o == null){
			li = (Listitem) lb.getSelectedItem();
		}else{
			li = (Listitem)((Listcell)o).getParent();
		}
		int idx = li.getIndex();

		label = ((LayerFilter)_layer_filters_selected.get(idx)).layer.display_name;

			System.out.println("deleteSelectedFilters(" + label + ")");


			for(Object oi : _layer_filters_selected){
				LayerFilter f =  (LayerFilter) oi;

				if(f.layer.display_name.equals(label)){
					((LayerFilter)oi).count = 0;

					_layer_filters_selected.remove(oi);
					int i = 0;
					for(i=0;i<_layer_filters_original.size();i++){
						if(((LayerFilter)_layer_filters_original.get(i)).layer.display_name.equals(label)){
							if(((LayerFilter)_layer_filters_original.get(i)).layer.type.equals("environmental")){
								//Clients.evalJavaScript("applyFilter(" + i + ",-999,100000);");
								//filteringImage.applyFilter(i,-999,100000);
							}else{
								String javascript = "applyFilterCtx(" + i + ",-2,true);";
								//Clients.evalJavaScript(javascript);
								//filteringImage.applyFilterCtx(i,-2,true);
							}
							//System.out.println("done client applyFilter call for idx=" + i);
							break;
						}
					}

					System.out.println("deleting from seletion list success!");

					break;
				}

			}
			li.detach();

			listFix();
	}

	public void onChangeSliderMax(){
		try{

			int curpos = popup_slider_max.getCurpos();

			double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

			popup_maximum.setValue(String.valueOf((float)(curpos/100.0*range+popup_filter.minimum_initial)));

			popup_filter.maximum_value = Double.parseDouble(popup_maximum.getValue());

			((Listcell)popup_item.getLastChild().getPreviousSibling()).setLabel(
					popup_filter.toString());

			clientFilter();
		}catch(Exception e){
			System.out.println("slider change max:" + e.toString());
		}
	}
	public void onChangeSliderMin(){
		try{

			int curpos = popup_slider_min.getCurpos();

			double range = popup_filter.maximum_initial - popup_filter.minimum_initial;

			popup_minimum.setValue(String.valueOf((float)(curpos/100.0*range+popup_filter.minimum_initial)));

			popup_filter.minimum_value = Double.parseDouble(popup_minimum.getValue());

			((Listcell)popup_item.getLastChild().getPreviousSibling()).setLabel(
					popup_filter.toString());

			clientFilter();

		}catch(Exception e){
			System.out.println("slider change min:" + e.toString());
		}
	}

	void clientFilter(){
		double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
		double maxcurpos =  ((popup_filter.maximum_value - popup_filter.minimum_initial)
				/ (range));
		double mincurpos = ((popup_filter.minimum_value - popup_filter.minimum_initial)
				/ (range));

		//System.out.println(popup_filter.maximum_initial + ", " + popup_filter.minimum_initial
			//	+ ", " + popup_filter.maximum_value + "," + popup_filter.minimum_value);
		String idx = popup_idx.getValue();

		//System.out.println("applying filter from idx:" + idx);

		int i = 0;
		int idx_n = Integer.parseInt(idx);
		if(idx_n < 19){
		//	System.out.println("clientFilter(" + idx + "," + mincurpos + "," + maxcurpos + ")");
			//Clients.evalJavaScript("applyFilter(" + idx + "," + mincurpos + "," + maxcurpos + ");");
			//filteringImage.applyFilter(Integer.valueOf(idx),mincurpos,maxcurpos);
			serviceUpdateTopFilter(popup_filter.layer.display_name, popup_filter.minimum_value, popup_filter.maximum_value,false);
		//	System.out.println("done client applyFilter call for idx=" + i);
		}else{
		//	System.out.println("selected catagories (clientFilter()): " + popup_filter.catagories.length);

//			LayerFilter lf = popup_filter; //(LayerFilter)_layer_filters_original.get(idx_n);
//
//			//System.out.println("selected catagories (obj): " + lf.catagories.length);
//
//			String javascript = "";
//			javascript += "applyFilterCtx(" + idx_n + ",-2,false);"; 	//should take out missing value
//			//filteringImage.applyFilterCtx(idx_n,-2,false);
//			for(int k=0;k<lf.catagory_names.length;k++){
//				int j = 0;
//				for(j=0;j<lf.catagories.length;j++){
//					if(k == lf.catagories[j]){
//						System.out.println("show: layer=" + idx_n + " value=" + k);
//						javascript += "applyFilterCtx(" + idx_n + "," + k + ",true);"; 	//should take out missing value
//				//		filteringImage.applyFilterCtx(idx_n,k,true);
//						break;
//					}
//				}
//				if(j == lf.catagories.length){
//					//unhide
//					//System.out.println("hide: layer=" + idx_n + " value=" + k);
//					//javascript += "applyFilterCtx(" + idx_n + "," + k + ",false);"; 	//should take out missing value
//					//filteringImage.applyFilterCtx(idx_n,k,false);
//				}
//			}
			
			
	//		Clients.evalJavaScript(javascript);
			
			Set selected = popup_listbox.getSelectedItems();
			int [] items_selected = new int[selected.size()];
			int pos = 0;

			for(Object o : selected){
				Listitem li = (Listitem) o;
				items_selected[pos++] = li.getIndex();
			}

			popup_filter.catagories = items_selected;
			
			serviceUpdateTopFilter(popup_filter.layer.display_name, popup_filter.catagories,false);
		}
	}

	public void onClick$apply_continous(Event event){
		System.out.println("applycontinous" + event.toString());
		applyFilter();
	}

	public void onClick$apply_catagorical(Event event){
		System.out.println("applycontinous" + event.toString());
		applyFilter();
	}
	public void applyFilter(){
		if(popup_filter != null){

			/* two cases: catagorical and continous */
			if(popup_filter.catagory_names == null){
				try{
					popup_filter.minimum_value = Double.parseDouble(popup_minimum.getValue());
					popup_filter.maximum_value = Double.parseDouble(popup_maximum.getValue());
			//		System.out.println(popup_filter.minimum_value + "," + popup_filter.maximum_value);
					serviceUpdateTopFilter(popup_filter.layer.display_name, popup_filter.minimum_value, popup_filter.maximum_value, true);
				}catch(Exception e){
					System.out.println("value conversion error");
				}
			}else{
				Set selected = popup_listbox.getSelectedItems();
				int [] items_selected = new int[selected.size()];
				int pos = 0;

				for(Object o : selected){
					Listitem li = (Listitem) o;
					items_selected[pos++] = li.getIndex();
				}

				popup_filter.catagories = items_selected;
				
				serviceUpdateTopFilter(popup_filter.layer.display_name, popup_filter.catagories, true);
				
			//	System.out.println("selected catagories: " + popup_filter.catagories.length);
			}

			popup_catagorical.close();
			popup_continous.close();

			//popup_cell.setLabel(popup_filter.getFilterString());
			((Listcell)popup_item.getLastChild().getPreviousSibling()).setLabel(
					popup_filter.toString());
			Integer count = new Integer(0);

			LayerFilter [] layer_filters = getSelectedFilters();
			int c = 0;
			if(layer_filters != null){
				//c = FilteringIndex.listSpeciesCountGeo(layer_filters);
				c = serviceSpeciesCount();

				popup_filter.count = c;
			}

			/*
			 * Listcell lc =(Listcell) popup_cell.getParent().getLastChild();
			 */
			Listcell lc = ((Listcell)popup_item.getLastChild());
			lc.setLabel(String.valueOf(c));

			clientFilter();
		}
	}

	public int countSpeciesGeo(Integer return_count) {
		/*
		LayerFilter [] layer_filters = getSelectedFilters();
		if(layer_filters != null){
			int count = FilteringIndex.listSpeciesCountGeo(layer_filters);

			try{
				if(return_count == null){
					Messagebox.show("found " + count + " unique species");
				}else{
					return_count = count;
					return count;
				}
			}catch (Exception e){
				System.out.println(e.toString());
			}
		}
		return 0;*/
		int count = serviceSpeciesCount();
		if(return_count != null){
			return_count = count;
		}
		return count;
	}

	public LayerFilter [] getSelectedFilters(){
		LayerFilter [] f = new LayerFilter[_layer_filters_selected.size()];
		_layer_filters_selected.toArray(f);
		return f;
	}

	public void onClick$download(){
		LayerFilter [] layer_filters = getSelectedFilters();
		if(layer_filters != null){
			StringBuffer sb = new StringBuffer();
			for(String s : results){
				sb.append(s);
				sb.append("\r\n");
			}
			org.zkoss.zhtml.Filedownload.save(sb.toString(),"text/plain","filter.csv");
		}else{

		}
	}
	
	public void onClick$downloadsamples(){
		LayerFilter [] layer_filters = getSelectedFilters();
		if(layer_filters != null){
			try{
				System.out.println("attempt to download: " + satServer  + this.serviceSamplesList());
				Filedownload.save(new URL(satServer + this.serviceSamplesList()), "application/zip");
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{

		}
	}
	
	public void listFix(){
		int i;
		List list = lb.getItems();
		for(i=0;list != null && i<list.size()-1;i++){
			Listitem li = (Listitem)list.get(i);
			((Listcell)li.getFirstChild()).setLabel("");

			li.setDisabled(true);
		}
		if(list != null && list.size() > 0){
			Listitem li = (Listitem)list.get(i);
			((Listcell)li.getFirstChild()).setLabel("remove");

			li.setDisabled(false);
		}
	}

	public void onClick$results_prev(Event event){
		if(results_pos == 0){
			return;
		}

		seekToResultsPosition(results_pos-15);
	}

	public void onClick$results_next(Event event){
		if(results_pos+15 >= results.length){
			return;
		}

		seekToResultsPosition(results_pos+15);
	}

	public void onChanging$popup_results_seek(InputEvent event){
		//seek results list
		String search_for = event.getValue();
		//if(search_for.length() > 1){
		//	search_for = search_for.substring(0,1).toUpperCase() + search_for.substring(1,search_for.length()).toLowerCase();
		/*}else*/ if(search_for.length() > 0){
			search_for = search_for.toLowerCase();
		}

		int pos = java.util.Arrays.binarySearch(results,search_for);
		System.out.println("seek to: " + pos + " " + search_for);
		if(pos < 0){
			pos = (pos *-1) -1;
		}
		seekToResultsPosition(pos);
	}

	void seekToResultsPosition(int newpos){
		results_pos = newpos;

		if(results_pos < 0){
			results_pos = 0;
		}
		if(results_pos >= results.length){
			results_pos = results.length-1;
		}

		int sz = results_pos + 15;
		if(results.length < sz){
			sz = results.length;
		}

		String [] list = new String[sz-results_pos];
		int i;
		for(i=results_pos;i<sz;i++){
			list[i-results_pos] = results[i];
		}

		ListModelArray slm = new ListModelArray(list,true);

		popup_listbox_results.setModel(slm);
		results_label.setValue(results_pos + " to " + (sz) + " of " + results.length);
	}
}

