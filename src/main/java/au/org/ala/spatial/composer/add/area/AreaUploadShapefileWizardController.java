/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.UserDataDTO;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Zipper;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import org.apache.log4j.Logger;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author ajay
 */
public class AreaUploadShapefileWizardController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaUploadShapefileWizardController.class);
    private Imagemap img;
    private Media media;
    private Listbox lAttributes;
    private Button btnRefresh;
    private Button btnNext;
    private SimpleFeatureSource source;
    private SimpleFeatureCollection features;
    private String file;
    private String layername;

    @Override
    public void afterCompose() {
        super.afterCompose();

        Map args = Executions.getCurrent().getArg();

        layername = (String) args.get(StringConstants.LAYERNAME);

        media = (Media) args.get(StringConstants.MEDIA);
        LOGGER.debug("Got media in wizard");
        LOGGER.debug("m.getName(): " + media.getName());
        LOGGER.debug("getContentType: " + media.getContentType());
        LOGGER.debug("getFormat: " + media.getFormat());

        LOGGER.debug("Layer name: " + layername);

        processMedia();
    }

    private void processMedia() {
        Map input = Zipper.unzipFile(media.getName(), media.getStreamData(), CommonData.getSettings().getProperty(StringConstants.ANALYSIS_OUTPUT_DIR) + "layers/");
        String type = "";
        if (input.containsKey(StringConstants.TYPE)) {
            type = (String) input.get(StringConstants.TYPE);
        }
        if (input.containsKey(StringConstants.FILE)) {
            file = (String) input.get(StringConstants.FILE);
        }
        if ("shp".equalsIgnoreCase(type)) {
            loadShape(file);
        } else {
            LOGGER.debug("Unknown file type. ");
            getMapComposer().showMessage(CommonData.lang("error_unknown_file_type"));
        }
    }

    private void executeShapeImageRenderer(Filter filter) {
        try {
            LOGGER.debug("Generating image");

            SimpleFeatureCollection features1;

            if (filter == null) {
                features1 = source.getFeatures();
            } else {
                features1 = source.getFeatures(filter);
            }

            // Create a map content and add our shapefile to it
            MapContent map = new MapContent();

            org.geotools.styling.Style style = SLD.createSimpleStyle(source.getSchema());
            Layer layer = new FeatureLayer(features1, style);
            map.addLayer(layer);

            GTRenderer renderer = new StreamingRenderer();
            renderer.setMapContent(map);

            int imageWidth = 800;
            int imageHeight = 300;

            Rectangle imageBounds;
            ReferencedEnvelope mapBounds;
            mapBounds = map.getMaxBounds();
            double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
            if (heightToWidth * imageWidth > imageHeight) {
                imageBounds = new Rectangle(
                        0, 0, (int) Math.round(imageHeight / heightToWidth), imageHeight);
            } else {
                imageBounds = new Rectangle(
                        0, 0, imageWidth, (int) Math.round(imageWidth * heightToWidth));
            }

            BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);

            Graphics2D gr = image.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fill(imageBounds);


            renderer.paint(gr, imageBounds, mapBounds);
            img.setContent(image);

        } catch (Exception e) {
            LOGGER.debug("Unable to generate image for selected shapefile", e);
        }
    }

    private void loadShape(String filename) {

        CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        try {
            FileDataStore store = FileDataStoreFinder.getDataStore(new File(filename));
            source = store.getFeatureSource();

            features = source.getFeatures();

            Listhead lhd = new Listhead();
            SimpleFeatureType schema = features.getSchema();
            Listheader lh = new Listheader(StringConstants.ID);
            lh.setParent(lhd);
            for (AttributeType at : schema.getTypes()) {
                if (schema.getDescriptor(at.getName()) == null) {
                    continue;
                }
                lh = new Listheader(at.getName().toString());
                lh.setParent(lhd);
            }
            lhd.setParent(lAttributes);

            SimpleFeatureIterator fi = features.features();
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                Listitem li = new Listitem();
                Listcell lc;
                String value;

                //add identifier
                lc = new Listcell(f.getIdentifier().getID());
                lc.setParent(li);

                for (AttributeType at : schema.getTypes()) {
                    if (schema.getDescriptor(at.getName()) == null) {
                        continue;
                    }
                    Object obj = f.getAttribute(at.getName());
                    if (obj == null) {
                        value = f.getID();
                    } else {
                        value = String.valueOf(obj);
                    }
                    lc = new Listcell(value);

                    lc.setParent(li);
                }
                li.setValue(f.getIdentifier());
                li.setParent(lAttributes);
            }

            // loadFeatures
            // check if only a single feature,
            // if so, then select it and map it automatically
            LOGGER.debug("features.size(): " + features.size());
            if (features.size() > 1) {
                executeShapeImageRenderer(null);
            } else {
                LOGGER.debug("only a single feature, bypassing wizard...");
                fi = features.features();

                Set<FeatureId> ids = new HashSet<FeatureId>();
                ids.add(fi.next().getIdentifier());
                loadOnMap(ids);

                this.detach();
            }

            try {
                fi.close();
            } catch (Exception e) {
            }

        } catch (IOException e) {
            LOGGER.debug("IO Exception ", e);
        } catch (Exception e) {
            LOGGER.debug("Generic exception", e);

        }
    }

    public void onClick$btnSelectAll(Event event) {
        lAttributes.selectAll();
    }

    public void onClick$btnDeselectAll(Event event) {
        lAttributes.clearSelection();
    }

    public void onClick$btnRefresh(Event event) {

        Iterator<Listitem> it = lAttributes.getSelectedItems().iterator();

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

        Set<FeatureId> fids = new HashSet<FeatureId>();

        while (it.hasNext()) {
            Listitem li = it.next();
            fids.add(ff.featureId(li.getValue().toString()));
        }

        executeShapeImageRenderer(ff.id(fids));
    }

    public void onClick$btnNext(Event event) {
        try {
            CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            Iterator<Listitem> it = lAttributes.getSelectedItems().iterator();
            Set<FeatureId> ids = new HashSet<FeatureId>();
            while (it.hasNext()) {
                Listitem li = it.next();
                ids.add((FeatureId) li.getValue());
            }

            loadOnMap(ids);
            this.detach();
        } catch (Exception ex) {
            LOGGER.error("Error iterating thru' features", ex);
        }
    }

    private void loadOnMap(Set<FeatureId> ids) {
        try {
            final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            Filter filter = ff.id(ids);

            // set up the math transform used to process the data
            SimpleFeatureType schema = features.getSchema();
            CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
            CoordinateReferenceSystem wgsCRS = DefaultGeographicCRS.WGS84;
            // allow for some error due to different datums
            boolean lenient = true;
            if (dataCRS == null) {
                dataCRS = DefaultGeographicCRS.WGS84;
            }
            MathTransform transform = CRS.findMathTransform(dataCRS, wgsCRS, lenient);

            SimpleFeatureCollection sff = source.getFeatures(filter);
            SimpleFeatureIterator fif = sff.features();
            StringBuilder sb = new StringBuilder();
            StringBuilder sbGeometryCollection = new StringBuilder();
            boolean isGeometryCollection = false;
            List<Geometry> geoms = new ArrayList<Geometry>();
            while (fif.hasNext()) {
                SimpleFeature f = fif.next();

                LOGGER.debug("Selected Feature: " + f.getID() + " -> " + f.getAttribute("ECOREGION"));


                Geometry geom = (Geometry) f.getDefaultGeometry();
                geom = JTS.transform(geom, transform);
                String wktString = geom.toText();
                wktString = wktString.replaceAll(", ", ",");
                boolean valid = true;
                boolean multipolygon = false;
                boolean polygon = false;
                boolean geometrycollection = false;
                if (wktString.startsWith(StringConstants.MULTIPOLYGON + " ")) {
                    wktString = wktString.substring((StringConstants.MULTIPOLYGON + " ").length(), wktString.length() - 1);
                    multipolygon = true;
                } else if (wktString.startsWith(StringConstants.POLYGON + " ")) {
                    wktString = wktString.substring((StringConstants.POLYGON + " ").length());
                    polygon = true;
                } else if (wktString.startsWith(StringConstants.GEOMETRYCOLLECTION + " (")) {
                    wktString = wktString.substring((StringConstants.GEOMETRYCOLLECTION + " (").length(), wktString.length() - 1);
                    geometrycollection = true;
                    isGeometryCollection = true;
                } else {
                    valid = false;
                }
                if (valid) {
                    if (sb.length() > 0) {
                        sb.append(",");
                        sbGeometryCollection.append(",");
                    }
                    sb.append(wktString);

                    if (multipolygon) {
                        sbGeometryCollection.append(StringConstants.MULTIPOLYGON).append("(").append(wktString.replace("(((", "(("));
                        if (!wktString.endsWith(")))")) {
                            sbGeometryCollection.append(")");
                        }
                    } else if (polygon) {
                        sbGeometryCollection.append(StringConstants.POLYGON).append(wktString);
                    } else if (geometrycollection) {
                        sbGeometryCollection.append(wktString);
                    }
                }
            }
            String wkt;
            if (!isGeometryCollection) {
                if (!sb.toString().contains(")))")) {
                    sb.append(")");
                }

                wkt = StringConstants.MULTIPOLYGON + "(" + sb.toString().replace("(((", "((");
            } else {
                sbGeometryCollection.append(")");
                wkt = StringConstants.GEOMETRYCOLLECTION + "(" + sbGeometryCollection.toString();
                getMapComposer().showMessage("Shape is invalid: " + "GEOMETRYCOLLECTION not supported.");
            }

            GeometryFactory gf = new GeometryFactory();
            gf.createGeometryCollection(GeometryFactory.toGeometryArray(geoms));

            String msg = "";
            boolean invalid = false;
            try {
                WKTReader wktReader = new WKTReader();
                com.vividsolutions.jts.geom.Geometry g = wktReader.read(wkt);
                //NC 20130319: Ensure that the WKT is valid according to the WKT standards.
                IsValidOp op = new IsValidOp(g);
                if (!op.isValid()) {
                    //this will fix some issues
                    g = g.buffer(0);
                    op = new IsValidOp(g);
                }
                if (!op.isValid()) {
                    invalid = true;
                    LOGGER.warn(CommonData.lang(StringConstants.ERROR_WKT_INVALID) + " " + op.getValidationError().getMessage());
                    msg = op.getValidationError().getMessage();
                    //TODO Fix invalid WKT text using https://github.com/tudelft-gist/prepair maybe???
                } else if (g.isRectangle()) {
                    //NC 20130319: When the shape is a rectangle ensure that the points a specified in the correct order.
                    //get the new WKT for the rectangle will possibly need to change the order.

                    com.vividsolutions.jts.geom.Envelope envelope = g.getEnvelopeInternal();
                    String wkt2 = StringConstants.POLYGON + "(("
                            + envelope.getMinX() + " " + envelope.getMinY() + ","
                            + envelope.getMaxX() + " " + envelope.getMinY() + ","
                            + envelope.getMaxX() + " " + envelope.getMaxY() + ","
                            + envelope.getMinX() + " " + envelope.getMaxY() + ","
                            + envelope.getMinX() + " " + envelope.getMinY() + "))";
                    if (!wkt.equals(wkt2)) {
                        LOGGER.debug("NEW WKT for Rectangle: " + wkt);
                        msg = CommonData.lang("error_wkt_anticlockwise");
                        invalid = true;
                    }
                }
                if (!invalid) {
                    invalid = !op.isValid();
                }
            } catch (ParseException parseException) {
                LOGGER.error("error testing validity of uploaded shape file wkt", parseException);
            }

            if (invalid) {
                getMapComposer().showMessage(CommonData.lang(StringConstants.ERROR_WKT_INVALID) + " " + msg);
            } else {

                MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, layername, layername);
                UserDataDTO ud = new UserDataDTO(layername);
                ud.setFilename(media.getName());

                ud.setUploadedTimeInMs(System.currentTimeMillis());
                ud.setType("shapefile");

                String metadata = "";
                metadata += "User uploaded Shapefile \n";
                metadata += "Name: " + ud.getName() + " <br />\n";
                metadata += "Filename: " + ud.getFilename() + " <br />\n";
                metadata += "Date: " + ud.getDisplayTime() + " <br />\n";
                metadata += "Selected polygons (fid): <br />\n";
                metadata += "<ul>";
                metadata += "</ul>";

                mapLayer.getMapLayerMetadata().setMoreInfo(metadata);

                getMapComposer().replaceWKTwithWMS(mapLayer);
            }

        } catch (IOException e) {
            LOGGER.debug("IO Error retrieving geometry", e);
        } catch (Exception e) {
            LOGGER.debug("Generic Error retrieving geometry", e);
        }

    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
