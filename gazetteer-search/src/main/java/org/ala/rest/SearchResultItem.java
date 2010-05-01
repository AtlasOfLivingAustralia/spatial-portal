package org.ala.rest;

import java.util.List;
import org.apache.lucene.document.Fieldable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("result")
public class SearchResultItem {

    String name;
    String serial;
    String description;
    String state;
    String type;

    @XStreamAlias("xlink:href")
    @XStreamAsAttribute
    String link;

    SearchResultItem(String name, String serial, String description, String state) {
        this.name = name;
        this.serial = serial;
        this.description = description;
        this.state = state;

    }

    SearchResultItem(List<Fieldable> fields) {
        this.link = "http://localhost:8080/geoserver/rest/gazetteer/";
        String id = "";
        for (Fieldable field : fields) {
            if (field.name().contentEquals("name")) {
                this.name = field.stringValue();
            }
            if (field.name().contentEquals("serial")) {
                this.serial = field.stringValue();
            }
            if (field.name().contentEquals("type")) {
                this.description = field.stringValue();
            }
            if (field.name().contentEquals("state")) {
                this.state = field.stringValue();
            }
            if (field.name().contentEquals("type")) {
                this.type = field.stringValue();
            }
            if (field.name().contentEquals("id")) {
                id = field.stringValue();
            }
        }
        this.link += this.type + '/' + id.replace(" ", "_") + ".json";
    }
}
