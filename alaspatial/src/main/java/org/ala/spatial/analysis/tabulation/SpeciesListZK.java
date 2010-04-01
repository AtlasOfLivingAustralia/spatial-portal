package org.ala.spatial.analysis.tabulation;

import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.metainfo.EventHandler;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.metainfo.ZScript;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.ConditionImpl;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.ala.spatial.analysis.*;
import org.ala.spatial.util.*;
import org.ala.spatial.util.Grid;

import java.awt.image.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class SpeciesListZK extends Window {
	List _layer_filters = new ArrayList();
	List _layer_filters_original = new ArrayList();
	List _layer_filters_selected = new ArrayList();
		
	/**
	 * for functions in popup box
	 */
	SPLFilter popup_filter;
	Listcell popup_cell;
	
	String species_filter;
		
	public SpeciesListZK() {
		int i;
		TabulationSettings.load();

		SPLFilter layer_filter;
		
		/* list of all layers */
		for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
			layer_filter = 
				SpeciesListIndex.getLayerFilter(
						TabulationSettings.environmental_data_files[i]);
			_layer_filters.add(layer_filter);
			layer_filter = 
				SpeciesListIndex.getLayerFilter(
						TabulationSettings.environmental_data_files[i]);
			_layer_filters_original.add(layer_filter);
		}
		for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
			layer_filter = 
				SpeciesListIndex.getLayerFilter(
						TabulationSettings.geo_tables[i]);
			_layer_filters.add(layer_filter);
			layer_filter = 
				SpeciesListIndex.getLayerFilter(
						TabulationSettings.environmental_data_files[i]);
			_layer_filters_original.add(layer_filter);
		}
		
		System.out.println("done layer setup");		
	}
	
	public void addLayerToFilter(){
		Bandbox bb = (Bandbox) getFellow("bb");
		System.out.println("addlayertofilter:" + bb.getValue())
		;
		for(Object o : _layer_filters_selected){
			SPLFilter f = (SPLFilter) o;
			if(f.layer.display_name.equals(bb.getValue())){
				System.out.println("already added");
				return; //already added
			}
		}
		System.out.println("not already added");
		for(Object o : _layer_filters){
			SPLFilter f = (SPLFilter) o;
			System.out.println(f.layer.display_name);
			if(f.layer.display_name.equals(bb.getValue())){
				System.out.println("found");
				_layer_filters_selected.add(f);
			}
		}
		
		/* apply something to line onclick in lb */
		
		Listbox lb = (Listbox) getFellow("lb");
		lb.setItemRenderer(new ListitemRenderer(){
			public void render(Listitem li, Object data) {
                SPLFilter f = (SPLFilter)data;
                Listcell remove = new Listcell("remove");
                remove.setParent(li);
                remove.addEventListener("onClick",new EventListener(){
                	public void onEvent(Event event) throws Exception {
                		deleteSelectedFilters(event.getTarget());
                	}
                });
                
                new Listcell(f.layer.display_name).setParent(li);
                Listcell lc = new Listcell(f.getFilterString());
                lc.setParent(li);
                
                lc.addEventListener("onClick",new EventListener() {
        			public void onEvent(Event event) throws Exception {
        				showAdjustPopup(event.getTarget());
        			}
        		});  
                
                new Listcell("0").setParent(li);
            }
		});	
		
		lb.setModel(new SimpleListModel(_layer_filters_selected));				
	}
	
	public SPLFilter [] getSelectedFilters(){
		Listbox lb = null;
		try {
			lb = (Listbox) getFellow("lb");
		} catch (Exception e) {
			System.out.println("download():" + e.toString());
		}
		
		System.out.println("getSelectedFilters");
		
		SpeciesListIndex sli = new SpeciesListIndex();			
		
		SPLFilter [] layer_filters = null;
//		lb.selectAll();
		
		List items =  lb.getItems();		
		
		int i;
		
		if(items.size() > 0){
			layer_filters = new SPLFilter[items.size()];

			i = 0;
			for(Object o : items){
				Listitem li = (Listitem) o;
				System.out.println("label: " + ((Listcell)li.getFirstChild().getNextSibling()).getLabel());
				//SPLFilter lf = ((SPLFilter)li.getValue());
				for(int j=0;j<_layer_filters_selected.size();j++){
					SPLFilter lf = ((SPLFilter)_layer_filters_selected.get(j));
					if(lf.layer.display_name.equals(li.getLabel())){						
						System.out.println("layer(" + i + ") " + lf.layer.display_name);
						System.out.println(lf.minimum_value + "," + lf.maximum_value);
						layer_filters[i++] = lf;
						break;						
					}
				}
				
			}
		}
		
		return layer_filters;
	}
	public void countSpecies() {
		SPLFilter [] layer_filters = getSelectedFilters();
		if(layer_filters != null){
			int count = SpeciesListIndex.listSpeciesCount(layer_filters);			

			try{
				Messagebox.show("found " + count + " unique species");
			}catch (Exception e){
				System.out.println(e.toString());
			}
			
		}else{
			
		}	
	}
	
	
	public void countSpeciesGeo(Integer return_count) {
		SPLFilter [] layer_filters = getSelectedFilters();
		if(layer_filters != null){
			int count = SpeciesListIndex.listSpeciesCountGeo(layer_filters);			

			try{
				if(return_count == null){
					Messagebox.show("found " + count + " unique species");
				}else{
					return_count = count;
				}
			}catch (Exception e){
				System.out.println(e.toString());
			}
			
		}else{
			
		}	
	}
		
	public void download() {
		SPLFilter [] layer_filters = getSelectedFilters();
		if(layer_filters != null){
			String csv = SpeciesListIndex.listSpecies(layer_filters);
			
			org.zkoss.zhtml.Filedownload.save(csv,"text/plain","filter.csv");
		}else{
			
		}	
	}
	
	public void downloadGeo() {
		SPLFilter [] layer_filters = getSelectedFilters();
		if(layer_filters != null){
			String csv = SpeciesListIndex.listSpeciesGeo(layer_filters);
			
			org.zkoss.zhtml.Filedownload.save(csv,"text/plain","filter.csv");
		}else{
			
		}	
	}

	/**
	 * TODO: make it do something
	 */
	public void filterLayers(String filter) {
		if(filter == null){
			filter = "";
		}
		try {			
			Listbox bblb = (Listbox) getFellow("bblb");
			Bandbox bb = (Bandbox) getFellow("bb");		
			
			/* iterate through lb Listitem and set visible */
			for(Listitem li : (List<Listitem>)bblb.getItems()){
				SPLFilter lf = (SPLFilter)li.getValue();
				if(lf.layer.display_name.contains(filter)){
					if(!li.isVisible()){
						li.setVisible(true);
					}
				}else{
					if(li.isVisible()){
						li.setVisible(false);
					}
				}
			}
			bb.open();
		} catch (Exception e) {
			System.out.println("filterLayers()" + e.toString());
		}
	}

	public List getLayers() {
		return _layer_filters;
	}

	public void setLayers(List layers) {
		_layer_filters = layers;
	}
	
	public List getLayersSelected() {
		return _layer_filters_selected;
	}

	public void setLayersSelected(List layers) {
		_layer_filters_selected = layers;
	}
	
	
	public void showAdjustPopup(Object o){
		System.out.println("showAdjustPopup");
		if(o == null){
			//get end cell
			Listbox lb = (Listbox) getFellow("lb");
			int i = lb.getItemCount();
			Listitem li = (Listitem) lb.getItemAtIndex(i-1);
			o = li.getLastChild().getPreviousSibling();	
			System.out.println(li);
			System.out.println(o);
			
		}
		Listcell lc = (Listcell)o;
		Listitem li = (Listitem) lc.getParent();
	//	Listitem li = (Listitem)o;
	//	Listcell lc = (Listcell) li.getChildren().get(0);
		SPLFilter lf = (SPLFilter) _layer_filters_selected.get(li.getIndex());
		
		popup_filter = lf;
		popup_cell = lc;
		
		System.out.println("lf=" + lf);
		System.out.println(lf.getClass().toString());
		System.out.println("lflayer=" + lf.layer);
		System.out.println("lflayer name=" + lf.layer.name);
		System.out.println("lflayer display name=" + lf.layer.display_name);
		System.out.println("lflayer type =" + lf.layer.type);
		
		
		if(popup_filter.layer.type == "environmental"){
			Label l = (Label) getFellow("popup_range");		
			String csv = SpeciesListIndex.getLayerExtents(lf.layer.name);
			l.setValue(csv);
			
			Textbox tbmin = (Textbox) getFellow("popup_minimum");
			Textbox tbmax = (Textbox) getFellow("popup_maximum");
			Textbox tbidx = (Textbox) getFellow("popup_idx");
			try{
				tbmin.setValue(String.valueOf((float)popup_filter.minimum_value));
				tbmax.setValue(String.valueOf((float)popup_filter.maximum_value));
				int idx = 0;
				for(idx=0;idx<_layer_filters.size();idx++){
					if(((SPLFilter)_layer_filters.get(idx)).layer.name == lf.layer.name){
						System.out.println("popup for " + lf.layer.display_name);
						break;
					}
				}
				tbidx.setValue(String.valueOf(idx));
			}catch(Exception e){
				System.out.println("value conversion error");
			}		
			
			double range = popup_filter.maximum_initial - popup_filter.minimum_initial;		
			int maxcursor = (int) ((popup_filter.maximum_value - popup_filter.minimum_initial)
					/ (range) * 100);
			int mincursor = (int) ((popup_filter.minimum_value - popup_filter.minimum_initial) 
					/ (range) * 100);
			
			System.out.println("range:" + range + " mincursor:" + mincursor + " maxcursor:" + maxcursor);
			
			((Slider) getFellow("popup_slider_min")).setCurpos(mincursor);
			((Slider) getFellow("popup_slider_max")).setCurpos(maxcursor);					
			
			Popup p = (Popup) getFellow("popup_continous");
			p.open(lc);
		}else{ //catagorical values
			Listbox lb = (Listbox) getFellow("popup_listbox");
			lb.setModel(new SimpleListModel(lf.catagory_names));
			
			/* set check boxes */
			for(int i : lf.catagories){
				Listitem listitem = lb.getItemAtIndex(i);
				lb.addItemToSelection(listitem);
			}
			
			Popup p = (Popup) getFellow("popup_catagorical");
			p.open(lc);
		}
	}
	
	public void downloadMetaData(){
		String metadata= SamplingService.getLayerMetaData(popup_filter.layer.name);
		
		org.zkoss.zhtml.Filedownload.save(
				metadata,
				"text/plain",
				popup_filter.layer.display_name + "_metadata" + ".txt");
	}
	
	public void applyFilter(){
		if(popup_filter != null){
		
			/* two cases: catagorical and continous */
			if(popup_filter.catagory_names == null){
				Textbox tbmin = (Textbox) getFellow("popup_minimum");
				Textbox tbmax = (Textbox) getFellow("popup_maximum");			
				try{
					popup_filter.minimum_value = Double.parseDouble(tbmin.getValue());
					popup_filter.maximum_value = Double.parseDouble(tbmax.getValue());
				}catch(Exception e){
					System.out.println("value conversion error");
				}				
			}else{
				Listbox lb = (Listbox) getFellow("popup_listbox");
				Set selected = lb.getSelectedItems();				
				int [] items_selected = new int[selected.size()];
				int pos = 0;
				
				for(Object o : selected){
					Listitem li = (Listitem) o;
					items_selected[pos++] = li.getIndex();
				}
				
				popup_filter.catagories = items_selected;
			}
			
			((Popup) getFellow("popup_catagorical")).close();
			((Popup) getFellow("popup_continous")).close();
			
			popup_cell.setLabel(popup_filter.getFilterString());
			Integer count = 0;
			countSpeciesGeo(count);
			
			try{
				Messagebox.show("found " + count.toString() + " unique species");
			}catch (Exception e){
				System.out.println(e.toString());
			}
			
			((Listcell)popup_cell.getNextSibling()).setLabel(count.toString());
			
			clientFilter();
		}
	}
	
	public void onChangeSliderMax(){
		try{
			Textbox tbmax = (Textbox) getFellow("popup_maximum");	
			int curpos = ((Slider) getFellow("popup_slider_max")).getCurpos();
					
			double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
			
			tbmax.setValue(String.valueOf((float)(curpos/100.0*range+popup_filter.minimum_initial)));
					
			popup_filter.maximum_value = Double.parseDouble(tbmax.getValue());
			popup_cell.setLabel(popup_filter.getFilterString());
			
			clientFilter();
		}catch(Exception e){
			System.out.println("slider change max:" + e.toString());
		}		
	}
	public void onChangeSliderMin(){
		try{
			
			Textbox tbmin = (Textbox) getFellow("popup_minimum");			
			int curpos = ((Slider) getFellow("popup_slider_min")).getCurpos();
					
			double range = popup_filter.maximum_initial - popup_filter.minimum_initial;
			
			tbmin.setValue(String.valueOf((float)(curpos/100.0*range+popup_filter.minimum_initial)));
			
			popup_filter.minimum_value = Double.parseDouble(tbmin.getValue());
			popup_cell.setLabel(popup_filter.getFilterString());

			clientFilter();
			
		}catch(Exception e){
			System.out.println("slider change min:" + e.toString());
		}	
	}
	
	void clientFilter(){
		String idx = ((Textbox) getFellow("popup_idx")).getValue();
		double mincurpos = ((Slider) getFellow("popup_slider_min")).getCurpos()/100.0;
		double maxcurpos = ((Slider) getFellow("popup_slider_max")).getCurpos()/100.0;
		System.out.println("applying filter");
		Clients.evalJavaScript("applyFilter(" + idx + "," + mincurpos + "," + maxcurpos + ");");
	}
	
	public void deleteSelectedFilters(Object o){
		Listbox lb = null;
		lb = (Listbox) getFellow("lb");		
		//Set items =  lb.getSelectedItems();	
		
		//for(Object o : items){
			//Listitem li = (Listitem) o;
		Listitem li;
		String label;
		if(o == null){
			li = (Listitem) lb.getSelectedItem();
			
		}else{
			li = (Listitem)((Listcell)o).getParent();
		}
		label = ((Listitem) li.getNextSibling()).getLabel();
		
			System.out.println("deleteSelectedFilters(" + label + ")");
			
			for(Object oi : _layer_filters_selected){
				SPLFilter f =  (SPLFilter) oi;
				
				if(f.layer.display_name.equals(label)){
					_layer_filters_selected.remove(oi);
					System.out.println("deleting from seletion list success!");
					break;
				}
			}
			li.detach();			
		//}	
	}
}
