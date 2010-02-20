package au.org.emii.portal;


import au.org.emii.portal.menu.MenuItem;
import au.org.emii.portal.menu.MenuGroup;
import au.org.emii.portal.user.PortalUser;
import au.org.emii.portal.user.PortalUserImpl;
import au.org.emii.portal.userdata.UserDataManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;

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
    public static final int VIEW_FACILITY = 0;
    public static final int VIEW_REGION = 1;
    public static final int VIEW_USER = 2;
    public static final int VIEW_REALTIME = 3;
    public final static int LAYER_TAB = 0;
    public final static int SEARCH_TAB = 1;
    public final static int LINK_TAB = 2;

    /*
     * User info for logged in users - just gets set to a new instance
     * when we do clone
     */
    private PortalUser portalUser = new PortalUserImpl();
    /**
     * Nasty zk hack - have to get and hold a reference to the
     * error message iframe's media content otherwise if it's
     * been dereferenced and the browser requests it you get
     * a SEVERE error (harmless but very annoying)
     */
    private StringMedia rawErrorMessageMedia = null;
    private List<Facility> facilities = new ArrayList<Facility>();
    private List<Region> regions = new ArrayList<Region>();
    private List<Facility> realtime = new ArrayList<Facility>();
    /* Datasources - Discovery and Service both resolve to MapLayer instances,
     * static links are handled separately
     */
    private List<MapLayer> mapLayers = new ArrayList<MapLayer>();
    private List<MapLayer> baseLayers = new ArrayList<MapLayer>();
    private List<Link> links = new ArrayList<Link>();
    private List<MapLayer> activeLayers = new ArrayList<MapLayer>();
    private List<MapLayer> userDefinedLayers = new ArrayList<MapLayer>();
    private MenuGroup userDefinedMenu = null;
    private boolean displayingUserDefinedMenuTree = false;
    private List<Link> staticMenuLinks = new ArrayList<Link>();
    /**
     * The current view we are displaying to the user
     */
    private int currentView = VIEW_FACILITY;
    private UserDataManager userDataManager = null;
    /**
     * The current view we need for displaying the menu
     * EG, we may be displaying the regions panel but
     * we want to display an invisible menu on the
     * facilities panel because the user hasn't selected
     * a radio button yet
     */
    private int viewForCurrentMenu = VIEW_FACILITY;

    private MapLayer currentBaseLayer = null;
    /**
     * The id of the currently selected facility or region :)
     */
    private String selectedFacilityOrRegionId = null;
    private String onIframeMapFullyLoaded =
            "alert('onIframeMapFullyLoaded function has not been replaced"
            + " - possible race conditon'); ";
    private BoundingBox defaultBoundingbox = new BoundingBox();
    /**
     * All supported search catalogues
     */
    private List<SearchCatalogue> searchCatalogues = new ArrayList<SearchCatalogue>();
    /**
     * The selected search catalogue - must exist in searchCatalogues
     */
    private SearchCatalogue selectedSearchCatalogue = null;
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
     * Disable the search system?
     */
    private boolean searchDisabled = false;
    /**
     * Disable the login/user accounts system
     */
    private boolean loginDisabled = false;
    /**
     * Disable the realtime tab (left menu)
     */
    private boolean realtimeDisabled = false;
    /**
     * Disable the region tab (left menu)
     */
    private boolean regionDisabled = false;
    /**
     * Disable the facility tab (left menu)
     */
    private boolean facilityDisabled = false;

    /**
     * Disable depth servlet in UI (depth/altitude for map points)
     */
    private boolean depthServletDisabled = false;

    /**
     * Disable layers menu - left hand side
     */
    private boolean layersDisabled = false;

    /**
     * Disable user defined menu - left hand side
     */
    private boolean userDefinedDisabled = false;

    /**
     * Disable links menu - left hand side
     */
    private boolean linksDisabled = false;

    /**
     * @return the userDataManager
     */
    public UserDataManager getUserDataManager() {
        return userDataManager;
    }

    /**
     * @param userDataManager the userDataManager to set
     */
    public void setUserDataManager(UserDataManager userDataManager) {
        this.userDataManager = userDataManager;
    }


    public int getCurrentNavigationTab() {
        return currentNavigationTab;
    }

    public void setCurrentNavigationTab(int currentNavigationTab) {
        this.currentNavigationTab = currentNavigationTab;
    }

    public void setFacilities(List<Facility> facilities) {
        this.facilities = facilities;
    }

    public List<Facility> getFacilities() {
        return facilities;
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public List<MapLayer> getMapLayers() {
        return mapLayers;
    }

    public void setMapLayers(List<MapLayer> mapLayers) {
        this.mapLayers = mapLayers;
    }

    public List<MapLayer> getBaseLayers() {
        return baseLayers;
    }

    public void setBaseLayers(List<MapLayer> baseLayers) {
        this.baseLayers = baseLayers;
    }

    public List<Link> getLinks() {
        return this.links;
    }

    public void addMapLayer(MapLayer mapLayer) {
        mapLayers.add(mapLayer);
    }

    public void addBaseLayer(MapLayer mapLayer) {
        baseLayers.add(mapLayer);
    }

    public void addLink(Link link) {
        links.add(link);
    }

    public void addStaticMenuLink(Link link) {
        staticMenuLinks.add(link);
    }

    public void addFacility(Facility facility) {
        facilities.add(facility);
    }

    public void addRealtime(Facility facility) {
        realtime.add(facility);
    }

    public void addRegion(Region region) {
        regions.add(region);
    }

    public void addSearchCatalogue(SearchCatalogue searchCatalogue) {
        searchCatalogues.add(searchCatalogue);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        // step 0: setup
        PortalSession portalSession = (PortalSession) super.clone();
        return portalSession;
    }

    public void setUserDefinedMenu(MenuGroup userDefinedMenu) {
        this.userDefinedMenu = userDefinedMenu;
    }

    public List<MapLayer> getActiveLayers() {
        return activeLayers;
    }

    public void setActiveLayers(List<MapLayer> activeLayers) {
        this.activeLayers = activeLayers;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void setUserDefinedLayers(List<MapLayer> userDefined) {
        this.userDefinedLayers = userDefined;
    }

    public List<MapLayer> getUserDefinedLayers() {
        return userDefinedLayers;
    }

    public void setSelectedFacilityOrRegionId(String selectedFacilityOrRegionId) {
        this.selectedFacilityOrRegionId = selectedFacilityOrRegionId;
    }

    public String getSelectedFacilityOrRegionId() {
        return selectedFacilityOrRegionId;
    }

    public MenuGroup getMenuForUserDefined() {
        return userDefinedMenu;
    }

    public boolean isDisplayingUserDefinedMenuTree() {
        return displayingUserDefinedMenuTree;
    }

    public void setDisplayingUserDefinedMenuTree(
            boolean displayingUserDefinedMenuTree) {
        this.displayingUserDefinedMenuTree = displayingUserDefinedMenuTree;
    }

    public int getCurrentView() {
        return currentView;
    }

    public void setCurrentView(int currentView) {
        this.currentView = currentView;
    }

    public MapLayer getCurrentBaseLayer() {
        return currentBaseLayer;
    }

    public void setCurrentBaseLayer(MapLayer currentBaseLayer) {
        this.currentBaseLayer = currentBaseLayer;
    }

    public int getIndexOfCurrentBaseLayer() {
        return baseLayers.indexOf(currentBaseLayer);
    }

    public List<Link> getStaticMenuLinks() {
        return staticMenuLinks;
    }

    public void setStaticMenuLinks(List<Link> staticMenuLinks) {
        this.staticMenuLinks = staticMenuLinks;
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

    public int getViewForCurrentMenu() {
        return viewForCurrentMenu;
    }

    public void setViewForCurrentMenu(int viewForCurrentMenu) {
        this.viewForCurrentMenu = viewForCurrentMenu;
    }

    public BoundingBox getDefaultBoundingBox() {
        return defaultBoundingbox;
    }


    public void setDefaultBoundingbox(BoundingBox defaultBoundingbox) {
        this.defaultBoundingbox = defaultBoundingbox;
    }

    public List<SearchCatalogue> getSearchCatalogues() {
        return searchCatalogues;
    }

    public void setSearchCatalogues(List<SearchCatalogue> searchCatalogues) {
        this.searchCatalogues = searchCatalogues;
    }

    public SearchCatalogue getSelectedSearchCatalogue() {
        return selectedSearchCatalogue;
    }

    public void setSelectedSearchCatalogue(SearchCatalogue selectedSearchCatalogue) {
        this.selectedSearchCatalogue = selectedSearchCatalogue;
    }

    /**
     * Fetch the search terms applicable to the current search
     * catalogue - contains builtin npe proctection if the
     * selectedSearchCatalogue field has not been set (eg if
     * you have errors in your configuration)
     * @return
     */
    public List<String> getSelectedSearchCatalogueTerms() {
        return (selectedSearchCatalogue != null)
                ? selectedSearchCatalogue.getSearchKeywords()
                : null;
    }

    public List<Facility> getRealtime() {
        return realtime;
    }

    public void setRealtime(List<Facility> realtime) {
        this.realtime = realtime;
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


    public boolean isLoggedIn() {
        return portalUser.isLoggedIn();
    }

    public boolean isAdmin() {
        return portalUser.isAdmin();
    }

    public PortalUser getPortalUser() {
        return portalUser;
    }

    public void setPortalUser(PortalUser portalUser) {
        this.portalUser = portalUser;
    }

    public boolean isFacilityDisabled() {
        return facilityDisabled;
    }

    public void setFacilityDisabled(boolean facilityDisabled) {
        this.facilityDisabled = facilityDisabled;
    }

    public boolean isLoginDisabled() {
        return loginDisabled;
    }

    public void setLoginDisabled(boolean loginDisabled) {
        this.loginDisabled = loginDisabled;
    }

    public boolean isRealtimeDisabled() {
        return realtimeDisabled;
    }

    public void setRealtimeDisabled(boolean realtimeDisabled) {
        this.realtimeDisabled = realtimeDisabled;
    }

    public boolean isRegionDisabled() {
        return regionDisabled;
    }

    public void setRegionDisabled(boolean regionDisabled) {
        this.regionDisabled = regionDisabled;
    }

    public boolean isSearchDisabled() {
        return searchDisabled;
    }

    public void setSearchDisabled(boolean searchDisabled) {
        this.searchDisabled = searchDisabled;
    }

    public boolean isDepthServletDisabled() {
        return depthServletDisabled;
    }

    public void setDepthServletDisabled(boolean depthServletDisabled) {
        this.depthServletDisabled = depthServletDisabled;
    }

    public boolean isLayersDisabled() {
        return layersDisabled;
    }

    public void setLayersDisabled(boolean layersDisabled) {
        this.layersDisabled = layersDisabled;
    }

    public boolean isLinksDisabled() {
        return linksDisabled;
    }

    public void setLinksDisabled(boolean linksDisabled) {
        this.linksDisabled = linksDisabled;
    }

    public boolean isUserDefinedDisabled() {
        return userDefinedDisabled;
    }

    public void setUserDefinedDisabled(boolean userDefinedDisabled) {
        this.userDefinedDisabled = userDefinedDisabled;
    }

    public MenuGroup getUserDefinedMenu() {
        return userDefinedMenu;
    }
}
