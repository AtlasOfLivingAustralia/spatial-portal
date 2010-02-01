/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.commons.io.FileUtils;


/**
 * Load ISO country codes and names from xml file, return them as a map of code
 * -> value
 *
 * @author geoff
 */
public class IsoCountries {

        private static Logger logger = Logger.getLogger(IsoCountries.class);
        public final static int KEY = 0;
        public final static int VALUE = 1;

        /**
         * Load the iso country codes (for use within registration system) into
         * a map.  This will be stored in the config class and be made available
         * through the user's sesion
         *
         * @param filename XML file of country codes, obtained from
         * web/geonetwork/loc/ru/xml/countries.xml within the geonetwork source
         * tree and existing somewhere on the classpath for this project
         * @return A list of ISO country codes.  Each element contains a two element array
         * of country code followed by value.  Use the integer constants above
         * to reference them
         */
        public static List<String[]> parseCountryCodesFromFile(String filename) {

                // use a linked hash map to preseve ordering
                List<String[]> codes = new ArrayList<String[]>();
                try {
                        List<String> lines = FileUtils.readLines(new File(IsoCountries.class.getClassLoader().getResource(filename).getFile()));
                        for (String line : lines) {
                                if (line != null && line.contains("iso2")) {
                                        // the key is whatever is between the first and second double quote
                                        String key = line.replaceAll("^[^\"]*\"([^\"]*)\".*$", "$1");

                                        // the value is whatever is between the >< element delimiters
                                        String value = line.replaceAll("^[^>]*>([^>]*)<.*$","$1");

                                        codes.add(new String[] {key,value});
                                }
                                // else skip lines that dont have iso2 in them (xml prologue, containing element, etc)
                        }
                } catch (IOException ex) {
                        logger.error("Error reading iso country files",ex);
                }
                logger.debug("loaded " + codes.size() + " country codes from " + filename);

                return codes;
        }
}
