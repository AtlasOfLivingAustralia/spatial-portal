package org.ala.spatial.web.zk;


import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.ala.spatial.analysis.service.OccurrencesService;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.analysis.index.*;
import org.ala.spatial.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.util.Clients;

public class SamplingZK extends Window {
	
	private String geoServer = "http://localhost:8080"; //"http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";  
    private String satServer = geoServer;  
    
    
	//List selected_layers = new ArrayList();
	List _layers = new ArrayList();
	//List _combobox = new ArrayList();

	/**
	 * for functions in popup box
	 */
	Layer popup_layer;

	String species_filter;

	//public Listhead results_listbox_head;

	public SamplingZK() {
		int i;
		TabulationSettings.load();
		geoServer = TabulationSettings.alaspatial_path;
		satServer = TabulationSettings.alaspatial_path;

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

			String [] list = OccurrencesService.filterSpecies(filter,40);
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
			
			Label lb_points = (Label) getFellow("lb_points");
    		String points = lb_points.getValue();
    		if(points.length() == 0){
    			points = "none";
    		}
    		SimpleRegion sr = SimpleRegion.parseSimpleRegion(points);

			String csv_filename = ss.sampleSpecies(species, layers, sr, null);

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

				BufferedInputStream bif = new BufferedInputStream(new FileInputStream(csv_filename));
				byte [] batch = new byte[10000];
				int read;

				while((read = bif.read(batch)) > 0){
					out.write(batch,0,read);
				}
				//out.write(csv.getBytes());
				System.out.println("b: ");
				/* add each catagorical layer */
				if(layers != null){
					for(String s : layers){
						String csv = FilteringIndex.getLayerExtents(s);
						if(csv != null && csv.length() > 0 &&
								csv.contains("<br>")){
							entry = new ZipEntry(
									Layers.layerNameToDisplayName(s)
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
			filter = filter.toLowerCase();

			Listbox lb = (Listbox) getFellow("lb");
			Textbox tb = (Textbox) getFellow("tb");

			/* iterate through lb Listitem and set visible */
			for(Listitem li : (List<Listitem>)lb.getItems()){
				Layer l = (Layer)li.getValue();
				if(l.display_name.toLowerCase().contains(filter)){
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
		String csv = FilteringIndex.getLayerExtents(l.name);
		h.setContent(csv);

		Popup p = (Popup) getFellow("p");
		//li.setPopup(p);
		p.open(lc);

		//org.zkoss.zhtml.Filedownload.save(csv,"text/plain",l.display_name + "_extents" + ".csv");
	}

	public void downloadMetaData(){
		String metadata= Layers.getLayerMetaData(popup_layer.name);

		org.zkoss.zhtml.Filedownload.save(
				metadata,
				"text/plain",
				popup_layer.display_name + "_metadata" + ".txt");
	}

	public void openResults(){
		System.out.println("openResults");

		Popup results = (Popup)getFellow("results");



		Bandbox bb = null;
		Listbox lb = null;
		try {
			lb = (Listbox) getFellow("lb");
			bb = (Bandbox) getFellow("bb");
		} catch (Exception e) {
			System.out.println("openResults():" + e.toString());
		}

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
		
		Label lb_points = (Label) getFellow("lb_points");
		String points = lb_points.getValue();
		if(points.length() == 0){
			points = "none";
		}
		SimpleRegion sr = SimpleRegion.parseSimpleRegion(points);

		String [][] csv_filename = ss.sampleSpecies(species, layers, sr, null, 20);
//		System.out.println("got a " + csv_filename.length + " x " + csv_filename[0].length);
		int j;
		if(csv_filename != null){

			//Listbox rlb = (Listbox)results.getFellow("results_listbox");


			try{
				Rows rows = (Rows) results.getFellow("results_rows");

				//remove existing rows
				List l = rows.getChildren();
				System.out.println(l);
				if(l != null){
					for(j=l.size()-1;j>=0;j--){
						Row r = (Row)l.get(j);
						System.out.println("detaching: " + ((Label)r.getChildren().get(0)).getValue());
						r.detach();
					}
				}


				/* setup contextual number to name lookups */
				LayerFilter [] layerfilters = new LayerFilter[csv_filename[0].length];
				for(i=0;i<csv_filename[0].length;i++){
					if(csv_filename[0][i] != null){
						String display_name = csv_filename[0][i].trim();
						for(int k=0;k<_layers.size();k++){
							Layer layer = (Layer)_layers.get(k);
							if(layer.display_name.equals(display_name)){
								layerfilters[i] = FilteringIndex.getLayerFilter(layer.name);
								System.out.println("made layerfilter: " + layer);
							}
						}
					}
				}

				/* add rows */
				for(j=0;j<csv_filename.length;j++){
					Row r = new Row();
					r.setParent(rows);
					for(i=0;i<csv_filename[j].length;i++){
						Label label = new Label(csv_filename[j][i]);
						label.setParent(r);

						if(j == 0){
							System.out.println("adding header: " + csv_filename[j][i]);
						}

						//add event listener for contextual columns
						if(j==0){ //add for header row
							 label.addEventListener("onClick",new EventListener(){
						                	public void onEvent(Event event) throws Exception {
						                		String display_name = ((Label)event.getTarget()).getValue().trim();
						                		for(int k=0;k<_layers.size();k++){
													Layer layer = (Layer)_layers.get(k);
													if(layer.display_name.equals(display_name)){
														popup_layer = layer;

								                		Html h = (Html) getFellow("h");
								                		String csv = FilteringIndex.getLayerExtents(layer.name);
								                		h.setContent(csv);

								                		Popup p = (Popup) getFellow("p");
								                		//li.setPopup(p);
								                		p.open(event.getTarget());
													}
						                		}
						                	}
				                });
						}else{
							/* is catagorical layer */
							if(i < layerfilters.length && layerfilters[i] != null && layerfilters[i].catagory_names != null){
								try {
									int idx = Integer.parseInt(csv_filename[j][i]);
									label.setTooltiptext(layerfilters[i].catagory_names[idx]);
								}catch (Exception e){
									System.out.println(e.toString());
								}
							}
						}
					}
				}

			}catch (Exception e){
				e.printStackTrace();
			}


		}


		results.open(30,30);//.open();//(false);
	}
	
	public void showPoints() {
        try {
        	Bandbox bb = (Bandbox) getFellow("bb");
    		Label lb_points = (Label) getFellow("lb_points");
System.out.println("LL:" + bb.getValue());
                String [] selection = bb.getValue().toLowerCase().split("/");
                String species = bb.getValue();
                if(selection.length > 1){
                    species = selection[1].trim();
                }
    		
    		String points = lb_points.getValue();
    		if(points.length() == 0){
    			points = "none";
    		}
    		
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "ws/sampling/process/points?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(species, "UTF-8"));
            sbProcessUrl.append("&points=" + URLEncoder.encode(points, "UTF-8"));
            

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: \n" + slist);
            
/* TODO: make service response include longlat bounds and image resolution */
			
            String client_request = "drawCircles('" + slist + "');";
            System.out.println("evaljavascript: " + client_request);                      
            Clients.evalJavaScript(client_request);
                        

        } catch (Exception ex) {
            System.out.println("Opps!: ");
            ex.printStackTrace(System.out);
        }

    }

}
