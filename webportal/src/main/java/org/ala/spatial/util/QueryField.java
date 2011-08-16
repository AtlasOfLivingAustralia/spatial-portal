/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

import java.util.ArrayList;

/**
 *
 * @author Adam
 */
public class QueryField extends ArrayList {

    String name;
    String displayName;
    boolean store;

    public QueryField(String name, String displayName) {
        super();

        this.name = name;
        store = false;
    }

    public QueryField(String name, String displayName, boolean store) {
        super();

        this.name = name;
        this.displayName = displayName;
        this.store = store;
    }

    public boolean isStored() {
        return store;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
