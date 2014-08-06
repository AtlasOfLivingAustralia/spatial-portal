/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.UserData;
import au.org.ala.spatial.util.Zipper;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
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
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.xml.sax.helpers.NamespaceSupport;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zul.*;
import org.zkoss.zul.Button;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author ajay
 */
public class AreaUploadShapefileWizardController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(AreaUploadShapefileWizardController.class);
    Imagemap img;
    Media media;
    Listbox lAttributes;
    Button btnRefresh;
    Button btnNext;
    SimpleFeatureSource source;
    SimpleFeatureCollection features;
    String file;
    String layername;

    @Override
    public void afterCompose() {
        super.afterCompose();

        Map args = Executions.getCurrent().getArg();

        layername = (String) args.get("layername");

        media = (Media) args.get("media");
        logger.debug("Got media in wizard");
        logger.debug("m.getName(): " + media.getName());
        logger.debug("getContentType: " + media.getContentType());
        logger.debug("getFormat: " + media.getFormat());

        logger.debug("Layer name: " + layername);

        processMedia();

        img.addEventListener("onClick", new EventListener() {

            public void onEvent(Event event) throws Exception {
                //imageClicked(event);
            }
        });
    }

    private void processMedia() {
        Map input = Zipper.unzipFile(media.getName(), media.getStreamData(), CommonData.settings.getProperty("analysis_output_dir") + "layers/");
        String type = "";
        //String file = "";
        if (input.containsKey("type")) {
            type = (String) input.get("type");
        }
        if (input.containsKey("file")) {
            file = (String) input.get("file");
        }
        if (type.equalsIgnoreCase("shp")) {
            loadShape(file);
        } else {
            logger.debug("Unknown file type. ");
            getMapComposer().showMessage(CommonData.lang("error_unknown_file_type"));
        }
    }

    private void executeShapeImageRenderer(Filter filter) {
        try {
            logger.debug("Generating image");

            SimpleFeatureCollection features1;

            if(filter == null) {
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

            Rectangle imageBounds = null;
            ReferencedEnvelope mapBounds = null;
            try {
                mapBounds = map.getMaxBounds();
                double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
                if (heightToWidth * imageWidth > imageHeight) {
                    imageBounds = new Rectangle(
                            0, 0, (int)Math.round(imageHeight / heightToWidth), imageHeight);
                } else {
                    imageBounds = new Rectangle(
                            0, 0, imageWidth, (int) Math.round(imageWidth * heightToWidth));
                }
            } catch (Exception e) {
                // failed to access map layers
                throw new RuntimeException(e);
            }

            BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);

            Graphics2D gr = image.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fill(imageBounds);


            renderer.paint(gr, imageBounds, mapBounds);
            img.setContent(image);

        } catch (Exception e) {
            logger.debug("Unable to generate image for selected shapefile", e);
        }
    }

    private void loadShape(String filename) {

        //final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        String userFilter = "none";
        try {
            FileDataStore store = FileDataStoreFinder.getDataStore(new File(filename));
            source = store.getFeatureSource();

            features = source.getFeatures();

            Listhead lhd = new Listhead();
            SimpleFeatureType schema = features.getSchema();
            Listheader lh = new Listheader("id");
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
                Listcell lc = null;
                String value = "";

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
                //li.setValue(value);
                li.setParent(lAttributes);
            }

            // loadFeatures
            // check if only a single feature,
            // if so, then select it and map it automatically
            logger.debug("features.size(): " + features.size());
            if (features.size() > 1) {
                executeShapeImageRenderer(null);
            } else {
                logger.debug("only a single feature, bypassing wizard...");
                fi = features.features();

                Set<FeatureId> IDs = new HashSet<FeatureId>();
                IDs.add(fi.next().getIdentifier());
                loadOnMap(IDs);

                this.detach();
            }

        } catch (IOException e) {
            logger.debug("IO Exception ", e);
        } catch (Exception e) {
            logger.debug("Generic exception", e);

        }
    }

    public void onClick$btnSelectAll(Event event) {
        lAttributes.selectAll();
    }

    public void onClick$btnDeselectAll(Event event) {
        lAttributes.clearSelection();
    }

    public void onClick$btnRefresh(Event event) {
        String column = "fid";
        String filter = "";

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
            final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            Iterator<Listitem> it = lAttributes.getSelectedItems().iterator();
            Set<FeatureId> IDs = new HashSet<FeatureId>();
            while (it.hasNext()) {
                Listitem li = it.next();
                IDs.add((FeatureId) li.getValue());
            }

            loadOnMap(IDs);
            this.detach();
        } catch (Exception ex) {
            logger.error("Error iterating thru' features", ex);
        }
    }

    private void loadOnMap(Set<FeatureId> IDs) {
        try {
            String column = "fid";
            final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            Filter filter = ff.id(IDs);

            // set up the math transform used to process the data
            SimpleFeatureType schema = features.getSchema();
            CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
            CoordinateReferenceSystem wgsCRS = DefaultGeographicCRS.WGS84;
            boolean lenient = true; // allow for some error due to different datums
            MathTransform transform = CRS.findMathTransform(dataCRS, wgsCRS, lenient);

            SimpleFeatureCollection sff = source.getFeatures(filter);
            SimpleFeatureIterator fif = sff.features();
            StringBuilder sb = new StringBuilder();
            StringBuilder sbGeometryCollection = new StringBuilder();
            boolean isGeometryCollection = false;
            ArrayList<Geometry> geoms = new ArrayList<Geometry>();
            while (fif.hasNext()) {
                SimpleFeature f = fif.next();

                logger.debug("Selected Feature: " + f.getID() + " -> " + f.getAttribute("ECOREGION"));

                //geoms.add((Geometry) f.getDefaultGeometry());
                Geometry geom = (Geometry) f.getDefaultGeometry();
                geom = JTS.transform(geom, transform);
                String wktString = geom.toText();
                wktString = wktString.replaceAll(", ", ",");
                boolean valid = true;
                boolean multipolygon = false;
                boolean polygon = false;
                boolean geometrycollection = false;
                if (wktString.startsWith("MULTIPOLYGON ")) {
                    wktString = wktString.substring("MULTIPOLYGON (".length(), wktString.length() - 1);
                    multipolygon = true;
                } else if (wktString.startsWith("POLYGON ")) {
                    wktString = wktString.substring("POLYGON ".length());
                    polygon = true;
                } else if (wktString.startsWith("GEOMETRYCOLLECTION (")) {
                    wktString = wktString.substring("GEOMETRYCOLLECTION (".length(), wktString.length() - 1);
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
                        sbGeometryCollection.append("MULTIPOLYGON(").append(wktString).append(")");
                    } else if (polygon) {
                        sbGeometryCollection.append("POLYGON").append(wktString);
                    } else if (geometrycollection) {
                        sbGeometryCollection.append(wktString);
                    }
                }
            }
            String wkt = "";
            if (!isGeometryCollection) {
                sb.append(")");
                wkt = "MULTIPOLYGON(" + sb.toString();
            } else {
                sbGeometryCollection.append(")");
                wkt = "GEOMETRYCOLLECTION(" + sbGeometryCollection.toString();
                getMapComposer().showMessage("Shape is invalid: " + "GEOMETRYCOLLECTION not supported.");
            }

            GeometryFactory gf = new GeometryFactory();
            GeometryCollection gcol = gf.createGeometryCollection(GeometryFactory.toGeometryArray(geoms));

            String msg = "";
            boolean invalid = false;
            try {
                WKTReader wktReader = new WKTReader();
                com.vividsolutions.jts.geom.Geometry g = wktReader.read(wkt);
                //NC 20130319: Ensure that the WKT is valid according to the WKT standards.
                //logger.debug("GEOMETRY TYPE: " + g.getGeometryType());
                IsValidOp op = new IsValidOp(g);
                if (!op.isValid()) {
                    invalid = true;
                    logger.warn(CommonData.lang("error_wkt_invalid") + " " + op.getValidationError().getMessage());
                    msg = op.getValidationError().getMessage();
                    //TODO Fix invalid WKT text using https://github.com/tudelft-gist/prepair maybe???
                } else if (g.isRectangle()) {
                    //NC 20130319: When the shape is a rectangle ensure that the points a specified in the correct order.
                    //get the new WKT for the rectangle will possibly need to change the order.

                    com.vividsolutions.jts.geom.Envelope envelope = g.getEnvelopeInternal();
                    String wkt2 = "POLYGON(("
                            + envelope.getMinX() + " " + envelope.getMinY() + ","
                            + envelope.getMaxX() + " " + envelope.getMinY() + ","
                            + envelope.getMaxX() + " " + envelope.getMaxY() + ","
                            + envelope.getMinX() + " " + envelope.getMaxY() + ","
                            + envelope.getMinX() + " " + envelope.getMinY() + "))";
                    if (!wkt.equals(wkt2)) {
                        logger.debug("NEW WKT for Rectangle: " + wkt);
                        msg = CommonData.lang("error_wkt_anticlockwise");
                        invalid = true;
                    }
                }
                if (!invalid) {
                    invalid = !op.isValid();
                }
            } catch (ParseException parseException) {
                logger.error("error testing validity of uploaded shape file wkt", parseException);
            }

            if (invalid) {
                getMapComposer().showMessage(CommonData.lang("error_wkt_invalid") + " " + msg);
            } else {

                MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, layername, layername);
                UserData ud = new UserData(layername);
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
            logger.debug("IO Error retrieving geometry", e);
        } catch (Exception e) {
            logger.debug("Generic Error retrieving geometry", e);
        }

    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void imageClicked(Event event) {
        try {
            logger.debug("*************************");
            logger.debug("Image clicked.");
            logger.debug(event.getClass().getCanonicalName());
            logger.debug(event.getData());
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                logger.debug(me.getX() + ", " + me.getY());
            }
            logger.debug("*************************");
            String wkt4326 = "GEOGCS[" + "\"WGS 84\"," + "  DATUM[" + "    \"WGS_1984\"," + "    SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]]," + "    TOWGS84[0,0,0,0,0,0,0]," + "    AUTHORITY[\"EPSG\",\"6326\"]]," + "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," + "  UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]]," + "  AXIS[\"Lat\",NORTH]," + "  AXIS[\"Long\",EAST]," + "  AUTHORITY[\"EPSG\",\"4326\"]]";
            CoordinateReferenceSystem wgsCRS = CRS.parseWKT(wkt4326);

            NamespaceSupport ns = new NamespaceSupport();
            RendererUtilities.worldToScreenTransform(null, null, wgsCRS);
        } catch (Exception e) {
            logger.error("error after clicking on image", e);
        }
    }
}
