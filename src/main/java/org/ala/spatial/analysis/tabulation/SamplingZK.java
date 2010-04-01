package org.ala.spatial.analysis.tabulation;

import org.zkoss.zul.*;

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

public class SamplingZK extends Window {
	//List selected_layers = new ArrayList();
	List _layers = new ArrayList();
	//List _combobox = new ArrayList();
	
	/**
	 * for functions in popup box
	 */
	Layer popup_layer;
	
	String species_filter;
	
	public SamplingZK() {
		int i;
		TabulationSettings.load();

		/* list of all layers */
		for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
			_layers.add(TabulationSettings.environmental_data_files[i]);
		}
		for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
			_layers.add(TabulationSettings.geo_tables[i]);
		}	

		System.out.println("done layer setup");		
	}
	
	public void filterSpecies(String filter){
		Bandbox bb = (Bandbox) getFellow("bb");
		Listbox bblb = (Listbox) getFellow("bblb");
		
		System.out.println("combobox value=" + filter);
		
		if(filter.length() >= 1){
			SamplingService ss = new SamplingService();
			
			String [] list = ss.filterSpecies(filter,40);
			if(list == null){
				list = new String[1];
				list[0] = "";
			}
			System.out.print("#" + list.length);
			
			bblb.setModel(new SimpleListModel(list));
			if(list.length < 20){
				bblb.setRows(list.length);
			}else{
				bblb.setRows(20);
			}
			
			bb.open();
		}else{
			String [] list = new String[1];
			list[0] = "";
			bblb.setModel(new SimpleListModel(list));
		}
	}
	
	public void download() {
		Bandbox bb = null;
		Listbox lb = null;
		try {
			lb = (Listbox) getFellow("lb");
			bb = (Bandbox) getFellow("bb");
		} catch (Exception e) {
			System.out.println("download():" + e.toString());
		}
		
			System.out.println("sampling");
			
			SamplingService ss = new SamplingService();			
			
			String species_long = bb.getValue().toLowerCase();
			
			if(species_long == null || species_long.length() < 5){
				return; //TODO make nice
			}
			
			String species = species_long;
			String type;
			if(species_long.contains("/")){
				species = species_long.split("/")[0].trim();
				type = species_long.split("/")[1].trim();
				
				/* join for query */
				species = species + " / " + type;
			}
			
			String [] layers = null;
			
			System.out.println("species=" + species);
			
			Set selected =  lb.getSelectedItems();
			
			int i;
			
			if(selected.size() > 0){
				layers = new String[selected.size()];

				i = 0;
				for(Object o : selected){
					Listitem li = (Listitem) o;
					Layer l = ((Layer)li.getValue());
					System.out.println("layer(" + i + ") " + l.name);
					layers[i++] = l.name;
				}
			}
			
			String csv = ss.sampleSpecies(species, layers);
			
			//org.zkoss.zhtml.Filedownload.save(csv,"text/plain",species + ".csv");
			
			//save to a file
			try{
				
				/* create zip stream */
				System.out.println("10: ");
				File temporary_file0 = java.io.File.createTempFile("sample",".zip");
				System.out.println("1: " + temporary_file0.getPath());
				FileOutputStream dest = new FileOutputStream(temporary_file0);
				ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
				System.out.println("a: " + temporary_file0.getPath());
				
				/* species sample.csv */
				ZipEntry entry = new ZipEntry(species.replace("/ ","") + "_sample.csv");
				out.putNextEntry(entry);
				out.write(csv.getBytes());
				System.out.println("b: ");
				/* add each catagorical layer */
				if(layers != null){
					for(String s : layers){
						csv = SpeciesListIndex.getLayerExtents(s);
						if(csv != null && csv.length() > 0 &&
								csv.contains("<br>")){
							entry = new ZipEntry(
									SamplingService.layerNameToDisplayName(s)
									+ "_lookup_values.csv");
							out.putNextEntry(entry);
							out.write(csv.replace("<br>", "").getBytes());
						}
					}
				}
				
				out.close();
				
				/* read in for 'download' */
				byte [] data = new byte[(int)temporary_file0.length()];
				FileInputStream fis = new FileInputStream(temporary_file0);
				fis.read(data);
				fis.close();
				
				org.zkoss.zhtml.Filedownload.save(data,"application/zip",species + "_sample.zip");
				
			}catch (Exception e){
				System.out.println(e.toString());
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
			Listbox lb = (Listbox) getFellow("lb");
			Textbox tb = (Textbox) getFellow("tb");		
			
			/* iterate through lb Listitem and set visible */
			for(Listitem li : (List<Listitem>)lb.getItems()){
				Layer l = (Layer)li.getValue();
				if(l.display_name.contains(filter)){
					if(!li.isVisible()){
						li.setVisible(true);
					}
				}else{
					if(li.isVisible()){
						li.setVisible(false);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("filterLayers()" + e.toString());
		}
	}

	public List getLayers() {
		return _layers;
	}

	public void setLayers(List layers) {
		_layers = layers;
	}	
	
	
	
	public void showLayerExtents(Object o){
		//Listbox lb = (Listbox) getFellow("lb");
		Listcell lc = (Listcell)o;//lb.getSelectedItem();
		Listitem li = (Listitem) lc.getParent();
		Layer l = (Layer) li.getValue();
		popup_layer = l;
		
		Html h = (Html) getFellow("h");		
		String csv = SpeciesListIndex.getLayerExtents(l.name);
		h.setContent(csv);
		
		Popup p = (Popup) getFellow("p");
		//li.setPopup(p);	
		p.open(lc);
		
		//org.zkoss.zhtml.Filedownload.save(csv,"text/plain",l.display_name + "_extents" + ".csv");
	}
	
	public void downloadMetaData(){
		String metadata= SamplingService.getLayerMetaData(popup_layer.name);
		
		org.zkoss.zhtml.Filedownload.save(
				metadata,
				"text/plain",
				popup_layer.display_name + "_metadata" + ".txt");
	}
}
