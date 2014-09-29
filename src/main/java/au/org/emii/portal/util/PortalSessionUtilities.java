/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.util;

import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.value.BoundingBox;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author geoff
 */
public class PortalSessionUtilities {
    /**
     * Enumeration IDs as used in the config file xml schema - to allow
     * conversion between strings used in config file and ints used at runtime
     */
    private static final String LAYER_TAB = "LAYER";
    private static final String LINK_TAB = "LINK";
    private static final String SEARCH_TAB = "SEARCH";
    private static final String AREA_TAB = "AREA";
    private static final String MAP_TAB = "MAP";
    private static final String START_TAB = "START";
    private static final String LAYER_FACILITY_TAB = "FACILITY";
    private static final String LAYER_REGION_TAB = "REGION";
    private static final String LAYER_REALTIME_TAB = "REALTIME";
    private static final String LAYER_USER_TAB = "USER";
    private LanguagePack languagePack = null;
    private Settings settings = null;

    public MapLayer getUserDefinedById(PortalSession portalSession, String id) {
        for (MapLayer map : portalSession.getUserDefinedLayers()) {
            if (map != null && map.getId() != null && map.getId().equals(id)) {
                return map;
            }
        }
        return null;
    }

    /**
     * return the current bounding box - either the default bounding box or the
     * regional bounding box if a region has been selected
     *
     * @return
     */
    public BoundingBox getCurrentBoundingBox(PortalSession portalSession) {
        return portalSession.getDefaultBoundingBox();
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
}
