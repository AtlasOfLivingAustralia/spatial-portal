/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

/**
 *
 * @author geoff
 */
public interface ConfigurationFile {

    /**
     * Read and validate the configuration file, then return it as a string
     * if it validated.
     * @return Contents of config file as string if valid, otherwise null
     */
    String configurationFileContents();

    /**
     * Save the content string inside the config file.  Make sure you've validated
     * the content before attempting to save (use validateConfigFileContents())
     * @param content XML string to save as config file contents
     * @return true on success, otherwise false
     */
    boolean saveAsConfigFile(String content);

    boolean validateConfigFileContents(String content, boolean quiet);

    /**
     * Validate contents of config file to schema
     *
     * File contents must be UTF-8!
     * @param content
     * @return
     */
    boolean validateConfigFileContents(String content);

}
