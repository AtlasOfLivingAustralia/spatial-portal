/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.composer.admin;

import au.org.emii.portal.util.Validate;
import au.org.emii.portal.composer.GenericAutowireAutoforwardComposer;
import au.org.emii.portal.config.ConfigurationLoaderStage1Impl;
import au.org.emii.portal.config.ConfigurationFile;
import au.org.emii.portal.factory.PortalDocumentFactory;
import au.org.emii.portal.util.PortalNaming;
import au.org.emii.portal.util.PortalNamingUpdater;
import au.org.emii.portal.web.Log4jLoader;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 *
 * @author geoff
 */
public class ConfigComposer extends GenericAutowireAutoforwardComposer {
    /**
     * Portal name - zk autowired
     */
    private Textbox portalName = null;

    /**
     * Contents of config file - zk autowired
     */
    private Textbox config = null;

    /**
     * Error message - unable to save some files - zk autowired
     */
    private Label errorSaving =  null;

    /**
     * Error message - invalid xml in config file - zk autowired
     */
    private Label errorInvalid = null;

    /**
     * Error message - config saved ok - zk autowired
     */
    private Label successMessage = null;

    /**
     * Error message for when portal name is empty - zk autowired
     */
    private Label errorPortalName = null;

    /**
     * Error message for when config file contents is empty - zk autowired
     */
    private Label errorConfig = null;


    private Log4jLoader log4jLoader = null;

    private PortalNaming portalNaming = null;

    private PortalNamingUpdater portalNamingUpdater = null;

    private PortalDocumentFactory portalDocumentFactory = null;

    private ConfigurationFile configurationFile = null;

    /**
     * Activate button clicked
     */
    public void onClick$activate() {
        String portalNameValue = portalName.getValue();
        String configValue = config.getValue();
        hideMessages();
        if (validate(portalNameValue, configValue)) {
            // all valid - go ahead and save
            if (portalNamingUpdater.updatePortalName(portalNameValue, getPortalSession().getPortalUser().getUsername()) &&
                configurationFile.saveAsConfigFile(configValue)) {

                // rewrite and reload log4j.xml config file to update loggers with
                // changed portal name
                log4jLoader.load();

                // config file has been saved at this point - now kick the
                // the configuration loader thread is likely blocked, so wake it
                // up from its blocked state that will cause it to reload the
                // configuration file
                getConfigurationLoaderThread().interrupt();

                successMessage.setVisible(true);
            } else {
                errorSaving.setVisible(true);
            }
        }

    }

    /**
     * Hide all messages
     */
    private void hideMessages() {
        successMessage.setVisible(false);
        errorConfig.setVisible(false);
        errorInvalid.setVisible(false);
        errorPortalName.setVisible(false);
        errorSaving.setVisible(false);
    }

    /**
     * Validate form params for emptyness and config file contents for conformance
     * to schema
     * @param portalNameValue
     * @param configValue
     * @return
     */
    private boolean validate(String portalNameValue, String configValue) {
        boolean valid = true;

        if (Validate.empty(portalNameValue)) {
            errorPortalName.setVisible(true);
            valid = false;
        }

        if (Validate.empty(configValue)) {
            errorConfig.setVisible(true);
            valid = false;
        } else if (! configurationFile.validateConfigFileContents(configValue)) {
            errorInvalid.setVisible(true);
            valid = false;
        }
        return valid;
    }

    @Override
    public void afterCompose() {
        super.afterCompose();
        configurationFile = (ConfigurationFile) portalDocumentFactory;

        portalName.setValue(portalNaming.getPortalName());
        config.setValue(configurationFile.configurationFileContents());
    }

    /**
     * Convenience accesssor for ConfigurationLoader instance
     * @return
     */
    private ConfigurationLoaderStage1Impl getConfigurationLoader() {
        return (ConfigurationLoaderStage1Impl) getAttribute("configurationLoader", APPLICATION_SCOPE);
    }

     /**
     * Convenience accesssor for ConfigurationLoader *THREAD*
     * @return
     */
    private Thread getConfigurationLoaderThread() {
        return (Thread) getAttribute("configurationLoaderThread", APPLICATION_SCOPE);
    }

    public Log4jLoader getLog4jLoader() {
        return log4jLoader;
    }

    public void setLog4jLoader(Log4jLoader log4jLoader) {
        this.log4jLoader = log4jLoader;
    }

    public PortalNaming getPortalNaming() {
        return portalNaming;
    }

    public void setPortalNaming(PortalNaming portalNaming) {
        this.portalNaming = portalNaming;
    }

    public PortalNamingUpdater getPortalNamingUpdater() {
        return portalNamingUpdater;
    }

    public void setPortalNamingUpdater(PortalNamingUpdater portalNamingUpdater) {
        this.portalNamingUpdater = portalNamingUpdater;
    }

    public PortalDocumentFactory getPortalDocumentFactory() {
        return portalDocumentFactory;
    }

    public void setPortalDocumentFactory(PortalDocumentFactory portalDocumentFactory) {
        this.portalDocumentFactory = portalDocumentFactory;
    }

    
}
