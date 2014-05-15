/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.config;

import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.wms.RemoteMap;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.beans.factory.annotation.Required;

import java.util.HashMap;
import java.util.Properties;

/**
 * Configuration loader
 * ~~~~~~~~~~~~~~~~~~~~
 * Stage 2 proceeds after the spring context has been loaded and does the actual
 * application setup - with access to spring beans
 *
 * @author geoff
 */
public class ConfigurationLoaderStage2Impl implements ConfigurationLoaderStage2 {

    /**
     * Log4j
     */
    private static Logger logger = Logger.getLogger(ConfigurationLoaderStage2Impl.class);
    /**
     * Remote map URI mangler helper class - spring autowired
     */
    private RemoteMap remoteMap = null;
    /**
     * Parsed configuration file xml bean - NOT autowired by spring since
     * spring doesn't know about it - gets set by configuration loader
     * stage 1 before processing stage 2
     */
    private Properties portalDocument = null;
    /**
     * PortalSession instance - this will (eventually) become the master
     * portal session - this one can be spring injected(?) maybe...
     */
    private PortalSession workingPortalSession = null;
    /**
     * Settings instance - spring injected
     */
    private Settings settings = null;
    /**
     * Supplementary settings (key/value pairs) spring autowired
     */
    private Properties settingsSupplementary = null;
    private PortalSessionUtilities portalSessionUtilities = null;
    /**
     * Error occurred during loading (flag)
     */
    private boolean error = false;

    /**
     * Flag to indicate if we are currently reloading (inside load())
     */
    private boolean reloading = false;

    @Override
    public PortalSession load() {
        reloading = true;

        // protect against sticky error flag
        error = false;

        workingPortalSession = new PortalSession();

        // note - order is different to natural xml order due to dependencies
        // between stages, eg menu depends on datasource depends on uriresolver, etc

        settings();

        settingsSupplementary();

        finaliseSession();

        // If we were able to construct the working session with no errors,
        // return this to the user otherwise return null;
        PortalSession masterPortalSession;
        if (error) {
            logger.error(
                    "error constructing master portal session in stage 2 loader " +
                            "- error flag got set, returning null PortalSession instance " +
                            "and disabling portal"
            );
            masterPortalSession = null;
        } else {
            // all good
            masterPortalSession = workingPortalSession;
        }

        //geoserver/alaspatial analysis page data       
        CommonData.init(settingsSupplementary);

        cleanup();
        reloading = false;
        return masterPortalSession;
    }

    /**
     * Miscellaneous session settings
     */
    private void finaliseSession() {

        // set the initial bounding box in the master session
        workingPortalSession.setDefaultBoundingbox(settings.getDefaultBoundingBox());
    }

    /**
     * Cleanup any resources we are holding
     */
    @Override
    public void cleanup() {
        portalDocument = null;
        workingPortalSession = null;
    }

    @Override
    public void setProperties(Properties portalDocument) {
        this.portalDocument = portalDocument;
    }

    private void settings() {
        logger.debug("settings...");

        try {
            logger.debug("Settings from config file:");

            settings.setConfigRereadInitialInterval(Integer.parseInt(portalDocument.getProperty("configRereadInitialInterval")));
            settings.setConfigRereadInterval(Integer.parseInt(portalDocument.getProperty("configRereadInterval")));

            settings.setNetConnectSlowTimeout(Integer.parseInt(portalDocument.getProperty("netConnectSlowTimeout")));
            settings.setNetConnectTimeout(Integer.parseInt(portalDocument.getProperty("netConnectTimeout")));//xmlSettings.getNetConnectTimeout().intValue());
            settings.setNetReadSlowTimeout(Integer.parseInt(portalDocument.getProperty("netReadSlowTimeout")));//xmlSettings.getNetReadSlowTimeout().intValue());
            settings.setNetReadTimeout(Integer.parseInt(portalDocument.getProperty("netReadTimeout")));//xmlSettings.getNetReadTimeout().intValue());

            settings.setProxyAllowedHosts((portalDocument.getProperty("proxyAllowedHosts")));//xmlSettings.getProxyAllowedHosts());

            // remaining items are complex types so needs special handling
            settings.setDefaultBoundingBox(defaultBoundingBox());
        } catch (NullPointerException e) {
            // FIXME - make this message nicer
            logger.error("configuration is broken: missing settings value for " + e.getMessage(), e);
            error = true;
        }
    }

    /**
     * Process a bounding box declaration
     */
    private BoundingBox defaultBoundingBox() {
        BoundingBox bbox = new BoundingBox();
        try {
            bbox.setMinLatitude(Float.parseFloat(portalDocument.getProperty("defaultBoundingBox.minLatitude")));
            bbox.setMaxLatitude(Float.parseFloat(portalDocument.getProperty("defaultBoundingBox.maxLatitude")));
            bbox.setMinLongitude(Float.parseFloat(portalDocument.getProperty("defaultBoundingBox.minLongitude")));
            bbox.setMaxLongitude(Float.parseFloat(portalDocument.getProperty("defaultBoundingBox.maxLongitude")));
        } catch (Exception e) {
            logger.error("failed to parse defaultBoundBox values", e);
        }

        return bbox;
    }

    private void settingsSupplementary() {
        settingsSupplementary = portalDocument;
    }

    public RemoteMap getRemoteMap() {
        return remoteMap;
    }

    @Required
    public void setRemoteMap(RemoteMap remoteMap) {
        this.remoteMap = remoteMap;
    }

    @Override
    public PortalSession getWorkingPortalSession() {
        return workingPortalSession;
    }

    @Override
    public void setWorkingPortalSession(PortalSession workingPortalSession) {
        this.workingPortalSession = workingPortalSession;
    }


    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public boolean isError() {
        return error;
    }


    public PortalSessionUtilities getPortalSessionUtilities() {
        return portalSessionUtilities;
    }

    @Required
    public void setPortalSessionUtilities(PortalSessionUtilities portalSessionUtilities) {
        this.portalSessionUtilities = portalSessionUtilities;
    }

    @Override
    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

}
