/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.settings;

import java.util.Map;

/**
 * @author geoff
 */
public class SettingsSupplementaryImpl implements SettingsSupplementary {

    /**
     * Key/value pairs From the xml config file's configuration section
     */
    private Map<String, String> values = null;

    @Override
    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Read a value from the xml config file's configuration section.
     * <p/>
     * If the key is missing, throw a null pointer exception
     *
     * @param key
     * @return corresponding value as a String
     */
    public String getValue(String key) {
        String value;
        if (values != null) {
            value = values.get(key);
            if (value == null) {
                throw new NullPointerException(
                        "Value for key '" + key + "' missing from XML configuration file");
            }
        } else {
            throw new RuntimeException(
                    "Attempt to read configuration value for '" + key + "' with Config.getValue() "
                            + "before values have been loaded from configuration file");
        }
        return value;
    }

    /**
     * Read a value from the xml config file's configuration section and parse
     * an integer from it. Will throw a runtime exception if parsing fails
     * (NumberFormatException)
     *
     * @param key
     * @return
     */
    @Override
    public int getValueAsInt(String key) {
        return Integer.parseInt(getValue(key));
    }

    /**
     * Read a value from the xml config file's configuration section and parse a
     * float from it. Will throw a runtime exception if parsing fails
     * (NumberFormatException)
     *
     * @param key
     * @return
     */
    @Override
    public float getValueAsFloat(String key) {
        return Float.parseFloat(getValue(key));
    }

    /**
     * Read a value from the xml config file's configuration section and parse a
     * double from it. Will throw a runtime exception if parsing fails
     * (NumberFormatException)
     *
     * @param key
     * @return
     */
    @Override
    public double getValueAsDouble(String key) {
        return Double.parseDouble(getValue(key));
    }

    /**
     * Read a value from the xml config file's configuration section and parse a
     * boolean from it. Will throw a runtime exception if parsing fails
     *
     * @param key
     * @return
     */
    @Override
    public boolean getValueAsBoolean(String key) {
        return Boolean.parseBoolean(getValue(key));
    }

    /**
     * Non-static accessor for use in el
     *
     * @return
     */
    @Override
    public Map<String, String> getValues() {
        return values;
    }

    /**
     * Find the size of a pixel value key, eg (String) 320px will return (int)
     * 320
     *
     * @param key to lookup
     * @return
     */
    @Override
    public int getValueAsPx(String key) {
        String value = getValue(key);
        return Integer.parseInt(value.substring(0, value.length() - 2));
    }

}
