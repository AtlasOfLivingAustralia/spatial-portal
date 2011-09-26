/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.dto;

/**
 *
 * @author Adam
 */
public class IntersectionFile {
    String name;
    String filePath;
    String shapeFields;

    public IntersectionFile(String name, String filePath, String shapeFields) {
        this.name = name.trim();
        this.filePath = filePath.trim();
        this.shapeFields = (shapeFields == null)?null:shapeFields.trim();
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

}
