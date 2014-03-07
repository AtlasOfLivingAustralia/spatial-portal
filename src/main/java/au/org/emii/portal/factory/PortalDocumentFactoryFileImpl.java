/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.factory;

import au.org.emii.portal.config.ConfigurationFile;
import au.org.emii.portal.config.xmlbeans.PortalDocument;
import au.org.emii.portal.settings.Settings;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;

import java.io.*;

/**
 * Support for validation, reading and writing of configuration file
 * <p/>
 * File backed implementation of PortalDocumentFactory
 *
 * @author geoff
 */
public class PortalDocumentFactoryFileImpl implements PortalDocumentFactory, ConfigurationFile, InitializingBean {

    /**
     * Logger instance
     */
    private Logger logger = Logger.getLogger(this.getClass());

    private final static String CONFIG_FILE = "webportal_config.xml";
    private Settings settings = null;

    @Override
    public boolean validateConfigFileContents(String content, boolean quiet) {
        boolean valid = false;
        Level level = (quiet) ? Level.DEBUG : Level.INFO;
        try {
            InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
            PortalDocument doc = PortalDocument.Factory.parse(is);
            if (doc.validate()) {
                valid = true;
            }
        } catch (XmlException ex) {
            logger.log(level, "Invalid XML validating string for conformity to config file xml schema (not the real config file!) - CAUSE: " + ex.getMessage());
        } catch (IOException ex) {
            logger.error("IO error reading inputstream from byte array(?) should never happen - CAUSE: " + ex.getMessage());
        }
        return valid;

    }

    /**
     * Lookup the the name of the config file we should be reading from the
     * environement then validate and parse it returning a pointer to the root
     * element.
     * <p/>
     * If an error occurs here (null returned) then the system is FUBAR
     *
     * @return PortalDocument instance if reading succeeded, null if an error
     * was encountered
     */
    @Override
    public PortalDocument createPortalDocumentInstance() {
        PortalDocument portalDocument = null;

        // Have xmlbeans read the file and parse it
        InputStream is = null;
        try {
            is = new FileInputStream(getConfigFilename());

            portalDocument = PortalDocument.Factory.parse(is);

            // if the XML is valid, we're good to go...
            if (portalDocument.validate()) {
                logger.debug("configuration file is valid xml");
            } else {
                logger.error(
                        "invalid XML in configuration file! - validate manually with "
                                + "xmllint --schema on the command line to determine the problem!");
                portalDocument = null;
            }
        } catch (FileNotFoundException e) {
            portalDocument = null;
            logger.error("Could not load portal configuration file from: " + getConfigFilename());
        } catch (XmlException e) {
            portalDocument = null;
            logger.error("Unknown error while processing XML in configuration file - check this stack trace", e);
        } catch (IOException e) {
            portalDocument = null;
            logger.error("IOException reading configuration - should never happen, you may have big problems! - check this stack trace", e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                logger.error("Error closing " + getConfigFilename(), e);
            }
        }

        return portalDocument;
    }

    /**
     * Validate contents of config file to schema
     * <p/>
     * File contents must be UTF-8!
     *
     * @param content
     * @return
     */
    @Override
    public boolean validateConfigFileContents(String content) {
        return validateConfigFileContents(content, false);
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /**
     * Return full path and filename of config file
     *
     * @return
     */
    public String getConfigFilename() {
        return settings.getConfigPath() + File.separator + CONFIG_FILE;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(settings);
    }

}
