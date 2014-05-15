/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.settings;

import au.org.emii.portal.value.BoundingBox;

/**
 * Accessor class for 'well known' settings from the config file
 *
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

    // 'Well known' values
    // ~~~~~~~~~~~~~~~~~~~
    // Values that the system expects to be able to find on load
    /**
     * Mime type to be treated as XML - normaly application/xml
     */
    private String xmlMimeType = null;

    /**
     * Configuration file will be reread after this interval (ms)
     * <p/>
     * Handy values: 60000 = 1 minute 3600000 = 1 hour 86400000 = 24 hours
     */
    private int configRereadInterval = 0;

    /**
     * On first application load, the config file will be re-read after this
     * interval and will subsequently be reread after the
     * config_reread_interval.
     * <p/>
     * This is to handle servers that are not fully up yet being restarted at
     * the same time as us
     */
    private int configRereadInitialInterval = 0;

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
     * maximum time to wait for reading from a connection to finish under normal
     * conditions(ms)
     */
    private int netReadTimeout = 0;

    /**
     * maximum time to wait for a connection to establish under slow server
     * conditions (ms)
     */
    private int netConnectSlowTimeout = 0;

    /**
     * maximum time to wait for reading from a connection to finish under slow
     * server conditions (ms)
     */
    private int netReadSlowTimeout = 0;

    /**
     * remote request servlet will only allow requests to be forwarded to these
     * hosts. Content of this element must be a string of hosts delimited by
     * commas and enclosed in quotes.
     */
    private String proxyAllowedHosts = null;

    /**
     * Name of the Parameter containing the URL for requests made to the cache
     * reflector service
     */
    private String cacheParameter = null;

    /**
     * Time to keep an object in cache (SQUID) in seconds
     */
    private int cacheMaxAge = 0;

    /**
     * Path to configuration directory - from environment dir
     */
    private String configPath = null;

    @Override
    public int getCacheMaxAge() {
        return cacheMaxAge;
    }


    @Override
    public void setCacheMaxAge(int cacheMaxAge) {
        this.cacheMaxAge = cacheMaxAge;
    }

    @Override
    public String getCacheParameter() {
        return cacheParameter;
    }

    @Override

    public void setCacheParameter(String cacheParameter) {
        this.cacheParameter = cacheParameter;
    }

    @Override
    public int getConfigRereadInitialInterval() {
        return configRereadInitialInterval;
    }

    @Override

    public void setConfigRereadInitialInterval(int configRereadInitialInterval) {
        this.configRereadInitialInterval = configRereadInitialInterval;
    }

    @Override
    public int getConfigRereadInterval() {
        return configRereadInterval;
    }

    @Override

    public void setConfigRereadInterval(int configRereadInterval) {
        this.configRereadInterval = configRereadInterval;
    }

    @Override
    public BoundingBox getDefaultBoundingBox() {
        return defaultBoundingBox;
    }

    @Override

    public void setDefaultBoundingBox(BoundingBox defaultBoundingBox) {
        this.defaultBoundingBox = defaultBoundingBox;
    }

    @Override
    public int getNetConnectSlowTimeout() {
        return netConnectSlowTimeout;
    }

    @Override

    public void setNetConnectSlowTimeout(int netConnectSlowTimeout) {
        this.netConnectSlowTimeout = netConnectSlowTimeout;
    }

    @Override
    public int getNetConnectTimeout() {
        return netConnectTimeout;
    }

    @Override

    public void setNetConnectTimeout(int netConnectTimeout) {
        this.netConnectTimeout = netConnectTimeout;
    }

    @Override
    public int getNetReadSlowTimeout() {
        return netReadSlowTimeout;
    }

    @Override

    public void setNetReadSlowTimeout(int netReadSlowTimeout) {
        this.netReadSlowTimeout = netReadSlowTimeout;
    }

    @Override
    public int getNetReadTimeout() {
        return netReadTimeout;
    }

    @Override

    public void setNetReadTimeout(int netReadTimeout) {
        this.netReadTimeout = netReadTimeout;
    }

    @Override
    public String getProxyAllowedHosts() {
        return proxyAllowedHosts;
    }

    @Override

    public void setProxyAllowedHosts(String proxyAllowedHosts) {
        this.proxyAllowedHosts = proxyAllowedHosts;
    }

    @Override
    public String getXmlMimeType() {
        return xmlMimeType;
    }

    @Override

    public void setXmlMimeType(String xmlMimeType) {
        this.xmlMimeType = xmlMimeType;
    }

    public String getConfigPath() {
        return configPath;
    }

    @Override
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

}
