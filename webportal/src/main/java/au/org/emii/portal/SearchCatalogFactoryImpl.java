/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal;

import au.org.emii.portal.config.Settings;
import au.org.emii.portal.util.UriResolver;
import java.util.Map;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 * Factory for creating SearchCatalogue instances.  The factory is necessary
 * to allow spring to inject the uri resolver
 * @author geoff
 */
public abstract class SearchCatalogFactoryImpl implements SearchCatalogueFactory {

    private UriResolver uriResolver = null;
    private Settings settings = null;

    private Logger logger = Logger.getLogger(getClass());

    public UriResolver getUriResolver() {
        return uriResolver;
    }

    public void setUriResolver(UriResolver uriResolver) {
        this.uriResolver = uriResolver;
    }


    @Override
    public SearchCatalogue createInstance(au.org.emii.portal.config.xmlbeans.SearchCatalogue xmlSearchCatalog) {
        SearchCatalogue searchCatalog = null;
        String targetUri = uriResolver.resolve(xmlSearchCatalog);
        if (targetUri == null) {
            logger.error(
                String.format(
                    "Unable to resolve uri or uriIdRef to real URI for search catalogue %s " +
                    "uri '%s', uriIdRef '%s'", xmlSearchCatalog.getId(), xmlSearchCatalog.getUri(), xmlSearchCatalog.getUriIdRef()));
        } else {
            try {
                searchCatalog = createSearchCatalogueInstance();
                searchCatalog.setId(xmlSearchCatalog.getId());
                searchCatalog.setName(xmlSearchCatalog.getName());
                searchCatalog.setDescription(xmlSearchCatalog.getDescription());
                searchCatalog.setUri(targetUri);
                searchCatalog.setVersion(xmlSearchCatalog.getVersion());
                searchCatalog.setUsername(xmlSearchCatalog.getUsername());
                searchCatalog.setPassword(xmlSearchCatalog.getPassword());

                // lookup the mest configuration
                Map<String, au.org.emii.portal.mest.MestConfiguration> mestConfigurations = settings.getMestConfigurations();
                au.org.emii.portal.mest.MestConfiguration mestConfiguration = mestConfigurations.get(xmlSearchCatalog.getMestConfigurationIdRef());
                searchCatalog.setMestConfiguration(mestConfiguration);
            } catch (NullPointerException e) {
                // FIXME! - make pretty
                logger.error(
                        String.format(
                            "error loading SearchCatalogue '%s' missing data in config file " +
                            "or mestConfiguration '%s' does not exist or is disabled.",
                            xmlSearchCatalog.getId(),
                            xmlSearchCatalog.getMestConfigurationIdRef()),
                         e);
                // throw away the instance
                searchCatalog = null;
            }
        }
        return searchCatalog;
    }

    /**
     * spring method injection to create a search instance (so that annotations will
     * work)
     * @return
     */
    public abstract SearchCatalogue createSearchCatalogueInstance();

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }


}
