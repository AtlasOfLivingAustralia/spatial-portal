/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import au.org.emii.portal.util.UriResolver;
import au.org.emii.portal.BoundingBox;
import au.org.emii.portal.Facility;
import au.org.emii.portal.HttpAuthenticateProxy;
import au.org.emii.portal.LayerUtilities;
import au.org.emii.portal.Link;
import au.org.emii.portal.mest.MestSearchKeywords;
import au.org.emii.portal.MapLayer;
import au.org.emii.portal.PortalException;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.Region;
import au.org.emii.portal.RemoteMap;
import au.org.emii.portal.SearchCatalogueFactory;
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
import au.org.emii.portal.mest.MestConfigurationFactory;
import au.org.emii.portal.mest.webservice.MestWebServiceParameters;
import au.org.emii.portal.util.LayerGroupFactory;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.mest.webservice.MestWebServiceParametersImpl;
import au.org.emii.portal.service.MasterServiceFactoryImpl;
import au.org.emii.portal.service.MestServiceFactory;
import au.org.emii.portal.service.PropertiesServiceFactory;
import au.org.emii.portal.service.ServiceFactory;
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
public class ConfigurationLoaderStage2 {

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
    private PortalSession portalSession = null;

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

    private MasterServiceFactoryImpl masterServiceFactory = null;

    private MestConfigurationFactory mestConfigurationFactory = null;

    /**
     * Error occurred during loading (flag)
     */
    private boolean error = false;

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
    public void load() {
        // note - order is different to natural xml order due to dependencies
        // between stages, eg menu depends on datasource depends on uriresolver, etc

        uriResolver();
        proxyHack();

        settings();
        settingsSupplementary();
        

        // note - menu depends on datasources being loaded first!
        dataSource();
        blacklist();

        menu();


        search();
        
        userAccount();

        finaliseSession();

        cleanup();

    }

    /**
     * Miscellaneous session settings
     */
    private void finaliseSession() {

        // set the initial bounding box in the master session
        portalSession.setDefaultBoundingbox(settings.getDefaultBoundingBox());


        activeByDefault();
    }

