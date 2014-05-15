/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.lang;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Hold instance of the language pack
 *
 * @author geoff
 */
public class LanguagePackImpl implements LanguagePack {

    /**
     * messages and icons from the language pack
     */
    private ResourceBundle lang = ResourceBundle.getBundle("language");

    /**
     * Non-static accessor for use in el (doesn't seem to work - try getLangMap
     * (.langMap) instead...
     *
     * @return
     */
    @Override
    public ResourceBundle getLang() {
        return lang;
    }

    @Override
    public HashMap<String, String> getLangMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String key : lang.keySet()) {
            map.put(key, lang.getString(key));
        }

        return map;
    }

    /**
     * Retrieve a language string from the language pack.
     *
     * @param key
     * @return
     */
    @Override
    public String getLang(String key) {
        return lang.getString(key);
    }

    /**
     * Format a compound message: See
     * http://java.sun.com/j2se/1.4.2/docs/api/java/text/MessageFormat.html
     *
     * @param key       Key template message in language pack
     * @param arguments arguments for template
     * @return completed string
     */
    @Override
    public String getCompoundLang(String key, Object[] arguments) {
        return MessageFormat.format(getLang(key), arguments);
    }

}
