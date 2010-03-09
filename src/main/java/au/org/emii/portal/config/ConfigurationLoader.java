//package au.org.emii.portal.config;
//
//import au.org.emii.portal.Facility;
//import au.org.emii.portal.HttpAuthenticateProxy;
//import au.org.emii.portal.LayerUtilities;
//import au.org.emii.portal.Link;
//import au.org.emii.portal.mest.MESTSupport;
//import au.org.emii.portal.MapLayer;
//import au.org.emii.portal.PortalException;
//import au.org.emii.portal.PortalSession;
//import au.org.emii.portal.Region;
//import au.org.emii.portal.RemoteMap;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.UnsupportedEncodingException;
//import java.net.Authenticator;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.logging.Level;
//import javax.servlet.ServletContext;
//import org.apache.log4j.Logger;
//import org.apache.xmlbeans.XmlCursor;
//import org.apache.xmlbeans.XmlException;
//import org.springframework.context.ApplicationContext;
//import au.org.emii.portal.config.xmlbeans.PortalDocument;
//import au.org.emii.portal.config.xmlbeans.RegionLayerGroup;
//import au.org.emii.portal.config.xmlbeans.LayerGroup;
//import au.org.emii.portal.config.xmlbeans.SearchCatalogue;
//import au.org.emii.portal.config.xmlbeans.BaseLayer;
//import au.org.emii.portal.config.xmlbeans.Discovery;
//import au.org.emii.portal.config.xmlbeans.LayerIdentifier;
//import au.org.emii.portal.config.xmlbeans.LayerIdentifiers;
//import au.org.emii.portal.config.xmlbeans.Configuration;
//import au.org.emii.portal.config.xmlbeans.Service;
//import au.org.emii.portal.config.xmlbeans.StaticLink;
//import au.org.emii.portal.config.xmlbeans.Supplementary;
//import au.org.emii.portal.web.PortalSessionAccessor;
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import org.apache.commons.io.FileUtils;
//
//
///**
// * Process the xml configuration file
// * @author geoff
// *
// */
//public class ConfigurationLoader implements Runnable {
//
//
//    /**
//     * Keep a reference to the servlet context so we can publish generated
//     * objects as application scope attributes
//     */
//    private ServletContext servletContext = null;
//
//    /**
//     *  index of the ID value in the activeByDefault array
//     */
//    public final static int ID = 0;
//    /**
//     * index of the layer value in the activeByDefault array
//     */
//    public final static int LAYER = 1;
//
//    /**
//     * Helper class for parsing output from different remote map servers
//     */
//    private RemoteMap remoteMap = new RemoteMap();
//
//
//
//    /**
//     * Your not allowed to instantiate without giving us a session context
//     */
//    @SuppressWarnings("unused")
//    private ConfigurationLoader() {
//    }
//
//    /**
//     * A ServletContext reference is required at all times to enable us
//     * to set application scope variables
//     * @param servletContext
//     */
//    public ConfigurationLoader(ServletContext servletContext) {
//        this.servletContext = servletContext;
//    }
//
//
//
//
//
//    /**
//     * Load the configuration and process all directives
//     *
//     * XPATHS to Main sections of the config file:
//     *	/portal
//     *		configuration
//     *		settings
//     *  	facilities
//     *  		facility
//     *  			...
//     *  			menu
//     *    	regions
//     *  		region
//     *  			...
//     *  			menu
//     *		dataSource
//     *			discoveries
//     *			services
//     *			baseLayers
//     *			staticLinks
//     *			blacklist
//     *  	searchCatalogues
//     *
//     */
//    private void load() {
//        logger.info("(re)loading configuration file begins...");
//        PortalSession portalSession = null;
//        PortalDocument portalDocument = readConfigFile();
//        if (portalDocument != null) {
//            try {
//                /* First thing to do is load the <settings> section as items
//                 * from here are required to process the rest of the file correctly
//                 */
//                logger.debug("settings...");
//                settings(portalDocument, portalSession);
//
//                /* always make a new PortalSession instance - don't ask spring for
//                 * one, otherwise you will probably get one back that's already been
//                 * setup
//                 */
//                portalSession = new PortalSession();
//
//                logger.debug("datasource...");
//                dataSource(portalDocument, portalSession);
//
//                logger.debug("facilities and regions...");
//                facilitiesAndRegions(portalDocument, portalSession);
//
//                logger.debug("search catalogues...");
//                searchCatalogues(portalDocument, portalSession);
//
//                logger.debug("static links...");
//                staticLinksMenu(portalDocument, portalSession);
//
//                /* Now we need to store an application scope variable storing the
//                 * relevant details from the config file
//                 *
//                 *  Because its stored as method scope variables and published, we
//                 *  don't need to worry about concurrent modification.
//                 *
//                 *  Also give a copy of servlet context to PortalSessionAccessor
//                 *  so that it can be accessed via spring
//                 */
//                servletContext.setAttribute("masterPortalSession", portalSession);
//                PortalSessionAccessor.setServletContext(servletContext);
//                logger.debug(
//                        "finished building portalSession - final structure is: \n" + portalSession.dump());
//            } catch (PortalException exception) {
//                // This is our own hand made exception so we're not interested in
//                // the stack trace
//                logger.fatal(
//                        "Fatal error(s) occurred while loading configuration file: "
//                        + exception.getMessage());
//            }
//        } else {
//            logger.fatal("error encountered reading config file - can't setup application");
//        }
//    }
//
//    private void staticLinksMenu(PortalDocument portalDocument, PortalSession portalSession) {
//        /*
//         *
//         * FIXME!!
//         *
//        // static menu links
//        if (portalDocument.getPortal().getStaticLinksMenu().getDisabled()) {
//            logger.info("Static links menu is disabled in config file so will not be loaded");
//        } else {
//            for (String staticLinkId :
//                    portalDocument.getPortal().getStaticLinksMenu().getStaticLinkIdRefList()) {
//                Link link = portalSession.getLinkById(staticLinkId);
//                if (link != null) {
//                    portalSession.addStaticMenuLink(link);
//                } else {
//                    logger.debug(
//                            "Link " + staticLinkId + " referenced in settings will "
//                            + "not be displayed because there is no matching staticLink"
//                            + "declaration in the dataSoure section or it is disabled - "
//                            + "check config file");
//                }
//            }
//        }
//         */
//    }
//
//    /**
//     * Find and process the data sources (discoveries, services and baselayers)
//     * @param portalDocument
//     */
//    private void dataSource(PortalDocument portalDocument, PortalSession portalSession) {
//        // discovery
//        for (Discovery discovery :
//                portalDocument.getPortal().getDataSource().getDiscoveries().getDiscoveryList()) {
//            processDiscovery(discovery, portalSession);
//        }
//
//        // service
//        for (Service service :
//                portalDocument.getPortal().getDataSource().getServices().getServiceList()) {
//            processService(service, portalSession);
//        }
//
//        // baselayer
//        for (BaseLayer baseLayer :
//                portalDocument.getPortal().getDataSource().getBaseLayers().getBaseLayerList()) {
//            processBaseLayer(baseLayer, portalSession);
//        }
//
//
//        // default baselayer
//        String baseLayerId =
//                portalDocument.getPortal().getDataSource().getBaseLayers().getDefaultBaseLayerIdRef();
//        MapLayer baseLayer = portalSession.getBaseLayerById(baseLayerId);
//        if (baseLayer != null) {
//            portalSession.setCurrentBaseLayer(baseLayer);
//            logger.debug("BASELAYER: " + baseLayerId);
//        } else {
//            throw new PortalException(
//                    "Requested baselayer " + baseLayerId
//                    + " not found - cannot display portal");
//        }
//
//        // now check the services and discoveries against the blacklist -
//        // baselayers are not checked - use the disabled flag to stop
//        // to stop individual layers appearing
//        /*
//         *
//         * FIXME!!
//         *
//         *
//        blacklist(
//                portalDocument.getPortal().getSettings().getBlacklist(),
//                portalSession);
//*/
//        // static links
//        for (StaticLink staticLink :
//                portalDocument.getPortal().getDataSource().getStaticLinks().getStaticLinkList()) {
//            processStaticLink(staticLink, portalSession);
//        }
//
//        /* Check the datasource configuration is valid before returning.
//         * we want at least one base layer and at least one map layer
//         */
//        List<String> errorMessages = new ArrayList<String>();
//        if (portalSession.getMapLayers().size() == 0) {
//            errorMessages.add(
//                    "No valid map layers or they have all been disabled"
//                    + " - check dataSource declaration in config file");
//        }
//        if (portalSession.getBaseLayers().size() == 0) {
//            logger.fatal(
//                    "No valid base layers or they have all been disabled"
//                    + " - check dataSource declaration in config file");
//        }
//
//        if (errorMessages.size() > 0) {
//            throw new PortalException(
//                    "Invalid datasource configuration",
//                    errorMessages);
//        }
//    }
//
//    /**
//     * Remove completely any layers which have been blacklisted
//     */
//    /*
//     * FIXME!
//     *
//    private void blacklist(LayerIdentifiers blackList, PortalSession portalSession) {
//
//        if (blackList != null) {
//            for (LayerIdentifier layerIdentifier :
//                    blackList.getLayerList()) {
//                String id = layerIdentifier.getIdRef();
//                String layer = layerIdentifier.getLayer();
//
//                logger.info("BLACKLISTING LAYER: " + id + "::" + layer);
//
//                MapLayer item = portalSession.getMapLayerByIdAndLayer(id, layer);
//                if (item != null) {
//                    if (item.getParent() != null) {
//                        // map layers with parents are stored within
//                        // other map layers
//                        item.getParent().getChildren().remove(item);
//                    } else {
//                        // map layers without parents are stored in
//                        // the big list of layers
//                        portalSession.getMapLayers().remove(item);
//                    }
//                } else {
//                    logger.info(
//                            "layer referenced by blacklist '" + id + "::" + layer + "' was "
//                            + "not blacklisted because the layer isn't loaded'");
//                }
//            }
//        }
//
//    }
//     *
//     */
//
//    /**
//     * Return a list of the layers to be activated by default
//     */
//    private void activeByDefault(PortalDocument portalDocument, PortalSession portalSession) {
//
//        /* which layers (if any) need to be active by default - layers which
//         * are to be activated are added to the static field activeByDefault
//         * which gets accessed from the SessionInit class
//         */
//
//        /**
//         * FIXME!!
//         *
//        for (LayerIdentifier layerIdentifier :
//                portalDocument.getPortal().getSettings().getActiveByDefault().getLayerList()) {
//            String id = layerIdentifier.getIdRef();
//            logger.info("ACTIVATE BY DEFAULT: " + id);
//            MapLayer mapLayer = portalSession.getMapLayerById(id);
//            if (mapLayer != null) {
//                // activate the mapLayer by putting it in activeLayers
//                // and telling the system its been activated
//                mapLayer.setListedInActiveLayers(true);
//
//                // 'trick' the system into displaying the map layer
//                mapLayer.setDisplayed(true);
//
//                portalSession.getActiveLayers().add(mapLayer);
//            } else {
//                logger.warn(
//                        "Skipping activation of " + id + " specified in activeByDefault "
//                        + "because it is not a valid layer - check config file and/or discovery output");
//            }
//        }
//         * */
//    }
//
//    /**
//     * Load the list of search catalogues and default search terms
//     * @param portalDocument
//     * @param portalSession
//     */
//    private void searchCatalogues(PortalDocument portalDocument, PortalSession portalSession) {
///*
// *
// * FIXME!!
// *
// *
//        if (portalDocument.getPortal().getSearchCatalogues().getDisabled()) {
//            logger.info("search system disabled in config file");
//        } else {
//            String defaultSearchCatalogueId =
//                    portalDocument.getPortal().getSearchCatalogues().getDefaultSearchCatalogueIdRef();
//
//            boolean defaultSearchCatalogueFound = false;
//
//            for (SearchCatalogue configuredSearchCatalogue :
//                    portalDocument.getPortal().getSearchCatalogues().getSearchCatalogueList()) {
//                if (!configuredSearchCatalogue.getDisabled()) {
//                    au.org.emii.portal.SearchCatalogue searchCatalogue = new au.org.emii.portal.SearchCatalogue();
//                    searchCatalogue.copyFrom(configuredSearchCatalogue);
//                    portalSession.addSearchCatalogue(searchCatalogue);
//                    logger.debug("SEARCHCATALOGUE + " + searchCatalogue.getId());
//
//                    // lookup supported search terms and store in session
//                    searchCatalogue.setSearchTerms(MESTSupport.getSearchTerms(searchCatalogue));
//
//                    /* select the default search catalogue from config
//                     * file settings
//                     * /
//                    if (searchCatalogue.getId().equals(defaultSearchCatalogueId)) {
//                        portalSession.setSelectedSearchCatalogue(searchCatalogue);
//                        defaultSearchCatalogueFound = true;
//                    }
//                } else {
//                    logger.info(
//                            "Search catalogue: " + configuredSearchCatalogue.getId() + " at "
//                            + configuredSearchCatalogue.getUri() + " will not be used because "
//                            + "it is disabled");
//                }
//            }
//            if (!defaultSearchCatalogueFound) {
//               logger.error(
//                    "Default search catalogue " + defaultSearchCatalogueId
//                    + " requested in configuration file settings section "
//                    + "is not available - check it exists and is not disabled");
//            }
//        }
// *
// */
//    }
//
//    /**
//     * process the base layers
//     * @param service
//     * @return
//     */
//    private void processBaseLayer(BaseLayer baseLayer, PortalSession portalSession) {
//        if (!baseLayer.getDisabled()) {
//            MapLayer mapLayer = remoteMap.baseLayer(baseLayer);
//            if (mapLayer != null) {
//                portalSession.addBaseLayer(mapLayer);
//            }
//        } else {
//            logger.info(
//                    "skipping loading service " + baseLayer.getId() + " at "
//                    + baseLayer.getUri() + " because it is disabled in configuration file");
//        }
//    }
//
//    private void processService(Service service, PortalSession portalSession) {
//        if (!service.getDisabled()) {
//            MapLayer mapLayer = remoteMap.service(service);
//            if (mapLayer != null) {
//                mapLayer.setBaseLayer(false);
//                portalSession.addMapLayer(mapLayer);
//            }
//        } else {
//            logger.info(
//                    "skipping loading service " + service.getId() + " at "
//                    + service.getUri() + " because it is disabled in configuration file");
//        }
//    }
//
//    private void processDiscovery(Discovery discovery, PortalSession portalSession) {
//        if (!discovery.getDisabled()) {
//            MapLayer mapLayer;
//            logger.debug("discovery begins for id: " + discovery.getId());
//            if (discovery.getType().equals(LayerUtilities.AUTO_DISCOVERY_TYPE)) {
//                mapLayer = remoteMap.autoDiscover(
//                        discovery.getId(),
//                        discovery.getName(),
//                        discovery.getOpacity(),
//                        discovery.getUri(),
//                        discovery.getType());
//            } else {
//                /* why would you not want to always auto discover layers?  hmm
//                 * well, so you can get control over the flags do the discover
//                 * call for now, since autodiscover supports fixed versions
//                 * as well.  Will review this in v2...
//                 */
//                mapLayer = remoteMap.discover(discovery, false, false, false);
//            }
//
//            if (mapLayer != null) {
//                portalSession.addMapLayer(mapLayer);
//            }
//        } else {
//            logger.info(
//                    "skipping discovery from " + discovery.getId() + " at "
//                    + discovery.getUri() + " because it is disabled in configuration file");
//        }
//    }
//
//    /**
//     * copy the static link information from the xmlbean into a POJO.  Why not just
//     * keep the xmlbean and use it in our application?  Because to do so is highly
//     * inefficient in terms of memory usage
//     * @param staticLink
//     * @param portalSession
//     */
//    private void processStaticLink(StaticLink staticLink, PortalSession portalSession) {
//        if (!staticLink.getDisabled()) {
//            Link link = new Link();
//            link.copyFrom(staticLink);
//            portalSession.addLink(link);
//        } else {
//            logger.info(
//                    "skipping static link declaration " + staticLink.getId()
//                    + " for " + staticLink.getUri() + " because it is disabled");
//        }
//    }
//
//    private void settings(PortalDocument portalDocument, PortalSession portalSession) {
//        /*
//         * FIXME!!
//         *
//        logger.debug("loading settings");
//
//        // active by default
//        activeByDefault(portalDocument, portalSession);
//
//        // default facility/region selection
//        String defaultSelection = portalDocument.getPortal().getSettings().getDefaultSelection().toString();
//        String defaultSelectionId = null;
//        int defaultView;
//        if (defaultSelection.equals("REGION")) {
//            defaultSelectionId =
//                    portalDocument.getPortal().
//                    getSettings().
//                    getDefaultRegionIdRef();
//            defaultView = PortalSession.VIEW_REGION;
//        } else if (defaultSelection.equals("FACILITY")) {
//            defaultSelectionId =
//                    portalDocument.getPortal().
//                    getSettings().
//                    getDefaultFacilityIdRef();
//            defaultView = PortalSession.VIEW_FACILITY;
//        } else {
//            logger.warn(
//                    "Unknown defaultSelection: " + defaultSelection
//                    + " in config file.  Will attempt to use first listed"
//                    + " region ID instead");
//            defaultSelectionId = portalSession.getRegions().get(0).getId();
//            defaultView = PortalSession.VIEW_FACILITY;
//        }
//
//        portalSession.setSelectedFacilityOrRegionId(
//                defaultSelectionId);
//        portalSession.setCurrentView(defaultView);
//
//
//        // default bounding box
//        portalSession.getDefaultBoundingBox().copyFrom(
//                portalDocument.getPortal().getSettings().getDefaultBoundingBox());
//
//         *
//         */
//
//    }
//
//    private void facilitiesAndRegions(PortalDocument portalDocument, PortalSession portalSession) {
//        /*
//         * FIXME!
//         *
//        // facilities
//        for (LayerGroup configuredFacility :
//                portalDocument.getPortal().getFacilities().getFacilityList()) {
//
//            Facility facility = processFacilityOrRegion(configuredFacility, portalSession, Facility.class);
//            if (facility != null) {
//                portalSession.addFacility(facility);
//            }
//        }
//
//
//        // regions
//        for (RegionLayerGroup configuredRegion :
//                portalDocument.getPortal().getRegions().getRegionList()) {
//
//            Region region = processFacilityOrRegion(configuredRegion, portalSession, Region.class);
//            if (region != null) {
//                portalSession.addRegion(region);
//            }
//        }
//
//        // realtime (optional)
//        if (portalDocument.getPortal().getRealtimeDataProviders() != null) {
//            for (LayerGroup configuredFacility :
//                    portalDocument.getPortal().getRealtimeDataProviders().getRealtimeDataProviderList()) {
//
//                Facility facility = processFacilityOrRegion(configuredFacility, portalSession, Facility.class);
//                if (facility != null) {
//                    portalSession.addRealtime(facility);
//                }
//            }
//        }
//
//        // check we have at least one region or facility
//        if (portalSession.getFacilities().size()
//                + portalSession.getRegions().size() == 0) {
//            throw new PortalException(
//                    "No valid Facilities or Regions or they have all "
//                    + "been disabled - check facilities and regions "
//                    + "declarations in config file");
//        }
//         *
//         */
//    }
//
//    private <T extends Facility> T processFacilityOrRegion(LayerGroup configured, PortalSession portalSession, Class<T> clazz) {
//        T facilityOrRegion = null;
//        if (!configured.getDisabled()) {
//            try {
//                facilityOrRegion = clazz.newInstance();
//                facilityOrRegion.copyFrom(configured, portalSession);
//            } // swallow exceptions;
//            catch (InstantiationException e) {
//            } catch (IllegalAccessException e) {
//            }
//        } else {
//            logger.info(
//                    "skipped facility or region " + configured.getId()
//                    + " Known as " + configured.getName() + "because it is disabled");
//        }
//        return facilityOrRegion;
//    }
//
//
//
//    @Override
//    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
//    public void run() {
//        running = true;
//        boolean firstRun = true;
//        while (running) {
//            load();
//            try {
//
//                /* if this is the first run, then set rereadInterval to a smallish
//                 * value of the order of one minute or so and then do a re-read of
//                 * the config file.
//                 *
//                 * This is to handle attempting the case when the server we want
//                 * to connect to exists within the same servlet container we are
//                 * running from and the servlet container is not fully up yet.
//                 *
//                 * Therefore we will wait a little while and reload everything.
//                 * Hopefully the server is now up and running, if not it must have a
//                 * problem or we the config_reread_initial_interval value is too
//                 * small.
//                 *
//                 * In any case, we will recheck it at our next scheduled config
//                 * file re-read
//                 */
//                try {
//                    if (firstRun) {
//                        rereadInterval = Config.getValueAsInt("config_reread_initial_interval");
//                        firstRun = false;
//                    } else {
//                        rereadInterval = Config.getValueAsInt("config_reread_interval");
//                    }
//                    logger.debug(
//                            "menu (re)load attempt finished, goinging to sleep for " + rereadInterval + "ms");
//                } catch (RuntimeException e) {
//                    rereadInterval = BROKEN_CONFIG_RELOAD;
//                    logger.fatal(
//                            "unable to parse integer from config_reread_interval "
//                            + "or config_reread_initial_interval "
//                            + "in configuration file - your configuration is broken. "
//                            + "Will attempt reload in " + BROKEN_CONFIG_RELOAD / 60000
//                            + " minutes.");
//                }
//
//                Thread.sleep(rereadInterval);
//            } catch (InterruptedException e) {
//                logger.debug(
//                        "Configuration Loader was interrupted during its sleep, probably "
//                        + "not important");
//            }
//        }
//    }
//
//
//
//    /**
//     * Get a static log4j instance - to save repetition
//     * @return
//     */
//    private static Logger getStaticLogger() {
//        return Logger.getLogger(ConfigurationLoader.class);
//    }
//}
