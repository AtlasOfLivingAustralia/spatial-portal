/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.config;

import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.UriResolver;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.net.HttpAuthenticateProxy;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.wms.RemoteMap;
import au.org.emii.portal.config.xmlbeans.PortalDocument;
import au.org.emii.portal.config.xmlbeans.Supplementary;
import au.org.emii.portal.config.xmlbeans.UriEndPoints;
import au.org.emii.portal.config.xmlbeans.UriResolver.Mapping;
import au.org.emii.portal.util.PortalSessionUtilities;
import java.net.Authenticator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ala.spatial.util.CommonData;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.beans.factory.annotation.Required;

/**
 * Configuration loader
 * ~~~~~~~~~~~~~~~~~~~~
 * Stage 2 proceeds after the spring context has been loaded and does the actual
 * application setup - with access to spring beans
 * @author geoff
 */
public class ConfigurationLoaderStage2Impl implements ConfigurationLoaderStage2 {

    /**
     * Log4j
     */
    private Logger logger = Logger.getLogger(getClass());
    /**
     * Service resolver instance - spring autowired
     */
    private UriResolver uriResolver = null;
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
    private HttpAuthenticateProxy httpAuthenticateProxy = null;
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
     *
     * XPATHS to Main sections of the config file:
     *
     * NOTE:
     * * denotes element repeated 0 or more times
     * This list is a broad overview, its not conclusive!
     *
     * /portal
     *     |-settings
     *     |  |-'well known' values
     *     |  |-mestConfigurations
     *     |  |   |-mestConfiguration*
     *     |  |-supplementary
     *     |      |- key/value pairs*
     *     |-menu
     *     |  |-facilities
     *     |  |   |-facilitiy*
     *     |  |       |-menu
     *     |  |-regions
     *     |  |   |-region*
     *     |  |       |-menu
     *     |  |-realtimes
     *     |  |   |-realtime*
     *     |  |       |-menu
     *     |  | staticLinks
     *     |      |-staticLinkIdRef*
     *     |-uriResolver
     *     |  |-mapping*
     *     |  |-uriEndPoint*
     *     |-search
     *     |  |-searchCatalogue*
     *     |-dataSource
     *     |  |-activeByDefault
     *     |  |-blacklist
     *     |  |-discoveries
     *     |  |   |-discovery*
     *     |  |-services
     *     |  |   |-service*
     *     |  |-baseLayers
     *     |  |   |-baseLayer*
     *     |  |-staticLinks
     *     |      |-staticLink*
     *     |-userAccount
     *       |-mestUserManagementService*
     */
    @Override
    public PortalSession load() {
        reloading = true;

        // protect against sticky error flag
        error = false;

        workingPortalSession = new PortalSession();

        // note - order is different to natural xml order due to dependencies
        // between stages, eg menu depends on datasource depends on uriresolver, etc

        uriResolver();


        settings();
        proxyHack();

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
        CommonData.init(settingsSupplementary.getValue(CommonData.SAT_URL), settingsSupplementary.getValue(CommonData.GEOSERVER_URL));
        
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
            logger.info("Settings from config file:");
            settings.setCacheMaxAge(xmlSettings.getCacheMaxAge().intValue());
            settings.setCacheParameter(xmlSettings.getCacheParameter());
            settings.setCacheUrl(xmlSettings.getCacheUrl());
            settings.setConfigRereadInitialInterval(xmlSettings.getConfigRereadInitialInterval().intValue());
            settings.setConfigRereadInterval(xmlSettings.getConfigRereadInterval().intValue());
            settings.setDisableDepthServlet(xmlSettings.getDisableDepthServlet());
            settings.setNetConnectSlowTimeout(xmlSettings.getNetConnectSlowTimeout().intValue());
            settings.setNetConnectTimeout(xmlSettings.getNetConnectTimeout().intValue());
            settings.setNetReadSlowTimeout(xmlSettings.getNetReadSlowTimeout().intValue());
            settings.setNetReadTimeout(xmlSettings.getNetReadTimeout().intValue());
            settings.setProxyAllowedHosts(xmlSettings.getProxyAllowedHosts());
            settings.setProxyHost(xmlSettings.getProxyHost());
            settings.setProxyNonProxyHosts(xmlSettings.getProxyNonProxyHosts());
            settings.setProxyPassword(xmlSettings.getProxyPassword());
            settings.setProxyPort(xmlSettings.getProxyPort().intValue());
            settings.setProxyRequired(xmlSettings.getProxyRequired());
            settings.setProxyUsername(xmlSettings.getProxyUsername());
            settings.setXmlMimeType(xmlSettings.getXmlMimeType());
            settings.setIsoCountriesFilename(xmlSettings.getIsoCountriesFilename());

            // remaining items are complex types so needs special handling
            settings.setDefaultBoundingBox(defaultBoundingBox());
        } catch (NullPointerException e) {
            // FIXME - make this message nicer
            logger.error("configuration is broken: missing settings value for " + e.getMessage(), e);
            error = true;
        }
    }

    /**
     * Nasty hack to connect to the proxy - would be nice if we didn't need it at all...
     */
    private void proxyHack() {
        if (settings.isProxyRequired()) {
            logger.debug("*** Enabling HTTP proxy support ***");

            System.setProperty("http.proxyHost", settings.getProxyHost());
            System.setProperty("http.proxyPort", String.valueOf(settings.getProxyPort()));
            logger.debug(String.format(
                    "http.proxyHost='%s' http.proxyPort='%s'",
                    System.getProperty("http.proxyHost"),
                    System.getProperty("http.proxyPort")));

            // just strip out any whitespace...
            String noProxyHosts = settings.getProxyNonProxyHosts();
            noProxyHosts = noProxyHosts.replaceAll("\\s+", "");

            logger.debug("config: no proxying for : " + noProxyHosts);
            System.setProperty("http.nonProxyHosts", noProxyHosts);

            Authenticator.setDefault(httpAuthenticateProxy);
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
     *
     * Children of configure should be key values pairs, e.g.:
     *
     * <config>
     * 	<myval1>foo</myval1>
     *  <myval2>foo</myval2>
     *  ...
     *  <myvaln>foo</myvaln>
     * </config
     *
     * Will produce a hashmap like this:
     * value['myval1']='foo'
     * value['myval2']='foo'
     * value['myval3']='foo'
     * value['myvaln']='foo'
     *
     * ... which is then accessible by calling Config.getValue:
     * System.out.println(Config.getValue('myval1'));
     *
     * prints 'foo'
     *
     *
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
                logger.info("settings/supplementary/" + key + " ==> " + value);
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
    
    private void uriResolver() {
        /*
        logger.debug("uriResolver...");
        uriResolver.clear();
        List<Mapping> mappings = portalDocument.getPortal().getUriResolver().getMappingList();
        List<UriEndPoints.UriEndPoint> endpoints = portalDocument.getPortal().getUriResolver().getUriEndPoints().getUriEndPointList();

        Map<String, String> endPointMap = new HashMap<String, String>();

        // build a list of valid endpoint URIs
        for (UriEndPoints.UriEndPoint endPoint : endpoints) {
            if (endPoint.getDisabled()) {
                logger.info("Disabling URI endpoint " + endPoint.getId() + " because it is disabled");
            } else {
                endPointMap.put(endPoint.getId(), endPoint.getUri());
            }
        }

        // now map these to the requested uriEndPointIdRef ids in the mappings
        for (Mapping mapping : mappings) {
            String uriEndPointIdRef = mapping.getUriEndPointIdRef();
            String uriId = mapping.getUriId();
            String targetUri = endPointMap.get(uriEndPointIdRef);

            if (targetUri == null) {
                logger.error(
                        "Requested mapping to uriEndPointIdRef " + uriEndPointIdRef
                        + " failed because the uriEndPoint does not exist or is disabled");
            } else {
                logger.info(String.format("enabling mapping '%s' ==> '%s'", uriId, targetUri));
                uriResolver.put(uriId, targetUri);
            }
        }*/
    }

    public UriResolver getUriResolver() {
        return uriResolver;
    }

    @Required
    public void setUriResolver(UriResolver uriResolver) {
        this.uriResolver = uriResolver;
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

    public HttpAuthenticateProxy getHttpAuthenticateProxy() {
        return httpAuthenticateProxy;
    }

    @Required
    public void setHttpAuthenticateProxy(HttpAuthenticateProxy httpAuthenticateProxy) {
        this.httpAuthenticateProxy = httpAuthenticateProxy;
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
