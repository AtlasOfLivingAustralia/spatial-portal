/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.gazetteer;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.net.HttpConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import org.ala.rest.GazetteerSearch;
import org.ala.rest.SearchResultItem;
import org.apache.commons.io.IOUtils;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;
import au.org.emii.portal.wms.GenericServiceAndBaseLayerSupport;


/**
 *
 * @author brendon
 */
public class GazetteerSearchController extends UtilityComposer {

    private Session sess = (Session) Sessions.getCurrent();
    private String sSearchTerm;
    private String sFeatureType;
    private Listbox gazetteerResults;
    private HttpConnection httpConnection = null;
    private Window gazetteerSearchResults;
    private GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport;


    public HttpConnection getHttpConnection() {
        return httpConnection;
    }
    //TODO get this from the config
    private String gazServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";
    private String gazSearchURL = "/geoserver/rest/gazetteer-search/result.json?q=";
    private JSONArray arr = null;
    private GazetteerSearch gsr = new GazetteerSearch();

    @Override
    public void afterCompose() {
        super.afterCompose();
        sSearchTerm = (String) sess.getAttribute("searchGazetteerTerm");
        searchGazetteer(getURI());
        renderGazetteerResults();
    }

    public void searchGazetteer(String p) {
        InputStream in = null;
        String json = null;
        URLConnection connection = null;

        try {

            connection = httpConnection.configureURLConnection(p);
            in = connection.getInputStream();
            json = IOUtils.toString(in);
            parseGazetteerSearchResult(json);



        } catch (IOException iox) {
            logger.debug(iox.toString());
        }

    }

    private String getURI() {
        String uri;


        // add the search term to the uri and append the required json to it
        uri = gazServer + gazSearchURL + forURL(sSearchTerm);

        return uri;
    }

    /**
     * string checking code, for spaces and special characters
     * @param aURLFragment
     * @return String
     */
    public static String forURL(String aURLFragment) {
        String result = null;
        try {
            result = URLEncoder.encode(aURLFragment, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    public void renderGazetteerResults() {
        String URL = null;

        for (SearchResultItem sri : gsr.getResults()) {
            Listitem li = new Listitem();
            Listcell lc = new Listcell();


            String sId = String.valueOf(sri.getSerial());

            // Add hyperlink to ala scientificName info page
            Toolbarbutton tbFull = new Toolbarbutton();
            tbFull.setLabel(sri.getName());
            tbFull.setSclass("z-label");
            tbFull.setHref(URL + String.valueOf(sri.getSerial()));
            tbFull.setTarget("_blank");

            lc.appendChild(tbFull);
            li.appendChild(lc);

        
            lc = new Listcell();
            Label lb = new Label();
            lb.setValue(sri.getFeatureType());
            lc.appendChild(lb);
            li.appendChild(lc);

	    //Add State
	    lc = new Listcell();
	    lb = new Label();
	    lb.setValue(sri.getState());
	    lc.appendChild(lb);
            li.appendChild(lc);


            //Add to  map button
            lc = new Listcell();
            Button btn = new Button();
            btn.setLabel("Add to map");
            btn.addEventListener("onClick", new myOnClickEventListener());
            btn.setId(sId);
            Label ln = new Label();
            ln.setValue(sri.getName());
            ln.setVisible(false);
            ln.setId("ln" + sId);
            lc.appendChild(btn);
            lc.appendChild(ln);
            li.appendChild(lc);

            // add the row to the listbox
            gazetteerResults.appendChild(li);


        }

        gazetteerResults.setVisible(true);
    }

    public void parseGazetteerSearchResult(String json) {

        json = json.replace("org.ala.rest.", "");
        JSONObject jo = JSONObject.fromObject(json);
        Map<String, Object> mapResponse = new HashMap<String, Object>();
        toJavaMap(jo, mapResponse);
    }

    private void addResult(JSONObject jk) {

        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.setRootClass(SearchResultItem.class);

        SearchResultItem sri = (SearchResultItem) JSONSerializer.toJava(jk, jsonConfig);

        gsr.getResults().add(sri);
        logger.debug(sri.getName());

    }

    private void toJavaMap(JSONObject o, Map<String, Object> b) {

        Iterator ji = o.keys();
        while (ji.hasNext()) {
            String key = (String) ji.next();
            Object val = o.get(key);
            if (val.getClass() == JSONObject.class) {
                Map<String, Object> sub = new HashMap<String, Object>();
                toJavaMap((JSONObject) val, sub);
                b.put(key, sub);
            } else if (val.getClass() == JSONArray.class) {
                List<Object> l = new ArrayList<Object>();
                arr = (JSONArray) val;
                logger.debug(key);

                for (int a = 0; a < arr.size(); a++) {
                    Map<String, Object> sub = new HashMap<String, Object>();
                    Object element = arr.get(a);
                    addResult((JSONObject) element);
                    if (element instanceof JSONObject) {
                        toJavaMap((JSONObject) element, sub);
                        l.add(sub);
                    } else {
                        l.add(element);
                    }
                }
                b.put(key, l);
            } else {
                b.put(key, val);
            }
        }


    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = gazetteerSearchResults.getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    public final class myOnClickEventListener implements EventListener {

        @Override
        public void onEvent(Event event) throws Exception {
            String label = null;
            String uri = null;
            String filter = null;
            String entity = null;
            MapLayer mapLayer = null;

            //get the entity value from the button id
            entity = event.getTarget().getId();
            //get the scientific name from the hidden label
            Label ln = (Label) event.getTarget().getFellow("ln" + entity);
            label = ln.getValue();

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //contruct the filter
            filter = "serial eq " + entity;

             //TODO these paramaters need to read from the config
            String layerName = "ALA:GeoRegionFeatures";
            String sld = "PointAndPoly";
            uri = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com/geoserver/wms?service=WMS";
            String format = "image/png";

            mapLayer = genericServiceAndBaseLayerSupport.createMapLayer("Gazetteer features " + label,label, "1.1.1", uri, layerName, format, sld, filter);
            mc.addUserDefinedLayerToMenu(mapLayer, true);
        }
    }
}


