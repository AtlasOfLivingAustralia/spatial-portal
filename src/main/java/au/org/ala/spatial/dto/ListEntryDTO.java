/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.ala.spatial.dto;

import org.json.simple.JSONObject;

/**
 * @author Adam
 */
public class ListEntryDTO {
    private String name;
    private String displayName;
    private String catagory1;
    private String catagory2;
    private String type;
    private float value;
    private JSONObject layerObject;
    private String domain;

    public ListEntryDTO(String name, String displayName, String catagory1, String catagory2, String type, String domain, JSONObject layerObject) {
        this.name = name;
        this.catagory1 = catagory1;
        this.catagory2 = catagory2;
        this.displayName = displayName;
        this.type = type;
        this.layerObject = layerObject;
        this.domain = domain;
        value = 0;
    }

    public String catagoryNames() {
        if (catagory2.length() > 0) {
            return " " + catagory1 + "; " + catagory2;
        } else {
            return " " + catagory1;
        }
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCatagory1() {
        return catagory1;
    }

    public String getCatagory2() {
        return catagory2;
    }

    public String getName() {
        return name;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public JSONObject getLayerObject() {
        return layerObject;
    }
}
