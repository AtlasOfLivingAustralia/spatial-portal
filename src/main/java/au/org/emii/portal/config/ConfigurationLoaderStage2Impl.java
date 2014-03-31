/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.config;

import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.config.xmlbeans.PortalDocument;
import au.org.emii.portal.config.xmlbeans.Supplementary;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.wms.RemoteMap;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.beans.factory.annotation.Required;

import java.util.HashMap;

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
    private PortalDocument portalDocument = null;
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
    private SettingsSupplementary settingsSupplementary = null;
    private PortalSessionUtilities portalSessionUtilities = null;
    /**
     * Error occurred during loading (flag)
     */
    private boolean error = false;

    /**
     * Flag to indicate if we are currently reloading (inside load())
     */
    private boolean reloading = false;

    /**
     * Load the configuration and process all directives
     * <p/>
     * XPATHS to Main sections of the config file:
     * <p/>
     * NOTE:
     * * denotes element repeated 0 or more times
     * This list is a broad overview, its not conclusive!
     * <p/>
     * /portal
     * |-settings
     * |  |-'well known' values
     * |  |-mestConfigurations
     * |  |   |-mestConfiguration*
     * |  |-supplementary
     * |      |- key/value pairs*
     * |-menu
     * |  |-facilities
     * |  |   |-facilitiy*
     * |  |       |-menu
     * |  |-regions
     * |  |   |-region*
     * |  |       |-menu
     * |  |-realtimes
     * |  |   |-realtime*
     * |  |       |-menu
     * |  | staticLinks
     * |      |-staticLinkIdRef*
     * |-uriResolver
     * |  |-mapping*
     * |  |-uriEndPoint*
     * |-search
     * |  |-searchCatalogue*
     * |-dataSource
     * |  |-activeByDefault
     * |  |-blacklist
     * |  |-discoveries
     * |  |   |-discovery*
     * |  |-services
     * |  |   |-service*
     * |  |-baseLayers
     * |  |   |-baseLayer*
     * |  |-staticLinks
     * |      |-staticLink*
     * |-userAccount
     * |-mestUserManagementService*
     */
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
        CommonData.init(settingsSupplementary.getValues());

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

    private void settings() {
        logger.debug("settings...");
        au.org.emii.portal.config.xmlbeans.Settings xmlSettings = portalDocument.getPortal().getSettings();
        try {
            logger.debug("Settings from config file:");

            settings.setConfigRereadInitialInterval(xmlSettings.getConfigRereadInitialInterval().intValue());
            settings.setConfigRereadInterval(xmlSettings.getConfigRereadInterval().intValue());

            settings.setNetConnectSlowTimeout(xmlSettings.getNetConnectSlowTimeout().intValue());
            settings.setNetConnectTimeout(xmlSettings.getNetConnectTimeout().intValue());
            settings.setNetReadSlowTimeout(xmlSettings.getNetReadSlowTimeout().intValue());
            settings.setNetReadTimeout(xmlSettings.getNetReadTimeout().intValue());

            settings.setProxyAllowedHosts(xmlSettings.getProxyAllowedHosts());

            settings.setXmlMimeType(xmlSettings.getXmlMimeType());

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
        bbox.copyFrom(portalDocument.getPortal().getSettings().getDefaultBoundingBox());

        return bbox;
    }

    /**
     * process the <config> directive.  Read it into a Map which gets stored
     * statically in the Config class
     * <p/>
     * Children of configure should be key values pairs, e.g.:
     * <p/>
     * <config>
     * <myval1>foo</myval1>
     * <myval2>foo</myval2>
     * ...
     * <myvaln>foo</myvaln>
     * </config
     * <p/>
     * Will produce a hashmap like this:
     * value['myval1']='foo'
     * value['myval2']='foo'
     * value['myval3']='foo'
     * value['myvaln']='foo'
     * <p/>
     * ... which is then accessible by calling Config.getValue:
     * logger.debug(Config.getValue('myval1'));
     * <p/>
     * prints 'foo'
     */
    private void settingsSupplementary() {
        Supplementary supplementary = portalDocument.getPortal().getSettings().getSupplementary();
        XmlCursor cursor = supplementary.newCursor();
        HashMap<String, String> supplementaryValues = new HashMap<String, String>();
        boolean finished = false;
        // enter the <configuration> block and read the first child if there is one
        if (cursor.toFirstChild()) {

            while (!finished) {

                String key = cursor.getName().getLocalPart();
                String value = cursor.getTextValue();
                logger.debug("settings/supplementary/" + key + " ==> " + value);
                supplementaryValues.put(key, value);
                finished = !cursor.toNextSibling();
            }
            cursor.dispose();
        } else {
            cursor.dispose();
            logger.error("Unable to select supplementary settings from configuration file");
            error = true;
        }

        /* now give the hashmap we made to the Config class which keeps values
         * for the duration of our application's life.
         */
        settingsSupplementary.setValues(supplementaryValues);
    }

    public RemoteMap getRemoteMap() {
        return remoteMap;
    }

    @Required
    public void setRemoteMap(RemoteMap remoteMap) {
        this.remoteMap = remoteMap;
    }

    @Override
    public PortalDocument getPortalDocument() {
        return portalDocument;
    }

    @Override
    public void setPortalDocument(PortalDocument portalDocument) {
        this.portalDocument = portalDocument;
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

    public SettingsSupplementary getSettingsSupplementary() {
        return settingsSupplementary;
    }

    @Required
    public void setSettingsSupplementary(SettingsSupplementary settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
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
