/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 *
 *
 * this is the javascipt function we need to add this to the map
 *
 *
 *
 * var options = {numZoomLevels: 3};

    var graphic = new OpenLayers.Layer.Image(
        'City Lights',
        'http://earthtrends.wri.org/images/maps/4_m_citylights_lg.gif',
        new OpenLayers.Bounds(-180, -88.759, 180, 88.759),
        new OpenLayers.Size(580, 288),
        options
    );
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
