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

    SearchResultItem(String layerName, String id) {
        this.name = id;
        this.type = layerName;
        this.link = "/geoserver/rest/gazetteer/";
        this.link += this.type + '/' + id.replace(" ", "_") + ".json";
    }

    SearchResultItem(List<Fieldable> fields, Boolean includeLink) {
        
        String id = "";
        this.description = "";
        for (Fieldable field : fields) {
            if (field.name().contentEquals("name")) {
                this.name = field.stringValue();
            }
            else if (field.name().contentEquals("serial")) {
                this.serial = field.stringValue();
            }
            else if (field.name().contentEquals("state")) {
                this.state = field.stringValue();
            }
            else if (field.name().contentEquals("type")) {
                this.type = field.stringValue();
            }
            else if (field.name().contentEquals("id")) {
                id = field.stringValue();
            }
            else
                this.description+=field.stringValue() + ",";
        }
        if (!description.contentEquals(""))
            description  = description.substring(0,description.length()-1);
        if (includeLink == true) {
            this.link = "/geoserver/rest/gazetteer/";
            this.link += this.type + '/' + id.replace(" ", "_") + ".json";
        }
    }
}
