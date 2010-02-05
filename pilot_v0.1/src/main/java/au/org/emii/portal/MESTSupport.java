package au.org.emii.portal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Utility methods for getting data from MEST search catalogues
 * @author geoff
 *
 */
public class MESTSupport {
	/**
	 * Logger instance
	 */
	private static final Logger logger = Logger.getLogger(MESTSupport.class.getName());
	
	public static List<String> getSearchTerms(SearchCatalogue sc) {
		// there are duplicates in the list so we use a hashset which only allows
		// one of a particular item
		HashSet<String> searchTerms = new HashSet<String>();
		List<String> sortedSearchTerms = null;
		String uri = sc.searchTermUri();
		InputStream in = null;
		
		try {
			logger.debug("requesting MEST keywords from '" + uri + "'");
			URLConnection con = HttpConnection.configureURLConnection(uri);
			in = con.getInputStream();
			String response = IOUtils.toString(in);
			if (response != null) {
				logger.debug("raw response: " + response.length() + " bytes");
				
				// remove html tags
				response = response.replaceAll("<\\/?(li|ul)>", "");
				
				// split on line breaks
				String[] terms = response.split("\n");
				
				// each term may contain '|' followed by more terms
				for (String string : terms) {
					String[] lineTerms = string.split("\\|");
					for (String term : lineTerms) {
						term = term.trim();
						if (! term.equals("")) {
							// strip trailing garbage ';' characters
							term = term.replaceAll(";$", "");
							searchTerms.add(term);
						}
					}
				}		
			
				sortedSearchTerms = new ArrayList<String>(searchTerms);
				Collections.sort(sortedSearchTerms);
			}
			else {
				logger.error(
						"No response (or response timed out) received from " +
						"MEST server while trying to obtain list of terms"
				);
			}
		} 
		catch (IOException e) {
			logger.error(e.getMessage() + " connecting to " + uri);
		}
		
		
		return sortedSearchTerms;
		
	}
}
