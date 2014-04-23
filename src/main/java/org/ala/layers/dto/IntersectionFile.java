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
package org.ala.layers.dto;

import java.util.HashMap;

/**
 * @author Adam
 */
public class IntersectionFile {

    String name;
    String filePath;
    String shapeFields;
    String layerName;
    String fieldId;
    String layerPid;
    String fieldName;
    String type;
    HashMap<Integer, GridClass> classes;

    public IntersectionFile(String name, String filePath, String shapeFields, String layerName, String fieldId, String fieldName, String layerPid, String type, HashMap<Integer, GridClass> classes) {
        this.name = name.trim();
        this.filePath = filePath.trim();
        this.shapeFields = (shapeFields == null) ? null : shapeFields.trim();
        this.layerName = layerName;
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.layerPid = layerPid;
        this.type = type;
        this.classes = classes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setShapeFields(String shapeFields) {
        this.shapeFields = shapeFields;
    }

    public String getShapeFields() {
        return shapeFields;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setClasses(HashMap<Integer, GridClass> classes) {
        this.classes = classes;
    }

    public HashMap<Integer, GridClass> getClasses() {
        return classes;
    }

    public void setLayerPid(String layerPid) {
        this.layerPid = layerPid;
    }

    public String getLayerPid() {
        return layerPid;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
