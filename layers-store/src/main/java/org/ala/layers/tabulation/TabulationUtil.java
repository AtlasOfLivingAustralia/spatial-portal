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

package org.ala.layers.tabulation;

import java.util.ArrayList;

/**
 *
 * @author Adam
 */
public class TabulationUtil {
    static public double calculateArea(String wkt) {
        double sumarea = 0;

        //GEOMETRYCOLLECTION
        String areaWorking = wkt;
        ArrayList<String> stringsList = new ArrayList<String>();
        if (areaWorking.startsWith("GEOMETRYCOLLECTION")) {
            //split out polygons and multipolygons
            areaWorking = areaWorking.replace(", ", ",");
            areaWorking = areaWorking.replace(") ", ")");
            areaWorking = areaWorking.replace(" )", ")");
            areaWorking = areaWorking.replace(" (", "(");
            areaWorking = areaWorking.replace("( ", "(");

            int posStart, posEnd, p1, p2;
            p1 = areaWorking.indexOf("POLYGON", 0);
            p2 = areaWorking.indexOf("MULTIPOLYGON", 0);
            if (p1 < 0) {
                posStart = p2;
            } else if (p2 < 0) {
                posStart = p1;
            } else {
                posStart = Math.min(p1, p2);
            }
            String endString = null;
            if (posStart == p1) {
                endString = "))";
            } else {
                endString = ")))";
            }
            posEnd = areaWorking.indexOf(endString, posStart);
            while (posStart > 0 && posEnd > 0) {
                //split multipolygons
                if (endString.length() == 3) {
                    for (String s : areaWorking.substring(posStart, posEnd - 1).split("\\)\\),\\(\\(")) {
                        stringsList.add(s);
                    }
                } else {
                    stringsList.add(areaWorking.substring(posStart, posEnd - 1));
                }

                posStart = posEnd;
                p1 = areaWorking.indexOf("POLYGON", posStart);
                p2 = areaWorking.indexOf("MULTIPOLYGON", posStart);
                if (p1 < 0) {
                    posStart = p2;
                } else if (p2 < 0) {
                    posStart = p1;
                } else {
                    posStart = Math.min(p1, p2);
                }
                if (posStart == p1) {
                    endString = "))";
                } else {
                    endString = ")))";
                }
                posEnd = areaWorking.indexOf(endString, posStart);
            }
            if (posStart >= 0) {
                stringsList.add(areaWorking.substring(posStart, areaWorking.length()));
            }
        } else if (areaWorking.startsWith("MULTIPOLYGON")) {
            //split
            for (String s : areaWorking.split("\\)\\),\\(\\(")) {
                stringsList.add(s);
            }
        } else if (areaWorking.startsWith("POLYGON")) {
            stringsList.add(areaWorking);
        }

        for (String w : stringsList) {
            if (w.contains("ENVELOPE")) {
                continue;
            }

            String[] areas = w.split("\\),\\(");
            double shapearea = 0;

            for (String area : areas) {
                area = area.replace("MULTIPOLYGON", "");
                area = area.replace("POLYGON", "");
                area = area.replace(")", "");
                area = area.replace("(", "");

                String[] areaarr = area.split(",");

                // check if it's the 'world' bbox
                boolean isWorld = true;
                for (int i = 0; i < areaarr.length - 1; i++) {
                    String[] darea = areaarr[i].split(" ");
                    if ((Double.parseDouble(darea[0]) < -174
                            && Double.parseDouble(darea[1]) < -84)
                            || (Double.parseDouble(darea[0]) < -174
                            && Double.parseDouble(darea[1]) > 84)
                            || (Double.parseDouble(darea[0]) > 174
                            && Double.parseDouble(darea[1]) > 84)
                            || (Double.parseDouble(darea[0]) > 174
                            && Double.parseDouble(darea[1]) < -84)) {
                        //return 510000000;
                    } else {
                        isWorld = false;
                    }
                }
                //if (isWorld) return (510000000 * 1000 * 1000 * 1L);
                if (isWorld) {
                    return 510000000000000L;
                }

                double totalarea = 0.0;
                String d = areaarr[0];
                for (int f = 1; f < areaarr.length - 2; ++f) {
                    totalarea += Mh(d, areaarr[f], areaarr[f + 1]);
                }

                shapearea += totalarea * 6378137 * 6378137;
            }

            sumarea += Math.abs(shapearea);
        }

        return sumarea;
    }

   static  private double Mh(String a, String b, String c) {
        return Nh(a, b, c) * hi(a, b, c);
    }

    static private double Nh(String a, String b, String c) {
        String[] poly = {a, b, c, a};
        double[] area = new double[3];
        int i = 0;
        double j = 0.0;
        for (i = 0; i < 3; ++i) {
            area[i] = vd(poly[i], poly[i + 1]);
            j += area[i];
        }
        j /= 2;
        double f = Math.tan(j / 2);
        for (i = 0; i < 3; ++i) {
            f *= Math.tan((j - area[i]) / 2);
        }
        return 4 * Math.atan(Math.sqrt(Math.abs(f)));
    }

    static private double hi(String a, String b, String c) {
        String[] d = {a, b, c};

        int i = 0;
        double[][] bb = new double[3][3];
        for (i = 0; i < 3; ++i) {
            String[] coords = d[i].split(" ");
            double lng = Double.parseDouble(coords[0]);
            double lat = Double.parseDouble(coords[1]);

            double y = Uc(lat);
            double x = Uc(lng);

            bb[i][0] = Math.cos(y) * Math.cos(x);
            bb[i][1] = Math.cos(y) * Math.sin(x);
            bb[i][2] = Math.sin(y);
        }

        return (bb[0][0] * bb[1][1] * bb[2][2] + bb[1][0] * bb[2][1] * bb[0][2] + bb[2][0] * bb[0][1] * bb[1][2] - bb[0][0] * bb[2][1] * bb[1][2] - bb[1][0] * bb[0][1] * bb[2][2] - bb[2][0] * bb[1][1] * bb[0][2] > 0) ? 1 : -1;
    }

   static  private double vd(String a, String b) {
        String[] coords1 = a.split(" ");
        double lng1 = Double.parseDouble(coords1[0]);
        double lat1 = Double.parseDouble(coords1[1]);

        String[] coords2 = b.split(" ");
        double lng2 = Double.parseDouble(coords2[0]);
        double lat2 = Double.parseDouble(coords2[1]);

        double c = Uc(lat1);
        double d = Uc(lat2);

        return 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((c - d) / 2), 2) + Math.cos(c) * Math.cos(d) * Math.pow(Math.sin((Uc(lng1) - Uc(lng2)) / 2), 2)));
    }

    static private double Uc(double a) {
        return a * (Math.PI / 180);
    }
}
