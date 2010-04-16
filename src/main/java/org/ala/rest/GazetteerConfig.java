package org.ala.rest;

import java.io.File;

public class GazetteerConfig {

    public GazetteerConfig(File file) {
        //open the gazetteer.xml
            //FIXME: read xml file
        //read in the  values
    }

    public String getIdAttributeName(String layerName) {
        //returns the gazetteer identifier for the layer
        //FIXME: read from xml
        if (layerName.contentEquals("GeoRegionFeatures"))
               return "id";
        if (layerName.contentEquals("NamedPlaces"))
               return "NAME";
        if (layerName.contentEquals("aus0"))
               return "name_0";
        if (layerName.contentEquals("aus1"))
               return "name_1";
        if (layerName.contentEquals("aus2"))
               return "name_2";
        if (layerName.contentEquals("imcra4_pb"))
               return "pb_name";
        if (layerName.contentEquals("ibra_reg_shape"))
               return "reg_name";
        if (layerName.contentEquals("all_gaz_2008"))
               return "record_id";
        else
            return "fixme";
    }


    public String getNameAttributeName(String layerName) {
        //returns the gazetteer identifier for the layer
        if (layerName.contentEquals("GeoRegionFeatures"))
               return "name";
        if (layerName.contentEquals("NamedPlaces"))
               return "NAME";
         if (layerName.contentEquals("aus0"))
               return "name_0";
        if (layerName.contentEquals("aus1"))
               return "name_1";
        if (layerName.contentEquals("aus2"))
               return "name_2";
        if (layerName.contentEquals("imcra4_pb"))
               return "pb_name";
        if (layerName.contentEquals("ibra_reg_shape"))
               return "reg_name";
        if (layerName.contentEquals("all_gaz_2008"))
               return "name";
        else
            return "fixme";
    }
}