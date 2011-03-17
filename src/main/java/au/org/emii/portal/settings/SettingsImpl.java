/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.settings;

import au.org.emii.portal.util.ResolveHostName;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.aspect.CheckNotNull;
import au.org.emii.portal.aspect.LogSetterValue;


/**
 * Accessor class for 'well known' settings from the config file
 * @author geoff
 */
public class SettingsImpl implements Settings {

    // Default selections
    // ~~~~~~~~~~~~~~~~~~
    // Default selections used for blank portal sessions

    /**
     * default bounding box - portal zooms here on loading
     */
    private BoundingBox defaultBoundingBox = null;

    /**
     * disable looking up depth/altitude for points clicked on map - if enabled, requires
     * access to a massive database...
     */
    private boolean disableDepthServlet = false;



    // 'Well known' values
    // ~~~~~~~~~~~~~~~~~~~
    // Values that the system expects to be able to find on load

    /**
     * Mime type to be treated as XML - normaly application/xml
     */
    private String xmlMimeType = null;

    /**
     * Configuration file will be reread after this interval (ms)
     *
     * Handy values:
     *  60000 		= 1 minute
     *  3600000 	= 1 hour
     *  86400000 	= 24 hours
     */
    private int configRereadInterval = 0;

    /**
     * On first application load, the config file will be re-read after this
     * interval and will subsequently be reread after the config_reread_interval.
     *
     * This is to handle servers that are not fully up yet being restarted
     * at the same time as us
     */
    private int configRereadInitialInterval = 0;

    /**
     * Filename within classpath for xml file of ISO country codes - this is
     * normally obtained from a geonetwork installation, and gets used to build
     * a list of countries when a user registers with the portal.
     *
     * The file is normally found at web/geonetwork/loc/ru/xml/countries.xml
     * within the geonetwork and is shipped with the default build of the
     * portal.  Therefore, this value is normally just 'countries.xml'
     */
    private String isoCountriesFilename = null;

    // Network timeouts
    // ~~~~~~~~~~~~~~~~
    // Connection timeouts for connecting to servers - used to prevent hung
    // connections when a server is down

    /**
     * maximum time to wait for a connection to establish under normal
     * conditions (ms)
     */
    private int netConnectTimeout = 0;

    /**
     * maximum time to wait for reading from a connection to finish
     * under normal conditions(ms)
     */
    private int netReadTimeout = 0;

    /**
     * maximum time to wait for a connection to establish under slow
     * server conditions (ms)
     */
    private int netConnectSlowTimeout = 0;

    /**
     * maximum time to wait for reading from a connection to finish
     * under slow server conditions (ms)
     */
    private int netReadSlowTimeout = 0;

    // Proxy server
    // ~~~~~~~~~~~~
    // Welcome to 1990 :-P

    /**
     * is proxy server required?
     */
    private boolean proxyRequired = false;

    /**
     * proxy server host or IP address - do not prefix with http://..!
     */
    private String proxyHost = null;

    /**
     * proxy server port number
     */
    private int proxyPort = 0;

    /**
     * proxy server username
     */
    private String proxyUsername = null;

    /**
     * proxy server password
     */
    private String proxyPassword = null;

    /**
     * proxy will NOT be contacted for hosts listed here.  Content of this element
     * must be a string of hosts delimited by commas and enclosed in quotes.  Eg:
     * 'localhost', 'obsidian', 'foo'.  You will want to use a CDATA block for this
     */
    private String proxyNonProxyHosts = null;

    /**
     * remote request servlet will only allow requests to be forwarded to these
     * hosts.   Content of this element must be a string of hosts delimited by
     * commas and enclosed in quotes.
     */
    private String proxyAllowedHosts = null;
    
    // Cache Reflector
    // ~~~~~~~~~~~~~~~
    // RemoteRequest servlet setup - optionally alters URLs so that they are requested
    // through a local servlet, magically caching them with squid if configured
    // correctly

    /**
     * Relative URL for the cache reflector service
     */
    private String cacheUrl = null;

    /**
     * Name of the Parameter containing the URL for requests made to
     * the cache reflector service
     */
    private String cacheParameter = null;

    /**
     *  Time to keep an object in cache (SQUID) in seconds
     */
    private int cacheMaxAge = 0;

    /**
     * Portal name - from portal.properties.  This isn't in the config file
     * because we always want to know our name, even if the config file is
     * missing or invalid
     */
    private String portalName = null;

    /**
     * Fully qualified hostname for the server we are running on
     */
    private String hostName = null;

    /**
     * Host name (for this host) resolver - spring injected
     */
    private ResolveHostName resolveHostName = null;

    /**
     * Path to configuration directory - from environment dir
     */
    private String configPath = null;


    @Override
    public int getCacheMaxAge() {
        return cacheMaxAge;
    }

    @LogSetterValue
    @Override
    public void setCacheMaxAge(int cacheMaxAge) {
        this.cacheMaxAge = cacheMaxAge;
    }

