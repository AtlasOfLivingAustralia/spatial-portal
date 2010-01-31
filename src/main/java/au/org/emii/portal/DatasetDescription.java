

package au.org.emii.portal;

/**
 *
 * @author Brendon
 */
public class DatasetDescription {
    
    protected String Abstract;

    /**
     * Get the value of Abstract
     *
     * @return the value of Abstract
     */
    public String getAbstract() {
        return Abstract;
    }

    /**
     * Set the value of Abstract
     *
     * @param Abstract new value of Abstract
     */
    public void setAbstract(String Abstract) {
        this.Abstract = Abstract;
    }

    
    protected String ServerURL;

    /**
     * Get the value of ServerURL
     *
     * @return the value of ServerURL
     */
    public String getServerURL() {
        return ServerURL;
    }

    /**
     * Set the value of ServerURL
     *
     * @param ServerURL new value of ServerURL
     */
    public void setServerURL(String ServerURL) {
        this.ServerURL = ServerURL;
    }

        protected String LayerName;

    /**
     * Get the value of LayerName
     *
     * @return the value of LayerName
     */
    public String getName() {
        return LayerName;
    }
    
        protected String LayerTitle;

    /**
     * Get the value of LayerTitle
     *
     * @return the value of LayerTitle
     */
    public String getTitle() {
        return LayerTitle;
    }

    /**
     * Set the value of LayerTitle
     *
     * @param LayerTitle new value of LayerTitle
     */
    public void setTitle(String LayerTitle) {
        this.LayerTitle = LayerTitle;
    }


    /**
     * Set the value of LayerName
     *
     * @param LayerName new value of LayerName
     */
    public void setName(String LayerName) {
        this.LayerName = LayerName;
    }

}
