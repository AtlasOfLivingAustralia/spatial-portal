/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
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
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
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
public class AddLayerController extends UtilityComposer {
    String winTop = "300px";
    String winLeft = "500px";
    boolean includeAnalysisLayersForUploadQuery = false;
    EnvironmentalList lbListLayers;
    boolean environmentalOnly = false;
    boolean fullList = false;
    SelectedLayersCombobox selectedLayersCombobox;
    Map<String, Object> params;
    Menupopup mpLayer2, mpLayer1;
    EnvLayersCombobox cbLayer, cbLayer1, cbLayer2;
    Button btnOk, btnCancel;
    Fileupload fileUpload;
    boolean includeAnalysisLayersForAnyQuery = false;
    boolean mpLayersIncludeAnalysisLayers = false;
    Textbox tLayerList;
    Div dLayerSummary;
    Label lLayersSelected;
    Button bLayerListDownload1;
    Button bLayerListDownload2;
    
    
    public void setIncludeAnalysisLayersForUploadQuery(boolean includeAnalysisLayersForUploadQuery) {
        this.includeAnalysisLayersForUploadQuery = includeAnalysisLayersForUploadQuery;
    }
    
    public void onClick$btnClearSelection(Event event) {
        lbListLayers.clearSelection();
        lbListLayers.updateDistances();
        toggles();
        btnOk.setDisabled(true);
    }
    
    public void loadGridLayers(boolean environmentalOnly, boolean fullList) {
        this.environmentalOnly = environmentalOnly;
        this.fullList = fullList;

        if (selectedLayersCombobox != null) {
            selectedLayersCombobox.init(getMapComposer().getLayerSelections(), getMapComposer(), false);
        }
        try {

            if (fullList) {
                lbListLayers.init(getMapComposer(), CommonData.satServer, environmentalOnly, false);
                lbListLayers.updateDistances();
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (int i = 0; i < layers.size(); i++) {
                    MapLayer lyr = layers.get(i);
                    //System.out.println(lyr.getDisplayName());
                }
            }

            String layers = (String) params.get("environmentalLayerName");
            if (layers != null) {
                lbListLayers.selectLayers(layers.split(","));
            }

            lbListLayers.renderAll();
        } catch (Exception e) {
            System.out.println("Unable to load species layers:");
            e.printStackTrace(System.out);
        }
    }
    
    void addToMpLayers(MapLayer ml, boolean analysis) {
        //get layer name
        String name = null;
        String url = ml.getUri();
        if(analysis) {
            name = ml.getName();
        } else {
            int p1 = url.indexOf("ALA:") + 4;
            int p2 = url.indexOf("&",p1);
            if(p1 > 4) {
                if(p2 < 0) p2 = url.length();
                name = url.substring(p1,p2);
            }
        }

        //cbLayer1
        Menuitem mi = new Menuitem(ml.getDisplayName());
        mi.setValue(name);
        mi.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {

            public void onEvent(Event event) throws Exception {
                Menuitem mi = (Menuitem) event.getTarget();
                cbLayer1.setValue(mi.getValue() + " ");
                cbLayer1.refresh(mi.getValue());
                for(Object o: cbLayer1.getItems()) {
                    Comboitem ci = (Comboitem) o;
                    JSONObject jo = (JSONObject) ci.getValue();
                    if(jo.getString("name").equals(mi.getValue())) {
                        cbLayer1.setSelectedItem(ci);
                        cbLayer1.setText(ci.getLabel());
                        toggles();
                        return;
                    }
                }
            }
        });
        mi.setParent(mpLayer1);