    @Override
    public String getCacheParameter() {
        return cacheParameter;
    }

    @Override
    @CheckNotNull
    @LogSetterValue
    public void setCacheParameter(String cacheParameter) {
        this.cacheParameter = cacheParameter;
    }

    @Override
    public String getCacheUrl() {
        return cacheUrl;
    }

    @Override
    @CheckNotNull
    @LogSetterValue
    public void setCacheUrl(String cacheUrl) {
        this.cacheUrl = cacheUrl;
    }

    @Override
    public int getConfigRereadInitialInterval() {
        return configRereadInitialInterval;
    }

    @Override
    @LogSetterValue
    public void setConfigRereadInitialInterval(int configRereadInitialInterval) {
        this.configRereadInitialInterval = configRereadInitialInterval;
    }

    @Override
    public int getConfigRereadInterval() {
        return configRereadInterval;
    }

    @Override
    @LogSetterValue
    public void setConfigRereadInterval(int configRereadInterval) {
        this.configRereadInterval = configRereadInterval;
    }

    @Override
    public BoundingBox getDefaultBoundingBox() {
        return defaultBoundingBox;
    }

    @Override
    @CheckNotNull
    public void setDefaultBoundingBox(BoundingBox defaultBoundingBox) {
        this.defaultBoundingBox = defaultBoundingBox;
    }

    @Override
    public boolean isDisableDepthServlet() {
        return disableDepthServlet;
    }

    @Override
    @LogSetterValue
    public void setDisableDepthServlet(boolean disableDepthServlet) {
        this.disableDepthServlet = disableDepthServlet;
    }

    @Override
    public int getNetConnectSlowTimeout() {
        return netConnectSlowTimeout;
    }

    @Override
    @LogSetterValue
    public void setNetConnectSlowTimeout(int netConnectSlowTimeout) {
        this.netConnectSlowTimeout = netConnectSlowTimeout;
    }

    @Override
    public int getNetConnectTimeout() {
        return netConnectTimeout;
    }

    @Override
    @LogSetterValue
    public void setNetConnectTimeout(int netConnectTimeout) {
        this.netConnectTimeout = netConnectTimeout;
    }

    @Override
    public int getNetReadSlowTimeout() {
        return netReadSlowTimeout;
    }

    @Override
    @LogSetterValue
    public void setNetReadSlowTimeout(int netReadSlowTimeout) {
        this.netReadSlowTimeout = netReadSlowTimeout;
    }

    @Override
    public int getNetReadTimeout() {
        return netReadTimeout;
    }

    @Override
    @LogSetterValue
    public void setNetReadTimeout(int netReadTimeout) {
        this.netReadTimeout = netReadTimeout;
    }

    @Override
    public String getProxyAllowedHosts() {
        return proxyAllowedHosts;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setProxyAllowedHosts(String proxyAllowedHosts) {
        this.proxyAllowedHosts = proxyAllowedHosts;
    }

    @Override
    public String getProxyHost() {
        return proxyHost;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    @Override
    public String getProxyNonProxyHosts() {
        return proxyNonProxyHosts;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setProxyNonProxyHosts(String proxyNonProxyHosts) {
        this.proxyNonProxyHosts = proxyNonProxyHosts;
    }

    @Override
    public String getProxyPassword() {
        return proxyPassword;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    @Override
    public int getProxyPort() {
        return proxyPort;
    }

    @Override
    @LogSetterValue
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    @Override
    public boolean isProxyRequired() {
        return proxyRequired;
    }

    @Override
    @LogSetterValue
    public void setProxyRequired(boolean proxyRequired) {
        this.proxyRequired = proxyRequired;
    }

    @Override
    public String getProxyUsername() {
        return proxyUsername;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    @Override
    public String getXmlMimeType() {
        return xmlMimeType;
    }

    @CheckNotNull
    @Override
    @LogSetterValue
    public void setXmlMimeType(String xmlMimeType) {
        this.xmlMimeType = xmlMimeType;
    }

    public ResolveHostName getResolveHostName() {
        return resolveHostName;
    }

    public void setResolveHostName(ResolveHostName resolveHostName) {
        this.resolveHostName = resolveHostName;
    }

    public String getHostName() {
        if (hostName == null) {
            hostName = resolveHostName.resolveHostName();
        }
        return hostName;
    }

    @Override
    public String getIsoCountriesFilename() {
        return isoCountriesFilename;
    }

    @Override
    @CheckNotNull
    @LogSetterValue
    public void setIsoCountriesFilename(String isoCountriesFilename) {
        this.isoCountriesFilename = isoCountriesFilename;
    }

    @Override
    public String getPortalName() {
        return portalName;
    }

    @Override
    @CheckNotNull
    @LogSetterValue
    public void setPortalName(String portalName) {
        this.portalName = portalName;
    }

    public String getConfigPath() {
        return configPath;
    }

    @Override
    @CheckNotNull
    @LogSetterValue
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }


}
