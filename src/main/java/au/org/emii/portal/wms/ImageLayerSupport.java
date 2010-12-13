/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.wms;

/**
 *
 * @author brendon
 */
public class ImageLayerSupport {

    private String imageURL;
    private String imageName;
    private String boundingBox; // comma separated
    private int width;
    private int height;
    private int zoomLevels;

    public String getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getZoomLevels() {
        return zoomLevels;
    }

    public void setZoomLevels(int zoomLevels) {
        this.zoomLevels = zoomLevels;
    }
    
}
