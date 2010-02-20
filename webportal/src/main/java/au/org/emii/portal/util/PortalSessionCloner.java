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
import au.org.emii.portal.TreeMenuItem;
import au.org.emii.portal.TreeMenuValue;
import au.org.emii.portal.menu.MenuItem;
import au.org.emii.portal.user.PortalUserImpl;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 *
 * @author geoff
 */
public class PortalSessionCloner {
    private PortalSessionUtilities portalSessionUtilities = null;
    private Logger logger = Logger.getLogger(getClass());

    public PortalSessionUtilities getPortalSessionUtilities() {
        return portalSessionUtilities;
    }

    public void setPortalSessionUtilities(PortalSessionUtilities portalSessionUtilities) {
        this.portalSessionUtilities = portalSessionUtilities;
    }

    public PortalSession clone(PortalSession masterPortalSession) throws CloneNotSupportedException {
        PortalSession portalSession = (PortalSession) masterPortalSession.clone();
        
        /* super.clone will leave references to existing objects
         * in place, e.g.  portalSession.mapLayers == mapLayers is
         * currently true - to fix this, we will re-init all these
         * fields now
         *
         * although we don't mind sharing these between users, they
         * have to be defined 'later' when we call clone() because
         * the config file has not yet been loaded if we try earlier
         */
        portalSession.setFacilities(new ArrayList<Facility>());
        portalSession.setRegions(new ArrayList<Region>());
        portalSession.setRealtime(new ArrayList<Facility>());
        portalSession.setMapLayers(new ArrayList<MapLayer>());
        portalSession.setBaseLayers(new ArrayList<MapLayer>());
        portalSession.setLinks(new ArrayList<Link>());
        portalSession.setActiveLayers(new ArrayList<MapLayer>());
        portalSession.setUserDefinedLayers(new ArrayList<MapLayer>());
        portalSession.setUserDefinedMenu(null);
        portalSession.setStaticMenuLinks(new ArrayList<Link>());
        portalSession.setSearchCatalogues(new ArrayList<SearchCatalogue>());
        portalSession.setPortalUser(new PortalUserImpl());
        portalSession.setSelectedSearchCatalogue(null);



        // step1: data sources and search catalogues
        // maplayers
        if (masterPortalSession.getMapLayers() != null) {
            for (MapLayer mapLayer : masterPortalSession.getMapLayers()) {
                portalSession.addMapLayer((MapLayer) mapLayer.clone());
            }
        }

        // baselayers
        if (masterPortalSession.getBaseLayers() != null) {
            for (MapLayer baseLayer : masterPortalSession.getBaseLayers()) {
                portalSession.addBaseLayer((MapLayer) baseLayer.clone());
            }
        }

        // links
        if (masterPortalSession.getLinks() != null) {
            for (Link link : masterPortalSession.getLinks()) {
                portalSession.addLink((Link) link.clone());
            }
        }

        // search catalogues
        if (masterPortalSession.getSearchCatalogues() != null) {
            for (SearchCatalogue searchCatalogue : masterPortalSession.getSearchCatalogues()) {
                portalSession.addSearchCatalogue(
                        (SearchCatalogue) searchCatalogue.clone());
            }
        }

        // put default search catalogue back
        if (masterPortalSession.getSelectedSearchCatalogue() != null) {
            portalSession.setSelectedSearchCatalogue(
                    portalSessionUtilities.getSearchCatalogueById(
                        masterPortalSession,
                        masterPortalSession.getSelectedSearchCatalogue().getId()));
        }

        // now we can put the baselayer back
        if (masterPortalSession.getCurrentBaseLayer() != null) {
            portalSession.setCurrentBaseLayer(
                    portalSessionUtilities.getBaseLayerById(
                        masterPortalSession,
                        masterPortalSession.getCurrentBaseLayer().getId()));
        }

        // step 2: copy regions/facilities and settings

        // default map bounding box
        if (masterPortalSession.getDefaultBoundingBox() == null) {
            portalSession.setDefaultBoundingbox((BoundingBox) masterPortalSession.getDefaultBoundingBox().clone());
        }

        // facilities
        if (masterPortalSession.getFacilities() != null) {
            for (Facility facility : masterPortalSession.getFacilities()) {
                Facility clone = (Facility) facility.clone();
                portalSession.addFacility(clone);

                // put back the .value field
                cloneValueField(
                        facility.getMenu(),
                        clone.getMenu(),
                        portalSession);

            }
        }


        // regions
        if (masterPortalSession.getRegions() != null) {
            for (Region region : masterPortalSession.getRegions()) {
                Region clone = (Region) region.clone();
                portalSession.addRegion(clone);

                // put back the .value field
                cloneValueField(
                        region.getMenu(),
                        clone.getMenu(),
                        portalSession);
            }
        }

        // realtime
        if (masterPortalSession.getRealtime() != null) {
            for (Facility rt : masterPortalSession.getRealtime()) {
                Facility clone = (Facility) rt.clone();
                portalSession.addRealtime(clone);

                // put back the .value field
                cloneValueField(
                        rt.getMenu(),
                        clone.getMenu(),
                        portalSession);

            }
        }

        // Step 3: put back the value field for regions/facilities
        if (masterPortalSession.getStaticMenuLinks() != null) {
            for (Link link : masterPortalSession.getStaticMenuLinks()) {
                portalSession.addStaticMenuLink((Link) link.clone());
            }
        }

        // step 4: clone active layers
        if (masterPortalSession.getActiveLayers() != null) {
            for (MapLayer mapLayer : masterPortalSession.getActiveLayers()) {
                portalSession.getActiveLayers().add(
                        portalSessionUtilities.getMapLayerById(
                            portalSession,
                            mapLayer.getId()));
            }
        }

        /* step 5: skip things
         *
         * o	userDefined
         * o	UserDefinedMenu
         * All get skipped because for new sessions they should
         * all be empty lists/objects
         */

        /* step 6: create an initial user defined menu tree - isn't done on creating
         * during stage 2 because otherwise we would have to bother cloning everything
         */
         portalSessionUtilities.initUserDefinedMenu(portalSession);

         logger.debug("Session cloned as: " + portalSessionUtilities.dump(portalSession));
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
                            portalSessionUtilities.getLinkById(
                                cloneSession,
                                originalMenuItem.getValue().getId());

                } else if (originalMenuItem.isValueMapLayerInstance()) {
                    value =
                            portalSessionUtilities.getMapLayerById(
                                cloneSession,
                                originalMenuItem.getValue().getId());
                }

                if (value != null) {
                    cloneMenuItem.setValue(value);
                }

            }

            for (TreeMenuItem originalChild : original.getChildren()) {
                cloneValueField(
                        originalChild,
                        (TreeMenuItem) clone.getChild(clone, original.getChildren().indexOf(originalChild)),
                        cloneSession);
            }
        }
    }
}
