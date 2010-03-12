/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.settings;

import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.mest.MestConfiguration;
import au.org.emii.portal.value.SearchCatalogue;
import java.util.Map;

/**
 *
 * @author geoff
 */
public interface Settings {

    public int getCacheMaxAge();

    public String getCacheParameter();

    public String getCacheUrl();

    public int getConfigRereadInitialInterval();

    public int getConfigRereadInterval();

    public BoundingBox getDefaultBoundingBox();

    public Map<String, MestConfiguration> getMestConfigurations();

    public int getNetConnectSlowTimeout();

    public int getNetConnectTimeout();

    public int getNetReadSlowTimeout();

    public int getNetReadTimeout();

    public String getProxyAllowedHosts();

    public String getProxyHost();

    public String getProxyNonProxyHosts();

    public String getProxyPassword();

    public int getProxyPort();

    public String getProxyUsername();

    public String getXmlMimeType();

    public boolean isDisableDepthServlet();

    public boolean isProxyRequired();

    public void setCacheMaxAge(int cacheMaxAge);

    public void setCacheParameter(String cacheParameter);

    public void setCacheUrl(String cacheUrl);

    public void setConfigRereadInitialInterval(int configRereadInitialInterval);

    public void setConfigRereadInterval(int configRereadInterval);

    public void setDefaultBoundingBox(BoundingBox defaultBoundingBox);

    public void setDisableDepthServlet(boolean disableDepthServlet);

    public void setMestConfigurations(Map<String, MestConfiguration> mestConfigurations);

    public void setNetConnectSlowTimeout(int netConnectSlowTimeout);

    public void setNetConnectTimeout(int netConnectTimeout);

    public void setNetReadSlowTimeout(int netReadSlowTimeout);

    public void setNetReadTimeout(int netReadTimeout);

    public void setProxyAllowedHosts(String proxyAllowedHosts);

    public void setProxyHost(String proxyHost);

    public void setProxyNonProxyHosts(String proxyNonProxyHosts);

    public void setProxyPassword(String proxyPassword);

    public void setProxyPort(int proxyPort);

    public void setProxyRequired(boolean proxyRequired);

    public void setProxyUsername(String proxyUsername);

    public void setXmlMimeType(String xmlMimeType);

    public String getIsoCountriesFilename();

    public void setIsoCountriesFilename(String isoCountriesFilename);

    public String getPortalName();

    public void setPortalName(String portalName);


    public boolean isDisableAdminConsole();

    public void setDisableAdminConsole(boolean disableAdminConsole);

    public boolean isDisableFacility();

    public void setDisableFacility(boolean disableFacility);

    public boolean isDisableLayers();

    public void setDisableLayers(boolean disableLayers);

    public boolean isDisableLinks();

    public void setDisableLinks(boolean disableLinks);

    public boolean isDisablePortalUsers();

    public void setDisablePortalUsers(boolean disablePortalUsers);

    public boolean isDisableRealtime();

    public void setDisableRealtime(boolean disableRealtime);

    public boolean isDisableRegion();

    public void setDisableRegion(boolean disableRegion);

    public boolean isDisableSearch();

    public void setDisableSearch(boolean disableSearch);

    public boolean isDisableUserDefined();

    public void setDisableUserDefined(boolean disableUserDefined);

    public boolean isDisableTopLinks();

    public void setDisableTopLinks(boolean disableTopLinks);

    public Map<String, SearchCatalogue> getSearchCatalogues();

    public void setSearchCatalogues(Map<String, SearchCatalogue> searchCatalogues);

    public String getConfigPath();

    public void setConfigPath(String configPath);
}
