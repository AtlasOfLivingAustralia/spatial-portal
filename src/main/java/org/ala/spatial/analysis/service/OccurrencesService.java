package org.ala.spatial.analysis.service;

import org.ala.spatial.analysis.index.OccurrencesIndex;

/**
 * facilitates access into occurrences index, 
 * 
 * records, species, points
 * 
 * @author adam
 *
 */
public class OccurrencesService {	
	/**
     * returns a list of (species names / type / count) for valid 
     * .beginsWith matches
     * 
     * @param filter begins with text to search for
     * @param limit limit on output
     * @return formatted species matches as String[]
     */
	static public String [] filterSpecies(String filter,int limit) {
		return OccurrencesIndex.filterIndex(filter,limit);
	}
}