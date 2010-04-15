package org.ala.rest;

import java.io.File;

public class GazetteerConfig {

    public GazetteerConfig(File file) {
        //open the gazetteer.xml

        //read in the  values
    }

    public String getIdAttributeName(String layerName) {
        //returns the gazetteer identifier for the layer
        if (layerName.contentEquals("GeoRegionFeatures"))
               return "id";
        if (layerName.contentEquals("NamedPlaces"))
               return "NAME";
        else
            return "fixme";
    }


    public String getNameAttributeName(String layerName) {
        //returns the gazetteer identifier for the layer
        if (layerName.contentEquals("GeoRegionFeatures"))
               return "name";
        if (layerName.contentEquals("NamedPlaces"))
               return "NAME";
        else
            return "fixme";
    }
}