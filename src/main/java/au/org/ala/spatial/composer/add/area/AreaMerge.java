package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.LayersUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.util.GeometryCombiner;
import com.vividsolutions.jts.io.WKTReader;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.legend.Facet;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;
import org.zkoss.zul.Calendar;

import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Only does (area or area or area) not (area and area and area) so
 * only layers with WKT are usable.
 *
 * @author Adam
 */
public class AreaMerge extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaMerge.class);

    Textbox txtLayerName;
    Button btnOk;
    Vbox vboxAreas;

    @Override
    public void afterCompose() {
        super.afterCompose();
        btnOk.setDisabled(false);

        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));

        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (int i = 0; i < layers.size(); i++) {
            //must be non-envelope or "cl" layer-field that is not
            // 'grid as contextual'

            MapLayer ml = layers.get(i);

            boolean isGrid = false;
            if(ml.getFacets() != null) {
                String wkt = ml.getWKT();
                if(wkt != null && wkt.startsWith("ENVELOPE")) {
                    //get fid
                    String fid = wkt.substring(9,wkt.indexOf(','));

                    if(fid.startsWith("cl")) {

                        JSONArray ja = CommonData.getLayerListJSONArray();
                        for(int j=0;j<ja.size();j++) {
                            JSONObject jo = (JSONObject) ja.get(j);
                            if(jo.getString("id").equalsIgnoreCase(fid)) {
                                if(jo.containsKey("fields")) {
                                    JSONArray fields = jo.getJSONArray("fields");
                                    for(int k=0;k<fields.size();k++) {
                                        JSONObject field = (JSONObject)fields.getJSONObject(k);
                                        if(field.getString("type").equalsIgnoreCase("a") ||
                                                field.getString("type").equalsIgnoreCase("b")) {
                                            isGrid = true;
                                            break;
                                        }
                                    }
                                }

                                break;
                            }
                        }
                    } else {
                        isGrid = true;
                    }
                }
            }

            if(ml.getFacets() == null || !isGrid) {
                Checkbox cb = new Checkbox();
                cb.setLabel(layers.get(i).getDisplayName());
                cb.setValue(layers.get(i));
                cb.setParent(vboxAreas);
            }
        }
    }

    public void onClick$btnOk(Event event) {
        if (!validate()) {
            getMapComposer().showMessage("Select 2 or more areas to join.");
            return;
        }

        mergeAreas();

        ok = true;
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getMapComposer();
        this.detach();
    }

    private boolean validate() {
        int count = 0;
        for(int i=0;i<vboxAreas.getChildren().size();i++) {
            if(((Checkbox) vboxAreas.getChildren().get(i)).isChecked()) {
                count++;
            }
        }

        return count >= 2;
    }

    void mergeAreas() {
        ArrayList<Facet> facets = new ArrayList<Facet>();
        ArrayList<Geometry> wkt = new ArrayList<Geometry>();
        WKTReader wktReader = new WKTReader();

        String layer_display_names = "";

        for(int i=0;i<vboxAreas.getChildren().size();i++) {
            Checkbox cb = ((Checkbox) vboxAreas.getChildren().get(i));
            if(cb.isChecked()) {
                MapLayer ml = (MapLayer)cb.getValue();
                if(layer_display_names.length() > 0) {
                    layer_display_names += ", ";
                }
                layer_display_names += ml.getDisplayName();

                if(ml != null) {
                    if (ml.getFacets() != null) {
                        facets.addAll(ml.getFacets());
                    }
//                  else {
                        try {
                            //get actual WKT when 'envelope' is specified
                            String w = ml.getWKT();
                            if (w.startsWith("ENVELOPE")) {
                                //should only be one pid
                                String pid = w.substring(w.indexOf(',') + 1, w.length()-1);
                                w = Util.readUrl(CommonData.layersServer + "/shape/wkt/" + pid);
                            }
                            Geometry g = wktReader.read(w);
                            if(g != null) {
                                wkt.add(g);
                            }
                        } catch (Exception e) {
                            logger.error("cannot parse WKT for map layer: " + ml.getDisplayName() + " for WKT: " + ml.getWKT());
                        }
//                    }
                } else {
                    String swkt = null;
                    if (cb.getLabel().equalsIgnoreCase("Australia")) {
                        swkt = CommonData.AUSTRALIA_WKT;
                    } else if(cb.getLabel().equalsIgnoreCase("Current Extent")) {
                        swkt = getMapComposer().getViewArea();
                    } else {
                        logger.error("cannot determine what this checked area is: " + cb.getLabel());
                    }
                    if(swkt != null) {
                        try {
                            Geometry g = wktReader.read(swkt);
                            if(g != null) {
                                wkt.add(g);
                            }
                        } catch (Exception e) {
                            logger.error("cannot parse WKT for map layer: " + ml.getDisplayName() + " for WKT: " + swkt);
                        }
                    }
                }
            }
        }

        //produce single geometry
        Geometry geometry = null;
        if (wkt.size() > 0) {
            geometry = wkt.get(0);
            for(int i=1;i<wkt.size();i++) {
                geometry = GeometryCombiner.combine(geometry, wkt.get(i));
            }
        }

        String finalWkt = (geometry == null)?null:geometry.toString();
        //finalWkt = Util.reduceWKT(finalWkt);

        MapComposer mc = getMapComposer();

        String layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
        MapLayer mapLayer = mc.addWKTLayer(finalWkt, layerName, txtLayerName.getValue());

        //if possible, use facets instead of WKT with biocache
        if(wkt.size() == facets.size()) {
            //change to a single OR facet.
            //Because all facet areas are single or multiple, ORs just need to OR for joining.
            String fq = facets.get(0).toString();
            for(int i=1;i<facets.size();i++) {
                fq += " OR " + facets.get(i).toString();
            }
            ArrayList<Facet> array = new ArrayList<Facet>();
            array.add(Facet.parseFacet(fq));
            mapLayer.setFacets(array);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        String metadata = "";
        metadata += "Merged WKT layers\nLayers: " + layer_display_names + "\n";
        metadata += "Name: " + layerName + " <br />\n";
        metadata += "Date: " + formatter.format(calendar.getTime()) + " <br />\n";

        mapLayer.getMapLayerMetadata().setMoreInfo(metadata);

        //reapply layer name
        getMapComposer().getMapLayer(layerName).setDisplayName(txtLayerName.getValue());
        getMapComposer().redrawLayersList();

        ok = true;

        this.detach();
    }
}
