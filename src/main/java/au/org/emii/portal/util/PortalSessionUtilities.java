/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.util;

import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.menu.Facility;
import au.org.emii.portal.menu.Link;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.menu.Region;
import au.org.emii.portal.menu.TreeChildIdentifier;
import au.org.emii.portal.menu.TreeMenuItem;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MenuGroup;
import au.org.emii.portal.menu.MenuItem;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.value.SearchCatalogue;
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
    private final static String LAYER_FACILITY_TAB = "FACILITY";
    private final static String LAYER_REGION_TAB = "REGION";
    private final static String LAYER_REALTIME_TAB="REALTIME";
    private final static String LAYER_USER_TAB="USER";
    
    private Logger logger = Logger.getLogger(getClass());

    public MapLayer getUserDefinedById(PortalSession portalSession, String id) {
        return listUtilities.findInList(id, portalSession.getUserDefinedLayers());
    }

    public MapLayer getBaseLayerById(PortalSession portalSession, String id) {
        return listUtilities.findInList(id, portalSession.getBaseLayers());
    }

    public Link getLinkById(PortalSession portalSession, String id) {
        return listUtilities.findInList(id, portalSession.getLinks());
    }

    /**
     * return the current bounding box - either the default bounding box
     * or the regional bounding box if a region has been selected
     * @return
     */
    public BoundingBox getCurrentBoundingBox(PortalSession portalSession) {

        BoundingBox bbox;
        String id = portalSession.getSelectedMenuId();
        if ((portalSession.getTabForCurrentMenu() == PortalSession.LAYER_REGION_TAB) && (id != null)) {
            Region region = portalSession.getRegions().get(id);
            if (region == null) {
                logger.warn(
                        "VIEW_REGION is selected but no bounding box is available for region id="
                        + id + " will continue anyway using the default bounding box");
                bbox = portalSession.getDefaultBoundingBox();
            } else {
                bbox = region.getBoundingBox();
            }

        } else {
            bbox = portalSession.getDefaultBoundingBox();
        }

        return bbox;
    }

    public MenuGroup getMenu(PortalSession portalSession, int view, String id) {

        MenuGroup menu = null;

        if (view == PortalSession.LAYER_USER_TAB) {
            menu = portalSession.getMenuForUserDefined();
        } else {
            Facility selectedView = null;
            boolean proceed = false;

            switch (view) {
                case PortalSession.LAYER_FACILITY_TAB:
                    selectedView = portalSession.getFacilities().get(id);
                    proceed = true;
                    break;
                case PortalSession.LAYER_REGION_TAB:
                    selectedView = portalSession.getRegions().get(id);
                    proceed = true;
                    break;
                case PortalSession.LAYER_REALTIME_TAB:
                    selectedView = portalSession.getRealtimes().get(id);
                    proceed = true;
                    break;
                default:
                    logger.error("menu for was requested for unsupported view: " + view);
                    proceed = false;
            }

            if (proceed) {
                if (selectedView == null) {
                    logger.warn("no facility available for id=" + id + " view=" + view);
                } else {
                    menu = selectedView.getMenu();
                }
            }
        }
        return menu;
    }

    public Facility getSelectedFacilityOrRegion(PortalSession portalSession, String id) {
        Facility selected;
        switch (portalSession.getCurrentLayerTab()) {
            case PortalSession.LAYER_FACILITY_TAB:
                selected = portalSession.getFacilities().get(id);
                break;
            case PortalSession.LAYER_REGION_TAB:
                selected = portalSession.getRegions().get(id);
                break;
            default:
                selected = null;
                break;
        }
        return selected;
    }

    public void initUserDefinedMenu(PortalSession portalSession) {
        if (portalSession.getUserDefinedMenu() == null) {
            MenuGroup userDefinedMenu = new MenuGroup();
  
            userDefinedMenu.setId(settingsSupplementary.getValue("user_defined_layer_group_id"));

            // these are from the lang pack and describe the root menu
            // item which never gets rendered AFAIK
            userDefinedMenu.setName(languagePack.getLang("user_defined_layer_group_label"));
            userDefinedMenu.setDescription(languagePack.getLang("user_defined_layer_group_label"));

            portalSession.setUserDefinedMenu(userDefinedMenu);
        }
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
            dump.append("FACILITIES:\n");
            if (portalSession.getFacilities() != null) {
                for (Facility facility : portalSession.getFacilities().values()) {
                    dump.append(facility.dump());
                }
            }

            dump.append("REGIONS:\n");
            if (portalSession.getRegions() != null) {
                for (Region region : portalSession.getRegions().values()) {
                    dump.append(region.dump() + "\n");
                }
            }

            dump.append("\nMAPLAYERS (from DataSource declaration");
            if (portalSession.getMapLayers() != null) {
                for (MapLayer mapLayer : portalSession.getMapLayers()) {
                    dump.append(mapLayer.dump("") + "\n");
                }
            }

            dump.append("\nBASELAYERS (from DataSource declaration):\n");
            if (portalSession.getBaseLayers() != null) {
                for (MapLayer baseLayer : portalSession.getBaseLayers()) {
                    dump.append(baseLayer.dump() + "\n");
                }
            }

            dump.append("\nLINKS (from DataSource declaration):\n");
            if (portalSession.getLinks() != null) {
                for (Link link : portalSession.getLinks()) {
                    dump.append(link.dump() + "\n");
                }
            }
        }
        return dump.toString();
    }

    public MenuItem addUserDefinedMapLayer(PortalSession portalSession, MapLayer mapLayer) {
        initUserDefinedMenu(portalSession);

        // add to the list (like a datasource)
        portalSession.getUserDefinedLayers().add(mapLayer);

        // create and add a holder for it and insert into the menu
        MenuItem item = new MenuItem(mapLayer);
        portalSession.getUserDefinedMenu().addChild(item);
        return item;
    }

    public TreeChildIdentifier removeUserDefinedMapLayer(PortalSession portalSession, MenuItem itemToRemove) {
        TreeMenuItem parent = null;
        TreeChildIdentifier id = null;
        if ((itemToRemove != null) && itemToRemove.isValueMapLayerInstance()) {
            portalSession.getUserDefinedLayers().remove(itemToRemove.getValueAsMapLayer());

            parent = findInTree(portalSession.getUserDefinedMenu(), (Object) itemToRemove);
            // nuke the layer from the list

            if (parent != null) {
                id = new TreeChildIdentifier(parent, parent.getChildren().indexOf(itemToRemove));
                parent.getChildren().remove(itemToRemove);
            }
        }
        return id;
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
            } else {
                logger.debug(
                        "item is not a menuitem - couldn't do comparison: Type is " + root.getConcreteType());
            }

            if (!root.isLeaf()) {
                while ((parent == null) && (i < childCount)) {
                    parent = findInTree((TreeMenuItem) root.getChild(i), targetValue);
                    i++;
                }
            }
        }
        return parent;
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
            tab = PortalSession.LINK_TAB;
        } else if (str.equals(SEARCH_TAB)) {
            tab = PortalSession.SEARCH_TAB;
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

    public SearchCatalogue getSelectedSearchCatalogue(PortalSession portalSession) {
        return settings.getSearchCatalogues().get(portalSession.getSelectedSearchCatalogueId());
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    

}
