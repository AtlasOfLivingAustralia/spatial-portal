/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.wms.WMSStyle;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import net.sf.json.JSONObject;
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
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.api.Window;

/**
 *
 * @author ajay
 */
public class AddLayerController extends AddToolComposer {

    int generation_count = 1;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Add layers";
        this.totalSteps = 1;

        this.setIncludeAnalysisLayersForUploadQuery(true);
        //this.loadAreaLayers();
        this.loadGridLayers(false, true);
        this.updateWindowTitle();
        
        

    }



    @Override
    public void onLastPanel() {
        super.onLastPanel();
        //this.updateName("My Prediction model for " + rgSpecies.getSelectedItem().getLabel());
        //this.updateName(getMapComposer().getNextAreaLayerName("My layer"));

    }

    

    @Override
    void fixFocus() {
        System.out.println(currentStep);
        switch (currentStep) {
            
            case 1:
                selectedLayersCombobox.setFocus(true);
                break;
           
        }
    }
    
    @Override
    public void onClick$btnOk(Event event){
        super.onClick$btnOk(event);
        if (currentStep == 1){
            loadMap(event);
        }
    }
    
    @Override
    public void loadMap(Event event) {
         if (lbListLayers.getSelectedLayers().length > 0) {
            String[] sellayers = lbListLayers.getSelectedLayers();
            int i = 0;
                for (String s : sellayers) {
                    i++;
                    System.out.println("s:"+s);
                    String treeName = CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                    String treePath = CommonData.geoServer + "/gwc/service/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:"+s+"&format=image/png&styles=";
                    String legendurl = CommonData.geoServer+ "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + s;
                    
                    getMapComposer().addWMSLayer(s, treeName,treePath,(float) 0.75, null, legendurl,LayerUtilities.WKT, null, null,null);
                    //getMapComposer().addWMSLayer(pid, layerLabel, mapurl, (float) 0.5, null, legendurl, LayerUtilities.ALOC, null, null);
                }
            
                
            /*
             * ArrayList<ListEntry> lE = lbListLayers.listEntries;
            for (ListEntry le : lE){
                String treeName = le.name;
                String treePath = CommonData.geoServer + "/gwc/service/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:"+treeName+"&format=image/png&styles=";
                String treeDisplayName = le.displayname;
                String treeUid = le.uid;
                String treeMetadata = CommonData.satServer + "/layers/" + treeUid;
                String treeType = le.type;
                getMapComposer().addWMSLayer(treeName, treeDisplayName,
                        treePath,
                        (float) 0.75, treeMetadata, null, 20, null, null, null);
            }
            * 
            */       
        }
        this.detach();
    }
    
    
    
}
