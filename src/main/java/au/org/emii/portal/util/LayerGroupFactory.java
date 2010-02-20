/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.Facility;
import au.org.emii.portal.Link;
import au.org.emii.portal.MapLayer;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.TreeMenuItem;
import au.org.emii.portal.config.xmlbeans.LayerGroup;
import au.org.emii.portal.menu.MenuGroup;
import au.org.emii.portal.menu.MenuItem;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;

/**
 *
 * @author geoff
 */
public class LayerGroupFactory {

    private Logger logger = Logger.getLogger(getClass());
    private PortalSessionUtilities portalSessionUtilities = null;

    public <T extends Facility> T createInstance(Class<T> clazz, LayerGroup xmlLayerGroup, PortalSession portalSession) {
        T facilityOrRegion = null;
        try {
            facilityOrRegion = clazz.newInstance();
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LayerGroupFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LayerGroupFactory.class.getName()).log(Level.SEVERE, null, ex);
        }

        facilityOrRegion.setId(xmlLayerGroup.getId());
        facilityOrRegion.setName(xmlLayerGroup.getName());
        facilityOrRegion.setDescription(xmlLayerGroup.getDescription());

        if (xmlLayerGroup.getMenu().getDisabled()) {
            
            logger.info("skipping menu '" + xmlLayerGroup.getId() + "' because it is disabled in config file");
        } else {
            MenuGroup menuGroup = new MenuGroup();
            populateMenuInstance(menuGroup, xmlLayerGroup.getMenu(), portalSession);
            facilityOrRegion.setMenu(menuGroup);
        }

        return facilityOrRegion;
    }

    public void populateMenuInstance(   MenuGroup menuGroup,
                                        au.org.emii.portal.config.xmlbeans.MenuGroup xmlMenuGroup,
                                        PortalSession portalSession) {
        
        menuGroup.setId(xmlMenuGroup.getId());
        menuGroup.setName(xmlMenuGroup.getName());
        menuGroup.setDescription(xmlMenuGroup.getDescription());

        /* iterate over the child elements.  This has to be done with an
         * xml cursor to preserve ordering.  The xmlbeans API only
         * supports getting three separate lists.
         */
        XmlCursor cursor = xmlMenuGroup.getChildren().newCursor();
        boolean finished = false;
        cursor.toFirstChild();

        while (!finished) {
            /* cursor contains a menuGroup, dataSourceIdRef or staticLinkIdRef
             * element.
             */
            TreeMenuItem node = getNodeAtCursor(cursor, portalSession);
            if (node != null) {
                menuGroup.addChild(node);
            }
            finished = !cursor.toNextSibling();
        }
        cursor.dispose();
    }

    private TreeMenuItem getNodeAtCursor(XmlCursor cursor, PortalSession portalSession) {
        TreeMenuItem node = null;
        String type = cursor.getName().getLocalPart();
        if (type.equals("menuGroup")) {
            au.org.emii.portal.config.xmlbeans.MenuGroup item = (au.org.emii.portal.config.xmlbeans.MenuGroup) cursor.getObject().changeType(au.org.emii.portal.config.xmlbeans.MenuGroup.type);
            node = new MenuGroup();
            populateMenuInstance((MenuGroup) node, item, portalSession);
        } else if (type.equals("disoveryOrServiceIdRef")) {
            String id = cursor.getTextValue();

            /* child is a DataSource (MapLayer after parsing the config file)
             * identified by id
             */
            MapLayer mapLayer = portalSessionUtilities.getMapLayerById(portalSession,id);
            if (mapLayer != null) {
                node = new MenuItem(mapLayer);
            } else {
                logger.warn("disoveryOrServiceIdRef: '" + id + "' not found in portal session so not adding to menu");
            }

        } else if (type.equals("staticLinkIdRef")) {
            String id = cursor.getTextValue();

            /* child is a StaticLink (Link after parsing the config file)
             * identified by id
             */
            Link link = portalSessionUtilities.getLinkById(portalSession,id);

            if (link != null) {
                node = new MenuItem(link);
            }
            else {
                logger.warn("staticLinkIdRef: '" + id + "' not found in portal session so not adding to menu");
            }


        } else {
            logger.warn("unable to process unsupported menu child: " + type);
        }

        return node;
    }

    public PortalSessionUtilities getPortalSessionUtilities() {
        return portalSessionUtilities;
    }

    public void setPortalSessionUtilities(PortalSessionUtilities portalSessionUtilities) {
        this.portalSessionUtilities = portalSessionUtilities;
    }

    
}
