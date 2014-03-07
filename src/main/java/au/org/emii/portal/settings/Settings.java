/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.settings;

import au.org.emii.portal.value.BoundingBox;

/**
 * @author geoff
 */
public interface Settings {

    public int getCacheMaxAge();

    public String getCacheParameter();

    public int getConfigRereadInitialInterval();

    public int getConfigRereadInterval();

    public BoundingBox getDefaultBoundingBox();

    public int getNetConnectSlowTimeout();

    public int getNetConnectTimeout();

    public int getNetReadSlowTimeout();

    public int getNetReadTimeout();

    public String getProxyAllowedHosts();

    public String getXmlMimeType();

    public void setCacheMaxAge(int cacheMaxAge);

    public void setCacheParameter(String cacheParameter);

    public void setConfigRereadInitialInterval(int configRereadInitialInterval);

    public void setConfigRereadInterval(int configRereadInterval);

    public void setDefaultBoundingBox(BoundingBox defaultBoundingBox);

    public void setNetConnectSlowTimeout(int netConnectSlowTimeout);

    public void setNetConnectTimeout(int netConnectTimeout);

    public void setNetReadSlowTimeout(int netReadSlowTimeout);

    public void setNetReadTimeout(int netReadTimeout);

    public void setProxyAllowedHosts(String proxyAllowedHosts);

    public void setXmlMimeType(String xmlMimeType);

    public String getConfigPath();

    public void setConfigPath(String configPath);
}
