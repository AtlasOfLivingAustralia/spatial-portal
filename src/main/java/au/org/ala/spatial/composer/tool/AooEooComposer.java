/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.legend.LegendObject;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.QueryUtil;
import au.org.ala.spatial.util.SpatialUtil;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.log4j.Logger;

/**
 * @author ajay
 */
public class AooEooComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(AooEooComposer.class);
    
    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "AOO EOO";
        this.totalSteps = 2;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.updateWindowTitle();
    }

    @Override
    public boolean onFinish() {
        SelectedArea sa = getSelectedArea();
        Query q = getSelectedSpecies();
        Query newQ = QueryUtil.queryFromSelectedArea(q, sa, false, null);
        
        // this should not take long, fetch unique points at 0.02 resolution
        LegendObject legend = newQ.getLegend("point-0.02");
        
        StringBuilder sb = new StringBuilder();
        int pointCount = processLegend(legend, sb);
        
        // aoo = 2km * 2km * number of 2km by 2km grid cells with an occurrence
        double aoo = 2.0 * 2.0 * pointCount;

        // eoo, want > 3 points
        int pos = 0;
        String [] facets = new String[] { "point-0.01", "point-0.001", "point-0.0001", "lat_long"};
        while (pointCount < 3 && pos < facets.length) {
            legend = newQ.getLegend(facets[pos]);
            sb = new StringBuilder();
            pointCount = processLegend(legend, sb);
            pos ++;
        }
        double eoo = 0;
        WKTReader reader = new WKTReader();
        try {
            Geometry g = reader.read(sb.toString());
            Geometry convexHull = g.convexHull();
            String wkt = convexHull.toText().replace(" (", "(").replace(", ", ",");

            eoo = SpatialUtil.calculateArea(wkt);
            
            if (eoo > 0) {
                String name = "Area of Occupancy: " + q.getName();
                MapLayer ml = getMapComposer().addWKTLayer(wkt, name, name);

                String metadata = "<html><body><table><tr><td>Species</td><td>" + q.getName() + "</td></tr>" +
                        "<tr><td>Area of Occupancy</td><td>" + String.format("%.0f", aoo ) + " sq km</td></tr>" +
                        "<tr><td>Extent of Occurrence</td><td>" + (String.format("%.0f", eoo / 1000.0 / 1000.0)) + " sq km</td></tr></table></body></html>";
                ml.getMapLayerMetadata().setMoreInfo("Area of Occupancy and Extent of Occurrence\n" + metadata);
            } else {
                //trigger eoo unavailable message
                pointCount = 2;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //show results
        String message = "Area of Occupancy: " + String.format("%.0f", aoo) + " sq km\r\nExtent of Occurrence: " + (String.format("%.0f",eoo / 1000.0 / 1000.0)) + " sq km";
        if (pointCount < 3) {
            message = "Area of Occupancy: " + String.format("%.0f", aoo) + " sq km\r\nExtent of Occurrence: insufficient unique occurrence locations";
        }
        getMapComposer().showMessage(message);
        
        this.detach();

        return true;
    }
    
    private int processLegend(LegendObject legend, StringBuilder sb) {
        int pointCount = 0;
        sb.append("GEOMETRYCOLLECTION(");
        for(String key : legend.getCategories().keySet()) {
            try {
                //key=latitude,longitude
                String [] ll = key.split(",");
                String s = "POINT(" + Double.parseDouble(ll[1]) + " " + Double.parseDouble(ll[0]) + ")";
                if (pointCount > 0) {
                    sb.append(",");
                }
                sb.append(s);
                pointCount++;
            } catch (Exception e) {
            }
        }
        sb.append(")");
        
        return pointCount;
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                if (rSpeciesSearch.isChecked()) {
                    searchSpeciesACComp.getAutoComplete().setFocus(true);
                } else {
                    rgSpecies.setFocus(true);
                }
                break;
            default:
                LOGGER.error("invalid step for AooEooComposer: " + currentStep);
        }
    }
}
