/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.legend.Facet;
import au.org.ala.legend.LegendObject;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.QueryUtil;
import au.org.ala.spatial.util.SpatialUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Filedownload;

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
        Facet f = new Facet("occurrence_status_s", "absent", false);
        q = q.newFacet(f, false);
        Query newQ = QueryUtil.queryFromSelectedArea(q, sa, false, null);
        
        // this should not take long, fetch unique points at 0.02 resolution
        LegendObject legend = newQ.getLegend("point-0.02");
        
        StringBuilder sb = new StringBuilder();
        int pointCount = processLegend(legend, sb);
        
        // aoo = 2km * 2km * number of 2km by 2km grid cells with an occurrence
        double aoo = 2.0 * 2.0 * pointCount;

        // eoo, use actual points
        legend = newQ.getLegend("lat_long");
        sb = new StringBuilder();
        pointCount = processLegend(legend, sb);
        
        double eoo = 0;
        WKTReader reader = new WKTReader();
        String metadata = null;
        try {
            Geometry g = reader.read(sb.toString());
            Geometry convexHull = g.convexHull();
            String wkt = convexHull.toText().replace(" (", "(").replace(", ", ",");

            eoo = SpatialUtil.calculateArea(wkt);
            
            if (eoo > 0) {
                String name = "Extent of occurrence (area): " + q.getName();
                MapLayer ml = getMapComposer().addWKTLayer(wkt, name, name);

                metadata = "<html><body>" +
                        "<div class='aooeoo'>" +
                        "<div>The Sensitive Data Service may have changed the location of taxa that have a sensitive status." +
                        " It is wise to first map the taxa and examine each record, then filter these records to create the " +
                        "desired subset, then run the tool on the new filtered taxa layer.</div><br />" +
                        "<table >" +
                        "<tr><td>Number of records used for the calculations</td><td>" + newQ.getOccurrenceCount() + "</td></tr>" +
                        "<tr><td>Species</td><td>" + q.getName() + "</td></tr>" +
                        "<tr><td>Area of Occupancy (AOO: 0.02 degree grid)</td><td>" + String.format("%.0f", aoo) + " sq km</td></tr>" +
                        "<tr><td>Extent of Occurrence (EOO: Minimum convex hull)</td><td>" + (String.format("%.2f", eoo / 1000.0 / 1000.0)) + " sq km</td></tr></table></body></html>" +
                        "</div>";
                ml.getMapLayerMetadata().setMoreInfo("Area of Occupancy and Extent of Occurrence\n" + metadata);

                name = "Extent of occurrence (points): " + q.getName();
                MapLayer ml2 = getMapComposer().mapSpecies(newQ, name, StringConstants.SPECIES,
                        newQ.getOccurrenceCount(), LayerUtilitiesImpl.SPECIES, null,
                        0, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
                ml2.getMapLayerMetadata().setMoreInfo("Area of Occupancy and Extent of Occurrence\n" + metadata);
            } else {
                //trigger eoo unavailable message
                pointCount = 2;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //show results
        String message = "The Sensitive Data Service may have changed the location of taxa that have a sensitive status. " +
                "It is wise to first map the taxa and examine each record, then filter these records to create the " +
                "desired subset, then run the tool on the new filtered taxa layer.\r\n" +
                "\r\nNumber of records used for the calculations: " + newQ.getOccurrenceCount() +
                "\r\nSpecies: " + q.getName() +
                "\r\nArea of Occupancy: " + String.format("%.0f", aoo) + " sq km" +
                "\r\nExtent of Occurrence: " +
                (pointCount < 3 ? "insufficient unique occurrence locations" :
                        (String.format("%.2f", eoo / 1000.0 / 1000.0) + " sq km"));
        if (metadata != null) {
            Event ev = new Event(StringConstants.ONCLICK, null, "Area of Occupancy and Extent of Occurrence\n" + metadata);
            getMapComposer().openHTML(ev);
        } else {
            getMapComposer().showMessage(message);
        }

        //download metadata as text
        Filedownload.save(message, "text/plain", "Calculated AOO and EOO.txt");

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
