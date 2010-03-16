package org.ala.spatial.web;

import java.util.Arrays;
import java.util.Iterator;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author ajay
 */
public class SpeciesAutoComplete extends Combobox {

    public SpeciesAutoComplete() {
        refresh(""); //init the child comboitems
    }

    public SpeciesAutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        refresh(value); //refresh the child comboitems
    }

    /** Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
        }
    }

    /** Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {
        int j = Arrays.binarySearch(_dict, val);
        if (j < 0) {
            j = -j - 1;
        }

        Iterator it = getItems().iterator();
        for (int cnt = 10; --cnt >= 0 && j < _dict.length && _dict[j].startsWith(val); ++j) {
            if (it != null && it.hasNext()) {
                ((Comboitem) it.next()).setLabel(_dict[j]);
            } else {
                it = null;
                new Comboitem(_dict[j]).setParent(this);
            }
        }

        while (it != null && it.hasNext()) {
            it.next();
            it.remove();
        }
    }
    private static String[] _dict = { //alphabetic order
        "abacus", "accuracy", "acuity", "adage", "afar", "after", "apple",
        "bible", "bird", "bingle", "blog",
        "cabane", "cape", "cease", "cedar",
        "dacron", "definable", "defacto", "deluxe",
        "each", "eager", "effect", "efficacy",
        "far", "far from",
        "girl", "gigantean", "giant",
        "home", "honest", "huge",
        "information", "inner",
        "jump", "jungle", "jungle fever",
        "kaka", "kale", "kame",
        "lamella", "lane", "lemma",
        "master", "maxima", "music",
        "nerve", "new", "number",
        "omega", "opera",
        "pea", "peace", "peaceful",
        "rock",
        "sound", "spread", "student", "super",
        "tea", "teacher",
        "unit", "universe",
        "vector", "victory",
        "wake", "wee", "weak",
        "xeme",
        "yea", "yellow",
        "zebra", "zk"};
}
