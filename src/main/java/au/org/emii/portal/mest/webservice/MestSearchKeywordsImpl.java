package au.org.emii.portal.mest.webservice;

import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.net.HttpConnectionImpl;
import au.org.emii.portal.value.SearchCatalogue;
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
public class MestSearchKeywordsImpl implements MestSearchKeywords {
	/**
	 * Logger instance
	 */
	private final Logger logger = Logger.getLogger(getClass());
    private HttpConnection httpConnection = null;
	
    @Override
	public List<String> getSearchTerms(SearchCatalogue sc) {
		// there are duplicates in the list so we use a hashset which only allows
		// one of a particular item
		HashSet<String> searchTerms = new HashSet<String>();
        // initialise to empty list to prevent npe on use
		List<String> sortedSearchTerms = new ArrayList<String>();
		String uri = sc.searchKeywordsUri();
		InputStream in = null;
		
		try {
			URLConnection con = httpConnection.configureURLConnection(uri);
			in = con.getInputStream();
			String response = IOUtils.toString(in);
			if (response != null) {
				logger.info("requesting MEST keywords from '" + uri + "' - raw response: " + response.length() + " bytes");
				
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
						"MEST server at '" + uri + "'while trying to obtain list of terms"
				);
			}
		} 
		catch (IOException e) {
			logger.error(e.getMessage() + " connecting to " + uri);
		}
		
		
		return sortedSearchTerms;
		
	}

    public HttpConnection getHttpConnection() {
        return httpConnection;
    }

    public void setHttpConnection(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }


}
