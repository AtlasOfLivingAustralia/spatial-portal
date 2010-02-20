/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import au.org.emii.portal.aspect.CheckNotNull;
import au.org.emii.portal.aspect.LogSetterValue;
import au.org.emii.portal.config.xmlbeans.PortalDocument;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.Required;

/**
 * Support for validation, reading and writing of configuration file
 * @author geoff
 */
public class ConfigurationFile {

    /**
     * Logger instance
     */
    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * filename for config file - as found in the system property WEBPORTAL_CONFIG_FILE
     *
     * HINT: start jvm with -DWEBPORTAL_CONFIG_FILE=CONFIG_FILE_PATH
     * eg: -DWEBPORTAL_CONFIG_FILE="/path/to/config/file.xml"
     *
     * This will be wired up by spring
     */
    private String filename = null;

    /**
     * Save the content string inside the config file.  Make sure you've validated
     * the content before attempting to save (use validateConfigFileContents())
     * @param content XML string to save as config file contents
     * @return true on success, otherwise false
     */
    public boolean saveAsConfigFile(String content) {
        boolean saved = false;
        try {
            FileUtils.writeStringToFile(new File(filename), content);
            saved = true;
        } catch (IOException ex) {
            logger.error("Error saving config file.  CAUSE: " + ex.getMessage());
        }

        return saved;
    }

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
            logger.log(level,"Invalid XML validating string for conformity to config file xml schema (not the real config file!) - CAUSE: " + ex.getMessage());
        } catch (IOException ex) {
            logger.error("IO error reading inputstream from byte array(?) should never happen - CAUSE: " + ex.getMessage());
        }
        return valid;

    }

            /**
     * Lookup the the name of the config file we should be reading from the environement
     * then validate and parse it returning a pointer to the root element.
     *
     * If an error occurs here (null returned) then the system is FUBAR
     *
     * @return PortalDocument instance if reading succeeded, null if an error was encountered
     */
    public PortalDocument readConfigFile() {
        PortalDocument portalDocument = null;

        // Have xmlbeans read the file and parse it
        try {
            InputStream is = new FileInputStream(filename);
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
            logger.error("Could not load portal configuration file from: " + filename);
        } catch (XmlException e) {
            portalDocument = null;
            logger.error("Unknown error while processing XML in configuration file - check this stack trace", e);
        } catch (IOException e) {
            portalDocument = null;
            logger.error("IOException reading configuration - should never happen, you may have big problems! - check this stack trace", e);
        }

        return portalDocument;
    }

    /**
     * Validate contents of config file to schema
     *
     * File contents must be UTF-8!
     * @param content
     * @return
     */
    public boolean validateConfigFileContents(String content) {
        return validateConfigFileContents(content, false);
    }

    public String getFilename() {
        return filename;
    }

    @Required
    @CheckNotNull
    @LogSetterValue
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Read and validate the configuration file, then return it as a string
     * if it validated.
     * @return Contents of config file as string if valid, otherwise null
     */
    public String configurationFileContents() {
        PortalDocument doc = readConfigFile();
        return (doc == null) ? null : doc.toString();
    }
}
