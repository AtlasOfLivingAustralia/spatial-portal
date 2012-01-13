/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerSelection;
import au.org.emii.portal.util.LayerUtilities;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.ala.logger.client.RemoteLogger;
import org.ala.spatial.data.Facet;
import org.ala.spatial.data.Query;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.data.QueryField;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.UploadQuery;
import org.ala.spatial.sampling.SimpleShapeFile;
import org.ala.spatial.util.ListEntry;
import org.ala.spatial.util.SelectedArea;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.*;
import org.zkoss.zul.api.Window;
import org.zkoss.zul.Fileupload;


/**
 *
 * @author ajay
 */
public class AddLayerController extends AddToolComposer {
    @Override
    public void afterCompose() {
        super.afterCompose();
        
        this.setTitle("");

        this.setIncludeAnalysisLayersForUploadQuery(true);
        
        //this.setIncludeAnalysisLayersForAnyQuery(true);
        //this.loadAreaLayers();
        this.loadGridLayers(false, true);
    }

    public void onClick$btnOk(Event event) {
        super.onClick$btnOk(event); 
        loadMap(event);   
    }
    
    
    
    
    @Override
    void toggles() {
        btnOk.setDisabled(true);
        btnOk.setVisible(true);

        if (fileUpload != null) {
            fileUpload.setVisible(false);
        }
        if(lbListLayers != null) {
            
                btnOk.setDisabled(false);
            
            updateLayerSelectionCount();
        }
        if(lbListLayers != null) {
            if(bLayerListDownload1 != null && bLayerListDownload2 != null) {
                bLayerListDownload1.setDisabled(lbListLayers.getSelectedCount() == 0);
                bLayerListDownload2.setDisabled(lbListLayers.getSelectedCount() == 0);
            }
        }
    }
    

    
    public void loadMap(Event event) {
        if (lbListLayers.getSelectedLayers().length > 0) {
            String[] sellayers = lbListLayers.getSelectedLayers();
            int i = 0;
                for (String s : sellayers) {
                    i++;
                    System.out.println("i="+i);
                    System.out.println("s:"+s);
                    String treeName = CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                    String treePath = CommonData.geoServer + "/gwc/service/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:"+s+"&format=image/png&styles=";
                    
                    getMapComposer().addWMSLayer(s, treeName,treePath,(float) 0.75, null, null, 20, null, null, null);
                    
                }      
        }
        this.detach();
    } 
}
