/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.settings;

import java.util.Map;

/**
 * @author geoff
 */
public interface SettingsSupplementary {

    /**
     * Read a value from the xml config file's configuration section.
     * <p/>
     * If the key is missing, throw a null pointer exception
     *
     * @param key
     * @return corresponding value as a String
     */
    String getValue(String key);

    /**
     * Read a value from the xml config file's configuration section and parse a
     * boolean from it. Will throw a runtime exception if parsing fails
     *
     * @param key
     * @return
     */
    boolean getValueAsBoolean(String key);

    /**
     * Read a value from the xml config file's configuration section and parse a
     * double from it. Will throw a runtime exception if parsing fails
     * (NumberFormatException)
     *
     * @param key
     * @return
     */
    double getValueAsDouble(String key);

    /**
     * Read a value from the xml config file's configuration section and parse a
     * float from it. Will throw a runtime exception if parsing fails
     * (NumberFormatException)
     *
     * @param key
     * @return
     */
    float getValueAsFloat(String key);

    /**
     * Read a value from the xml config file's configuration section and parse
     * an integer from it. Will throw a runtime exception if parsing fails
     * (NumberFormatException)
     *
     * @param key
     * @return
     */
    int getValueAsInt(String key);

    /**
     * Find the size of a pixel value key, eg (String) 320px will return (int)
     * 320
     *
     * @param key to lookup
     * @return
     */
    int getValueAsPx(String key);

    /**
     * Non-static accessor for use in el
     *
     * @return
     */
    Map<String, String> getValues();

    void setValues(Map<String, String> values);

}
