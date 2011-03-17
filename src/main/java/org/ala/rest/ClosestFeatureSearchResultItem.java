package org.ala.rest;

import java.util.List;
import org.apache.lucene.document.Fieldable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import java.io.Serializable;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

@XStreamAlias("result")
public class ClosestFeatureSearchResultItem implements Serializable {

    String id;
    String name;
    String serial;
    String description;
    String state;
    String layerName;
    String layerAlias;
    String idAttribute1;
    String distance;
    String bearing;
    @XStreamAlias("xlink:href")
    @XStreamAsAttribute
    String link;
    private static final Logger logger = Logging.getLogger("org.ala.rest.SearchResultItem");
    //GazetteerConfig gc = GeoServerExtensions.bean(GazetteerConfig.class);

    ClosestFeatureSearchResultItem(String layerName, String name, String idAttribute1, String distance, String bearing) {
        GazetteerConfig gc = new GazetteerConfig();
        layerAlias = gc.getLayerAlias(layerName);
        if (layerAlias.compareTo("") == 0){
            layerAlias = layerName;
        }
        this.id = layerAlias + "/" + idAttribute1;
        this.name = name;
        this.layerName = layerName;
        this.idAttribute1 = idAttribute1;
        this.distance = distance;
        this.bearing = bearing;
        this.link = "/gazetteer/";
        this.link += this.id.replace(" ", "_") + ".json";
    }

    ClosestFeatureSearchResultItem(List<Fieldable> fields, Boolean includeLink, String distance, String bearing) {

        this.description = "";
        for (Fieldable field : fields) {
            if (field.name().contentEquals("name")) {
                this.name = field.stringValue();
            } else if (field.name().contentEquals("serial")) {
                this.serial = field.stringValue();
            } else if (field.name().contentEquals("state")) {
                this.state = field.stringValue();
            } else if (field.name().contentEquals("layerName")) {
                GazetteerConfig gc = new GazetteerConfig();
                //if a layer alias exists the id will always use the alias in preference to the layer name
                layerAlias = gc.getLayerAlias(field.stringValue());
                if (layerAlias.compareTo("") == 0) {
                    layerAlias = layerName;
                }
            } else if (field.name().contentEquals("idAttribute1")) {
                this.idAttribute1 = field.stringValue();
            } else {
                this.description += field.stringValue() + ",";
            }
        }
        this.id = this.layerAlias + "/" + idAttribute1.replace(" ", "_");
        this.distance = distance;
        this.bearing = bearing;

        if (!description.contentEquals("")) {
            description = description.substring(0, description.length() - 1);
        }
        if (includeLink == true) {
            this.link = "/gazetteer/";
            this.link += id + ".json";
        }
    }
}
