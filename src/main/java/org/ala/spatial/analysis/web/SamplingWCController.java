package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zkmax.zul.Filedownload;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleGroupsModel;

/**
 *
 * @author ajay
 */
public class SamplingWCController extends UtilityComposer {

    private Combobox sac;
    private Listbox lbenvlayers;
    private Button btnMapSpecies;
    private Button btnPreview;
    private Popup results;
    private Popup p;
    private Html h;
    private Rows results_rows;
    private Button btnDownload;
    private List layers;
    private Map layerdata;
    private String selectedLayer;
    private String geoServer = "http://ec2-184-73-34-104.compute-1.amazonaws.com";  // http://localhost:8080
    private String satServer = geoServer;

    @Override
    public void afterCompose() {
        super.afterCompose();

        layers = new Vector();
        layerdata = new Hashtable<String, String[]>();

        // Load the layers
        //setupEnvironmentalLayers();
        //setupContextualLayers();
        //lbenvlayers.setModel(new SimpleListModel(layers));

        //lbenvlayers.setModel(new SimpleGroupsModel((Object[][]) layerdata.values().toArray(),layerdata.keySet().toArray()));

        String[][] datas = new String[][]{
            setupEnvironmentalLayers(),
            setupContextualLayers()
        };

        lbenvlayers.setItemRenderer(new ListitemRenderer() {

            public void render(Listitem li, Object data) {
                try {


                    String layername = (String) data;
                    li.setWidth(null);
                    Listcell lc = new Listcell(layername);
                    lc.setParent(li);
                    
                    selectedLayer = layername.replaceAll(" ", "_");

                    HttpClient client = new HttpClient();
                    GetMethod get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + selectedLayer + "/extents"); // testurl
                    get.addRequestHeader("Accept", "text/plain");

                    int result = client.executeMethod(get);
                    String slist = get.getResponseBodyAsString();

                    h.setContent(slist);
                    lc.setPopup(p);
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }

            }
        });
        lbenvlayers.setModel(new SimpleGroupsModel(datas, new String[]{"Environmental", "Contextual"}));
    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private String[] setupEnvironmentalLayers() {
        String[] aslist = null;
        try {

            String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/environmental/string";

            //Messagebox.show("Loading env data from: " + envurl);


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(envurl);
            get.addRequestHeader("Content-type", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            //Messagebox.show("done loading data: " + slist);

            aslist = slist.split("\n");

            if (aslist.length > 0) {


                //lbenvlayers.setModel(new SimpleListModel(aslist));
                layers.addAll(Arrays.asList(aslist));
                layerdata.put("Environmental", aslist);

            }


        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }

        return aslist;
    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private String[] setupContextualLayers() {
        String[] aslist = null;
        try {

            String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/contextual/string";

            //Messagebox.show("Loading env data from: " + envurl);


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(envurl);
            get.addRequestHeader("Content-type", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            //Messagebox.show("done loading data: " + slist);

            aslist = slist.split("\n");

            if (aslist.length > 0) {


                //lbenvlayers.setModel(new SimpleListModel(aslist));
                layers.addAll(Arrays.asList(aslist));
                layerdata.put("Contextual", aslist);
            }


        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }

        return aslist;
    }

    public void onClick$btnMapSpecies(Event event) {
        try {
            //status.setValue("clicked new value selected: " + sac.getText() + " - " + sac.getValue());
            //System.out.println("Looking up taxon names for " + sac.getValue());
            //Messagebox.show("Hello world!, i got clicked");

            loadSpeciesOnMap();

        } catch (Exception ex) {
            System.out.println("Got an error clicking button!!");
            ex.printStackTrace(System.out);
        }
    }

    public void onClick$btnDownloadMetadata(Event event) {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + selectedLayer + "/extents"); // testurl
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();


            Filedownload.save(slist,"text/plain",selectedLayer + "_metadata.txt");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void onClick$btnPreview(Event event) {
        try {

            StringBuffer sbenvsel = new StringBuffer();

            if (lbenvlayers.getSelectedCount() > 0) {
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }

                }
            }


            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/sampling/process/preview?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(sac.getValue(), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            //String testurl = satServer + "/alaspatial/ws/sampling/test";

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl 
            //get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: \n" + slist);

            String[] aslist = slist.split(";");
            System.out.println("Result count: " + aslist.length);
            for (int i = 0; i < aslist.length; i++) {
                String[] rec = aslist[i].split("~");
                System.out.println("Column Count: " + rec.length);
                //System.out.println()

            }

            /*
            //JSONObject jsonObject = JSONObject.fromObject( slist );
            //List expected = JSONArray.toList( jsonObject.getJSONArray( "array" ) );
            JSONArray ja = JSONArray.fromObject(slist);
            //System.out.println("JSON RESPONSE: \n" + ja.toString(2));
            List respData = (List) JSONArray.toCollection(ja);
            System.out.println("Result return size: " + respData.size());
            Iterator it = respData.iterator();
            while (it.hasNext()) {
            Object o = it.next();
            System.out.println(o.getClass());
            System.out.println(o);
            MorphDynaBean bean = ((MorphDynaBean) o);
            System.out.println(bean.toString());
            }
             */


            // load into the results popup
            int j;
            //remove existing rows
            List l = results_rows.getChildren();
            System.out.println(l);
            if (l != null) {
                for (j = l.size() - 1; j >= 0; j--) {
                    Row r = (Row) l.get(j);
                    System.out.println("detaching: " + ((Label) r.getChildren().get(0)).getValue());
                    r.detach();
                }
            }
            /* setup contextual number to name lookups */
            /*
            SPLFilter[] splfilters = new SPLFilter[csv_filename[0].length];
            for (i = 0; i < csv_filename[0].length; i++) {
            if (csv_filename[0][i] != null) {
            String display_name = csv_filename[0][i].trim();
            for (int k = 0; k < _layers.size(); k++) {
            Layer layer = (Layer) _layers.get(k);
            if (layer.display_name.equals(display_name)) {
            splfilters[i] = SpeciesListIndex.getLayerFilter(layer);
            System.out.println("made splfilter: " + layer);
            }
            }
            }
            }
             *
             */

            // add rows
            for (int i = 0; i < aslist.length; i++) {
                String[] rec = aslist[i].split("~");
                System.out.println("Column Count: " + rec.length);
                //System.out.println()

                Row r = new Row();
                r.setParent(results_rows);
                // set the value
                for (int k = 0; k < rec.length; k++) {
                    Label label = new Label(rec[k]);
                    label.setParent(r);

                    /*
                    //add event listener for contextual columns
                    if (j == 0) { //add for header row
                    label.addEventListener("onClick", new EventListener() {
                    
                    public void onEvent(Event event) throws Exception {
                    String display_name = ((Label) event.getTarget()).getValue().trim();
                    for (int k = 0; k < _layers.size(); k++) {
                    Layer layer = (Layer) _layers.get(k);
                    if (layer.display_name.equals(display_name)) {
                    popup_layer = layer;
                    
                    Html h = (Html) getFellow("h");
                    String csv = SpeciesListIndex.getLayerExtents(layer.name);
                    h.setContent(csv);
                    
                    Popup p = (Popup) getFellow("p");
                    //li.setPopup(p);
                    p.open(event.getTarget());
                    }
                    }
                    }
                    });
                    } else {
                    // is catagorical layer
                    if (i < splfilters.length && splfilters[i] != null && splfilters[i].catagory_names != null) {
                    try {
                    int idx = Integer.parseInt(csv_filename[j][i]);
                    label.setTooltiptext(splfilters[i].catagory_names[idx]);
                    } catch (Exception e) {
                    System.out.println(e.toString());
                    }
                    }
                    }
                     */

                }
            }



        } catch (Exception e) {
            System.out.println("Exception calling sampling.preview:");
            e.printStackTrace(System.out);
        }

        results.open(100, 100);

    }

    public void onClick$btnDownload(Event event) {
        try {

            StringBuffer sbenvsel = new StringBuffer();

            if (lbenvlayers.getSelectedCount() > 0) {
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }

                }
            }


            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/sampling/process/download?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(sac.getValue(), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            //String testurl = satServer + "/alaspatial/ws/sampling/test";

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            //get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: \n" + slist);


            if (slist.equalsIgnoreCase("")) {
                Messagebox.show("Unable to download sample file. Please try again", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
            } else {
                /*
                URL url = new URL(satServer + "/alaspatial" + slist);
                url.openStream();
                org.zkoss.zhtml.Filedownload.save(url.openStream(), "application/zip", sac.getValue() + "_sample.zip");
                 *
                 */

                //Filedownload.save(satServer + "/alaspatial" + slist, null);
                //Messagebox.show("Downloading sample file...", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
                System.out.println("Sending file to user: " + satServer + "/alaspatial" + slist);
                Filedownload.save(new URL(satServer + "/alaspatial" + slist), "application/zip");

            }


        } catch (Exception e) {
            System.out.println("Exception calling sampling.download:");
            e.printStackTrace(System.out);
        }

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

    private void loadSpeciesOnMap() {
        String taxon = sac.getValue();
        String uri = null;
        String filter = null;

        // capitalise the taxon name
        System.out.print("Changing taxon name from '" + taxon);
        taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1);
        System.out.println("' to '" + taxon + "' ");


        //uri = "http://ec2-184-73-34-104.compute-1.amazonaws.com/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:occurrencesv1&styles=&srs=EPSG:4326&format=image/png";
        uri = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:occurrencesv1&styles=&srs=EPSG:4326&format=image/png";

        //get the current MapComposer instance
        MapComposer mc = getThisMapComposer();

        //contruct the filter
        //filter = "<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+mapWMS+entity+"&type=1&unit=1]]></Literal></PropertyIsEqualTo></Filter>";
        //lets try it in cql
        filter = "species eq '" + taxon + "'";

        logger.debug(filter);
        //mc.addWMSLayer(label, uri, 1, filter);
        mc.addWMSGazetteerLayer(taxon, uri, 1, filter);


    }
}
