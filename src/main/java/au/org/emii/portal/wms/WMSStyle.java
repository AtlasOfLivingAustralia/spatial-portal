package au.org.emii.portal.wms;

import java.io.Serializable;

/**
 * Representation of WMS style information.
 * <p/>
 * IMPORTANT - PLEASE READ!
 * If you add/change any fields in this class, you MUST update the
 * deepCopy method or you will get 'mysterious failures' when you
 * come to use the class.
 *
 * @author geoff
 */
public class WMSStyle implements Cloneable, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * id for the to enable Hibernate Serialization
     */
    private long id = 0;
    /**
     * name of style as known to the server
     */
    private String name = null;
    /**
     * title of style for display to user
     */
    private String title = null;
    /**
     * longer human readable description of style (abstract)
     */
    private String description = null;
    /**
     * URI of where to obtain a map legend from (legendURL)
     */
    private String legendUri = null;
    /**
     * image format of legendUri
     */
    private String legendFormat = null;

    private long maplayerid;

    public long getMaplayerid() {
        return maplayerid;
    }

    public void setMaplayerid(long maplayerid) {
        this.maplayerid = maplayerid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLegendUri() {
        return legendUri;
    }

    public void setLegendUri(String legendUri) {
        this.legendUri = legendUri;
    }

    public String getLegendFormat() {
        return legendFormat;
    }

    public void setLegendFormat(String legendFormat) {
        this.legendFormat = legendFormat;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
