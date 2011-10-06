/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.layers.intersect;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Adam
 */
public class SimpleShapeFileCache {

    HashMap<String, SimpleShapeFile> cache;

    public SimpleShapeFileCache(String[] shapeFileNames, String[] columns) {
        cache = new HashMap<String, SimpleShapeFile>();
        update(shapeFileNames, columns);
    }

    public SimpleShapeFile get(String shapeFileName) {
        return cache.get(shapeFileName);
    }

    public void update(String[] layers, String[] columns) {
        //remove layers no longer required
        ArrayList<String> toRemove = new ArrayList<String>();
        for (String key : cache.keySet()) {
            int j = 0;
            for (j = 0; j < layers.length; j++) {
                if (key.equals(layers[j])) {
                    break;
                }
            }
            if (j == layers.length) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            cache.remove(key);
        }

        //add layers not loaded
        System.out.println("start caching shape files");
        System.gc();
        System.out.println("Memory usage (total/used/free):" + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB / " + (Runtime.getRuntime().totalMemory() / 1024 / 1024 - Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB / " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB");
        for (int i = 0; i < layers.length; i++) {
            if (get(layers[i]) == null) {
                try {
                    SimpleShapeFile ssf = new SimpleShapeFile(layers[i], columns[i]);
                    System.gc();
                    System.out.println(layers[i] + " loaded, Memory usage (total/used/free):" + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB / " + (Runtime.getRuntime().totalMemory() / 1024 / 1024 - Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB / " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB");

                    ssf.reduce(0);
                    if (ssf != null) {
                        cache.put(layers[i], ssf);
                    }
                } catch (Exception e) {
                    System.out.println("error with shape file: " + layers[i] + ", field: " + columns[i]);
                    e.printStackTrace();
                }
            }
        }
    }
}
