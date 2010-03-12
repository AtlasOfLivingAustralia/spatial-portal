/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.factory;

import au.org.emii.portal.value.SearchCatalogue;


/**
 *
 * @author geoff
 */
public interface SearchCatalogueFactory {

    public SearchCatalogue createInstance(au.org.emii.portal.config.xmlbeans.SearchCatalogue xmlSearchCatalog);

}
