package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AddSpeciesInArea extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    String selectedMethod = "";
    String pid = "";
    Radiogroup rgArea;
    Radio rAreaWorld, rAreaCustom, rAreaSelected, rAreaAustralia;
    Button btnCancel, btnOk;
    Textbox tToolName;
    boolean setCustomArea = false;
    String lsid;

    String rank;
    String taxon;
    private String name;
    private String s;
    private int type;
    private int featureCount;

    boolean filterGrid = false;
    boolean filter = false;
    boolean byLsid = false;
    private String metadata;
    private boolean allSpecies = false;

    @Override
    public void afterCompose() {
        super.afterCompose();

        //loadAreaLayers();
    }

    public void loadAreaLayers() {
        try {

            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgArea");
            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrent");

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);
            }

            if(!allSpecies) {
                //rgArea.setSelectedItem(rAreaWorld);
                rAreaSelected = rAreaWorld; //set as default in the zul
            } else {
                rAreaWorld.setVisible(false);
                rAreaAustralia.setVisible(false);

                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible()) {
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        rgArea.setSelectedItem(rAreaSelected);
                        Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Unable to load active area layers:");
            e.printStackTrace(System.out);
        }
    }

    public void onCheck$rgArea(Event event) {
        if (rgArea == null) {
            return;
        }
        rAreaSelected = rgArea.getSelectedItem();
        try {
            rAreaSelected = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onClick$btnOk(Event event) {       

        try {
            if (rAreaSelected == rAreaCustom) {
                Map<String, Object> winProps = new HashMap<String, Object>();
                winProps.put("parent", this);
                winProps.put("parentname", "AddSpeciesInArea");
                winProps.put("lsid", lsid);
                winProps.put("rank", rank);
                winProps.put("taxon", taxon);
                winProps.put("name", name);
                winProps.put("s", s);
                winProps.put("featureCount", featureCount);
                winProps.put("filter", filter);
                winProps.put("filterGrid", filterGrid);
                winProps.put("byLsid", byLsid);
                winProps.put("metadata", metadata);
                winProps.put("type", type);

                Window window = (Window) Executions.createComponents("WEB-INF/zul/AddArea.zul", getMapComposer(), winProps);
                window.setAttribute("winProps", winProps, true);
                window.doModal();
            } else {
                onFinish();
            }
        } catch (Exception ex) {
            Logger.getLogger(AddSpeciesInArea.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.detach();
    }

    public void onFinish() {
        try {            
            String wkt = getSelectedArea();
            wkt = wkt.replace("MULTIPOLYGON(((", "GEOMETRYCOLLECTION(POLYGON((").replace("),(", "),POLYGON(");

            String spname = name + ";" + lsid;
            boolean setupMetadata = true;

            MapLayer ml = null;

            if (byLsid) {
                ml = getMapComposer().mapSpeciesByLsid(Util.newLsidArea(lsid, wkt),spname, s, featureCount, type, wkt);
            } else if(filter) {
                ml = getMapComposer().mapSpeciesByLsidFilter(Util.newLsidArea(lsid, wkt), spname, s, featureCount, type, wkt);
            } else if(filterGrid) {
                ml = getMapComposer().mapSpeciesByLsidFilterGrid(Util.newLsidArea(lsid, wkt), spname, s, featureCount, type, wkt);
            } else if (rank != null && taxon != null && lsid != null) {
                String sptaxon = taxon+";"+lsid;
                ml = getMapComposer().mapSpeciesByLsid(Util.newLsidArea(lsid, wkt), sptaxon, rank, 0, LayerUtilities.SPECIES, wkt);
                setupMetadata = false;
            } else {
                StringBuffer sbProcessUrl = new StringBuffer();
                sbProcessUrl.append("/filtering/apply");
                sbProcessUrl.append("/pid/" + URLEncoder.encode("none", "UTF-8"));
                sbProcessUrl.append("/species/count");
                String[] out = postInfo(sbProcessUrl.toString(), wkt).split("\n");
                //int results_count = Integer.parseInt(out[0]);
                int results_count_occurrences = Integer.parseInt(out[1]);

                //test limit
                if (results_count_occurrences > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
                    //register points with a new id for mapping
                    String lsid = registerPointsInArea(wkt);
                    sbProcessUrl = new StringBuffer();
                    String activeAreaLayerName = getSelectedAreaDisplayName();
                    ml = getMapComposer().mapSpeciesByLsid(lsid, "Occurrences in " + activeAreaLayerName, "species", results_count_occurrences, LayerUtilities.SPECIES, wkt);

                    //getMapComposer().updateUserLogAnalysis("Sampling", sbProcessUrl.toString(), "", CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString(), pid, "map species in area");
                } else {
                    getMapComposer().showMessage(results_count_occurrences
                            + " occurrences in this area.\r\nSelect an area with fewer than "
                            + settingsSupplementary.getValueAsInt("max_record_count_map")
                            + " occurrences");
                }

                setupMetadata = false;
            }

            if (setupMetadata) {
                MapLayerMetadata md = ml.getMapLayerMetadata();
                if (md == null) {
                    md = new MapLayerMetadata();
                    ml.setMapLayerMetadata(md);
                }
                md.setMoreInfo(metadata);
                //md.setSpeciesDisplayLsid(lsid);
                //md.setSpeciesRank(rank);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String registerPointsInArea(String area) {
        //register with alaspatial using data.getPid();
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("species/area/register");

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString());
            post.addParameter("area", area);
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();

            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getSelectedArea() {
        //String area = rgArea.getSelectedItem().getValue();
        String area = rAreaSelected.getValue(); 

        try {
            if (area.equals("current")) {
                area = getMapComposer().getViewArea();
            } else if (area.equals("australia")) {
                area = "POLYGON((112.0 -44.0,112.0 -9.0,154.0 -9.0,154.0 -44.0,112.0 -44.0))";
            } else if (area.equals("world")) {
                //area = "POLYGON((-180 -90,-180 90.0,180.0 90.0,180.0 -90.0,-180.0 -90.0))";
                area = "POLYGON((-179.999 -84.999,-179.999 84.999,179.999 84.999,179.999 -84.999,-179.999 -84.999))";
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getDisplayName())) {
                        area = ml.getWKT();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected area");
            e.printStackTrace(System.out);
        }

        return area;
    }

    public String getSelectedAreaName() {
        String area = rAreaSelected.getLabel();
        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (MapLayer ml : layers) {
            if (area.equals(ml.getDisplayName())) {
                area = ml.getName();
                break;
            }
        }

        return area;
    }

    public String getSelectedAreaDisplayName() {
        String areaName = rAreaSelected.getLabel();

        return areaName;
    }

    private String postInfo(String urlPart, String wkt) {
        try {
            HttpClient client = new HttpClient();

            PostMethod post = new PostMethod(CommonData.satServer + "/alaspatial/ws" + urlPart); // testurl

            post.addRequestHeader("Accept", "application/json, text/javascript, */*");
            post.addParameter("area", wkt);

            int result = client.executeMethod(post);

            //TODO: confirm result
            String slist = post.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    void setSpeciesParams(String lsid, String rank, String taxon) {
        this.lsid = lsid;
        this.rank = rank;
        this.taxon = taxon;
    }

    void setSpeciesFilterGridParams(String lsid, String name, String s, int featureCount, int type, String metadata, String rank) {
        this.lsid = lsid;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.filterGrid = true;
        this.metadata = metadata;
        this.rank = rank;
    }
    void setSpeciesFilterParams(String lsid, String name, String s, int featureCount, int type, String metadata, String rank) {
        this.lsid = lsid;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.filter = true;
        this.metadata = metadata;
        this.rank = rank;
    }

    void setSpeciesByLsidParams(String lsid, String name, String s, int featureCount, int type, String metadata) {
        this.lsid = lsid;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.byLsid = true;
        this.metadata = metadata;
    }

    void setAllSpecies(boolean isAllSpecies) {
        this.allSpecies = isAllSpecies;
    }
}
