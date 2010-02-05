package au.org.emii.portal;

import java.io.Serializable;
import java.util.List;

public class SearchCatalogue extends AbstractIdentifier {

	private static final long serialVersionUID = 1L;
	private String uri = null;
	private String protocol = null;
	private String version = null;
	
	/**
	 * List of search terms that are findable in this mest instance
	 * obtained by asking the MEST for a list of search terms
	 */
	private List<String> searchTerms = null;
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public void copyFrom(au.org.emii.portal.config.SearchCatalogue sc) {
		setId(sc.getId());
		setName(sc.getName());
		setDescription(sc.getDescription());
		setUri(sc.getUri());
		setProtocol(sc.getProtocol());
		setVersion(sc.getVersion());
	}
	
	public String searchTermUri() {
		return 
			protocol + "://" + 
			uri + 
			Config.getValue("mest_keyword_path");
	}
	public List<String> getSearchTerms() {
		return searchTerms;
	}
	public void setSearchTerms(List<String> searchTerms) {
		this.searchTerms = searchTerms;
	}
	
}
