/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.grid;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.ala.layers.intersect.Grid;

/**
 *
 * @author Adam
 */
public class Envelope {
    /**
     * Get a grid envelope as WKT.
     *
     * Envelope one or more 'grid file name,minimum,maximum'.
     *
     * @param params comma separated grid file name,min,max,field.id2,min,max
     * @return wkt representing the envelope as String.
     */
    static public String getGridEnvelopeAsWkt(String params) {
        String[] p = params.split(",");
        Geometry geomIntersect = null;
        for (int i = 0; i < p.length; i += 3) {
            Grid g = new Grid(p[i]);
            Geometry thisGeometry = Grid2Shape.grid2Multipolygon(g.getGrid(), Double.parseDouble(p[i + 1]), Double.parseDouble(p[i + 2]), g.nrows, g.ncols, g.xmin, g.ymin, g.xres, g.yres);

            if (geomIntersect == null) {
                geomIntersect = thisGeometry;
            } else {
                geomIntersect = geomIntersect.intersection(thisGeometry);
            }
        }

        //only want polygons
        StringBuilder sb = new StringBuilder();
        if (geomIntersect instanceof Polygon || geomIntersect instanceof MultiPolygon) {
            sb.append(geomIntersect.toText());
        } else if (geomIntersect instanceof GeometryCollection) {
            sb.append("MULTIPOLYGON(");
            int count = 0;
            for (int i = 0; i < geomIntersect.getNumGeometries(); i++) {
                if (geomIntersect.getGeometryN(i) instanceof Polygon) {
                    if (count > 0) {
                        sb.append(",");
                    }
                    count++;
                    sb.append(geomIntersect.getGeometryN(i).toText().substring(8));
                } else if (geomIntersect.getGeometryN(i) instanceof MultiPolygon) {
                    if (count > 0) {
                        sb.append(",");
                    }
                    count++;
                    String s = geomIntersect.getGeometryN(i).toText();
                    sb.append(s.substring(14, s.length() - 1));
                } else if (geomIntersect.getGeometryN(i) instanceof GeometryCollection) {
                    System.out.println("GridCacheReader.getGridEnvelopeAsWkt not processed: GeometryCollection inside of GeometryCollection");
                }
            }
            sb.append(")");
        }

        String wkt = sb.toString().replace(", ", ",").replace(" (","(");
        return wkt;
    }
}