        //cbLayer2
        mi = new Menuitem(ml.getDisplayName());
        mi.setValue(name);
        mi.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {

            public void onEvent(Event event) throws Exception {
                Menuitem mi = (Menuitem) event.getTarget();
                cbLayer2.setValue(mi.getValue() + " ");
                cbLayer2.refresh(mi.getValue());
                for(Object o: cbLayer2.getItems()) {
                    Comboitem ci = (Comboitem) o;
                    JSONObject jo = (JSONObject) ci.getValue();
                    if(jo.getString("name").equals(mi.getValue())) {
                        cbLayer2.setSelectedItem(ci);
                        cbLayer2.setText(ci.getLabel());
                        toggles();
                        return;
                    }
                }
            }
        });
        mi.setParent(mpLayer2);
    }
    
    
    @Override
    public void afterCompose() {
        super.afterCompose();
        
        winTop = this.getTop();
        winLeft = this.getLeft();

        this.setIncludeAnalysisLayersForUploadQuery(true);
        //this.loadAreaLayers();
        this.loadGridLayers(false, true);
        
        if(lbListLayers != null) {
            lbListLayers.clearSelection();
            lbListLayers.updateDistances();
        }
        
        if(mpLayer1 != null && mpLayer2 != null) {
            for(MapLayer ml : getMapComposer().getGridLayers()) {
                addToMpLayers(ml, false);
            }
        }

    }
    
    public void onClick$btnCancel(Event event) {
        if (lbListLayers != null) {
            lbListLayers.clearSelection();            
            toggles();
        }
        this.detach();
    }

    public void onClick$btnOk(Event event){
        boolean test = includeAnalysisLayersForAnyQuery;
        if (selectedLayersCombobox != null) {
                            if((selectedLayersCombobox.getIncludeAnalysisLayers()) != test) {
                                selectedLayersCombobox.init(getMapComposer().getLayerSelections(), getMapComposer(), test);
                            }
                        }
                        if(lbListLayers != null) {
                            if((lbListLayers.getIncludeAnalysisLayers()) != test) {
                                String [] selectedLayers = lbListLayers.getSelectedLayers();
                                lbListLayers.init(getMapComposer(), CommonData.satServer, environmentalOnly, test);
                                lbListLayers.updateDistances();

                                if (selectedLayers != null && selectedLayers.length > 0) {
                                    lbListLayers.selectLayers(selectedLayers);
                                }

                                lbListLayers.renderAll();
                            }
                        }
                        if (cbLayer != null) {
                            if((cbLayer.getIncludeAnalysisLayers()) != test) {
                                cbLayer.setIncludeAnalysisLayers(test);
                            }
                        }
                        if (cbLayer1 != null) {
                            if((cbLayer1.getIncludeAnalysisLayers()) != test) {
                                cbLayer1.setIncludeAnalysisLayers(test);
                            }
                        }
                        if (cbLayer2 != null) {
                            if((cbLayer2.getIncludeAnalysisLayers()) != test) {
                                cbLayer2.setIncludeAnalysisLayers(test);
                            }
                        }
                        if(mpLayer1 != null && mpLayer2 != null &&
                                mpLayersIncludeAnalysisLayers != test) {
                            //remove
                            while(mpLayer1.getChildren().size() > 0) {
                                mpLayer1.removeChild(mpLayer1.getFirstChild());
                            }
                            while(mpLayer2.getChildren().size() > 0) {
                                mpLayer2.removeChild(mpLayer1.getFirstChild());
                            }
                            //add
                            for(MapLayer ml : getMapComposer().getGridLayers()) {
                                addToMpLayers(ml, false);
                            }
                            mpLayersIncludeAnalysisLayers = test;
                            if(mpLayersIncludeAnalysisLayers) {
                                for(MapLayer ml : getMapComposer().getAnalysisLayers()) {
                                    if(ml.getSubType() != LayerUtilities.ALOC) {
                                        addToMpLayers(ml, true);
                                    }
                                }
                            }
                        }
        toggles();
       
        loadMap(event);
        
    }
    public String getSelectedLayers() {
        String layers = "";

        try {
            if (lbListLayers.getSelectedLayers().length > 0) {
                String[] sellayers = lbListLayers.getSelectedLayers();
                for (String l : sellayers) {
                    layers += l + ":";
                }
                layers = layers.substring(0, layers.length() - 1);
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected layers");
            e.printStackTrace(System.out);
        }

        return layers;
    }
    public void onSelect$lbListLayers(Event event) { 
        toggles();
    }
    public void onChange$cbLayer2(Event event) {
        toggles();
    }

    public void onChange$cbLayer1(Event event) {
        toggles();
    }
    
    public void onSelect$selectedLayersCombobox(Event event) {
        Comboitem ci = selectedLayersCombobox.getSelectedItem();
        if (ci != null && lbListLayers != null) {
            String layersList = null;
            if (ci.getValue() != null && ci.getValue() instanceof LayerSelection) {
                layersList = ((LayerSelection) ci.getValue()).getLayers();
            } else {
                if (ci.getValue() == null) {
                    if(ci.getLabel().toLowerCase().contains("paste")) {
                        org.zkoss.zul.Window window = (org.zkoss.zul.Window) Executions.createComponents("WEB-INF/zul/PasteLayerList.zul", this, null);

                        try {
                            window.doModal();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else if(ci.getLabel().toLowerCase().contains("import")) {
                        org.zkoss.zul.Window window = (org.zkoss.zul.Window) Executions.createComponents("WEB-INF/zul/UploadLayerList.zul", this, null);

                        try {
                            window.doModal();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    selectedLayersCombobox.setSelectedIndex(-1);
                }
            }
            selectLayerFromList(layersList);
        }
    }

    public void selectLayerFromList(String layersList) {
        if (layersList == null) {
            return;
        }

        //check the whole layer string as well as the one at the end
        String[] layers = layersList.split(",");
        String [] list = new String[layers.length * 2];
        for (int i = 0; i < layers.length; i++) {
            int p1 = layers[i].lastIndexOf('(');
            int p2 = layers[i].lastIndexOf(')');
            if (p1 >= 0 && p2 >= 0 && p1 < p2) {
                list[i*2] = layers[i].substring(p1 + 1, p2).trim();
            }
            list[i*2+1] = layers[i];
        }
        lbListLayers.selectLayers(list);        
        toggles();
    }

    public void saveLayerSelection() {
        //save layer selection
        if (lbListLayers != null && lbListLayers.getSelectedCount() > 0) {
            String list = getLayerListShortText();
            LayerSelection ls = new LayerSelection(getLayerListText(), list);
            getMapComposer().addLayerSelection(ls);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < getMapComposer().getLayerSelections().size(); i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                sb.append(getMapComposer().getLayerSelections().get(i).toString());
                sb.append(" // ");
                sb.append(getMapComposer().getLayerSelections().get(i).getLayers());
            }

            try {
                Cookie c = new Cookie("analysis_layer_selections", URLEncoder.encode(sb.toString(),"UTF-8"));
                c.setMaxAge(Integer.MAX_VALUE);
                ((HttpServletResponse) Executions.getCurrent().getNativeResponse()).addCookie(c);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void updateLayerListText() {
        try {
            if (lbListLayers != null && lbListLayers.getSelectedCount() > 0
                    && tLayerList != null) {
                tLayerList.setText(getLayerListText());
                if(dLayerSummary != null) {
                    dLayerSummary.setVisible(tLayerList.getText().length() > 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getLayerListText() {
        StringBuilder sb = new StringBuilder();
        for (String s : lbListLayers.getSelectedLayers()) {
            try {
                String displayname = CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                if (displayname != null && displayname.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(displayname).append(" (").append(s).append(")");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    String getLayerListShortText() {
        StringBuilder sb = new StringBuilder();
        for (String s : lbListLayers.getSelectedLayers()) {
            try {
                String displayname = CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                if (displayname != null && displayname.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public void onChange$cbLayer(Event event) {
        //seek to and select the same layer in the list
        if(lbListLayers != null && cbLayer.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) cbLayer.getSelectedItem().getValue();
            String [] layer = jo.getString("name").split("/");
            lbListLayers.selectLayers(layer);            
        }
        toggles();
    }

    public void onClick$bLayerListDownload1(Event event) {
        downloadLayerList();
    }
    
    public void onClick$bLayerListDownload2(Event event) {
        downloadLayerList();
    }

    void downloadLayerList() {
        SimpleDateFormat sdf = new SimpleDateFormat("ddmmyyyy_hhmm");
        Filedownload.save(getLayerListShortText(), "text/plain", "layer_selection_" + sdf.format(new Date()) + ".txt");
    }

    void updateLayerSelectionCount() {
        if(lLayersSelected != null && lbListLayers != null) {
            if(lbListLayers.getSelectedCount() == 1) {
                lLayersSelected.setValue("1 layer selected");
            } else {
                lLayersSelected.setValue(lbListLayers.getSelectedCount() + " layers selected");
            }
        }
    }

    public void onUpload$uploadLayerList(Event event) {
        doFileUpload(event);
    }

    

    public void doFileUpload(Event event) {
        UploadEvent ue = null;
        if (event instanceof UploadEvent) {
            ue = (UploadEvent) event;
        } else if (event instanceof ForwardEvent) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            System.out.println("unable to upload file");
            return;
        } else {
            System.out.println("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            boolean loaded = false;
            try {
                loadLayerList(m.getReaderData());
                loaded = true;
                System.out.println("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(new String(m.getByteData())));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new InputStreamReader(m.getStreamData()));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(m.getStringData()));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage("Unable to load your file.");
                    System.out.println("unable to load user layer list: ");
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadLayerList(Reader r) throws Exception {
        CSVReader reader = new CSVReader(r);
        //one line, read it
        StringBuilder sb = new StringBuilder();
        for(String s : reader.readNext()) {
            if(sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s);
        }        
        reader.close();
        selectLayerFromList(sb.toString());
        updateLayerSelectionCount();
    }
    

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
