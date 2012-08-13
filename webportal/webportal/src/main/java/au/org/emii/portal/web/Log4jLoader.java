/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.web;

import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.util.PortalNaming;
import au.org.emii.portal.util.ResolveHostName;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;

/**
 * Read log4j_template, substitute variables in the pattern, then save it
 * to log4j.xml and get log4j to reload its configuration.
 *
 * I was originally doing this with MDC but it doesn't work properly if
 * you want to change values from another thread (I do) and also leaks
 * references on server shutdown.
 *
 */
public class Log4jLoader implements InitializingBean {

    /**
     * Resolve the current hostname
     */
    private ResolveHostName resolveHostname = null;

    /**
     * Resolve the current portal name
     */
    private PortalNaming portalNaming = null;

    /**
     * Filename for template - THIS IS WITHIN PORTAL CONFIGURATION
     * DIR!
     */
    private static final String TEMPLATE_FILENAME = "log4j_template.xml";

    /**
     * Filename for real log4j.xml configuration - THIS IS WITHIN PORTAL CONFIGURATION
     * DIR!
     */
    private static final String LOG4J_FILENAME = "log4j.xml";

    /**
     * Logger instance - used for debugging this class
     */
    private static Logger logger = Logger.getLogger(Log4jLoader.class);
    /**
     * Regular expression to replace with server name
     */
    private static final String SERVER_NAME_REPLACE = "\\*\\*SERVER_NAME\\*\\*";
    /**
     * Regular expression to replace with portal name
     */
    private static final String PORTAL_NAME_REPLACE = "\\*\\*PORTAL_NAME\\*\\*";

    private Settings settings = null;

    private String getTemplateFilename() {
        return settings.getConfigPath() + File.separator + TEMPLATE_FILENAME;
    }

    private String getFinalFilename() {
        return settings.getConfigPath() + File.separator + LOG4J_FILENAME;
    }

    /**
     * Read the log4j template file and return its contents
     * @return contents of TEMPLATE_URL file as a String, null if there was a
     * problem reading the file
     */
    private String getXmlTemplateFileContents() {
        String content = null;
        try {
            content = FileUtils.readFileToString(new File(getTemplateFilename()));
        } catch (IOException ex) {
            logger.error(String.format("unable to read '%s' - reason %s", getTemplateFilename(), ex.getMessage()));
        }
        return content;
    }

    /**
     * Perform variable substitution on the log4j template, save this as the
     * real log4j.xml configuration file, then reload log4j
     */
    public void load() {
        logger.info("reloading log4j configuration");
        rewriteLog4jXml(getXmlTemplateFileContents());

        DOMConfigurator.configure(getFinalFilename());
    }

    /**
     * If passed in template is not empty, perform variable substitution and write
     * it to LOG4J_URL
     * @param template Contents of the template file - previously obtained by
     * calling getXmlTemplateFileContents()
     */
    private void rewriteLog4jXml(String template) {
        if (Validate.empty(template)) {
            logger.error(String.format(
                    "refusing to replace '%s' because template file '%s' does not exist or is empty",
                    getFinalFilename(), getTemplateFilename()));
        } else {
            template = template.replaceAll(SERVER_NAME_REPLACE, resolveHostname.resolveHostName());
            template = template.replaceAll(PORTAL_NAME_REPLACE, portalNaming.getPortalName());

            try {
                FileUtils.writeStringToFile(new File(getFinalFilename()), template);
                logger.info("log4j configuration updated");
            } catch (IOException ex) {
                logger.error("error writing log4j.xml: " + ex.getMessage());
            }
        }
    }

    public PortalNaming getPortalNaming() {
        return portalNaming;
    }

    @Required
    public void setPortalNaming(PortalNaming portalNaming) {
        this.portalNaming = portalNaming;
    }

    public ResolveHostName getResolveHostname() {
        return resolveHostname;
    }

    @Required
    public void setResolveHostname(ResolveHostName resolveHostname) {
        this.resolveHostname = resolveHostname;
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(settings, "settings must be set");
        Assert.notNull(resolveHostname, "resolveHostname must be set");
        Assert.notNull(portalNaming, "portalNaming must be set");
    }

    
}
