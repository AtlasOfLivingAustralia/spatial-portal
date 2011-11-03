/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.geotools.geometry.jts.JTSFactoryFinder;

/**
 *
 * @author Adam
 */
public class Grid2Shape {
    static final byte lEdge = 0x01;
    static final byte rEdge = 0x02;
    static final byte tEdge = 0x04;
    static final byte bEdge = 0x08;

    static public String grid2Wkt(byte [] data, int presenceValue, int nrows, int ncols, double minx, double miny, double resx, double resy)  {
        int gid = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("MULTIPOLYGON(");
        gid = getPolygons(sb,gid, data, presenceValue, nrows, ncols, resy, resy, minx, miny);
        sb.append(")");

        return sb.toString();
    }

    static int getX(int pos, int ncols) {
        return pos % ncols;
    }

    static int getY(int pos, int ncols) {
        return (int) (pos / ncols);
    }

    static int getPos(int x, int y, int ncols) {
        return x + y * ncols;
    }

    static int getPolygons(StringBuilder sb, int gid, byte[] data, int thisPolygon, int nrows, int ncols, double xres, double yres, double minx, double miny) {
        int[] image = new int[data.length];
        byte[] edges = new byte[data.length];

        HashMap<Integer, Entry<Integer, Set<Integer>>> polygons = new HashMap<Integer, Entry<Integer, Set<Integer>>>();
        findEdges(data, image, edges, thisPolygon, nrows, ncols, polygons);

        gid = edgesToPolyons(sb, gid, thisPolygon, edges, image, nrows, ncols, yres, yres, minx, miny, polygons);

        return gid;
    }