    /**
     * Cleanup any resources we are holding
     */
    private void cleanup() {
        portalDocument = null;
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
            settings.setDefaultSelection(xmlSettings.getDefaultSelection().toString());
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
                logger.info("loaded MEST configuration: " +  mestConfiguration.getId());
            } else {
                logger.debug("skipped loading mest configuration " + mestConfiguration.getId() +
                        " - factory returned null but previous error should explain reason.");
            }
        }
       return mestConfigurations;
    }


    private void menu() {
        logger.debug("menu...");
        Menu xmlMenu = getPortalDocument().getPortal().getMenu();

        if (xmlMenu.getDisableLayersMenu()) {
            logger.info("Disabling layers menu because it is disabled in config file");
            portalSession.setLayersDisabled(true);
        }

        if (xmlMenu.getDisableLinksMenu()) {
            logger.info("disabling links menu because it is disabled in config file");
            portalSession.setLinksDisabled(true);
        }

        if (xmlMenu.getDisableUserDefined()) {
            logger.info("disabling user defined menu because it is disabled in config file");
            portalSession.setUserDefinedDisabled(true);
        }

        // facilities
        if (xmlMenu.getFacilities().getDisabled()) {
            logger.info("not loading any facilities because they are disabled in config file");
        } else {
            for (LayerGroup configuredFacility : xmlMenu.getFacilities().getFacilityList()) {
                Facility facility = processFacilityOrRegion(configuredFacility, portalSession, Facility.class);
                if (facility != null) {
                    portalSession.addFacility(facility);
                }
            }
        }

        // regions
        if (xmlMenu.getRegions().getDisabled()) {
            logger.info("not loading any regions because they are disabled in config file");
        }
        for (RegionLayerGroup configuredRegion : xmlMenu.getRegions().getRegionList()) {
            Region region = processFacilityOrRegion(configuredRegion, portalSession, Region.class);
            if (region != null) {
                portalSession.addRegion(region);
            }
        }

        // realtime
        if (xmlMenu.getRealtimes().getDisabled()) {
            logger.info("not loading any realtimes because they are disabled in config file");
        }
        for (LayerGroup configuredFacility : xmlMenu.getRealtimes().getRealtimeList()) {

            Facility facility = processFacilityOrRegion(configuredFacility, portalSession, Facility.class);
            if (facility != null) {
                portalSession.addRealtime(facility);
            }
        }

        // top right menu links
        if (xmlMenu.getTopRightLinks().getDisabled()) {
            logger.info("Top right links menu is disabled in config file so will not be loaded");
        } else {
            for (String staticLinkId : xmlMenu.getTopRightLinks().getStaticLinkIdRefList()) {
                Link link = portalSessionUtilities.getLinkById(portalSession,staticLinkId);
                if (link != null) {
                    portalSession.addStaticMenuLink(link);
                } else {
                    logger.debug(
                            "Link " + staticLinkId + " referenced in settings will "
                            + "not be displayed because there is no matching staticLink"
                            + "declaration in the dataSoure section or it is disabled - "
                            + "check config file");
                }
            }
        }

    }

    private <T extends Facility> T processFacilityOrRegion(LayerGroup configured, PortalSession portalSession, Class<T> clazz) {
        T facilityOrRegion = null;
        if (!configured.getDisabled()) {
            facilityOrRegion = layerGroupFactory.createInstance(clazz, configured, portalSession);
        } else {
            logger.info(
                    "skipped facility or region " + configured.getId()
                    + " Known as " + configured.getName() + "because it is disabled");
        }
        return facilityOrRegion;
    }

    private void uriResolver() {
        logger.debug("uriResolver...");
        uriResolver.clear();
        List<Mapping> mappings = portalDocument.getPortal().getUriResolver().getMappingList();
        List<UriEndPoints.UriEndPoint> endpoints = portalDocument.getPortal().getUriResolver().getUriEndPoints().getUriEndPointList();

        Map<String,String> endPointMap = new HashMap<String, String>();

        // build a list of valid endpoint URIs
        for (UriEndPoints.UriEndPoint endPoint : endpoints) {
            if (endPoint.getDisabled()) {
                logger.info("Disabling URI endpoint " + endPoint.getId() + " because it is disabled");
            } else {
                endPointMap.put(endPoint.getId(), endPoint.getUri());
            }
        }

        // now map these to the requested uriEndPointIdRef ids in the mappings
        for (Mapping mapping: mappings) {
            String uriEndPointIdRef = mapping.getUriEndPointIdRef();
            String uriId = mapping.getUriId();
            String targetUri = endPointMap.get(uriEndPointIdRef);

            if (targetUri == null) {
                logger.error(
                        "Requested mapping to uriEndPointIdRef " + uriEndPointIdRef +
                        " failed because the uriEndPoint does not exist or is disabled");
            } else {
                logger.info(String.format("enabling mapping '%s' ==> '%s'", uriId, targetUri));
                uriResolver.put(uriId, targetUri);
            }
        }
    }

    private void search() {
        logger.debug("search...");
        if (portalDocument.getPortal().getSearch().getDisabled()) {
            logger.info("search system disabled in config file");
            portalSession.setSearchDisabled(true);
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

                    au.org.emii.portal.SearchCatalogue searchCatalogue = searchCatalogueFactory.createInstance(xmlSearchCatalogue);
                    if (searchCatalogue == null) {
                        logger.info("Error creating search catalogue id '" + xmlSearchCatalogue.getId() + "' (skipping)");
                    } else {
                        portalSession.addSearchCatalogue(searchCatalogue);
                        logger.debug("SEARCHCATALOGUE + " + searchCatalogue.getId());

                        // lookup supported search terms and store in session
                        searchCatalogue.setSearchKeywords(mestSearchKeywords.getSearchTerms(searchCatalogue));

                        /* select the default search catalogue from config
                         * file settings
                         */
                        if (searchCatalogue.getId().equals(defaultSearchCatalogueId)) {
                            portalSession.setSelectedSearchCatalogue(searchCatalogue);
                            defaultSearchCatalogueFound = true;
                        }
                    }
                }
            }
            if (!defaultSearchCatalogueFound) {
               portalSession.setSearchDisabled(true);
               logger.error(
                    "Default search catalogue " + defaultSearchCatalogueId
                    + " requested in configuration file settings section "
                    + "is not available - check it exists and is not disabled.  " +
                    "To prevent your users receiving error messages, the search" +
                    "system has been automatically disabled");
            }
        }
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
            processDiscovery(discovery, portalSession);
        }

        // service
        for (Service service :
                portalDocument.getPortal().getDataSource().getServices().getServiceList()) {
            processService(service, portalSession);
        }

        // baselayer
        for (BaseLayer baseLayer :
                portalDocument.getPortal().getDataSource().getBaseLayers().getBaseLayerList()) {
            processBaseLayer(baseLayer, portalSession);
        }


        // default baselayer
        String baseLayerId = portalDocument.getPortal().getDataSource().getBaseLayers().getDefaultBaseLayerIdRef();
        MapLayer baseLayer = portalSessionUtilities.getBaseLayerById(portalSession,baseLayerId);
        if (baseLayer != null) {
            portalSession.setCurrentBaseLayer(baseLayer);
            logger.debug("BASELAYER: " + baseLayerId);
        } else {
            throw new PortalException(
                    "Requested baselayer " + baseLayerId
                    + " not found - cannot display portal");
        }

        // static links
        for (StaticLink staticLink :
                portalDocument.getPortal().getDataSource().getStaticLinks().getStaticLinkList()) {
            processStaticLink(staticLink, portalSession);
        }

        /* Check the datasource configuration is valid before returning.
         * we want at least one base layer and at least one map layer
         */
        List<String> errorMessages = new ArrayList<String>();
        if (portalSession.getMapLayers().size() == 0) {
            errorMessages.add(
                    "No valid map layers or they have all been disabled"
                    + " - check dataSource declaration in config file");
        }
        if (portalSession.getBaseLayers().size() == 0) {
            logger.fatal(
                    "No valid base layers or they have all been disabled"
                    + " - check dataSource declaration in config file");
        }

        if (errorMessages.size() > 0) {
            throw new PortalException(
                    "Invalid datasource configuration",
                    errorMessages);
        }
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
                            "error loading account manager" + xmlManager.getId() + " missing data in config file" +
                            "or mestConfiguration " + xmlManager.getMestConfigurationIdRef() + " does not exist or " +
                            "is disabled.  Root cause:" + e.getMessage());
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
        masterServiceFactory.setAdminConsoleAccountMangerId(adminConsoleAccountManagerId);
        masterServiceFactory.setForgottenPasswordAccountManagerId(forgottenPasswordAccountManagerId);
        masterServiceFactory.setPortalUserAccountManagerId(portalUserAccountManagerId);
        masterServiceFactory.setRegistrationAccountManagerId(registrationAccountManagerId);
        masterServiceFactory.setUserInfoAccountManagerId(userInfoAccountManagerId);

        // now map the corresponding factory that handles each of the above requests
        // this is so that we can find the class we need to process the request
        setupServiceFactory(factories, portalUserAccountManagerId);
        setupServiceFactory(factories, adminConsoleAccountManagerId);
        setupServiceFactory(factories, forgottenPasswordAccountManagerId);
        setupServiceFactory(factories, registrationAccountManagerId);
        setupServiceFactory(factories, userInfoAccountManagerId);


        masterServiceFactory.setFactories(factories);
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
                String filename = xmlManager.getFilename();
                if (xmlManager.getClasspathResource()) {
                    // need to resolve filename
                    URL url = this.getClass().getResource(filename);
                    if (url == null) {
                        logger.error("unable to find file: '" + filename + "' on the classpath");
                        filename = null;
                    } else {
                        filename = url.getFile();
                    }
                }
                logger.info("loading Properties Account Manager with properties file " + filename);
                services.put(xmlManager.getId(), filename);
            }
        }
        propertiesServiceFactory.setServiceMap(services);
    }

    private void userAccount() {
        logger.debug("userAccount...");
        UserAccount xmlUserAccount = portalDocument.getPortal().getUserAccount();
        if (xmlUserAccount.getPortalUsersDisabled()) {
            settings.setPortalUsersDisabled(true);
            logger.info("Disabling user account feature because it is disabled");
        }

        if (xmlUserAccount.getAdminConsoleDisabled()) {
            settings.setAdminConsoleDisabled(true);
            logger.info("Disabling admin console feature because it is disabled");
        }

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

            MapLayer item = portalSessionUtilities.getMapLayerByIdAndLayer(portalSession, id, layer);
            if (item != null) {
                if (item.getParent() != null) {
                    // map layers with parents are stored within
                    // other map layers
                    item.getParent().getChildren().remove(item);
                } else {
                    // map layers without parents are stored in
                    // the big list of layers
                    portalSession.getMapLayers().remove(item);
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
            MapLayer mapLayer = portalSessionUtilities.getMapLayerById(portalSession, id);
            if (mapLayer != null) {
                // activate the mapLayer by putting it in activeLayers
                // and telling the system its been activated
                mapLayer.setListedInActiveLayers(true);

                // 'trick' the system into displaying the map layer
                mapLayer.setDisplayed(true);

                portalSession.getActiveLayers().add(mapLayer);
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

    public PortalDocument getPortalDocument() {
        return portalDocument;
    }

    public void setPortalDocument(PortalDocument portalDocument) {
        this.portalDocument = portalDocument;
    }

    public PortalSession getPortalSession() {
        return portalSession;
    }

    public void setPortalSession(PortalSession portalSession) {
        this.portalSession = portalSession;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

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

    public MasterServiceFactoryImpl getMasterServiceFactory() {
        return masterServiceFactory;
    }

    @Required
    public void setMasterServiceFactory(MasterServiceFactoryImpl masterServiceFactory) {
        this.masterServiceFactory = masterServiceFactory;
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



}
