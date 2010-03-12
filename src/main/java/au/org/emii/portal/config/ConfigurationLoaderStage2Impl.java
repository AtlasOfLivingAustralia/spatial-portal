/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.config;

import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.UriResolver;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.menu.Facility;
import au.org.emii.portal.net.HttpAuthenticateProxy;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.menu.Link;
import au.org.emii.portal.mest.webservice.MestSearchKeywords;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.menu.Region;
import au.org.emii.portal.wms.RemoteMap;
import au.org.emii.portal.factory.SearchCatalogueFactory;
import au.org.emii.portal.config.xmlbeans.Service;
import au.org.emii.portal.config.xmlbeans.BaseLayer;
import au.org.emii.portal.config.xmlbeans.Discovery;
import au.org.emii.portal.config.xmlbeans.LayerGroup;
import au.org.emii.portal.config.xmlbeans.LayerIdentifiers;
import au.org.emii.portal.config.xmlbeans.Menu;
import au.org.emii.portal.config.xmlbeans.MestAccountManager;
import au.org.emii.portal.config.xmlbeans.SearchCatalogue;
import au.org.emii.portal.mest.MestConfiguration;
import au.org.emii.portal.config.xmlbeans.PortalDocument;
import au.org.emii.portal.config.xmlbeans.PropertiesAccountManager;
import au.org.emii.portal.config.xmlbeans.RegionLayerGroup;
import au.org.emii.portal.config.xmlbeans.Settings.MestConfigurations;
import au.org.emii.portal.config.xmlbeans.StaticLink;
import au.org.emii.portal.config.xmlbeans.Supplementary;
import au.org.emii.portal.config.xmlbeans.UriEndPoints;
import au.org.emii.portal.config.xmlbeans.UriResolver.Mapping;
import au.org.emii.portal.config.xmlbeans.UserAccount;
import au.org.emii.portal.factory.MestConfigurationFactory;
import au.org.emii.portal.util.LayerGroupFactory;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.mest.webservice.MestWebServiceParameters;
import au.org.emii.portal.mest.webservice.MestWebServiceParametersImpl;
import au.org.emii.portal.factory.MasterServiceFactoryConfigurer;
import au.org.emii.portal.factory.MestServiceFactory;
import au.org.emii.portal.factory.PropertiesServiceFactory;
import au.org.emii.portal.menu.MenuGroup;
import au.org.emii.portal.service.ServiceFactory;
import java.io.File;
import java.net.Authenticator;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private MestSearchKeywords mestSearchKeywords = null;
    private HttpAuthenticateProxy httpAuthenticateProxy = null;
    private SearchCatalogueFactory searchCatalogueFactory = null;
    private LayerGroupFactory layerGroupFactory = null;
    private PortalSessionUtilities portalSessionUtilities = null;
    private MestServiceFactory mestServiceFactory = null;
    private PropertiesServiceFactory propertiesServiceFactory = null;
    private MasterServiceFactoryConfigurer masterServiceFactoryConfigurer = null;
    private MestConfigurationFactory mestConfigurationFactory = null;
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


        // note - menu depends on datasources being loaded first!
        dataSource();
        blacklist();

        menu();


        search();

        userAccount();

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


        activeByDefault();
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
            settings.setMestConfigurations(mestConfigurations());
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

    /**
     * Process mest configurations from settings section
     */
    private Map<String, MestConfiguration> mestConfigurations() {
        Map<String, MestConfiguration> mestConfigurations = new HashMap<String, MestConfiguration>();
        MestConfigurations xmlMestConfigurations = portalDocument.getPortal().getSettings().getMestConfigurations();
        for (au.org.emii.portal.config.xmlbeans.MestConfiguration xmlMestConfiguration : xmlMestConfigurations.getMestConfigurationList()) {
            MestConfiguration mestConfiguration = mestConfigurationFactory.createInstance(xmlMestConfiguration);
            if (mestConfiguration != null) {
                mestConfigurations.put(mestConfiguration.getId(), mestConfiguration);
                logger.info("loaded MEST configuration: " + mestConfiguration.getId());
            } else {
                logger.debug("skipped loading mest configuration " + mestConfiguration.getId()
                        + " - factory returned null but previous error should explain reason.");
            }
        }
        return mestConfigurations;
    }

    private void menu() {
        logger.debug("menu...");
        Menu xmlMenu = getPortalDocument().getPortal().getMenu();

        // Do not do this!:
        // if (xmlMenu.getDisableLinksMenu()) {
        //      settings.setDisableLinks(true);
        // }
        //
        // if (xmlMenu.getDisableUserDefined()) { ...
        //
        // ... thinking that it's safe practice because the field is initialised
        // to false - it will work the first time but will become 'sticky' since
        // settings class is a singleton and exists for the entire life of the
        // webapp - subsequent config file changes will not take effect.



        if (xmlMenu.getDisableLayersTab()) {
            logger.info("Disabling layers tab because it is disabled in config file");
        }
        settings.setDisableLayers(xmlMenu.getDisableLayersTab());

        if (xmlMenu.getDisableLinksTab()) {
            logger.info("disabling links tab because it is disabled in config file");
        }
        settings.setDisableLinks(xmlMenu.getDisableLinksTab());

        if (xmlMenu.getDisableUserDefinedTab()) {
            logger.info("disabling user defined tab because it is disabled in config file");
        }
        settings.setDisableUserDefined(xmlMenu.getDisableUserDefinedTab());

        // facilities
        if (xmlMenu.getFacilities().getDisabled()) {
            logger.info("not loading any facilities because they are disabled in config file");
        } else {
            for (LayerGroup configuredFacility : xmlMenu.getFacilities().getFacilityList()) {
                Facility facility = processFacilityOrRegion(configuredFacility, workingPortalSession, Facility.class);
                if (facility != null) {
                    workingPortalSession.addFacility(facility);
                }
            }
        }

        // regions
        if (xmlMenu.getRegions().getDisabled()) {
            logger.info("not loading any regions because they are disabled in config file");
        }
        for (RegionLayerGroup configuredRegion : xmlMenu.getRegions().getRegionList()) {
            Region region = processFacilityOrRegion(configuredRegion, workingPortalSession, Region.class);
            if (region != null) {
                workingPortalSession.addRegion(region);
            }
        }

        // realtime
        if (xmlMenu.getRealtimes().getDisabled()) {
            logger.info("not loading any realtimes because they are disabled in config file");
        }
        for (LayerGroup configuredFacility : xmlMenu.getRealtimes().getRealtimeList()) {

            Facility facility = processFacilityOrRegion(configuredFacility, workingPortalSession, Facility.class);
            if (facility != null) {
                workingPortalSession.addRealtime(facility);
            }
        }

        // top right menu links
        if (xmlMenu.getTopRightLinks().getDisabled()) {
            logger.info("Top right links menu is disabled in config file so will not be loaded");
        } else {
            for (String staticLinkId : xmlMenu.getTopRightLinks().getStaticLinkIdRefList()) {
                Link link = portalSessionUtilities.getLinkById(workingPortalSession, staticLinkId);
                if (link != null) {
                    workingPortalSession.addStaticMenuLink(link);
                } else {
                    logger.debug(
                            "Link " + staticLinkId + " referenced in settings will "
                            + "not be displayed because there is no matching staticLink"
                            + "declaration in the dataSoure section or it is disabled - "
                            + "check config file");
                }
            }
        }

        // setup default menu selections - these are limited to enumerated values
        // for schema validation so should not need to be checked for validity
        workingPortalSession.setLayerTab(
            portalSessionUtilities.convertLayerView(xmlMenu.getDefaultLayerView().toString()));
        workingPortalSession.setCurrentNavigationTab(
            portalSessionUtilities.convertTab(xmlMenu.getDefaultTab().toString()));

        // check that the requested menuid exists before setting it to prevent
        // NPE at page load if menu does not actually exist
        String defaultMenuId = xmlMenu.getDefaultMenuIdRef().toString();
        MenuGroup menu = portalSessionUtilities.getMenu(workingPortalSession, workingPortalSession.getCurrentLayerTab(), defaultMenuId);
        if (menu == null) {
            logger.error("ignoring requested default menu selection of '" +
                    defaultMenuId + "' from config file, because target menu " +
                    "does not exist or is disabled");
        } else {
            workingPortalSession.setSelectedMenuId(defaultMenuId);
        }

        // normally it's enought just to not load things we don't want to appear in the menus
        // but for the top links section, we provide a convienience flag to indicate whether
        // or not to the menu has been completely disabled, so that the ui designer can hide
        // the menu easily if required
        settings.setDisableTopLinks(xmlMenu.getTopRightLinks().getDisabled());
    }

    private <T extends Facility> T processFacilityOrRegion(LayerGroup configured, PortalSession portalSession, Class<T> clazz) {
        T facilityOrRegion = null;
        if (!configured.getDisabled()) {
            facilityOrRegion = layerGroupFactory.createInstance(clazz, configured, portalSession);
        } else {
            logger.info(
                    "skipped facility or region " + configured.getId()
                    + " Known as " + configured.getName() + " because it is disabled");
        }
        return facilityOrRegion;
    }

    private void uriResolver() {
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
        }
    }

    private void search() {
        logger.debug("search...");
        Map<String, au.org.emii.portal.value.SearchCatalogue> searchCatalogues = new HashMap<String, au.org.emii.portal.value.SearchCatalogue>();
        if (portalDocument.getPortal().getSearch().getDisabled()) {
            logger.info("search system disabled in config file");
            settings.setDisableSearch(true);
        } else {
            String defaultSearchCatalogueId =
                    portalDocument.getPortal().getSearch().getDefaultSearchCatalogueIdRef();

            boolean defaultSearchCatalogueFound = false;

            for (SearchCatalogue xmlSearchCatalogue :
                    portalDocument.getPortal().getSearch().getSearchCatalogueList()) {
                if (xmlSearchCatalogue.getDisabled()) {
                    logger.info(
                            "Search catalogue: " + xmlSearchCatalogue.getId() + " at "
                            + uriResolver.resolve(xmlSearchCatalogue) + " will not be used because "
                            + "it is disabled");
                } else {

                    au.org.emii.portal.value.SearchCatalogue searchCatalogue = searchCatalogueFactory.createInstance(xmlSearchCatalogue);
                    if (searchCatalogue == null) {
                        logger.info("Error creating search catalogue id '" + xmlSearchCatalogue.getId() + "' (skipping)");
                    } else {
                        searchCatalogues.put(searchCatalogue.getId(), searchCatalogue);
                        logger.debug("SEARCHCATALOGUE + " + searchCatalogue.getId());

                        // lookup supported search terms and store in session
                        searchCatalogue.setSearchKeywords(mestSearchKeywords.getSearchTerms(searchCatalogue));

                        // select the default search catalogue from config
                        // file settings - check every search catalogue as we add
                        // them, this way the desired catalogue is guaranteed to
                        // exist
                        if (searchCatalogue.getId().equals(defaultSearchCatalogueId)) {
                            workingPortalSession.setSelectedSearchCatalogueId(defaultSearchCatalogueId);
                            defaultSearchCatalogueFound = true;
                        }
                    }
                }
            }
            if (!defaultSearchCatalogueFound) {
                settings.setDisableSearch(true);
                logger.error(
                        "Default search catalogue " + defaultSearchCatalogueId
                        + " requested in configuration file settings section "
                        + "is not available - check it exists and is not disabled.  "
                        + "To prevent your users receiving error messages, the search"
                        + "system has been automatically disabled");
            }
        }
        
        // always put in the map of search catalogues even if empty map to avoid
        // any npes on use
        settings.setSearchCatalogues(searchCatalogues);
    }

    private void processDiscovery(Discovery discovery, PortalSession portalSession) {
        if (!discovery.getDisabled()) {
            MapLayer mapLayer;
            logger.debug("discovery begins for id: " + discovery.getId());
            if (discovery.getType().equals(LayerUtilities.AUTO_DISCOVERY_TYPE)) {
                String targetUri = uriResolver.resolve(discovery);
                mapLayer = remoteMap.autoDiscover(
                        discovery.getId(),
                        discovery.getName(),
                        discovery.getOpacity(),
                        targetUri,
                        discovery.getType());
            } else {
                /* why would you not want to always auto discover layers?  hmm
                 * well, so you can get control over the flags do the discover
                 * call for now, since autodiscover supports fixed versions
                 * as well.  Will review this in v2...
                 */
                mapLayer = remoteMap.discover(discovery, false, false, false);
            }

            if (mapLayer != null) {
                portalSession.addMapLayer(mapLayer);
            } else {
                logger.info("not adding discovery '" + discovery.getId() +
                        "' because couldn't find any layers to display");
            }
        } else {
            logger.info(
                    "skipping discovery from " + discovery.getId() + " at "
                    + uriResolver.resolve(discovery) + " because it is disabled in configuration file");
        }
    }

    /**
     * process the base layers
     * @param service
     * @return
     */
    private void processBaseLayer(BaseLayer baseLayer, PortalSession portalSession) {
        if (!baseLayer.getDisabled()) {
            MapLayer mapLayer = remoteMap.baseLayer(baseLayer);
            if (mapLayer != null) {
                portalSession.addBaseLayer(mapLayer);
            }
        } else {
            logger.info(
                    "skipping loading service " + baseLayer.getId() + " at "
                    + uriResolver.resolve(baseLayer) + " because it is disabled in configuration file");
        }
    }

    private void processService(Service service, PortalSession portalSession) {
        if (!service.getDisabled()) {
            MapLayer mapLayer = remoteMap.service(service);
            if (mapLayer != null) {
                mapLayer.setBaseLayer(false);
                portalSession.addMapLayer(mapLayer);
            }
        } else {
            logger.info(
                    "skipping loading service " + service.getId() + " at "
                    + uriResolver.resolve(service) + " because it is disabled in configuration file");
        }
    }

    private void dataSource() {
        logger.debug("dataSource...");

        // discovery
        for (Discovery discovery :
                portalDocument.getPortal().getDataSource().getDiscoveries().getDiscoveryList()) {
            processDiscovery(discovery, workingPortalSession);
        }

        // service
        for (Service service :
                portalDocument.getPortal().getDataSource().getServices().getServiceList()) {
            processService(service, workingPortalSession);
        }

        // baselayer
        for (BaseLayer baseLayer :
                portalDocument.getPortal().getDataSource().getBaseLayers().getBaseLayerList()) {
            processBaseLayer(baseLayer, workingPortalSession);
        }


        // default baselayer
        String baseLayerId = portalDocument.getPortal().getDataSource().getBaseLayers().getDefaultBaseLayerIdRef();
        MapLayer baseLayer = portalSessionUtilities.getBaseLayerById(workingPortalSession, baseLayerId);
        if (baseLayer != null) {
            workingPortalSession.setCurrentBaseLayer(baseLayer);
            logger.debug("BASELAYER: " + baseLayerId);
        } else {
            throw new NullPointerException(
                    "Requested baselayer " + baseLayerId
                    + " not found - cannot display portal");
        }

        // static links
        for (StaticLink staticLink :
                portalDocument.getPortal().getDataSource().getStaticLinks().getStaticLinkList()) {
            processStaticLink(staticLink, workingPortalSession);
        }

        /* Check the datasource configuration is valid before returning.
         * we want at least one base layer and at least one map layer
         */
        List<String> errorMessages = new ArrayList<String>();
        if (workingPortalSession.getMapLayers().size() == 0) {
            errorMessages.add(
                    "No valid map layers or they have all been disabled"
                    + " - check dataSource declaration in config file");
        }
        if (workingPortalSession.getBaseLayers().size() == 0) {
            logger.fatal(
                    "No valid base layers or they have all been disabled"
                    + " - check dataSource declaration in config file");
        }

// don't like the exception handling here - commented out so I can check how this
// how to handle this normally
//        if (errorMessages.size() > 0) {
//            throw new PortalException(
//                    "Invalid datasource configuration",
//                    errorMessages);
//        }
    }

    /**
     * copy the static link information from the xmlbean into a POJO.  Why not just
     * keep the xmlbean and use it in our application?  Because to do so is highly
     * inefficient in terms of memory usage
     * @param staticLink
     * @param portalSession
     */
    private void processStaticLink(StaticLink staticLink, PortalSession portalSession) {
        if (!staticLink.getDisabled()) {
            Link link = new Link();
            link.copyFrom(staticLink);
            portalSession.addLink(link);
        } else {
            logger.info(
                    "skipping static link declaration " + staticLink.getId()
                    + " for " + staticLink.getUri() + " because it is disabled");
        }
    }

    private void loadMestAccountManagers() {
        Map<String, MestWebServiceParameters> services = new HashMap<String, MestWebServiceParameters>();
        List<MestAccountManager> xmlManagers = portalDocument.getPortal().getUserAccount().getAccountManagers().getMestAccountManagerList();
        for (MestAccountManager xmlManager : xmlManagers) {
            String id = xmlManager.getId();
            if (xmlManager.getDisabled()) {
                logger.info("skipping MEST account manager " + id + " because it is disabled");
            } else {
                // FIXME should probabably be created using spring method injection... and in another class...
                MestWebServiceParameters parameters = new MestWebServiceParametersImpl();

                try {
                    // set simple string properties
                    parameters.setAdministratorProfile(xmlManager.getAdministratorProfile());
                    parameters.setEmailTemplatePasswordChanged(xmlManager.getEmailTemplatePasswordChanged());
                    parameters.setEmailTemplateRegistration(xmlManager.getEmailTemplateRegistration());
                    parameters.setEmailTemplateResetPassword(xmlManager.getEmailTemplateResetPassword());
                    parameters.setNewUserProfile(xmlManager.getNewUserProfile());
                    parameters.setPassword(xmlManager.getPassword());
                    parameters.setUsername(xmlManager.getUsername());

                    // resolve the base uri
                    parameters.setServiceBasePath(uriResolver.resolve(xmlManager));

                    // give reference to the correct mest configuration
                    parameters.setMestConfiguration(
                            settings.getMestConfigurations().get(
                            xmlManager.getMestConfigurationIdRef()));

                    services.put(id, parameters);
                    logger.info("loaded MEST account manager " + id);
                } catch (NullPointerException e) {
                    logger.warn(
                            "error loading account manager" + xmlManager.getId() + " missing data in config file"
                            + "or mestConfiguration " + xmlManager.getMestConfigurationIdRef() + " does not exist or "
                            + "is disabled.  Root cause:" + e.getMessage());
                }
            }
        }
        mestServiceFactory.setServices(services);
    }

    private void loadMasterServiceFactory() {
        Map<String, ServiceFactory> factories = new HashMap<String, ServiceFactory>();
        UserAccount xmlUserAccount = portalDocument.getPortal().getUserAccount();

        // lookup the Account Manager entries that the user specified should handle
        // each supported action.
        String portalUserAccountManagerId = xmlUserAccount.getPortalUserAccountManagerIdRef();
        String adminConsoleAccountManagerId = xmlUserAccount.getAdminConsoleAccountManagerIdRef();
        String forgottenPasswordAccountManagerId = xmlUserAccount.getForgottenPasswordAccountManagerIdRef();
        String registrationAccountManagerId = xmlUserAccount.getRegistrationAccountManagerIdRef();
        String userInfoAccountManagerId = xmlUserAccount.getUserInfoAccountManagerIdRef();

        // set the key names to lookup in the master service factory -
        // these were what the user had configured in the config file
        masterServiceFactoryConfigurer.setAdminConsoleAccountMangerId(adminConsoleAccountManagerId);
        masterServiceFactoryConfigurer.setForgottenPasswordAccountManagerId(forgottenPasswordAccountManagerId);
        masterServiceFactoryConfigurer.setPortalUserAccountManagerId(portalUserAccountManagerId);
        masterServiceFactoryConfigurer.setRegistrationAccountManagerId(registrationAccountManagerId);
        masterServiceFactoryConfigurer.setUserInfoAccountManagerId(userInfoAccountManagerId);

        // now map the corresponding factory that handles each of the above requests
        // this is so that we can find the class we need to process the request
        setupServiceFactory(factories, portalUserAccountManagerId);
        setupServiceFactory(factories, adminConsoleAccountManagerId);
        setupServiceFactory(factories, forgottenPasswordAccountManagerId);
        setupServiceFactory(factories, registrationAccountManagerId);
        setupServiceFactory(factories, userInfoAccountManagerId);


        masterServiceFactoryConfigurer.setFactories(factories);
    }

    private void setupServiceFactory(Map<String, ServiceFactory> factories, String id) {
        ServiceFactory found = null;

        // now we look for the id in the mest factories and and then the properties factories
        // not particularly pretty but should be safe - the schema enforces the constraint
        // that all IDs must be unique accross the config file
        List<MestAccountManager> mestManagers = portalDocument.getPortal().getUserAccount().getAccountManagers().getMestAccountManagerList();
        List<PropertiesAccountManager> propertiesManagers = portalDocument.getPortal().getUserAccount().getAccountManagers().getPropertiesAccountManagerList();

        // todo - this could be made more efficiant but probably not worth
        // bothering with

        // mest
        for (MestAccountManager mestAccountManager : mestManagers) {
            if (mestAccountManager.getId().equals(id)) {
                found = mestServiceFactory;
            }
        }

        // properties
        for (PropertiesAccountManager propertiesAccountManager : propertiesManagers) {
            if (propertiesAccountManager.getId().equals(id)) {
                found = propertiesServiceFactory;
            }
        }


        if (found == null) {
            logger.error("Requested Account Manager " + id + " not found in configuration file or is disabled");
        } else {
            factories.put(id, found);
            logger.info("Master Service Factory: " + id + " ==> " + found.getClass().getName());
        }
    }

    private void loadPropertiesAccountManagers() {
        Map<String, String> services = new HashMap<String, String>();
        List<PropertiesAccountManager> xmlManagers = portalDocument.getPortal().getUserAccount().getAccountManagers().getPropertiesAccountManagerList();
        for (PropertiesAccountManager xmlManager : xmlManagers) {
            if (xmlManager.getDisabled()) {
                logger.info("skipping properties account manager " + xmlManager.getId() + " because it is disabled");
            } else {
                File file = new File(xmlManager.getFilename());
                if (file.exists() && file.canRead()) {
                    logger.info("loading Properties Account Manager with properties file " + file.getAbsolutePath());
                    services.put(xmlManager.getId(), file.getAbsolutePath());
                } else {
                    logger.warn("file " + file.getAbsolutePath() + " (for user logins) does not exist or is not readable - ignoring");
                }
            }
        }
        propertiesServiceFactory.setServiceMap(services);
    }

    private void userAccount() {
        logger.debug("userAccount...");
        UserAccount xmlUserAccount = portalDocument.getPortal().getUserAccount();
        if (xmlUserAccount.getPortalUsersDisabled()) {
            logger.info("Disabling user account feature because it is disabled");
        }
        settings.setDisablePortalUsers(xmlUserAccount.getPortalUsersDisabled());

        if (xmlUserAccount.getAdminConsoleDisabled()) {   
            logger.info("Disabling admin console feature because it is disabled");
        }
        settings.setDisableAdminConsole(xmlUserAccount.getAdminConsoleDisabled());

        loadMestAccountManagers();
        loadPropertiesAccountManagers();
        loadMasterServiceFactory();

    }

    /**
     * Remove completely any layers which have been blacklisted
     */
    private void blacklist() {

        for (LayerIdentifiers.Layer layerIdentifier :
                portalDocument.getPortal().getDataSource().getBlacklist().getLayerList()) {
            String id = layerIdentifier.getIdRef();
            String layer = layerIdentifier.getLayer();

            logger.info("BLACKLISTING LAYER: " + id + "::" + layer);

            MapLayer item = portalSessionUtilities.getMapLayerByIdAndLayer(workingPortalSession, id, layer);
            if (item != null) {
                if (item.getParent() != null) {
                    // map layers with parents are stored within
                    // other map layers
                    item.getParent().getChildren().remove(item);
                } else {
                    // map layers without parents are stored in
                    // the big list of layers
                    workingPortalSession.getMapLayers().remove(item);
                }
            } else {
                logger.info(
                        "layer referenced by blacklist '" + id + "::" + layer + "' was "
                        + "not blacklisted because the layer isn't loaded'");
            }
        }

    }

    /**
     * Return a list of the layers to be activated by default
     */
    private void activeByDefault() {

        /* which layers (if any) need to be active by default - layers which
         * are to be activated are added to the static field activeByDefault
         * which gets accessed from the SessionInit class
         */
        for (LayerIdentifiers.Layer layerIdentifier :
                portalDocument.getPortal().getDataSource().getActiveByDefault().getLayerList()) {
            String id = layerIdentifier.getIdRef();
            logger.info("ACTIVATE BY DEFAULT: " + id);
            MapLayer mapLayer = portalSessionUtilities.getMapLayerById(workingPortalSession, id);
            if (mapLayer != null) {
                // activate the mapLayer by putting it in activeLayers
                // and telling the system its been activated
                mapLayer.setListedInActiveLayers(true);

                // 'trick' the system into displaying the map layer
                mapLayer.setDisplayed(true);

                workingPortalSession.getActiveLayers().add(mapLayer);
            } else {
                logger.warn(
                        "Skipping activation of " + id + " specified in activeByDefault "
                        + "because it is not a valid layer - check config file and/or discovery output");
            }
        }
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

    public SearchCatalogueFactory getSearchCatalogueFactory() {
        return searchCatalogueFactory;
    }

    @Required
    public void setSearchCatalogueFactory(SearchCatalogueFactory searchCatalogueFactory) {
        this.searchCatalogueFactory = searchCatalogueFactory;
    }

    public LayerGroupFactory getLayerGroupFactory() {
        return layerGroupFactory;
    }

    @Required
    public void setLayerGroupFactory(LayerGroupFactory layerGroupFactory) {
        this.layerGroupFactory = layerGroupFactory;
    }

    public PortalSessionUtilities getPortalSessionUtilities() {
        return portalSessionUtilities;
    }

    @Required
    public void setPortalSessionUtilities(PortalSessionUtilities portalSessionUtilities) {
        this.portalSessionUtilities = portalSessionUtilities;
    }

    public MasterServiceFactoryConfigurer getMasterServiceFactoryConfigurer() {
        return masterServiceFactoryConfigurer;
    }

    @Required
    public void setMasterServiceFactoryConfigurer(MasterServiceFactoryConfigurer masterServiceFactoryConfigurer) {
        this.masterServiceFactoryConfigurer = masterServiceFactoryConfigurer;
    }


    public MestServiceFactory getMestServiceFactory() {
        return mestServiceFactory;
    }

    @Required
    public void setMestServiceFactory(MestServiceFactory mestServiceFactory) {
        this.mestServiceFactory = mestServiceFactory;
    }

    public PropertiesServiceFactory getPropertiesServiceFactory() {
        return propertiesServiceFactory;
    }

    @Required
    public void setPropertiesServiceFactory(PropertiesServiceFactory propertiesServiceFactory) {
        this.propertiesServiceFactory = propertiesServiceFactory;
    }

    public MestConfigurationFactory getMestConfigurationFactory() {
        return mestConfigurationFactory;
    }

    @Required
    public void setMestConfigurationFactory(MestConfigurationFactory mestConfigurationFactory) {
        this.mestConfigurationFactory = mestConfigurationFactory;
    }

    public MestSearchKeywords getMestSearchKeywords() {
        return mestSearchKeywords;
    }

    @Required
    public void setMestSearchKeywords(MestSearchKeywords mestSearchKeywords) {
        this.mestSearchKeywords = mestSearchKeywords;
    }

    @Override
    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }
    
}
