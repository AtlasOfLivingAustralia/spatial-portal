package org.ala.spatial.search;

import java.util.ArrayList;
import java.util.List;

public class TaxaCommonSearchSummary {
	
	private int recordsReturned;
	private int totalRecords;
	private int startIndex;
	private String sort;
	private String dir;
	private String pageSize;
	private List<String> results = new ArrayList<String>();
	private List<TaxaCommonSearchResult> resultList = new ArrayList<TaxaCommonSearchResult>();
	
	
	
	public List<TaxaCommonSearchResult> getResultList() {
		return resultList;
	}
	public void setResultList(List<TaxaCommonSearchResult> resultList) {
		this.resultList = resultList;
	}
	public void addResult(TaxaCommonSearchResult ts){
		resultList.add(ts);
	}
	public void removeResult(TaxaCommonSearchResult ts) {
		resultList.remove(ts);
	}
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
	
	
	

}
