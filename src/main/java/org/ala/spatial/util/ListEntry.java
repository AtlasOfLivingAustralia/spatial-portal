/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

import net.sf.json.JSONObject;

/**
 *
 * @author Adam
 */
public class ListEntry {
    public String name;
    public String displayname;
    public String catagory1;
    public String catagory2;
    public String type;
    public float value;
    public JSONObject layerObject;
    public String domain;

    public ListEntry(String name_, String displayname_, String catagory1_, String catagory2_, String type_, String domain, JSONObject layerObject) {
        name = name_;
        catagory1 = catagory1_;
        catagory2 = catagory2_;
        displayname = displayname_;
        type = type_; 
        this.layerObject = layerObject;
        this.domain = domain;
        value = 0;
    }

    public ListEntry(String string, String displayName, String string0, String string1, String string2, Object object) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String catagoryNames() {
        if (catagory2.length() > 0) {
            return " " + catagory1 + "; " + catagory2;
        } else {
            return " " + catagory1;
        }
    }
}
