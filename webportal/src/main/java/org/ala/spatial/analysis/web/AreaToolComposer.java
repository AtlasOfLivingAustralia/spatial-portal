/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import java.util.List;
import java.util.Map;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.data.SolrQuery;
import org.ala.spatial.util.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AreaToolComposer extends UtilityComposer {

//    public String boxGeom;
//    public Textbox displayGeom;
//    String layerName;
//    Textbox txtLayerName;
//    Button btnNext;
//    Button btnOk;
//    Button btnClear;
    String layerName;
    SettingsSupplementary settingsSupplementary;
    boolean isAnalysisChild = false;
    AddToolComposer analysisParent = null;
    Map winProps = null;
    public boolean ok = false;

    @Override
    public void afterCompose() {
        super.afterCompose();
        //txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));

        Component parent = this.getParent();
  //      System.out.println("Parent: " + parent.getId() + " - " + parent.getWidgetClass());

        winProps = Executions.getCurrent().getArg();

        if (parent.getId().equals("addtoolwindow")) {
            analysisParent = (AddToolComposer) this.getParent();
            isAnalysisChild = true;
        } else {
            isAnalysisChild = false;
        }
    }

    @Override
    public void detach() {
        super.detach();
        String parentname = (String) winProps.get("parentname");
        if (isAnalysisChild) {
            //analysisParent.hasCustomArea = true;
            analysisParent.resetWindow(ok?layerName:null);
        } else if (parentname != null && parentname.equals("AddSpeciesInArea")) {
            //was OK clicked?
            if (ok) {
                //map
                String wkt = null;
                try {
                    wkt = getMapComposer().getMapLayer(layerName).getWKT();
                } catch (Exception e) {}

                if(winProps.get("lsid") == null) {
                    mapSpeciesInArea();
                } else if (winProps.get("filter") != null && (Boolean) winProps.get("filter")) {
                    MapLayer ml = getMapComposer().mapSpecies(
                            new SolrQuery((String) winProps.get("lsid"), wkt, null, null)
                            , (String) winProps.get("name")
                            , (String) winProps.get("s")
                            , (Integer) winProps.get("featureCount")
                            , (Integer) winProps.get("type")
                            , wkt, -1);
                    MapLayerMetadata md = ml.getMapLayerMetadata();
                    if (md == null) {
                        md = new MapLayerMetadata();
                        ml.setMapLayerMetadata(md);
                    }
                    md.setMoreInfo((String) winProps.get("metadata"));
                    md.setSpeciesRank((String) winProps.get("rank"));
                } else if (winProps.get("filterGrid") != null && (Boolean) winProps.get("filterGrid")) {
                    MapLayer ml = getMapComposer().mapSpecies(
                            new SolrQuery((String) winProps.get("lsid"),wkt, null, null)
                            , (String) winProps.get("name")
                            , (String) winProps.get("s")
                            , (Integer) winProps.get("featureCount")
                            , (Integer) winProps.get("type")
                            , wkt, -1);
                    MapLayerMetadata md = ml.getMapLayerMetadata();
                    if (md == null) {
                        md = new MapLayerMetadata();
                        ml.setMapLayerMetadata(md);
                    }
                    md.setMoreInfo((String) winProps.get("metadata"));
                    md.setSpeciesRank((String) winProps.get("rank"));
                } else if (winProps.get("byLsid") != null && (Boolean) winProps.get("byLsid")) {
                    MapLayer ml = getMapComposer().mapSpecies(
                            new SolrQuery((String) winProps.get("lsid"),wkt, null,null)
                            ,(String) winProps.get("name")
                            , (String) winProps.get("s")
                            , (Integer) winProps.get("featureCount")
                            , (Integer) winProps.get("type")
                            , wkt, -1);
                    MapLayerMetadata md = ml.getMapLayerMetadata();
                    if (md == null) {
                        md = new MapLayerMetadata();
                        ml.setMapLayerMetadata(md);
                    }
                    md.setMoreInfo((String) winProps.get("metadata"));
                } else {              
                    getMapComposer().mapSpecies(
                            new SolrQuery((String) winProps.get("lsid"),wkt, null, null),
                            (String) winProps.get("taxon"),
                            (String) winProps.get("rank"),
                            0, LayerUtilities.SPECIES
                            , wkt, -1);
                }
            } //else cancel clicked, don't return to mapspeciesinarea popup
        }
    }

    /**
     * maps species in area for the topmost polygon layer
     */
    void mapSpeciesInArea() {
        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        if (layers == null || layers.size() == 0) {
            return;
        }
        try {
            String wkt = layers.get(0).getWKT();

            SolrQuery sq = new SolrQuery(null, wkt, null,  null);
            int results_count_occurrences = sq.getOccurrenceCount();

            //test limit
            if (results_count_occurrences > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
                String activeAreaLayerName = layers.get(0).getDisplayName();
                getMapComposer().mapSpecies(sq
                        , "Occurrences in " + activeAreaLayerName
                        , "species"
                        , results_count_occurrences
                        , LayerUtilities.SPECIES
                        , wkt, -1);

                //getMapComposer().updateUserLogAnalysis("Sampling", sbProcessUrl.toString(), "", CommonData.satServer + "/" + sbProcessUrl.toString(), pid, "map species in area");
            } else {
                getMapComposer().showMessage(results_count_occurrences
                        + " occurrences in this area.\r\nSelect an area with fewer than "
                        + settingsSupplementary.getValueAsInt("max_record_count_map")
                        + " occurrences");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
