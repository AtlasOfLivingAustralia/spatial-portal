package au.org.ala.portal;

import java.util.ArrayList;
import java.util.List;

public class TaxaScientificSearchSummary {
	
	private int recordsReturned;
	private int totalRecords;
	private int startIndex;
	private String sort;
	private String dir;
	private String pageSize;
	private List<String> results = new ArrayList<String>();
	private List<TaxaScientificSearchResult> resultList = new ArrayList<TaxaScientificSearchResult>();
	public int getRecordsReturned() {
		return recordsReturned;
	}
	public void setRecordsReturned(int recordsReturned) {
		this.recordsReturned = recordsReturned;
	}
	public int getTotalRecords() {
		return totalRecords;
	}
	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}
	public int getStartIndex() {
		return startIndex;
	}
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}
	public String getSort() {
		return sort;
	}
	public void setSort(String sort) {
		this.sort = sort;
	}
	public String getDir() {
		return dir;
	}
	public void setDir(String dir) {
		this.dir = dir;
	}
	public String getPageSize() {
		return pageSize;
	}
	public void setPageSize(String pageSize) {
		this.pageSize = pageSize;
	}
	public List<String> getResults() {
		return results;
	}
	public void setResults(List<String> results) {
		this.results = results;
	}
	public List<TaxaScientificSearchResult> getResultList() {
		return resultList;
	}
	public void setResultList(List<TaxaScientificSearchResult> resultList) {
		this.resultList = resultList;
	}
	
	public void addResult(TaxaScientificSearchResult ts){
		resultList.add(ts);
	}
	public void removeResult(TaxaScientificSearchResult ts) {
		resultList.remove(ts);
	}

}
