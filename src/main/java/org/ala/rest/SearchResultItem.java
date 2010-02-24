package org.ala.rest;

public class SearchResultItem {
    String name;
    Integer fid;
    
    SearchResultItem(String name, Integer fid) {
	this.name = "Some " + name + " place";
	this.fid = fid;
    }
}
