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
import org.zkoss.util.Pair;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Filedownload;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ajay
 */
public class AooEooComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(AooEooComposer.class);

    private Doublebox resolution;

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
        q = q.newFacet(new Facet("occurrence_status", "absent", false), false);
        Facet f = new Facet("occurrence_status", "absent", false);
        q = q.newFacet(f, false);
        Query newQ = QueryUtil.queryFromSelectedArea(q, sa, false, null);
        
        double gridSize = dResolution.doubleValue();

        // eoo, use actual points
        LegendObject legend = newQ.getLegend("lat_long");
        StringBuilder sb = new StringBuilder();
        int pointCount = processLegend(legend, sb);
        String aooWkt = aooWkt(legend, gridSize);

        // aoo = gridSize * gridSize * number of gridSize by gridSize cells with an occurrence * (approx sq degrees to sq km)
        double aoo = gridSize * gridSize * aooProcess(legend, gridSize) * 10000;

        double eoo = 0;
        WKTReader reader = new WKTReader();
        String metadata = null;
        try {
            Geometry g = reader.read(sb.toString());
            Geometry convexHull = g.convexHull();
            String wkt = convexHull.toText().replace(" (", "(").replace(", ", ",");

            eoo = SpatialUtil.calculateArea(wkt);

            //aoo area
            Geometry a = reader.read(aooWkt(legend, gridSize));
            Geometry aUnion = a.union();
            String aWkt = aUnion.toText().replace(" (", "(").replace(", ", ",");
            
            if (eoo > 0) {
                String name = "Extent of occurrence (area): " + q.getName();
                MapLayer ml = getMapComposer().addWKTLayer(wkt, name, name);

                name = "Area of occupancy (area): " + q.getName();
                MapLayer mla = getMapComposer().addWKTLayer(aWkt, name, name);

                metadata = "<html><body>" +
                        "<div class='aooeoo'>" +
                        "<div>The Sensitive Data Service may have changed the location of taxa that have a sensitive status." +
                        " It is wise to first map the taxa and examine each record, then filter these records to create the " +
                        "desired subset, then run the tool on the new filtered taxa layer.</div><br />" +
                        "<table >" +
                        "<tr><td>Number of records used for the calculations</td><td>" + newQ.getOccurrenceCount() + "</td></tr>" +
                        "<tr><td>Species</td><td>" + q.getName() + "</td></tr>" +
                        "<tr><td>Area of Occupancy (AOO: " + gridSize + " degree grid)</td><td>" + String.format("%.0f", aoo) + " sq km</td></tr>" +
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

    private int aooProcess(LegendObject legend, double gridSize) {
        Set set = new HashSet<Point2D>();
        for(String key : legend.getCategories().keySet()) {
            try {
                //key=latitude,longitude
                String [] ll = key.split(",");
                Point2D point = new Point2D.Float(round(Double.parseDouble(ll[1]), gridSize),
                        round(Double.parseDouble(ll[0]), gridSize));
                set.add(point);
            } catch (Exception e) {
            }
        }

        return set.size();
    }

    private float round(double d, double by) {
        long l = (long) (d / by);
        return (float) (l * by + (l < 0 ? -by : 0));
    }

    private String aooWkt(LegendObject legend, double gridSize) {
        int pointCount = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("MULTIPOLYGON(");
        for(String key : legend.getCategories().keySet()) {
            try {
                //key=latitude,longitude
                String [] ll = key.split(",");
                float x = round(Double.parseDouble(ll[1]), gridSize);
                float y = round(Double.parseDouble(ll[0]), gridSize);

                String s = "((" + x + " " + y + "," +
                        x + " " + (y + gridSize) + "," +
                        (x + gridSize) + " " + (y + gridSize) + "," +
                        (x + gridSize) + " " + y + "," +
                        x + " " + y + "))";
                if (pointCount > 0) {
                    sb.append(",");
                }
                sb.append(s);
                pointCount++;
            } catch (Exception e) {
            }
        }
        sb.append(")");

        return sb.toString();
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
