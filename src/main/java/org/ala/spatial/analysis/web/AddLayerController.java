/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
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
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;
    private Listbox lbListLayersCtx;
//    private String taxon = "";

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Add layers";
        this.totalSteps = 2;

        this.setIncludeAnalysisLayersForUploadQuery(true);
        //this.loadAreaLayers();
        this.loadGridLayers(false, true);
        this.updateWindowTitle();
        
        if(lbListLayers != null) {
            lbListLayers.clearSelection();
            lbListLayers.updateDistances();
        }

    }

    public void onClick$btnClearSelectionCtx(Event event) {
        lbListLayersCtx.clearSelection();

        // check if lbListLayers is empty as well,
        // if so, then disable the next button
        if (lbListLayers.getSelectedCount() == 0) {
            btnOk.setDisabled(true);
        }
    }

    public void onSelect$lbListLayersCtx(Event event) {
        btnOk.setDisabled(lbListLayersCtx.getSelectedCount() < 1);
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
                lbListLayers.setFocus(true);
                break;
           
        }
    }
    
    @Override
    public void onClick$btnOk(Event event){
        super.onClick$btnOk(event);
        if (currentStep == 2){
            loadMap(event);
        }
    }
    
    public void loadMap(Event event) {
        //String layername = tToolName.getValue();
        //System.out.println("layername:"+layername);
        
        
        if (lbListLayers.getSelectedLayers().length > 0) {
            String[] sellayers = lbListLayers.getSelectedLayers();
            int i = 0;
                for (String s : sellayers) {
                    i++;
                    System.out.println("i="+i);
                    System.out.println("s:"+s);
                    String treeName = CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                    String treePath = CommonData.geoServer + "/gwc/service/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:"+s+"&format=image/png&styles=";
                    //String treeMetadata = CommonData.satServer + "/layers/" + "660";  //http://spatial-dev.ala.org.au/alaspatial/layers/660";

                    /*
                     * getMapComposer().addWMSLayer(s, treeName,treePath,(float) 0.75, treeMetadata, null, subType, null, null, null);
                     * treeMetadata not necessary has to be known
                     * subType is actually not used in getMapComposer().addWMSLayer
                     */
                    getMapComposer().addWMSLayer(s, treeName,treePath,(float) 0.75, null, null, 20, null, null, null);
                    /*
                     * Problem: could not map contrextual layers
                     */
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

        //getMapComposer().showMessage("Reference number to retrieve results: " + pid);

        //showInfoWindow("/output/maxent/" + pid + "/species.html");
    }
    
    private String readUrl(String feature) {
        StringBuffer content = new StringBuffer();

        try {
            // Construct data

            // Send data
            URL url = new URL(feature);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                content.append(line);
            }
            conn.disconnect();
        } catch (Exception e) {
        }
        return content.toString();
    }

    String getStringValue(String startAt, String tag, String json) {
        String typeStart = "\"" + tag + "\":\"";
        String typeEnd = "\"";
        int beginning = startAt == null ? 0 : json.indexOf(startAt) + startAt.length();
        int start = json.indexOf(typeStart, beginning) + typeStart.length();
        int end = json.indexOf(typeEnd, start);
        System.out.println("start: "+start);
        System.out.println("end: "+end);
        return json.substring(start, end);
    }

    private Facet getFacetForObject(String pid, String name) {
        //get field.id.
        JSONObject jo = JSONObject.fromObject(readUrl(CommonData.layersServer + "/object/" + pid));
        String fieldId = jo.getString("fid");

        //get field objects.
        String objects = readUrl(CommonData.layersServer + "/field/" + fieldId);
        String lookFor = "\"name\":\"" + name + "\"";

        //create facet if name is unique.
        int p1 = objects.indexOf(lookFor);
        if (p1 > 0) {
            int p2 = objects.indexOf(lookFor, p1 + 1);
            if (p2 < 0) {
                /* TODO: use correct replacement in 'name' for " characters */
                /* this function is also in AreaRegionSelection */
                Facet f = new Facet(fieldId, "\"" + name + "\"", true);

                //test if this facet is in solr
                ArrayList<Facet> facets = new ArrayList<Facet>();
                facets.add(f);
                if (new BiocacheQuery(null, null, null, facets, false).getOccurrenceCount() > 0) {
                    return f;
                }
            }
        }

        return null;
    }
}
