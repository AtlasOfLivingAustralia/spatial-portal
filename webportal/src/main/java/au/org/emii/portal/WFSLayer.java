/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal;

import java.util.*;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.*;
import org.jdom.Document;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

public class WFSLayer {
    
     protected String serverURL;

    /**
     * Get the value of serverURL
     *
     * @return the value of serverURL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * Set the value of serverURL
     *
     * @param serverURL new value of serverURL
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
    public String getLayerName() {
        return layerName;
    }
    
        protected String layerTitle;

    /**
     * Get the value of layerTitle
     *
     * @return the value of layerTitle
     */
    public String getLayerTitle() {
        return layerTitle;
    }

    /**
     * Set the value of layerTitle
     *
     * @param layerTitle new value of layerTitle
     */
    public void setLayerTitle(String LayerTitle) {
        this.layerTitle = LayerTitle;
    }


    /**
     * Set the value of layerName
     *
     * @param layerName new value of layerName
     */
    public void setLayerName(String LayerName) {
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
