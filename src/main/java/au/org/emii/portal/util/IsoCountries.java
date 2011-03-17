/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import java.util.List;

/**
 *
 * @author geoff
 */
public interface IsoCountries {
    /**
     * Index of the country code in the array that gets returned for each country
     */
    public final static int CODE = 0;
    /**
     * Index of the country code in the array that gets returned for each country
     */
    public final static int NAME = 1;

    /**
     * Return a list of string arrays, one array per country code, with indexes
     * corresponding to KEY and VALUE as defined above
     * @return
     */
    public List<String[]> getCountries();

}
