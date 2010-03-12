/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.mest.webservice;

import au.org.emii.portal.value.SearchCatalogue;
import java.util.List;

/**
 *
 * @author geoff
 */
public interface MestSearchKeywords {

            List<String> getSearchTerms(SearchCatalogue sc);

}
