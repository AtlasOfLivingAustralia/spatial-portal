/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

/**
 * @author geoff
 */
public interface ConfigurationFile {

    boolean validateConfigFileContents(String content, boolean quiet);

    /**
     * Validate contents of config file to schema
     * <p/>
     * File contents must be UTF-8!
     *
     * @param content
     * @return
     */
    boolean validateConfigFileContents(String content);

}