    static void findEdges(byte[] data, int[] image, byte[] edges, float key, int nrows, int ncols, HashMap<Integer, Entry<Integer, Set<Integer>>> polygons) {
        int groups = 1;

        //row by row
        long time = System.currentTimeMillis() + 5000;
        int groupCount = 0;
        for (int i = 0; i < nrows; i++) {
            if (System.currentTimeMillis() > time) {
                System.out.print(String.format("\redge detection: %3.4f%%, found %d polygons         ", 100 * i / (double) nrows, groupCount));
                time = System.currentTimeMillis() + 5000;
            }

            for (int j = 0; j < ncols; j++) {
                int pos = getPos(j, i, ncols);
                if (data[pos] == key) {

                    //inherit group number from left
                    if (j > 0 && image[pos - 1] > 0) {
                        image[pos] = image[pos - 1];

                        //joining with another group
                        boolean joinedWithNewGroup = false;
                        if (i > 0 && data[pos - ncols] == key
                                && image[pos - ncols] != image[pos]) {
                            joinedWithNewGroup = true;
                            int oldGroup = Math.max(image[pos - ncols], image[pos]);
                            int newGroup = Math.min(image[pos - ncols], image[pos]);
                            for (int n = i; n >= 0; n--) {
                                boolean cellFound = false;
                                for (int m = 0; m < ncols; m++) {
                                    int p = getPos(m, n, ncols);
                                    if (image[p] == oldGroup) {
                                        image[p] = newGroup;
                                        cellFound = true;
                                    }
                                }
                                if (n != i && !cellFound) {
                                    break;
                                }
                            }

                            //merge interiorStarts
                            Set<Integer> oldStarts = polygons.get(oldGroup).getValue();
                            if (oldStarts != null) {
                                Set<Integer> newStarts = polygons.get(newGroup).getValue();
                                if (newStarts == null) {
                                    newStarts = new HashSet<Integer>();
                                    polygons.get(newGroup).setValue(newStarts);
                                }
                                newStarts.addAll(oldStarts);
                                polygons.put(oldGroup, null);
                            }
                            polygons.remove(oldGroup);

                            groupCount--;
                        }

                        //bottom edge: when not joined with a new group and one of
                        //first row
                        //or image(row-1,col) is != this image
                        //or edges(row,col-1) has lower edge
                        if (!joinedWithNewGroup
                                && (i == 0 || image[pos - ncols] != image[pos]
                                || (j > 0 && (edges[pos - 1] & bEdge) > 0))) {

                            //test for closed polygon
                            if (j > 0 && i > 0 && image[pos - ncols] == image[pos]) {
                                //track interior of polygon
                                if (image[pos - ncols - 1] != image[pos]) {
                                    Set<Integer> starts = polygons.get(image[pos]).getValue();
                                    if (starts == null) {
                                        starts = new HashSet<Integer>();
                                        polygons.get(image[pos]).setValue(starts);
                                    }
                                    starts.add(pos - ncols);
                                }
                            } else {
                                edges[pos] |= bEdge;
                            }

                        }

                        //left edge: when first column
                        //or image(row,col-1) is != this image
                        if (j == 0 || image[pos - 1] != image[pos]) {
                            edges[pos] |= lEdge;
                        }

                        //right edge: when last column
                        //or data(row,col+1) is != key
                        //(image value not yet assigned, use data)
                        if (j + 1 == ncols || data[pos + 1] != key) {
                            edges[pos] |= rEdge;
                        }

                        //top edge: when last row
                        //or data(row+1,col) is != key
                        if (i + 1 == nrows || data[pos + ncols] != key) {
                            edges[pos] |= tEdge;
                        }

                        //inherit from bottom
                    } else if (i > 0 && image[pos - ncols] > 0) {
                        image[pos] = image[pos - ncols];

                        //bottom edge: when first row
                        if (i == 0) {
                            edges[pos] |= bEdge;
                        }

                        //left edge: when first column
                        //or (never occurrs) image(row,col-1) is != this image
                        if (j == 0 || image[pos - 1] != image[pos]) {
                            edges[pos] |= lEdge;
                        }

                        //right edge: when last column
                        //or data(row,col+1) is != key
                        //(image value not yet assigned, use data)
                        if (j + 1 == ncols || data[pos + 1] != key) {
                            edges[pos] |= rEdge;
                        }

                        //top edge: when last row
                        //or data(row+1,col) is != key
                        if (i + 1 == nrows || data[pos + ncols] != key) {
                            edges[pos] |= tEdge;
                        }

                        //no inhertiance, new group
                    } else {
                        image[pos] = groups;
                        polygons.put(groups, new SimpleEntry<Integer, Set<Integer>>(pos, null));
                        groups++;

                        //left edge: (always true)
                        edges[pos] |= lEdge;

                        //bottom edge: (always true)
                        edges[pos] |= bEdge;

                        //right edge: when last column
                        //or data(row,col+1) is != key
                        //(image value not yet assigned, use data)
                        if (j + 1 == ncols || data[pos + 1] != key) {
                            edges[pos] |= rEdge;
                        }

                        //top edge: when last row
                        //or data(row+1,col) is != key
                        if (i + 1 == nrows || data[pos + ncols] != key) {
                            edges[pos] |= tEdge;
                        }

                        groupCount++;
                    }
                }
            }
        }
        System.out.println(String.format("\redge detection: 100%%, found %d polygons                ", groupCount));
    }

    static int edgesToPolyons(StringBuilder sb, int gid, int thisPolygon, byte[] edges, int[] image, int nrows, int ncols, double resx, double resy, double minx, double miny, HashMap<Integer, Entry<Integer, Set<Integer>>> polygons) {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

        //follow edges
        for (int key : polygons.keySet()) {
            if (polygons.get(key) == null) {
                continue;
            }
            
            if(sb.length() > 0 && sb.charAt(sb.length() -1) == ')') {
                sb.append(",");
            }

            sb.append("(");

            getLinearRing(sb,polygons.get(key).getKey(), null, edges, image, nrows, ncols, minx, miny, resx, resy, false);

            Set<Integer> holeStarts = polygons.get(key).getValue();
//            LinearRing[] holesArray = null;
            if (holeStarts != null) {
//                ArrayList<LinearRing> holes = new ArrayList<LinearRing>();
                while (holeStarts.size() > 0) {
                    sb.append(",");
                    //lEdge is the first test in getLinearRing, so it will pick up the interior cell correctly
                    getLinearRing(sb,holeStarts.iterator().next().intValue(), holeStarts, edges, image, nrows, ncols, minx, miny, resx, resy, true);
                }
//                if (holes.size() > 0) {
//                    holesArray = new LinearRing[holes.size()];
//                    holes.toArray(holesArray);
//                }
            }

            sb.append(")");

//            Polygon p = geometryFactory.createPolygon(lr, holesArray);

//            //"POLYGON " and replace ", " with ","
//            if(sb.length() > 0 && sb.charAt(sb.length() -1) == ')') {
//                sb.append(",");
//            }
//            sb.append(p.toText().substring(8).replace(", ",","));

            gid++;
        }
        return gid;
    }

