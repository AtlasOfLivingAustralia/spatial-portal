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

    int getCacheMaxAge();

    int getConfigRereadInitialInterval();

    void setConfigRereadInitialInterval(int configRereadInitialInterval);

    int getConfigRereadInterval();

    void setConfigRereadInterval(int configRereadInterval);

    BoundingBox getDefaultBoundingBox();

    void setDefaultBoundingBox(BoundingBox defaultBoundingBox);

    int getNetConnectSlowTimeout();

    void setNetConnectSlowTimeout(int netConnectSlowTimeout);

    int getNetConnectTimeout();

    void setNetConnectTimeout(int netConnectTimeout);

    int getNetReadSlowTimeout();

    void setNetReadSlowTimeout(int netReadSlowTimeout);

    int getNetReadTimeout();

    void setNetReadTimeout(int netReadTimeout);

    String getProxyAllowedHosts();

    void setProxyAllowedHosts(String proxyAllowedHosts);

    void setConfigPath(String configPath);
}
