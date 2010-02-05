package au.org.emii.portal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.ResourceBundle;

public class Config {
	
	/**
	 * name of the host we are running from
	 */
	private static String hostname = null;
	
	/**
	 * Key/value pairs
	 * From the xml config file's configuration section
	 */
	private static HashMap<String, String> values = null;
	
	/**
	 * messages and icons from the language pack
	 */
	private static ResourceBundle lang = ResourceBundle.getBundle("webportal");
	


	public static void setValues(HashMap<String, String> values) {
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
					"Value for key '" + key + "' missing from XML configuration file"
				);
			}
		}
		else {
			throw new RuntimeException(
					"Attempt to read configuration value with Config.getValue() " +
					"before values have been loaded from configuration file"
			);
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
	
	public HashMap<String,String> getLangMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		for (String key : lang.keySet()) {
			map.put(key, lang.getString(key));
		}
		
		return map;
	}

	
	public static void setLang(ResourceBundle lang) {
		Config.lang = lang;
	}

	public static HashMap<String, String> getStaticValues() {
		return values;
	}

	/**
	 * Non-static accessor for use in el
	 * @return
	 */
	public HashMap<String, String> getValues() {
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
		return Integer.parseInt(value.substring(0, value.length() -2));
	}
	
	public static String getHostname() {
		if (hostname == null) {
			try {
				hostname = InetAddress.getLocalHost().getCanonicalHostName();
			} 
			catch (UnknownHostException e) {
				// default to localhost
				hostname = "localhost";
			}
		}
		return hostname;
	}
}
