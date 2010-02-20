/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.web;

import au.org.emii.portal.Validate;
import au.org.emii.portal.config.PortalNaming;
import au.org.emii.portal.config.ResolveHostName;
import au.org.emii.portal.config.ResolveHostNameImpl;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Read log4j_template, substitute variables in the pattern, then save it
 * to log4j.xml and get log4j to reload its configuration.
 *
 * I was originally doing this with MDC but it doesn't work properly if
 * you want to change values from another thread (I do) and also leaks
 * references on server shutdown.
 *
 */
public class Log4jLoader {

    /**
     * Resolve the current hostname
     */
    private ResolveHostName resolveHostname = null;

    /**
     * Resolve the current portal name
     */
    private PortalNaming portalNaming = null;

    /**
     * Filename for template - / refers to /WEB-INF/classes which is /src/main/resources
     * at build time
     */
    private static final String TEMPLATE_FILENAME = "/log4j_template.xml";
    /**
     * URL for template - this resolves the template filename to a real filesystem path
     */
    private static URL TEMPLATE_URL = Log4jLoader.class.getResource(TEMPLATE_FILENAME);
    /**
     * Filename for real log4j.xml configuration
     */
    private static final String LOG4J_FILENAME = "/log4j.xml";
    /**
     * Filesystem path for log4j configuration
     */
    private static URL LOG4J_URL = Log4jLoader.class.getResource(LOG4J_FILENAME);
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

    /**
     * Read the log4j template file and return its contents
     * @return contents of TEMPLATE_URL file as a String, null if there was a
     * problem reading the file
     */
    private String getXmlTemplateFileContents() {
        String content = null;
        try {
            content = FileUtils.readFileToString(new File(TEMPLATE_URL.getFile()));
        } catch (IOException ex) {
            logger.error(String.format("unable to read '%s' - reason %s", TEMPLATE_FILENAME, ex.getMessage()));
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

        DOMConfigurator.configure(LOG4J_URL);
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
                    LOG4J_URL.getFile(), TEMPLATE_URL.getFile()));
        } else {
            template = template.replaceAll(SERVER_NAME_REPLACE, resolveHostname.resolveHostName());
            template = template.replaceAll(PORTAL_NAME_REPLACE, portalNaming.getPortalName());

            try {
                FileUtils.writeStringToFile(new File(LOG4J_URL.getFile()), template);
                logger.info("log4j configuration updated");
            } catch (IOException ex) {
                logger.error("error writing log4j.xml: " + ex.getMessage());
            }
        }
    }

    public PortalNaming getPortalNaming() {
        return portalNaming;
    }

    public void setPortalNaming(PortalNaming portalNaming) {
        this.portalNaming = portalNaming;
    }

    public ResolveHostName getResolveHostname() {
        return resolveHostname;
    }

    public void setResolveHostname(ResolveHostName resolveHostname) {
        this.resolveHostname = resolveHostname;
    }

    
}
