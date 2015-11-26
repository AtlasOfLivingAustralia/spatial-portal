package au.org.ala.spatial.composer.add.area;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.util.GeometryCombiner;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Only does (area or area or area) not (area and area and area) so
 * only layers with WKT are usable.
 *
 * @author Adam
 */
public class AreaMerge extends AreaToolComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaMerge.class);

    private Textbox txtLayerName;
    private Button btnOk;
    private Vbox vboxAreas;

    @Override
    public void afterCompose() {
        super.afterCompose();
        btnOk.setDisabled(false);

        txtLayerName.setValue(getMapComposer().getNextAreaLayerName(CommonData.lang(StringConstants.DEFAULT_AREA_LAYER_NAME)));

        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (int i = 0; i < layers.size(); i++) {
            //must be non-envelope or "cl" layer-field that is not
            // 'grid as contextual'

            MapLayer ml = layers.get(i);

            boolean isGrid = false;
            if (ml.getFacets() != null
                    && ml.testWKT() != null && ml.getWKT().startsWith(StringConstants.ENVELOPE)) {
                String wkt = ml.getWKT();

                //get fid
                String fid = wkt.substring(9, wkt.indexOf(','));

                if (fid.startsWith("cl")) {
                    JSONArray ja = CommonData.getLayerListJSONArray();
                    for (int j = 0; j < ja.size() && !isGrid; j++) {
                        JSONObject field = (JSONObject) ja.get(j);
                        JSONObject layer = (JSONObject) field.get("layer");
                        if (field.get(StringConstants.ID).toString().equalsIgnoreCase(fid)) {
                            isGrid = "a".equalsIgnoreCase(field.get(StringConstants.TYPE).toString()) ||
                                    "b".equalsIgnoreCase(field.get(StringConstants.TYPE).toString());
                        }
                    }
                } else {
                    isGrid = true;
                }
            }

            if (ml.getFacets() == null || !isGrid) {
                Checkbox cb = new Checkbox();
                cb.setLabel(layers.get(i).getDisplayName());
                cb.setValue(layers.get(i));
                cb.setParent(vboxAreas);
            }
        }
    }

    public void onClick$btnOk(Event event) {
        if (!validate()) {
            getMapComposer().showMessage(CommonData.lang("error_not_enough_areas_for_merging"));
            return;
        }

        mergeAreas();

        ok = true;
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    private boolean validate() {
        int count = 0;
        for (int i = 0; i < vboxAreas.getChildren().size(); i++) {
            if (((Checkbox) vboxAreas.getChildren().get(i)).isChecked()) {
                count++;
            }
        }

        return count >= 2;
    }

    void mergeAreas() {
        List<Facet> facets = new ArrayList<Facet>();
        List<Geometry> wkt = new ArrayList<Geometry>();
        WKTReader wktReader = new WKTReader();

        String layerDisplayNames = "";

        for (int i = 0; i < vboxAreas.getChildren().size(); i++) {
            Checkbox cb = (Checkbox) vboxAreas.getChildren().get(i);
            if (cb.isChecked()) {
                MapLayer ml = cb.getValue();
                if (layerDisplayNames.length() > 0) {
                    layerDisplayNames += ", ";
                }
                layerDisplayNames += ml.getDisplayName();

                if (ml != null) {
                    if (ml.getFacets() != null) {
                        facets.addAll(ml.getFacets());
                    }
                    try {
                        //get actual WKT when 'envelope' is specified
                        String w = ml.getWKT();
                        if (w.startsWith(StringConstants.ENVELOPE)) {
                            //should only be one pid
                            String pid = w.substring(w.indexOf(',') + 1, w.length() - 1);
                            w = Util.readUrl(CommonData.getLayersServer() + "/shape/wkt/" + pid);
                        }
                        Geometry g = wktReader.read(w);
                        if (g != null) {
                            wkt.add(g);
                        }
                    } catch (ParseException e) {
                        LOGGER.error("cannot parse WKT for map layer: " + ml.getDisplayName() + " for WKT: " + ml.getWKT());
                    }
                } else {
                    String swkt = null;
                    if (CommonData.getSettings().getProperty(CommonData.AUSTRALIA_NAME).equalsIgnoreCase(cb.getLabel())) {
                        swkt = CommonData.getSettings().getProperty(CommonData.AUSTRALIA_WKT);
                    } else if ("Current Extent".equalsIgnoreCase(cb.getLabel())) {
                        swkt = getMapComposer().getViewArea();
                    } else {
                        LOGGER.error("cannot determine what this checked area is: " + cb.getLabel());
                    }
                    if (swkt != null) {
                        try {
                            Geometry g = wktReader.read(swkt);
                            if (g != null) {
                                wkt.add(g);
                            }
                        } catch (ParseException e) {
                            LOGGER.error("cannot parse WKT for map layer: " + ml.getDisplayName() + " for WKT: " + swkt);
                        }
                    }
                }
            }
        }

        //produce single geometry
        Geometry geometry = null;
        if (!wkt.isEmpty()) {
            geometry = wkt.get(0);
            for (int i = 1; i < wkt.size(); i++) {
                geometry = GeometryCombiner.combine(geometry, wkt.get(i));
            }
        }

        String finalWkt = (geometry == null) ? null : geometry.toString();

        MapComposer mc = getMapComposer();

        layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
        MapLayer mapLayer = mc.addWKTLayer(finalWkt, layerName, txtLayerName.getValue());

        //if possible, use facets instead of WKT with biocache
        if (wkt.size() == facets.size()) {
            //change to a single OR facet.
            //Because all facet areas are single or multiple, ORs just need to OR for joining.
            String fq = facets.get(0).toString();
            for (int i = 1; i < facets.size(); i++) {
                fq += " OR " + facets.get(i).toString();
            }
            List<Facet> array = new ArrayList<Facet>();
            array.add(Facet.parseFacet(fq));
            mapLayer.setFacets(array);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        String metadata = "";
        metadata += "Merged WKT layers\nLayers: " + layerDisplayNames + "\n";
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
