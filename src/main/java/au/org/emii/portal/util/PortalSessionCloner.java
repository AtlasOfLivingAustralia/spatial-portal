/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
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
        portalSession.reset();

        // step1: data sources and search catalogues
        // maplayers
        if (masterPortalSession.getMapLayers() != null) {
            for (MapLayer mapLayer : masterPortalSession.getMapLayers()) {
                portalSession.addMapLayer((MapLayer) mapLayer.clone());
            }
        }

        // step 2: copy regions/facilities and settings

        // default map bounding box
        if (masterPortalSession.getDefaultBoundingBox() == null) {
            portalSession.setDefaultBoundingbox((BoundingBox) masterPortalSession.getDefaultBoundingBox().clone());
        }

        // step 4: clone active layers
        if (masterPortalSession.getActiveLayers() != null) {
            for (MapLayer mapLayer : masterPortalSession.getActiveLayers()) {
                portalSession.getActiveLayers().add((MapLayer) mapLayer.clone());
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

         logger.debug("Session cloned as: " + portalSessionUtilities.dump(portalSession));
         return portalSession;
    }
}
