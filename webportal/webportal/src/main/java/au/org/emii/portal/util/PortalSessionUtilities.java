/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.util;

import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.settings.Settings;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author geoff
 */
public class PortalSessionUtilities {

    private ListUtilities listUtilities = null;
    private LanguagePack languagePack = null;
    private SettingsSupplementary settingsSupplementary = null;
    private Settings settings = null;
    /**
     * Enumeration IDs as used in the config file xml schema - to allow conversion
     * between strings used in config file and ints used at runtime
     */
    private final static String LAYER_TAB = "LAYER";
    private final static String LINK_TAB = "LINK";
    private final static String SEARCH_TAB = "SEARCH";
    private final static String AREA_TAB = "AREA";
    private final static String MAP_TAB = "MAP";
    private final static String START_TAB = "START";
    private final static String LAYER_FACILITY_TAB = "FACILITY";
    private final static String LAYER_REGION_TAB = "REGION";
    private final static String LAYER_REALTIME_TAB = "REALTIME";
    private final static String LAYER_USER_TAB = "USER";
    private Logger logger = Logger.getLogger(getClass());

    public MapLayer getUserDefinedById(PortalSession portalSession, String id) {
        return listUtilities.findInList(id, portalSession.getUserDefinedLayers());
    }

    /**
     * return the current bounding box - either the default bounding box
     * or the regional bounding box if a region has been selected
     * @return
     */
    public BoundingBox getCurrentBoundingBox(PortalSession portalSession) {
        return portalSession.getDefaultBoundingBox();
    }

    public ListUtilities getListUtilities() {
        return listUtilities;
    }

    @Required
    public void setListUtilities(ListUtilities listUtilities) {
        this.listUtilities = listUtilities;
    }

    public SettingsSupplementary getSettingsSupplementary() {
        return settingsSupplementary;
    }

    @Required
    public void setSettingsSupplementary(SettingsSupplementary settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
    }

    public String dump(PortalSession portalSession) {
        StringBuffer dump = new StringBuffer();

        if (portalSession == null) {
            dump.append("*** Null portal session (usually indicates error loading) ***");
        } else {            
            dump.append("\nMAPLAYERS (from DataSource declaration");
            if (portalSession.getMapLayers() != null) {
                for (MapLayer mapLayer : portalSession.getMapLayers()) {
                    dump.append(mapLayer.dump("") + "\n");
                }
            }
        }
        return dump.toString();
    }

    public MapLayer getMapLayerByIdAndLayer(PortalSession portalSession, String id, String layer) {

        // get the MapLayer instance (if any) bound to the id
        MapLayer target = getMapLayerById(portalSession, id);
        if ((target != null) && (layer != null)) {
            // if there was a MapLayer instance matching the id then
            // try to get the corresponding layer
            target = target.findByLayer(layer);
        }

        return target;
    }

    /**
     * Return the MapLayer for a corresponding ID or null if there is no
     * match.
     *
     * Operates on the mapLayers list ONLY (e.g, NOT the user defined layers)
     * @param id
     * @return
     */
    public MapLayer getMapLayerById(PortalSession portalSession, String id) {
        MapLayer found = null;
        int i = 0;
        while (found == null && i < portalSession.getMapLayers().size()) {
            MapLayer search = portalSession.getMapLayers().get(i);
            if (search.getId().equals(id)) {
                found = search;
            } else if (search.hasChildren()) {
                // don't forget to inspect children when searching for maplayer
                // IDs - this is why you can't just use findInList()
                found = search.findById(id);
            }
            i++;
        }
        return found;
    }

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    @Required
    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public int convertTab(String str) {
        int tab;
        if (str.equals(LAYER_TAB)) {
            tab = PortalSession.LAYER_TAB;
        } else if (str.equals(LINK_TAB)) {
            tab = PortalSession.START_TAB;
        } else if (str.equals(SEARCH_TAB)) {
            tab = PortalSession.SEARCH_TAB;
        } else if (str.equals(AREA_TAB)) {
            tab = PortalSession.AREA_TAB;
        } else if (str.equals(MAP_TAB)) {
            tab = PortalSession.MAP_TAB;
        } else if (str.equals(START_TAB)) {
            tab = PortalSession.START_TAB;
        } else {
            tab = PortalSession.UNKNOWN;
        }
        return tab;
    }

    public int convertLayerView(String str) {
        int tab;
        if (str.equals(LAYER_FACILITY_TAB)) {
            tab = PortalSession.LAYER_FACILITY_TAB;
        } else if (str.equals(LAYER_REALTIME_TAB)) {
            tab = PortalSession.LAYER_REALTIME_TAB;
        } else if (str.equals(LAYER_REGION_TAB)) {
            tab = PortalSession.LAYER_REGION_TAB;
        } else if (str.equals(LAYER_USER_TAB)) {
            tab = PortalSession.LAYER_USER_TAB;
        } else {
            tab = PortalSession.UNKNOWN;
        }
        return tab;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /**
     * Get a map layer by URI and Layer
     * @param portalSession
     * @param id
     * @return first matching maplayer instance found by dft.  Null if there are
     * no matches
     */
    public MapLayer getMapLayerByUriAndLayer(PortalSession portalSession, String uri, String layer) {
        MapLayer found = null;
        int i = 0;
        while (found == null && i < portalSession.getMapLayers().size()) {
            MapLayer search = portalSession.getMapLayers().get(i);
            found = search.findByUriAndLayer(uri, layer);
            i++;
        }
        return found;
    }
    
}
