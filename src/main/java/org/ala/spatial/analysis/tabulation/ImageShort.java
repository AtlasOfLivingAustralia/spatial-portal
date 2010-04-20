package org.ala.spatial.analysis.tabulation;

import java.io.Serializable;

public class ImageShort implements Serializable {

    private static final long serialVersionUID = 8689255844671800194L;

    /**
     * default
     */
    //private static final long serialVersionUID = 1L;
    public short value;
    public int x;
    public int y;

    public ImageShort(short _value, int _x, int _y) {
        value = _value;
        x = _x;
        y = _y;
    }
}