     private static void getLinearRing(StringBuilder sb, int startPos, Set<Integer> holeStarts, byte[] edges, int[] image, int nrows, int ncols, double minx, double miny, double resx, double resy, boolean reverse) {
        ArrayList<double []> coords = new ArrayList<double[]>();

        byte startEdge = 0;
        boolean startCw = true;
        if ((edges[startPos] & lEdge) > 0) {
            startEdge = lEdge;
        } else if ((edges[startPos] & rEdge) > 0) {
            startEdge = rEdge;
        } else if ((edges[startPos] & tEdge) > 0) {
            startEdge = tEdge;
        } else if ((edges[startPos] & bEdge) > 0) {
            startEdge = bEdge;
        }
        if (holeStarts != null) {
            holeStarts.remove(startPos);
        }
        coords.add(createCoordinate(startPos, startEdge, ncols, nrows, minx, miny, resx, resy, true));

        Pos current = new Pos();
        current.cw = startCw;
        current.edge = startEdge;
        current.pos = startPos;
        moveNext(current, edges, image, nrows, ncols);
        if (holeStarts != null) {
            holeStarts.remove(current.pos);
        }

        while (current.pos != startPos || current.edge != startEdge || current.cw != startCw) {
            coords.add(createCoordinate(current.pos, current.edge, ncols, nrows, minx, miny, resx, resy, true));
            moveNext(current, edges, image, nrows, ncols);
            if (holeStarts != null) {
                holeStarts.remove(current.pos);
            }
        }

        coords.add(createCoordinate(current.pos, current.edge, ncols, nrows, minx, miny, resx, resy, true));

        sb.append("(");
//        Coordinate[] ca = new Coordinate[coords.size()];
        float plng,plat,tlng,tlat,nlng,nlat;
        if (!reverse) {
//            coords.toArray(ca);

            plng = tlng = nlng = (float) coords.get(0)[0];
            plat = tlat = nlat = (float)coords.get(0)[1];

            int count = 0;
            for(int i=0;i<coords.size();i++) {
                if(i+1 < coords.size()) {
                    nlng = (float)coords.get(i+1)[0];
                    nlat = (float)coords.get(i+1)[1];

                    //skip line midpoints                    
                    if(count == 0 || (!(plng == tlng && tlng == nlng)
                            && !(plat == tlat && tlat == nlat))) {
                        if(count > 0) sb.append(",");
                        sb.append(tlng).append(" ").append(tlat);
                        count++;
                    }
                } else {
                    if(count > 0) sb.append(",");
                    sb.append(tlng).append(" ").append(tlat);
                    count++;
                }

                plng = tlng;
                tlng = nlng;
                plat = tlat;
                tlat = nlat;
            }
        } else {

            plng = tlng = nlng = (float) coords.get(coords.size()-1)[0];
            plat = tlat = nlat = (float)coords.get(coords.size()-1)[1];
            int count = 0;
            for(int i=coords.size()- 1;i>=0;i--) {                
                if(i > 0) {
                    nlng = (float)coords.get(i-1)[0];
                    nlat = (float)coords.get(i-1)[1];

                    //skip line midpoints
                    
                    if(count == 0 || (!(plng == tlng && tlng == nlng)
                            && !(plat == tlat && tlat == nlat))) {
                        if(count > 0) sb.append(",");
                        sb.append(tlng).append(" ").append(tlat);
                        count++;
                    }
                } else {
                    if(count > 0) sb.append(",");
                    sb.append(tlng).append(" ").append(tlat);
                    count++;
                }

                plng = tlng;
                tlng = nlng;
                plat = tlat;
                tlat = nlat;
            }
        }
        sb.append(")");

//        LinearRing lr = new LinearRing(ca, null, 4326);

//        return lr;
    }

