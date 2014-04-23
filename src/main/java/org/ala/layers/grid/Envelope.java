/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.grid;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

import org.ala.layers.intersect.Grid;

/**
 * @author Adam
 */
public class Envelope {

    /**
     * Get a grid envelope as WKT.
     * <p/>
     * Envelope one or more 'grid file name,minimum,maximum'.
     *
     * @param params comma separated grid file name,min,max,field.id2,min,max
     * @return wkt representing the envelope as String.
     */
    static public String getGridEnvelopeAsWkt(String params) {
        String[] p = params.split(",");
        Geometry geomIntersect = null;
        if (p.length == 3) {
            Grid g = new Grid(p[0]);
            return Grid2Shape.grid2Wkt(g.getGrid(), Double.parseDouble(p[1]), Double.parseDouble(p[2]), g.nrows, g.ncols, g.xmin, g.ymin, g.xres, g.yres);
        } else {
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
                sb.append(geomIntersect.toText().replace(" (", "(").replace(", ", ","));
            } else if (geomIntersect instanceof GeometryCollection) {
                sb.append("MULTIPOLYGON(");
                int count = 0;
                for (int i = 0; i < geomIntersect.getNumGeometries(); i++) {
                    if (geomIntersect.getGeometryN(i) instanceof Polygon) {
                        if (count > 0) {
                            sb.append(",");
                        }
                        count++;
                        sb.append(geomIntersect.getGeometryN(i).toText().substring(8).replace(", ", ","));
                    } else if (geomIntersect.getGeometryN(i) instanceof MultiPolygon) {
                        if (count > 0) {
                            sb.append(",");
                        }
                        count++;
                        String s = geomIntersect.getGeometryN(i).toText().replace(", ", ",");
                        sb.append(s.substring(14, s.length() - 1));
                    } else if (geomIntersect.getGeometryN(i) instanceof GeometryCollection) {
                        System.out.println("GridCacheReader.getGridEnvelopeAsWkt not processed: GeometryCollection inside of GeometryCollection");
                    }
                }
                sb.append(")");
            }

            return sb.toString();
        }
    }

    /**
     * Get a grid envelope as MultiPolygon.
     * <p/>
     * Envelope one or more 'grid file name,minimum,maximum'.
     *
     * @param params comma separated grid file name,min,max,field.id2,min,max
     * @return wkt representing the envelope as String.
     */
    static public MultiPolygon getGridEnvelopeAsMultiPolygon(String params) {
        String[] p = params.split(",");
        Geometry geomIntersect = null;
        if (p.length == 3) {
            Grid g = new Grid(p[0]);
            return Grid2Shape.grid2Multipolygon(g.getGrid(), Double.parseDouble(p[1]), Double.parseDouble(p[2]), g.nrows, g.ncols, g.xmin, g.ymin, g.xres, g.yres);
        } else {
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
            ArrayList<Polygon> polygons = new ArrayList<Polygon>();
            if (geomIntersect instanceof Polygon) {
                polygons.add((Polygon) geomIntersect);
            } else if (geomIntersect instanceof GeometryCollection
                    || geomIntersect instanceof MultiPolygon) {
                for (int i = 0; i < geomIntersect.getNumGeometries(); i++) {
                    if (geomIntersect.getGeometryN(i) instanceof Polygon) {
                        polygons.add((Polygon) geomIntersect.getGeometryN(i));
                    } else if (geomIntersect.getGeometryN(i) instanceof MultiPolygon) {
                        MultiPolygon mp = (MultiPolygon) geomIntersect.getGeometryN(i);
                        for (int j = 0; j < mp.getNumGeometries(); j++) {
                            polygons.add((Polygon) mp.getGeometryN(j));
                        }
                    } else if (geomIntersect.getGeometryN(i) instanceof GeometryCollection) {
                        System.out.println("GridCacheReader.getGridEnvelopeAsMultiPolygon not processed: GeometryCollection inside of GeometryCollection");
                    }
                }
            }
            GeometryFactory geometryFactory = new GeometryFactory();
            Polygon[] pa = new Polygon[polygons.size()];
            polygons.toArray(pa);

            return geometryFactory.createMultiPolygon(pa);
        }
    }

    /**
     * Stream a grid envelope as WKT.
     * <p/>
     * Envelope one or more 'grid file name,minimum,maximum'.
     *
     * @param params comma separated grid file name,min,max,field.id2,min,max
     * @return wkt representing the envelope as String.
     */
    static public void streamGridEnvelopeAsWkt(String params, OutputStream os) throws IOException {
        String[] p = params.split(",");
        Geometry geomIntersect = null;
        if (p.length == 3) {
            Grid g = new Grid(p[0]);
            Grid2Shape.streamGrid2Wkt(os, g.getGrid(), Double.parseDouble(p[1]), Double.parseDouble(p[2]), g.nrows, g.ncols, g.xmin, g.ymin, g.xres, g.yres);
        } else {
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
            if (geomIntersect instanceof Polygon || geomIntersect instanceof MultiPolygon) {
                os.write(geomIntersect.toText().replace(" (", "(").replace(", ", ",").getBytes());
            } else if (geomIntersect instanceof GeometryCollection) {
                os.write("MULTIPOLYGON(".getBytes());
                int count = 0;
                for (int i = 0; i < geomIntersect.getNumGeometries(); i++) {
                    if (geomIntersect.getGeometryN(i) instanceof Polygon) {
                        if (count > 0) {
                            os.write(",".getBytes());
                        }
                        count++;
                        os.write(geomIntersect.getGeometryN(i).toText().substring(8).replace(", ", ",").getBytes());
                    } else if (geomIntersect.getGeometryN(i) instanceof MultiPolygon) {
                        if (count > 0) {
                            os.write(",".getBytes());
                        }
                        count++;
                        String s = geomIntersect.getGeometryN(i).toText().replace(", ", ",");
                        os.write(s.substring(14, s.length() - 1).getBytes());
                    } else if (geomIntersect.getGeometryN(i) instanceof GeometryCollection) {
                        System.out.println("GridCacheReader.getGridEnvelopeAsWkt not processed: GeometryCollection inside of GeometryCollection");
                    }
                }
                os.write(")".getBytes());
            }
        }
    }

    /**
     * Get a grid envelope as WKT with index.
     * <p/>
     * Only one envelope 'grid file name,minimum,maximum'.
     *
     * @param params comma separated grid file name,min,max,field.id2,min,max
     * @param map    whole grid layer mapping to individual polygons.
     *               Accumulative on the same layer with min and max values that do no overlap
     * @return Map containing "wkt" as String, "map" with updated input map,
     * "index" as csv with records
     * 'map value','wkt polygon character start position'.
     */
    static public Map getGridSingleLayerEnvelopeAsWktIndexed(String params, int[] map) {
        String[] p = params.split(",");
        Grid g = new Grid(p[0]);
        return Grid2Shape.grid2WktIndexed(g.getGrid(), Double.parseDouble(p[1]), Double.parseDouble(p[2]), g.nrows, g.ncols, g.xmin, g.ymin, g.xres, g.yres, map);
    }
}
