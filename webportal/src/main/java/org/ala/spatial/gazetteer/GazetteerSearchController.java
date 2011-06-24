/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.gazetteer;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.net.HttpConnection;

import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;

/**
 *
 * @author brendon
 * @author angus
 */
public class GazetteerSearchController extends UtilityComposer {

    private static final String GAZ_URL = "geoserver_url";

    private Session sess = (Session) Sessions.getCurrent();
    private String sSearchTerm;
    private HttpConnection httpConnection = null;
    private Window gazetteerSearchResults;
    private SettingsSupplementary settingsSupplementary = null;

    public HttpConnection getHttpConnection() {
        return httpConnection;
    }

    private String gazServer = null; 
    private String gazSearchURL = "/geoserver/rest/gazetteer/result.xml?q=";

    @Override
    public void afterCompose() {
        super.afterCompose();
        if (settingsSupplementary != null) {
            gazServer = settingsSupplementary.getValue(GAZ_URL);
        }
        sSearchTerm = (String) sess.getAttribute("searchGazetteerTerm");
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
            String entity = null;

            //get the entity value from the button id
            entity = event.getTarget().getId();
            Label ln = (Label) event.getTarget().getFellow("ln" + entity);
            label = ln.getValue();

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            logger.debug(gazServer + label);
       
            //add feature to the map as a new layer
            mc.addGeoJSON(entity, gazServer + label);
            
        }
    }
}

