package au.org.emii.portal;

import au.org.emii.portal.config.Config;
import au.org.emii.portal.user.PortalUser;
import au.org.emii.portal.user.PortalUserImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;


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
		"alert('onIframeMapFullyLoaded function has not been replaced" +
		" - possible race conditon'); ";
	
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
	 * Store instance of static class so it can be made available in EL
	 */
	private transient LayerUtilities layerUtilities = null;

	/**
	 * Store instance of static class so it can be made available in EL
	 */
	private transient Config config = null;
	
	public int getCurrentNavigationTab() {
		return currentNavigationTab;
	}

	public void setCurrentNavigationTab(int currentNavigationTab) {
		this.currentNavigationTab = currentNavigationTab;
	}

	private void initUserDefinedMenu() {
		if (userDefinedMenu == null) {
			userDefinedMenu = new MenuGroup();
			userDefinedMenu.setId(Config.getValue("user_defined_layer_group_id"));
			
			/* these are from the lang pack and describe the root menu
			 * item which never gets rendered AFAIK
			 */
			userDefinedMenu.setName(Config.getLang("user_defined_layer_group_label"));
			userDefinedMenu.setDescription(Config.getLang("user_defined_layer_group_label"));
		}
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
	
	/*
	public List<String> getBaseLayerIds() {
		List<String> baseLayerIds = new ArrayList<String>();
		for (MapLayer baseLayer : baseLayers) {
			baseLayerIds.add(baseLayer.getId());
		}
		return baseLayerIds;
	}
	*/
	
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
	
	public MenuItem addUserDefinedMapLayer(MapLayer mapLayer) {
		initUserDefinedMenu();
		
		// add to the list (like a datasource)
		userDefinedLayers.add(mapLayer);
		
		// create and add a holder for it and insert into the menu 
		MenuItem item = new MenuItem(mapLayer);
		userDefinedMenu.addChild(item);
		return item;
	}
	
	public TreeChildIdentifier removeUserDefinedMapLayer(MenuItem itemToRemove) {
		TreeMenuItem parent = null;
		TreeChildIdentifier id = null;
		if ((itemToRemove != null) && itemToRemove.isValueMapLayerInstance()) {
			initUserDefinedMenu();
			userDefinedLayers.remove(itemToRemove.getValueAsMapLayer());
			
			parent = findInTree(userDefinedMenu, (Object) itemToRemove);
			// nuke the layer from the list
			
			if (parent != null) {
				id = new TreeChildIdentifier(parent, parent.getChildren().indexOf(itemToRemove));
				parent.getChildren().remove(itemToRemove);
			}
		}
		return id;
	}
	
	public MapLayer getMapLayerByIdAndLayer(String id, String layer) {
		
		// get the MapLayer instance (if any) bound to the id
		MapLayer target = getMapLayerById(id);
		if ((target != null) && (layer != null)) {
			// if there was a MapLayer instance matching the id then
			// try to get the corresponding layer
			target = target.findByLayer(layer);
		}
			
		return target; 
	}
	
	/**
	 * Simple searching in a list.  Does not support recursive inspection
	 * @param <T>
	 * @param id
	 * @param search
	 * @return
	 */
	private <T extends AbstractIdentifier> T findInList(String id, List<? extends T> search) {
		T found = null;
		T inspect = null;
		String inspectId = null;
		int i = 0;
		if ((search != null) && (id != null)) {
			while ((found == null) && (i < search.size())) {
				inspect = search.get(i);
				if (inspect != null) {
					inspectId = inspect.getId();
					if (inspectId != null && inspectId.equals(id)) {
						found = inspect;
					}
				}
				i++;
			}
		}
		
		return found;	
		
	}
	
	/**
	 * Recursive find in tree.  Returns PARENT of found item
	 * @param list
	 * @param targetValue
	 * @return
	 */
	private <T extends TreeMenuItem> TreeMenuItem findInTree(T root, Object targetValue) {
		TreeMenuItem parent = null;
		if (root != null) {
			int childCount = root.getChildCount();
			int i = 0;
			// compare THIS tree item first...  
			if (root.getConcreteType() == TreeMenuItem.CONCRETE_TYPE_MENUITEM) {
				MenuItem rootMenuItem = (MenuItem) root;
				
				// comparison is against whole holder+value combo...
				//if (rootMenuItem.getValue() == targetValue) {
				if (rootMenuItem == targetValue) {
					// found the item
					parent = rootMenuItem.getParent();
				}
			}
			else {
				Logger logger = Logger.getLogger(this.getClass());
				logger.debug(
					"item is not a menuitem - couldn't do comparison: Type is " + root.getConcreteType()
				);
			}
		
			if (! root.isLeaf()) {
				while ((parent == null) && (i < childCount)) {
					parent = findInTree((TreeMenuItem)root.getChild(i), targetValue);
					i++;
				}
			}
		}
		return parent;
	}
	
	
	public MapLayer getUserDefinedById(String id) {
		return findInList(id, userDefinedLayers);
	}
	
	/**
	 * Return the MapLayer for a corresponding ID or null if there is no
	 * match.
	 * 
	 * Operates on the mapLayers list ONLY (e.g, NOT the user defined layers)
	 * @param id
	 * @return
	 */
	public MapLayer getMapLayerById(String id) {
		MapLayer found = null;
		int i = 0;
		while (found == null && i < mapLayers.size()) {
			MapLayer search = mapLayers.get(i); 
			if (search.getId().equals(id)) {
				found = search;
			}
			else if (search.hasChildren()) {
				// don't forget to inspect children when searching for maplayer
				// IDs - this is why you can't just use findInList()
				found = search.findById(id);
			}
			i++;
		}
		return found;
	}
	
	public MapLayer getBaseLayerById(String id) {
		return findInList(id, baseLayers);
	}
	
	public Link getLinkById(String id) {
		return findInList(id, links);
	}
	
	public SearchCatalogue getSearchCatalogueById(String id) {
		return (findInList(id, searchCatalogues));
	}
	
	public String dump() {
		StringBuffer dump = new StringBuffer();
		
		dump.append("FACILITIES:\n");
		for (Facility facility : facilities) {
			dump.append(facility.dump());
		}
		
		dump.append("REGIONS:\n");
		for (Region region : regions) {
			dump.append(region.dump() + "\n");
		}

                dump.append("\nMAPLAYERS (from DataSource declaration");
                for (MapLayer mapLayer : mapLayers) {
			dump.append(mapLayer.dump("") + "\n");
		}
		
                dump.append("\nBASELAYERS (from DataSource declaration):\n");
		for (MapLayer baseLayer: baseLayers) {
			dump.append(baseLayer.dump() + "\n");
		}
		
                dump.append("\nLINKS (from DataSource declaration):\n");
		for (Link link : links) {
			dump.append(link.dump() + "\n");
		}
		
		return dump.toString();
	}
	
        @Override
	public Object clone() throws CloneNotSupportedException {
		
		// OK its Clone time!  
		
		// step 0: setup
		PortalSession portalSession = (PortalSession) super.clone();
		/* super.clone will leave references to existing objects 
		 * in place, e.g.  portalSession.mapLayers == mapLayers is
		 * currently true - to fix this, we will re-init all these
		 * fields now
		 *
		 * although we don't mind sharing these between users, they
		 * have to be defined 'later' when we call clone() because 
		 * the config file has not yet been loaded if we try earlier
		 */
		portalSession.layerUtilities = new LayerUtilities();
		portalSession.config = new Config();
		
		portalSession.facilities = new ArrayList<Facility>();
		portalSession.regions = new ArrayList<Region>();
		portalSession.realtime = new ArrayList<Facility>();
		portalSession.mapLayers = new ArrayList<MapLayer>();
		portalSession.baseLayers = new ArrayList<MapLayer>();
		portalSession.links = new ArrayList<Link>();
		portalSession.activeLayers = new ArrayList<MapLayer>();
		portalSession.userDefinedLayers = new ArrayList<MapLayer>();
		portalSession.userDefinedMenu = null;
		portalSession.staticMenuLinks = new ArrayList<Link>();
		portalSession.searchCatalogues = new ArrayList<SearchCatalogue>();
                portalSession.portalUser = new PortalUserImpl();
		portalSession.selectedSearchCatalogue = null;
		

	
		// step1: data sources and search catalogues
		// maplayers
		if (mapLayers != null) {
			for (MapLayer mapLayer : mapLayers) {
				portalSession.addMapLayer((MapLayer) mapLayer.clone());
			}
		}
		
		// baselayers
		if (baseLayers != null) {
			for (MapLayer baseLayer : baseLayers) {
				portalSession.addBaseLayer((MapLayer) baseLayer.clone());
			}
		}
		
		// links
		if (links != null) {
			for (Link link : links) {
				portalSession.addLink((Link)link.clone());
			}
		}
		
		// search catalogues
		if (searchCatalogues != null) {
			for (SearchCatalogue searchCatalogue : searchCatalogues) {
				portalSession.addSearchCatalogue(
						(SearchCatalogue) searchCatalogue.clone()
				);
			}
		}
		
		// put default search catalogue back
		if (selectedSearchCatalogue != null) {
			portalSession.setSelectedSearchCatalogue(
					portalSession.getSearchCatalogueById(
							selectedSearchCatalogue.getId()
					)
			);
		}
		
		// now we can put the baselayer back
		if (currentBaseLayer != null) {
			portalSession.currentBaseLayer = portalSession.getBaseLayerById(currentBaseLayer.getId());
		}
		
		// step 2: copy regions/facilities and settings
		
		// default map bounding box
		if (defaultBoundingbox != null) {
			portalSession.defaultBoundingbox = (BoundingBox) portalSession.defaultBoundingbox.clone();
		}
		
		// facilities
		if (facilities != null) {
			for (Facility facility : facilities) {
				Facility clone = (Facility) facility.clone();
				portalSession.addFacility(clone);
	
				// put back the .value field
				cloneValueField(
						facility.getMenu(), 
						clone.getMenu(), 
						portalSession
				);
	
			}
		}
		
		
		// regions
		if (regions != null) {
			for (Region region : regions) {
				Region clone = (Region) region.clone();
				portalSession.addRegion(clone);
				
				// put back the .value field
				cloneValueField(
						region.getMenu(), 
						clone.getMenu(), 
						portalSession
				);
			}
		}
		
		// realtime
		if (realtime != null) {
			for (Facility rt : realtime) {
				Facility clone = (Facility) rt.clone();
				portalSession.addRealtime(clone);
	
				// put back the .value field
				cloneValueField(
						rt.getMenu(), 
						clone.getMenu(), 
						portalSession
				);
	
			}
		}
		
		// Step 3: put back the value field for regions/facilities
		if (staticMenuLinks != null) {
			for (Link link : staticMenuLinks) {
				portalSession.addStaticMenuLink((Link) link.clone()); 
			}
		}
		
		// step 4: clone active layers
		if (activeLayers != null) {
			for (MapLayer mapLayer : activeLayers) {
				portalSession.activeLayers.add(
						portalSession.getMapLayerById(
								mapLayer.getId()
						)
				);
			}
		}
		
		/* step 5: skip things
		 *
		 * o	userDefined
		 * o	UserDefinedMenu
		 * All get skipped because for new sessions they should
		 * all be empty lists/objects
		 */	

		return portalSession;
	}
	
	private void cloneValueField(TreeMenuItem original, TreeMenuItem clone, PortalSession cloneSession) {
		// MenuItem nodes are (at the moment) terminal, so only descend non
		// MenuItems
                if (original != null) {
                    if (original.getConcreteType() == TreeMenuItem.CONCRETE_TYPE_MENUITEM) {
                            // both original and clone must be MenuItem instances
                            MenuItem originalMenuItem = (MenuItem) original;
                            MenuItem cloneMenuItem = (MenuItem) clone;

                            TreeMenuValue value = null;
                            if (originalMenuItem.isValueLinkInstance()) {
                                    value =
                                            cloneSession.getLinkById(
                                                            originalMenuItem.getValue().getId()
                                            );

                            }
                            else if (originalMenuItem.isValueMapLayerInstance()) {
                                    value =
                                            cloneSession.getMapLayerById(
                                                            originalMenuItem.getValue().getId()
                                            );
                            }

                            if (value != null) {
                                    cloneMenuItem.setValue(value);
                            }

                    }

                    for (TreeMenuItem originalChild : original.getChildren()) {
                            cloneValueField(
                                            originalChild,
                                            (TreeMenuItem)clone.getChild(clone, original.getChildren().indexOf(originalChild)),
                                            cloneSession
                            );
                    }
                }
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
	
	public Region getRegionById(String id) {
		return findInList(id, regions);	
	}
	
	public Facility getFacilityById(String id) {
		 return findInList(id, facilities);
	}
	
	public Facility getRealtimeById(String id) {
		 return findInList(id, realtime);
	}
		
	public MenuGroup getMenu(int view, String id) {
		Logger logger = Logger.getLogger(this.getClass());
		MenuGroup menu = null;
		
		if (view == PortalSession.VIEW_USER) {
			menu = getMenuForUserDefined();	
		}
		else {
			Facility selectedView = null;
			boolean proceed = false;
		
			switch (view) {
			case PortalSession.VIEW_FACILITY:
				selectedView = getFacilityById(id);
				proceed = true;
				break;
			case PortalSession.VIEW_REGION:
				selectedView = getRegionById(id); 
				proceed = true;
				break;
			case PortalSession.VIEW_REALTIME:
				selectedView = getRealtimeById(id);
				proceed = true;
				break;
			default:
				logger.error("menu for was requested for unsupported view: " + view);
				proceed = false;
			}
		
			if (proceed) {
				if (selectedView == null) {
					logger.warn("no facility available for id=" + id + " view=" + view);
				}
				else {
					menu = selectedView.getMenu();		
				}
			}
		}		
		return menu;
	}

	public void setSelectedFacilityOrRegionId(String selectedFacilityOrRegionId) {
		this.selectedFacilityOrRegionId = selectedFacilityOrRegionId;
	}
	public String getSelectedFacilityOrRegionId() {
		return selectedFacilityOrRegionId;
	}
	
	public Facility getSelectedFacilityOrRegion(String id) {
		Facility selected;
		switch (currentView) {
		case PortalSession.VIEW_FACILITY:
			selected = getFacilityById(id);
			break;
		case PortalSession.VIEW_REGION:
			selected = getRegionById(id);
			break;
		default:
			selected = null;
			break;
		}
		return selected;
	}

	public MenuGroup getMenuForUserDefined() {
		initUserDefinedMenu();
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
	
	public List<String> check() {
		List<String> faults = new ArrayList<String>();
		
		// at least one base layer
		if (baseLayers.size() < 1) {
			faults.add("No valid baselayers available");
		}
		
		// default baselayer selected
		if (currentBaseLayer == null) {
			faults.add("Requested default base layer is not available.");
		}
		
		// at least one facility or region defined
		if (regions.size() + facilities.size() < 1) {
			faults.add("At least one region or facility must be defined");
		}
		
		// selectected facility/region set and valid
		if (getSelectedFacilityOrRegion(selectedFacilityOrRegionId) == null) {
			faults.add("Default facility/region selection is not valid");
		}
			
		return faults;
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
	
	/**
	 * return the current bounding box - either the default bounding box
	 * or the regional bounding box if a region has been selected
	 * @return
	 */
	public BoundingBox getCurrentBoundingBox() {
		Logger logger = Logger.getLogger(this.getClass());
		BoundingBox bbox;
		String id = getSelectedFacilityOrRegionId();
		if ((getViewForCurrentMenu() == VIEW_REGION) && (id != null)) {
			Region region = getRegionById(id);  
			if (region == null) {
				logger.warn(
						"VIEW_REGION is selected but no bounding box is available for region id=" 
						+ id + " will continue anyway using the default bounding box"
				);
				bbox = getDefaultBoundingBox();	
			}
			else {
				bbox = region.getBoundingBox();
			}
			
		}
		else {
			bbox = getDefaultBoundingBox();
		}
		
		return bbox;
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
		return (selectedSearchCatalogue != null) ?
			selectedSearchCatalogue.getSearchTerms() : 
			null;
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

	public LayerUtilities getLayerUtilities() {
		if (layerUtilities == null) {
			layerUtilities = new LayerUtilities();
		}
		return layerUtilities;
	}

	public void setLayerUtilities(LayerUtilities layerUtilities) {
		this.layerUtilities = layerUtilities;
	}

	public Config getConfig() {
		if (config == null) {
			config = new Config();
		}
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
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


}
