package org.ala.rest;

public class SearchResultItem {
    String name;
    String serial;
    String description;
    String state;

    SearchResultItem(String name, String serial,String description,String state) {
	this.name = name;
	this.serial = serial;
        this.description = description;
	this.state = state;
    }
}
