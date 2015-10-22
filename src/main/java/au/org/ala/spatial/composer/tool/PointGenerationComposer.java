/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.composer.input.UploadSpeciesController;
import au.org.ala.spatial.dto.UserDataDTO;
import au.org.ala.spatial.sampling.Sampling;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.ShapefileUtils;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.SelectedArea;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.zkoss.zul.Doublebox;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class PointGenerationComposer extends ToolComposer {

    private static final Logger LOGGER = Logger.getLogger(PointGenerationComposer.class);

    private Doublebox resolution;

    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Point Generation";
        this.totalSteps = 2;

        this.loadAreaLayers();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("Points on a grid"));
    }

    @Override
    public boolean onFinish() {

        SelectedArea sa = getSelectedArea();
        double[][] bbox = null;
        if (sa.getMapLayer() != null && sa.getMapLayer().getMapLayerMetadata() != null) {
            List<Double> bb = sa.getMapLayer().getMapLayerMetadata().getBbox();
            bbox = new double[2][2];

            bbox[0][0] = bb.get(0);
            bbox[0][1] = bb.get(1);
            bbox[1][0] = bb.get(2);
            bbox[1][1] = bb.get(3);
        } else {
            List<Double> bb = Util.getBoundingBox(sa.getWkt());
            bbox = new double[][]{{bb.get(0), bb.get(1)}, {bb.get(2), bb.get(3)}};
        }

        //with bounding box, cut points
        try {
            String wkt = (sa.getMapLayer() != null ? sa.getMapLayer().getWKT() : sa.getWkt());

            StringBuilder sb = new StringBuilder();
            sb.append("longitude,latitude");
            int count = 0;

            int width = (int) Math.ceil((bbox[1][0] - bbox[0][0]) / resolution.getValue());
            int height = (int) Math.ceil((bbox[1][1] - bbox[0][1]) / resolution.getValue());
            double startx = Math.floor(bbox[0][0] / resolution.getValue()) * resolution.getValue();
            double starty = Math.floor(bbox[0][1] / resolution.getValue()) * resolution.getValue();

            if (wkt == null) {
                if (sa.getFacets() != null) {

                    double[][] points = new double[(width + 1) * (height + 1)][2];
                    int pos = 0;
                    for (int i = 0; i <= width; i++) {
                        for (int j = 0; j <= height; j++) {
                            double lng = startx + i * resolution.getValue();
                            double lat = starty + j * resolution.getValue();

                            points[pos][0] = lng;
                            points[pos][1] = lat;

                            pos = pos + 1;
                        }
                    }

                    List<String> fields = new ArrayList<String>();
                    for (Facet f : sa.getFacets()) {
                        fields.add(f.getFields()[0]);
                    }

                    List<String[]> result = Sampling.sampling(fields, points);

                    for (int i = 0; i < result.size(); i++) {
                        Facet f = sa.getFacets().get(i);
                        String[] intersection = result.get(i);
                        for (int j = 0; j < intersection.length; j++) {
                            if (f.isValid("\"" + intersection[j] + "\"")) {
                                sb.append("\n");
                                sb.append(points[j][0]).append(",").append(points[j][1]);
                                count++;
                            }
                        }
                    }
                } else {
                    LOGGER.error("invalid area selected for point generation");
                    getMapComposer().showMessage("Unsupported area selected for point generation. Choose a different area.");
                    return false;
                }
            } else {
                //write temporary shapefile
                File tmp = File.createTempFile("tmp", ".shp");
                ShapefileUtils.saveShapefile(tmp, wkt, "tmp");

                FileDataStore store = FileDataStoreFinder.getDataStore(tmp);
                FeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);
                FeatureCollection featureCollection = featureSource.getFeatures();

                GeometryFactory gf = new GeometryFactory();
                List<Geometry> geometry = new ArrayList<Geometry>();
                FeatureIterator it = featureCollection.features();
                while (it.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) it.next();
                    geometry.add((Geometry) feature.getDefaultGeometry());
                }
                try {
                    it.close();
                } catch (Exception e) {
                }

                for (int i = 0; i <= width; i++) {
                    for (int j = 0; j <= height; j++) {
                        double lng = startx + i * resolution.getValue();
                        double lat = starty + j * resolution.getValue();
                        Coordinate c = new Coordinate(lng, lat);
                        Geometry point = gf.createPoint(c);

                        for (Geometry geom : geometry) {
                            if (geom.contains(point)) {
                                sb.append("\n");
                                sb.append(lng).append(",").append(lat);
                                count++;
                            }
                        }
                    }
                }

                //close tmp file
                try {
                    store.dispose();
                } catch (Exception e) {
                }
                //delete tmp files
                try {
                    FileUtils.deleteQuietly(tmp);
                    FileUtils.deleteQuietly(new File(tmp.getPath().replace(".shp", ".shx")));
                    FileUtils.deleteQuietly(new File(tmp.getPath().replace(".shp", ".fix")));
                    FileUtils.deleteQuietly(new File(tmp.getPath().replace(".shp", ".dbf")));
                    FileUtils.deleteQuietly(new File(tmp.getPath().replace(".shp", ".prj")));
                } catch (Exception e) {
                }
            }

            if (count <= 0) {
                getMapComposer().showMessage("No points generated. Try again with a smaller resolution or larger area.");
            } else if (count > Integer.parseInt(CommonData.getSettings().getProperty("max_record_count_upload"))) {
                getMapComposer().showMessage(count + " points generated. Maximum is " + CommonData.getSettings().getProperty("generated_points_max") + ".\n"
                        + "Try again with a larger resolution or smaller area.");
            } else {
                String name = tToolName.getValue();
                try {
                    UploadSpeciesController.loadUserPoints(new UserDataDTO(name), new StringReader(sb.toString()), true, name
                            , "points in " + getSelectedAreaDisplayName() + " on " + resolution.getValue() + " degree resolution grid."
                            , getMapComposer(), null);

                    detach();

                    return true;
                } catch (Exception e) {
                    LOGGER.error("failed to upload points");
                }
            }
        } catch (Exception e) {

        }

        return false;
    }
}
