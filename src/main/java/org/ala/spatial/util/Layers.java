/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.util;

import org.ala.layers.client.Client;
import org.ala.layers.dto.Field;

/**
 * Util function for returning a fieldId from a layer short name.
 *
 * @author Adam
 */
public class Layers {

    /**
     * Get fieldId from a layer short name.
     *
     * @param layerShortName layer short name as String.
     * @return fieldId as String, or the input parameter if unsuccessful.
     */
    public static String getFieldId(String layerShortName) {
        String field = layerShortName;
        // layer short name -> layer id -> field id
        try {
            String id = String.valueOf(Client.getLayerDao().getLayerByName(layerShortName).getId());
            for (Field f : Client.getFieldDao().getFields()) {
                if (f.getSpid() != null && f.getSpid().equals(id)) {
                    field = f.getId();
                    break;
                }
            }
        } catch (Exception e) {
            //don't care if unsuccessful.  May already be a fieldId.
        }
        return field;
    }
}
