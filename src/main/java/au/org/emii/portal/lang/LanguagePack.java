/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.lang;

import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * @author geoff
 */
public interface LanguagePack {

    /**
     * Format a compound message: See
     * http://java.sun.com/j2se/1.4.2/docs/api/java/text/MessageFormat.html
     *
     * @param key       Key template message in language pack
     * @param arguments arguments for template
     * @return completed string
     */
    String getCompoundLang(String key, Object[] arguments);

    /**
     * Non-static accessor for use in el (doesn't seem to work - try getLangMap
     * (.langMap) instead...
     *
     * @return
     */
    ResourceBundle getLang();

    /**
     * Retrieve a language string from the language pack.
     *
     * @param key
     * @return
     */
    String getLang(String key);

    HashMap<String, String> getLangMap();

}
