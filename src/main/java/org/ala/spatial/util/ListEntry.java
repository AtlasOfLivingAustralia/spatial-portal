/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

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
    public int row_in_list;
    public int row_in_distances;
    public String uid;

    public ListEntry(String name_, String displayname_, String catagory1_, String catagory2_, String type_, float value_, int row_list, int row_distances, String uid_) {
        name = name_;
        value = value_;
        row_in_list = row_list;
        row_in_distances = row_distances;
        catagory1 = catagory1_;
        catagory2 = catagory2_;
        displayname = displayname_;
        type = type_; 
        uid = uid_;
    }

    public String catagoryNames() {
        if (catagory2.length() > 0) {
            return " " + catagory1 + "; " + catagory2;
        } else {
            return " " + catagory1;
        }
    }
}
