package au.org.ala.portal;

public class SpeciesSearch {
	private enum SearchType { CommonName, ScientificName }
	private String searchTerm;
	private SearchType searchType;
	
	public SearchType getSearchType() {
		return searchType;
	}
	public void setSearchType(SearchType searchType) {
		this.searchType = searchType;
	}
	public String getSearchTerm() {
		return searchTerm;
	}
	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}
	
	

}
