/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

import org.ala.layers.client.Client;
import org.ala.layers.dto.Field;

/**
 *
 * @author Adam
 */
public class Layers {
    public static String getFieldId(String layerShortName) {
        String field = layerShortName;
        // layer short name -> layer id -> field id
        try {
            String id = String.valueOf(Client.getLayerDao().getLayerByName(layerShortName).getId());
            for(Field f : Client.getFieldDao().getFields()) {
                if(f.getSpid() != null && f.getSpid().equals(id)) {
                    field = f.getId();
                    break;
                }
            }
        } catch (Exception e) {}
        return field;
    }
}
