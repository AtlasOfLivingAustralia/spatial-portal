package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.LayerListComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import net.sf.json.JSONObject;
import org.ala.logger.client.RemoteLogger;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.Facet;
import org.ala.spatial.sampling.SimpleShapeFile;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Radio;

/**
 *
 * @author ajay
 */
public class AddLayerTreeController extends UtilityComposer {

    LayersAutoComplete lac;
    RemoteLogger remoteLogger;
    String shortName, treeName, treePath, treeMetadata, treePid;
    int treeSubType;
    String searchName, searchPath, searchMetadata;
    int searchSubType;
    Button btnOk;
    Div divSearch, divTree;
    Radio rSearch, rTree;

    @Override
    public void afterCompose() {
        super.afterCompose();

        rSearch.setChecked(true);
    }

    public void onClick$btnOk(Event event) {
        if (btnOk.isDisabled()) {
            return;
        }
        if (treeName != null) {
            System.out.println("treeName not null");

            String lyrSubType = "";
            if (treeSubType == LayerUtilities.CONTEXTUAL) {
                lyrSubType = "Contextual";
            } else if (treeSubType == LayerUtilities.GRID) {
                lyrSubType = "Environmental";
            }


            if (treePid != null) {
                System.out.println("treePid not null");
                //map layerbranch as polygon layer
                MapLayer mapLayer;
                mapLayer = getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(treeName), treeName, treePath, 0.6f, /*metadata url*/ null,
                        null, LayerUtilities.WKT, null, null);
                if (mapLayer != null) {
                    mapLayer.setWKT(readUrl(CommonData.layersServer + "/shape/wkt/" + treePid));
                    mapLayer.setPolygonLayer(true);

                    String object = readUrl(CommonData.layersServer + "/object/" + treePid);
                    String fid = getStringValue(null, "fid", object);
                    String bbox = getStringValue(null, "bbox", object);
                    String spid = getStringValue("\"id\":\"" + fid + "\"", "spid", readUrl(CommonData.layersServer + "/fields"));
                    
                    System.out.println("CommonData.layersServer: "+CommonData.layersServer);
                    System.out.println("pid: "+getMapComposer().getNextAreaLayerName(treeName));
                    System.out.println("layername: "+treeName);
                    System.out.println("object: "+object);
                    System.out.println("fid: "+fid);

                    MapLayerMetadata md = mapLayer.getMapLayerMetadata();
                    if (md == null) {
                        md = new MapLayerMetadata();
                        mapLayer.setMapLayerMetadata(md);
                    }
                    try {
                        double[][] bb = SimpleShapeFile.parseWKT(bbox).getBoundingBox();
                        ArrayList<Double> dbb = new ArrayList<Double>();
                        dbb.add(bb[0][0]);
                        dbb.add(bb[0][1]);
                        dbb.add(bb[1][0]);
                        dbb.add(bb[1][1]);
                        md.setBbox(dbb);
                    } catch (Exception e) {
                        System.out.println("failed to parse: " + bbox);
                        e.printStackTrace();
                    }
                    md.setMoreInfo(CommonData.satServer + "/layers/" + spid);

                    Facet facet = getFacetForObject(treePid, treeName);
                    if (facet != null) {
                        ArrayList<Facet> facets = new ArrayList<Facet>();
                        facets.add(facet);
                        mapLayer.setData("facets", facets);
                    }

                    getMapComposer().updateUserLogMapLayer("gaz", treeName + "|" + treePath);
                    remoteLogger.logMapArea(treeName, "Layer - " + lyrSubType, CommonData.layersServer + "/object/" + treePid, shortName, treeMetadata);
                    System.out.println("treePid: "+treePid);
                    System.out.println("shortName: "+shortName);
                    System.out.println("treeMetadata: "+treeMetadata);
                }
            } else {
                System.out.println("treePid null");
                getMapComposer().addWMSLayer(treeName, treeName,
                        treePath,
                        (float) 0.75, treeMetadata, null, treeSubType, null, null, null);
                remoteLogger.logMapArea(treeName, "Layer - " + lyrSubType, treePath, shortName, treeMetadata );
                System.out.println("treeName: "+treeName);
                System.out.println("treePath: "+treePath);
                System.out.println("treeMetadata: "+treeMetadata);
                System.out.println("treeSubType: "+treeSubType);
                
            }

            getMapComposer().updateUserLogMapLayer("env - tree - add", /*joLayer.getString("id")+*/ "|" + treeName);
            //remoteLogger.logMapArea(treeName, "env - tree - add", "");
        } else if (searchName != null) {
            System.out.println("treeName null");
            getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(searchName),searchName,
                    searchPath,
                    (float) 0.75, searchMetadata, null, searchSubType, null, null);
            System.out.println("CommonData.layersServer: "+CommonData.layersServer);
            System.out.println("getMapComposer().getNextAreaLayerName(searchName): "+getMapComposer().getNextAreaLayerName(searchName));
            System.out.println("searchName: "+searchName);
            System.out.println("searchPath: "+searchPath);
            System.out.println("searchSubType: "+searchSubType);

            getMapComposer().updateUserLogMapLayer("env - search - add", /*joLayer.getString("id")+*/ "|" + searchName);
            if (!rTree.isChecked()) {
                //JSONObject jo = (JSONObject) lac.getSelectedItem().getValue();
                String lyrSubType = "";
                if (searchSubType == LayerUtilities.CONTEXTUAL) {
                    lyrSubType = "Contextual";
                } else if (searchSubType == LayerUtilities.GRID) {
                    lyrSubType = "Environmental";
                }
                remoteLogger.logMapArea(searchName, "Layer - " + lyrSubType, searchPath, shortName, searchMetadata);
            } else {
                remoteLogger.logMapArea(searchName, "Layer", searchMetadata);
            }
            
        }

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onChange$lac(Event event) {
        searchName = null;
        btnOk.setDisabled(true);

        LayerListComposer llc = (LayerListComposer) getFellow("layerList").getFellow("layerswindow");

        if (lac.getItemCount() > 0 && lac.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) lac.getSelectedItem().getValue();
            String metadata = "";

            metadata = CommonData.layersServer + "/layers/" + jo.getString("id");

            setLayer(jo.getString("name"), jo.getString("displayname"), jo.getString("displaypath"), metadata,
                    jo.getString("type").equalsIgnoreCase("environmental") ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL);
        }
    }

    public void setLayer(String shortName, String name, String displaypath, String metadata, int subType) {
        setLayer(shortName, name, null, displaypath, metadata, subType);
    }

    public void setLayer(String shortName, String name, String pid, String displaypath, String metadata, int subType) {
        if (rTree.isChecked()) {
            this.shortName = shortName;
            treeName = name;
            treePid = pid;
            treePath = displaypath;
            treeMetadata = metadata;
            treeSubType = subType;
        } else {
            this.shortName = shortName; 
            searchName = name;
            searchPath = displaypath;
            searchMetadata = metadata;
            searchSubType = subType;
        }

        btnOk.setDisabled(false);
    }

    public void onCheck$rgAddLayer(Event event) {
        divSearch.setVisible(rSearch.isChecked());
        divTree.setVisible(rTree.isChecked());

        btnOk.setDisabled((rTree.isChecked() && treeName == null)
                || (rSearch.isChecked() && searchName == null));

//        if(rSearch.isChecked()) {
//            lac.setFocus(true);
//        } else {
//            ((LayerListComposer)((HtmlMacroComponent)getFellow("layerList")).getFellow("layerswindow")).setFocus(true);
//        }
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
