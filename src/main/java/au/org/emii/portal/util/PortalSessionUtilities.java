/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.util;

import au.org.emii.portal.BoundingBox;
import au.org.emii.portal.Facility;
import au.org.emii.portal.Link;
import au.org.emii.portal.MapLayer;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.Region;
import au.org.emii.portal.SearchCatalogue;
import au.org.emii.portal.TreeChildIdentifier;
import au.org.emii.portal.TreeMenuItem;
import au.org.emii.portal.config.SettingsSupplementary;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MenuGroup;
import au.org.emii.portal.menu.MenuItem;
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

    public SearchCatalogue getSearchCatalogueById(PortalSession portalSession, String id) {
        return listUtilities.findInList(id, portalSession.getSearchCatalogues());
    }

    public Region getRegionById(PortalSession portalSession, String id) {
        return listUtilities.findInList(id, portalSession.getRegions());
    }

    public Facility getFacilityById(PortalSession portalSession, String id) {
        return listUtilities.findInList(id, portalSession.getFacilities());
    }

    public Facility getRealtimeById(PortalSession portalSession, String id) {
        return listUtilities.findInList(id, portalSession.getRealtime());
    }

    /**
     * return the current bounding box - either the default bounding box
     * or the regional bounding box if a region has been selected
     * @return
     */
    public BoundingBox getCurrentBoundingBox(PortalSession portalSession) {

        BoundingBox bbox;
        String id = portalSession.getSelectedFacilityOrRegionId();
        if ((portalSession.getViewForCurrentMenu() == PortalSession.VIEW_REGION) && (id != null)) {
            Region region = getRegionById(portalSession, id);
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

        if (view == PortalSession.VIEW_USER) {
            menu = portalSession.getMenuForUserDefined();
        } else {
            Facility selectedView = null;
            boolean proceed = false;

            switch (view) {
                case PortalSession.VIEW_FACILITY:
                    selectedView = getFacilityById(portalSession, id);
                    proceed = true;
                    break;
                case PortalSession.VIEW_REGION:
                    selectedView = getRegionById(portalSession, id);
                    proceed = true;
                    break;
                case PortalSession.VIEW_REALTIME:
                    selectedView = getRealtimeById(portalSession, id);
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
        switch (portalSession.getCurrentView()) {
            case PortalSession.VIEW_FACILITY:
                selected = getFacilityById(portalSession, id);
                break;
            case PortalSession.VIEW_REGION:
                selected = getRegionById(portalSession, id);
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

        dump.append("FACILITIES:\n");
        for (Facility facility : portalSession.getFacilities()) {
            dump.append(facility.dump());
        }

        dump.append("REGIONS:\n");
        for (Region region : portalSession.getRegions()) {
            dump.append(region.dump() + "\n");
        }

        dump.append("\nMAPLAYERS (from DataSource declaration");
        for (MapLayer mapLayer : portalSession.getMapLayers()) {
            dump.append(mapLayer.dump("") + "\n");
        }

        dump.append("\nBASELAYERS (from DataSource declaration):\n");
        for (MapLayer baseLayer : portalSession.getBaseLayers()) {
            dump.append(baseLayer.dump() + "\n");
        }

        dump.append("\nLINKS (from DataSource declaration):\n");
        for (Link link : portalSession.getLinks()) {
            dump.append(link.dump() + "\n");
        }

        return dump.toString();
    }

    public MenuItem addUserDefinedMapLayer(PortalSession portalSession, MapLayer mapLayer) {

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


}
