/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.util;

import au.org.emii.portal.aspect.CheckNotNull;
import au.org.emii.portal.settings.Settings;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Required;

/**
 * Load ISO country codes and names from xml file
 *
 * @author geoff
 */
public class IsoCountriesImpl implements IsoCountries {

    private Logger logger = Logger.getLogger(getClass());

    /**
     * List of country codes
     */
    private List<String[]> countries = null;

    /**
     */
    

    private Settings settings = null;

    /**
     * Return a list of string arrays, one array per country code, with indexes
     * corresponding to KEY and VALUE as defined above
     * @return
     */
    @Override
    public List<String[]> getCountries() {
        if (countries == null) {
            countries = parseCountryCodes();
        }
        return countries;
    }


    private List<String> readFile() {
        List<String> lines = null;

        //  XML file of country codes, obtained from web/geonetwork/loc/ru/xml/countries.xml
        // within the geonetwork source tree and existing somewhere on the classpath
        URL url = getClass().getResource(settings.getIsoCountriesFilename());
        if (url == null) {
            logger.error(
                    "Error loading list of countries from xml file for registration system " +
                    "'"+ settings.getIsoCountriesFilename() + "' not found on classpath");
        } else {
            try {
                lines = FileUtils.readLines(new File(url.getFile()));
            } catch (IOException ex) {
                logger.error("Error reading iso country files.  Reason:  " + ex.getMessage());
                lines = null;
            }
        }
        return lines;
    }

    /**
     * Load the iso country codes (for use within registration system) into
     * a map.  This will be stored in the config class and be made available
     * through the user's sesion
     *
     * @return A list of ISO country codes.  Each element contains a two element array
     * of country code followed by value.  Use the integer constants above
     * to reference them
     */
    private List<String[]> parseCountryCodes() {

        // use a linked hash map to preseve ordering
        List<String[]> codes = new ArrayList<String[]>();
        List<String> lines = readFile();
        if (lines != null) {
            for (String line : lines) {
                if (line != null && line.contains("iso2")) {
                    // the key is whatever is between the first and second double quote
                    String key = line.replaceAll("^[^\"]*\"([^\"]*)\".*$", "$1");

                    // the value is whatever is between the >< element delimiters
                    String value = line.replaceAll("^[^>]*>([^>]*)<.*$", "$1");

                    codes.add(new String[]{key, value});
                }
                // else skip lines that dont have iso2 in them (xml prologue, containing element, etc)
            }
        }
        logger.info("loaded " + codes.size() + " country codes from " + settings.getIsoCountriesFilename());

        return codes;
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    @CheckNotNull
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    
}
