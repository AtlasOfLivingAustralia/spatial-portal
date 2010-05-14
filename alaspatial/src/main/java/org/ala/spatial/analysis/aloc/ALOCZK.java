package org.ala.spatial.analysis.aloc;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.net.URLEncoder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.util.Clients;
import org.ala.spatial.analysis.tabulation.SPLFilter;
import org.ala.spatial.analysis.tabulation.SpeciesListIndex;
import org.ala.spatial.analysis.tabulation.TabulationSettings;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Popup;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

public class ALOCZK extends GenericForwardComposer {
	
	private String geoServer = "http://localhost:8080"; //"http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";  
    private String satServer = geoServer;  
    
	List _layer_filters = new ArrayList();
	List _layer_filters_original = new ArrayList();
	List _layer_filters_selected = new ArrayList();
	
	/**
	 * for functions in popup box
	 */
	SPLFilter popup_filter;
	Listcell popup_cell;
	Listitem popup_item;
	Label lb_points;
	
	String temp_filename;
	
	String species_filter;	
	String [] results = null;
	
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
	
	public Button run_button;
	
	String results_path;
		
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		
		int i;
		TabulationSettings.load();
		geoServer = TabulationSettings.alaspatial_path;
		satServer = TabulationSettings.alaspatial_path;

		SPLFilter layer_filter;
		
