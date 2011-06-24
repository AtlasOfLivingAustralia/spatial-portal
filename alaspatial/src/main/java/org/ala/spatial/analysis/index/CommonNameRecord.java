/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

/**
 *
 * @author Adam
 */
public class CommonNameRecord {

    public String name;
    public String nameLowerCase;
    public String nameAZ;
    public int index;

    public CommonNameRecord(String name_, int index_) {
        name = name_;
        index = index_;
        nameLowerCase = name_.toLowerCase();
        nameAZ = nameLowerCase.replaceAll("[^a-z]", "");
    }
}
