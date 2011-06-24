package au.org.emii.portal.mest;

/**
 *
 * @author war398
 */
public class WMSLayer {

    protected String mestAbstract;

    /**
     * Get the value of Abstract
     *
     * @return the value of Abstract
     */
    public String getAbstract() {
        return mestAbstract;
    }

    /**
     * Set the value of Abstract
     *
     * @param Abstract new value of Abstract
     */
    public void setAbstract(String Abstract) {
        this.mestAbstract = Abstract;
    }
    protected String serverURL;

    /**
     * Get the value of ServerURL
     *
     * @return the value of ServerURL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * Set the value of ServerURL
     *
     * @param ServerURL new value of ServerURL
     */
    public void setServerURL(String ServerURL) {
        this.serverURL = ServerURL;
    }
    protected String layerName;

    /**
     * Get the value of layerName
     *
     * @return the value of layerName
     */
    public String getName() {
        return layerName;
    }
    protected String layerTitle;

    /**
     * Get the value of LayerTitle
     *
     * @return the value of LayerTitle
     */
    public String getTitle() {
        return layerTitle;
    }

    /**
     * Set the value of LayerTitle
     *
     * @param LayerTitle new value of LayerTitle
     */
    public void setTitle(String LayerTitle) {
        this.layerTitle = LayerTitle;
    }

    /**
     * Set the value of layerName
     *
     * @param layerName new value of layerName
     */
    public void setName(String LayerName) {
        this.layerName = LayerName;
    }
    protected String cswId;

    /**
     * Get the value of cswId
     *
     * @return the value of cswId
     */
    public String getCswId() {
        return cswId;
    }

    /**
     * Set the value of cswId
     *
     * @param cswId new value of cswId
     */
    public void setCswId(String CswId) {
        this.cswId = CswId;
    }
    protected String cswServerId;

    /**
     * Get the value of cswServerId
     *
     * @return the value of cswServerId
     */
    public String getCswServerId() {
        return cswServerId;
    }

    /**
     * Set the value of cswServerId
     *
     * @param cswServerId new value of cswServerId
     */
    public void setCswServerId(String CswServerId) {
        this.cswServerId = CswServerId;
    }
}