		/* list of all layers */
		for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
			layer_filter =	SpeciesListIndex.getLayerFilter(
						TabulationSettings.environmental_data_files[i]);
			_layer_filters.add(layer_filter);
			layer_filter = 
				SpeciesListIndex.getLayerFilter(
						TabulationSettings.environmental_data_files[i]);
			_layer_filters_original.add(layer_filter);
		}
			
		
		onChanging$cb(null);
		
		System.out.println("done layer setup");		
	}
	
	public void onChanging$cb(Event event){
		
		//if(event == null || !event.isChangingBySelectBack()){	
			Iterator it = cb.getItems().iterator();
			int i=0;
			while(i < _layer_filters_original.size()){
				if(it != null && it.hasNext()){
					((Comboitem) it.next()).setLabel(((SPLFilter)_layer_filters_original.get(i)).layer.display_name + " (Terrestrial)");
				}else{
					it = null;
					new Comboitem(((SPLFilter)_layer_filters_original.get(i)).layer.display_name + " (Terrestrial)").setParent(cb);
				}	
				System.out.println("*x*" + ((SPLFilter)_layer_filters_original.get(i)).layer.display_name);
				i++;
			}
			while(it != null && it.hasNext()){
				((Comboitem) it.next()).detach();
			}
		//}	
		
	}
	
	public void onChange$cb(Event event){
		String new_value = ""; 
	
		new_value = cb.getValue();
		if(new_value.equals("")){
			return;
		}
		new_value = (new_value.equals("")) ? "" : new_value.substring(0,new_value.indexOf("(")).trim(); 
		System.out.println("new value: " + new_value); 
		
		
		for(Object o : _layer_filters_selected){
			SPLFilter f = (SPLFilter) o;
			if(f.layer.display_name.equals(new_value)){
				System.out.println("already added");
				return; //already added
			}
		}
		System.out.println("not already added");
		for(Object o : _layer_filters){
			SPLFilter f = (SPLFilter) o;
			System.out.println(f.layer.display_name);
			if(f.layer.display_name.equals(new_value)){
				System.out.println("found");
				_layer_filters_selected.add(f);
			}
		}
		
		/* apply something to line onclick in lb */
		
		lb.setItemRenderer(new ListitemRenderer(){
			public void render(Listitem li, Object data) {
                SPLFilter f = (SPLFilter)data;
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
            }
		});	
		
		lb.setModel(new SimpleListModel(_layer_filters_selected));
		Listitem li = lb.getItemAtIndex(lb.getItemCount()-1);
		Listcell lc = (Listcell) li.getLastChild();
		System.out.println(lc);
		lc = (Listcell) lc.getPreviousSibling();
		
		
	}
	
	
	public void deleteSelectedFilters(Object o){
		Listbox lb = null;
		Listitem li;
		String label;
		if(o == null){
			li = (Listitem) lb.getSelectedItem();			
		}else{
			li = (Listitem)((Listcell)o).getParent();
		}
		int idx = li.getIndex();
		
		label = ((SPLFilter)_layer_filters_selected.get(idx)).layer.display_name;
		
			System.out.println("deleteSelectedFilters(" + label + ")");
			
			
			for(Object oi : _layer_filters_selected){
				SPLFilter f =  (SPLFilter) oi;
				
				if(f.layer.display_name.equals(label)){
					((SPLFilter)oi).count = 0;
					
					_layer_filters_selected.remove(oi);
										
					System.out.println("deleting from seletion list success!");
								
					break;
				}				
			}
			li.detach();			
	}
	public SPLFilter [] getSelectedFilters(){
		SPLFilter [] f = new SPLFilter[_layer_filters_selected.size()];
		_layer_filters_selected.toArray(f);
		return f;	
	}


	public void onClick$download(){
		//org.zkoss.zhtml.Filedownload.save(temp_filename,"text/plain","ALOC.png");
		//java.net.URL url = new java.net.URL(results_path);
		
		//org.zkoss.zhtml.Filedownload.save(url,"image/png");		
	}	

	public void onClick$run_button(){
		try {
			File temporary_file0 = java.io.File.createTempFile("ALOC_",".png");
			SPLFilter [] filters= getSelectedFilters();
			
			if(filters == null || filters.length == 0){
				return;
			}
			
			Layer [] layers = new Layer[filters.length];
			int i;
			for(i=0;i<layers.length;i++){
				layers[i] = filters[i].layer;
			}
					
			SimpleRegion sr = null;
			if(lb_points.getValue().length() > 0){
				sr = getSimpleRegion(lb_points.getValue());
			}
			
			//ALOC.run(temporary_file0.getPath(),layers,Integer.parseInt(number_of_groups.getValue()));
			/*ALOC.run(temporary_file0.getPath(),layers,Integer.parseInt(number_of_groups.getValue()),sr);
			
			temp_filename = temporary_file0.getPath();
			
			org.zkoss.image.Image image = new org.zkoss.image.AImage(temporary_file0.getPath());
			
			results_image.setContent(image);
			popup_results.open(cb);*/
			
			btnGenerate(layers,number_of_groups.getValue(),lb_points.getValue());
			if(download != null){	
				download.setVisible(true);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void btnGenerate(Layer [] layers,String number_of_groups, String points) {
        try {
            StringBuffer sbenvsel = new StringBuffer();
            for(int i=0;i<layers.length;i++){
               sbenvsel.append(layers[i].display_name);
                if (i<layers.length-1) {
                    sbenvsel.append(":");
                }                
            } 

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "ws/aloc/processgeo?");
            sbProcessUrl.append("gc=" + URLEncoder.encode(number_of_groups, "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            if(points.length() > 0){
            	sbProcessUrl.append("&points=" + URLEncoder.encode(points, "UTF-8"));
            }else{
            	sbProcessUrl.append("&points=" + URLEncoder.encode("none", "UTF-8"));
            }

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString()); 

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from ALOCWSController: \n" + slist);
            
/* TODO: make service response include longlat bounds and image resolution */
			String img = satServer + "output/aloc/" + slist + "/aloc.png";
            String client_request = "getALOCimage('" + img + "',112,-9,154,-44,252,210);";
            System.out.println("evaljavascript: " + client_request);                      
            Clients.evalJavaScript(client_request);
            
            results_path = img;
            

        } catch (Exception ex) {
            System.out.println("Opps!: ");
            ex.printStackTrace(System.out);
        }

    }
	
	SimpleRegion getSimpleRegion(String pointsString){
    	SimpleRegion simpleregion = new SimpleRegion();
    	String [] pairs = pointsString.split(",");
    	
    	double [][] points = new double[pairs.length][2];
    	for(int i=0;i<pairs.length;i++){    		
    		String [] longlat = pairs[i].split(":");
    		if(longlat.length == 2){
    			try{
	    			points[i][0] = Double.parseDouble(longlat[0]);
	    			points[i][1] = Double.parseDouble(longlat[1]);
    			}catch (Exception e){
    				e.printStackTrace();
    			}
	    		System.out.print("(" + points[i][0] + "," + points[i][1] + ")");
    		}else{
    			System.out.print("err:" + pairs[i]);
    		}
    		
    	}
    	
    	simpleregion.setPolygon(points);
    	
    	return simpleregion;
    }
	
}