      static double [] createCoordinate(int pos, byte edge, int ncols, int nrows, double minx, double miny, double resx, double resy, boolean cw) {
        if (!cw) {
            if (edge == lEdge) {
                edge = tEdge;
            }
            if (edge == tEdge) {
                edge = rEdge;
            }
            if (edge == rEdge) {
                edge = bEdge;
            }
            if (edge == bEdge) {
                edge = lEdge;
            }
        }
        double x = minx + getX(pos, ncols) * resx;
        double y = miny + (nrows - getY(pos, ncols) + 1) * resy;
        switch (edge) {
            case lEdge:
                break;
            case rEdge:
                y -= resy;
                x += resx;
                break;
            case tEdge:
                y -= resy;
                break;
            case bEdge:
                x += resx;
                break;
        }
        return new double[] {x, y};
    }

    static private void moveNext(Pos current, byte[] edges, int[] image, int nrows, int ncols) {
        int thisImage = image[current.pos];
        int x = getX(current.pos, ncols);
        int y = getY(current.pos, ncols);
        int checkPos;

        if (current.cw) {
            switch (current.edge) {
                case lEdge:
                    if ((edges[current.pos] & tEdge) > 0) {
                        current.edge = tEdge;
                        break;
                    }
                    if (y + 1 < nrows) {
                        checkPos = current.pos + ncols;
                        if (image[checkPos] == thisImage
                                && (edges[checkPos] & lEdge) > 0) {
                            current.pos = checkPos;
                            break;
                        }
                    }
                    if (y + 1 < nrows && x > 0) {
                        checkPos = current.pos + ncols - 1;
                        if (image[checkPos] == thisImage
                                && (edges[checkPos] & bEdge) > 0) {
                            current.edge = bEdge;
                            current.pos = checkPos;
                            break;
                        }
                    }
                    System.out.println("path not coded");
                    break;
                case rEdge:
                    if ((edges[current.pos] & bEdge) > 0) {
                        current.edge = bEdge;
                        break;
                    }
                    if (y > 0) {
                        checkPos = current.pos - ncols;
                        if (image[checkPos] == thisImage
                                && (edges[checkPos] & rEdge) > 0) {
                            current.pos = checkPos;
                            break;
                        }
                    }
                    if (y > 0 && x + 1 < ncols) {
                        checkPos = current.pos - ncols + 1;
                        if (image[checkPos] == thisImage
                                && (edges[checkPos] & tEdge) > 0) {
                            current.edge = tEdge;
                            current.pos = checkPos;
                            break;
                        }
                    }
                    System.out.println("path not coded");
                    break;
                case tEdge:
                    if ((edges[current.pos] & rEdge) > 0) {
                        current.edge = rEdge;
                        break;
                    }
                    if (x + 1 < ncols) {
                        checkPos = current.pos + 1;
                        if (image[checkPos] == thisImage
                                && (edges[checkPos] & tEdge) > 0) {
                            current.pos = checkPos;
                            break;
                        }
                    }
                    if (y + 1 < nrows && x + 1 < ncols) {
                        checkPos = current.pos + ncols + 1;
                        if (image[checkPos] == thisImage
                                && (edges[checkPos] & lEdge) > 0) {
                            current.edge = lEdge;
                            current.pos = checkPos;
                            break;
                        }
                    }
                    System.out.println("path not coded");
                    break;
                case bEdge:
                    if ((edges[current.pos] & lEdge) > 0) {
                        current.edge = lEdge;
                        break;
                    }
                    if (x > 0) {
                        checkPos = current.pos - 1;
                        if (image[checkPos] == thisImage
                                && (edges[checkPos] & bEdge) > 0) {
                            current.pos = checkPos;
                            break;
                        }
                    }
                    if (y > 0 && x > 0) {
                        checkPos = current.pos - ncols - 1;
                        if (image[checkPos] == thisImage
                                && (edges[checkPos] & rEdge) > 0) {
                            current.edge = rEdge;
                            current.pos = checkPos;
                            break;
                        }
                    }
                    System.out.println("path not coded");
                    break;
            }
        } else { //!currentCw
            System.out.println("path not coded");
        }
    }
}

class Pos {

    int pos;
    byte edge;
    boolean cw;

    public Pos() {
    }
}

