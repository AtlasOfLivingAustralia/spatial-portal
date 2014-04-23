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

package org.ala.layers.dao;

import java.util.List;

import org.ala.layers.dto.Layer;

/**
 * DAO for the Layer object
 *
 * @author ajay
 */
public interface LayerDAO {
    public List<Layer> getLayers();

    public List<Layer> getLayersByEnvironment();

    public List<Layer> getLayersByContextual();

    public List<Layer> getLayersByCriteria(String keywords);

    public Layer getLayerById(int id);

    public Layer getLayerByName(String name);

    public Layer getLayerByDisplayName(String name);

    public List<Layer> getLayersForAdmin();

    public Layer getLayerByIdForAdmin(int id);

    public Layer getLayerByNameForAdmin(String name);

    public void addLayer(Layer layer);

    public void updateLayer(Layer layer);
}
