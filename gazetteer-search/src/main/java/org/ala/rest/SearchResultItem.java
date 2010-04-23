package org.ala.rest;

import java.util.List;
import org.apache.lucene.document.Fieldable;

public class SearchResultItem {
    String name;
    String serial;
    String description;
    String state;
    String type;

    SearchResultItem(String name, String serial,String description,String state) {
	this.name = name;
	this.serial = serial;
        this.description = description;
	this.state = state;

    }

    SearchResultItem(List<Fieldable> fields) {
        for(Fieldable field : fields) {
            if (field.name().contentEquals("name"))
                this.name = field.stringValue();
            if (field.name().contentEquals("serial"))
                this.serial = field.stringValue();
            if (field.name().contentEquals("type"))
                this.description = field.stringValue();
            if (field.name().contentEquals("state"))
                this.state = field.stringValue();
            if (field.name().contentEquals("type"))
                this.type = field.stringValue();
        }

    }
}
