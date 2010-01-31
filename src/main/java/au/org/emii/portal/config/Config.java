package au.org.emii.portal.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

public class Config {

    /**
     * name of the host we are running from
     */
    private static String hostname = null;

    /**
     * Key/value pairs
     * From the xml config file's configuration section
     */
    private static Map<String, String> values = null;

    /**
     * Key/value pairs for iso country codes -> iso country names (for geonetwork)
     */
    private static List<String[]> countryCodes = null;

    /**
     * messages and icons from the language pack
     */
    private static ResourceBundle lang = ResourceBundle.getBundle("webportal");

    public static void setValues(Map<String, String> values) {
        Config.values = values;
    }

    /**
     * Read a value from the xml config file's configuration section.
     *
     * If the key is missing, throw a null pointer exception
     * @param key
     * @return corresponding value as a String
     */
    public static String getValue(String key) {
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
     * Read a value from the xml config file's configuration section
     * and parse an integer from it.  Will throw a runtime exception
     * if parsing fails (NumberFormatException)
     * @param key
     * @return
     */
    public static int getValueAsInt(String key) {
        return Integer.parseInt(getValue(key));
    }

    /**
     * Read a value from the xml config file's configuration section
     * and parse a float from it.  Will throw a runtime exception
     * if parsing fails (NumberFormatException)
     * @param key
     * @return
     */
    public static float getValueAsFloat(String key) {
        return Float.parseFloat(getValue(key));
    }

    /**
     * Read a value from the xml config file's configuration section
     * and parse a double from it.  Will throw a runtime exception
     * if parsing fails (NumberFormatException)
     * @param key
     * @return
     */
    public static double getValueAsDouble(String key) {
        return Double.parseDouble(getValue(key));
    }

    /**
     * Read a value from the xml config file's configuration section
     * and parse a boolean from it.  Will throw a runtime exception
     * if parsing fails
     * @param key
     * @return
     */
    public static boolean getValueAsBoolean(String key) {
        return Boolean.parseBoolean(getValue(key));
    }

    /**
     * Retrieve a language string from the language pack.
     * @param key
     * @return
     */
    public static String getLang(String key) {
        return lang.getString(key);
    }

    public static ResourceBundle getStaticLang() {
        return lang;
    }

    /**
     * Non-static accessor for use in el (doesn't seem to work -
     * try getLangMap (.langMap) instead...
     * @return
     */
    public ResourceBundle getLang() {
        return lang;
    }

    public HashMap<String, String> getLangMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String key : lang.keySet()) {
            map.put(key, lang.getString(key));
        }

        return map;
    }

    public static void setLang(ResourceBundle lang) {
        Config.lang = lang;
    }

    public static Map<String, String> getStaticValues() {
        return values;
    }

    /**
     * Non-static accessor for use in el
     * @return
     */
    public Map<String, String> getValues() {
        return values;
    }

    /**
     * Format a compound message:  See http://java.sun.com/j2se/1.4.2/docs/api/java/text/MessageFormat.html
     * @param key Key template message in language pack
     * @param arguments arguments for template
     * @return completed string
     */
    public static String getCompoundLang(String key, Object[] arguments) {
        return MessageFormat.format(getLang(key), arguments);
    }

    /**
     * Find the size of a pixel value key, eg (String) 320px will return
     * (int) 320
     *
     * @param key to lookup
     * @return
     */
    public static int getValueAsPx(String key) {
        String value = getValue(key);
        return Integer.parseInt(value.substring(0, value.length() - 2));
    }

    public static String getHostname() {
        if (hostname == null) {
            Logger logger = Logger.getLogger(Config.class);
            hostname = values.get("hostname");
            if (hostname == null) {
                try {
                    hostname = InetAddress.getLocalHost().getCanonicalHostName();
                } catch (UnknownHostException ex) {

                    try {
                        hostname = InetAddress.getLocalHost().getHostAddress();
                        logger.error(
                                "can't resolve " + hostname + " to a hostname - using " +
                                "naked IP address instead.  Please fix the broken DNS " +
                                "for this server or add <hostname>myhostname.com</myhostname> " +
                                "directive to config section in webportal config file");
                    } catch (UnknownHostException ex1) {
                        // can't even get an IP address - use "localhost"
                        logger.error(
                                "Your DNS is very broken - can't resolve an IP address for the server the portal is " +
                                "running on.  Requests to use the automatic squid caching for map layers will likely " +
                                "fail.  A quick workaround is to add a <hostname>myhostname.com</myhostname> " +
                                "directive to config section in webportal config file, but you should still fix " +
                                "your broken DNS");
                    }

                }
            }
            logger.info("setting hostname to '" + hostname + "'");
        }
        return hostname;
    }

        public static List<String[]> getCountryCodes() {
                return countryCodes;
        }

        public static void setCountryCodes(List<String[]> countryCodes) {
                Config.countryCodes = countryCodes;
        }


    
}
