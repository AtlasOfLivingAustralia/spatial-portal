package au.org.emii.portal.session;


import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.menu.MapLayer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the state of the portal.
 * 
 * On loading the web application a default PortalSession is built, when a 
 * user accesses the portal, the default instance is COPIED into their
 * HTTP session where it can be manipulated through the ZK GUI without 
 * affecting the other sessions 
 * @author geoff
 *
 */
public class PortalSession implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    public static final int UNKNOWN = -1;

    public static final int LAYER_FACILITY_TAB = 0;
    public static final int LAYER_REGION_TAB = 1;
    public static final int LAYER_USER_TAB = 2;
    public static final int LAYER_REALTIME_TAB = 3;


    public final static int LAYER_TAB = 0;
    public final static int SEARCH_TAB = 1;
    public final static int LINK_TAB = 2;
    public final static int START_TAB = 3;
    public final static int AREA_TAB = 4;
    public final static int MAP_TAB = 5;

    
    /**
     * Nasty zk hack - have to get and hold a reference to the
     * error message iframe's media content otherwise if it's
     * been dereferenced and the browser requests it you get
     * a SEVERE error (harmless but very annoying)
     */
    private StringMedia rawErrorMessageMedia = null;

    /* Datasources - Discovery and Service both resolve to MapLayer instances,
     * static links are handled separately
     */
    private List<MapLayer> mapLayers = null;
    private List<MapLayer> activeLayers = null;
    private List<MapLayer> userDefinedLayers = null;

    /**
     * The current view we are displaying to the user
     */
    private int currentLayerTab = LAYER_FACILITY_TAB;
    
    /**
     * The current view we need for displaying the menu
     * EG, we may be displaying the regions panel but
     * we want to display an invisible menu on the
     * facilities panel because the user hasn't selected
     * a radio button yet
     */
    private int tabForCurrentMenu = LAYER_FACILITY_TAB;

    private String onIframeMapFullyLoaded =
            "alert('onIframeMapFullyLoaded function has not been replaced"
            + " - possible race conditon'); ";
    private BoundingBox defaultBoundingbox = null;

    /**
     * Flag to indicate whether the map has been loaded successfully
     * if false, no openlayers javascript will be executed
     */
    private boolean mapLoaded = false;
    /**
     * Default navigation tab
     */
    private int currentNavigationTab = LAYER_TAB;
    /**
     * Are we hiding the left menu?
     */
    private boolean maximised = false;
    /**
     * Baselayer name
     */
    private String baseLayer = "normal";

    public PortalSession() {
        reset();
    }

    public int getCurrentNavigationTab() {
        return currentNavigationTab;
    }

    public void setCurrentNavigationTab(int currentNavigationTab) {
        this.currentNavigationTab = currentNavigationTab;
    }

    public List<MapLayer> getMapLayers() {
        return mapLayers;
    }

    public void setMapLayers(List<MapLayer> mapLayers) {
        this.mapLayers = mapLayers;
    }

    public void addMapLayer(MapLayer mapLayer) {
        mapLayers.add(mapLayer);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        // step 0: setup
        PortalSession portalSession = (PortalSession) super.clone();
        return portalSession;
    }

    public List<MapLayer> getActiveLayers() {
        return activeLayers;
    }

    public void setActiveLayers(List<MapLayer> activeLayers) {
        this.activeLayers = activeLayers;
    }

    public void setUserDefinedLayers(List<MapLayer> userDefined) {
        this.userDefinedLayers = userDefined;
    }

    public List<MapLayer> getUserDefinedLayers() {
        return userDefinedLayers;
    }

    public int getCurrentLayerTab() {
        return currentLayerTab;
    }

    public void setLayerTab(int layerTab) {
        this.currentLayerTab = layerTab;
    }

    /**
     * Check if the user defined view is displayable
     * @return
     */
    public boolean isUserDefinedViewDisplayable() {
        return true; // set always on as saved maps are in there now
        //return (userDefinedLayers.size() > 0);
    }

    public String getOnIframeMapFullyLoaded() {
        return onIframeMapFullyLoaded;
    }

    public void setOnIframeMapFullyLoaded(String onIframeMapFullyLoaded) {
        this.onIframeMapFullyLoaded = onIframeMapFullyLoaded;
    }

    public int getTabForCurrentMenu() {
        return tabForCurrentMenu;
    }

    public void setTabForCurrentMenu(int viewForCurrentMenu) {
        this.tabForCurrentMenu = viewForCurrentMenu;
    }

    public BoundingBox getDefaultBoundingBox() {
        return defaultBoundingbox;
    }


    public void setDefaultBoundingbox(BoundingBox defaultBoundingbox) {
        this.defaultBoundingbox = defaultBoundingbox;
    }

    public boolean isMapLoaded() {
        return mapLoaded;
    }

    public void setMapLoaded(boolean mapLoaded) {
        this.mapLoaded = mapLoaded;
    }

    public StringMedia getRawErrorMessageMedia() {
        return rawErrorMessageMedia;
    }

    public void setRawErrorMessageMedia(StringMedia rawErrorMessageMedia) {
        this.rawErrorMessageMedia = rawErrorMessageMedia;
    }

    public boolean isMaximised() {
        return maximised;
    }

    public void setMaximised(boolean maximised) {
        this.maximised = maximised;
    }

    public void reset() {        
        mapLayers = new ArrayList<MapLayer>();
        activeLayers = new ArrayList<MapLayer>();
        userDefinedLayers = new ArrayList<MapLayer>();
    }

    public String getBaseLayer() {
        return baseLayer;
    }

    public void setBaseLayer(String baseLayer) {
        this.baseLayer = baseLayer;
    }
}
