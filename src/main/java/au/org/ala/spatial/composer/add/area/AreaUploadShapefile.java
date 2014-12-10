package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.UserDataDTO;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.ShapefileUtils;
import au.org.ala.spatial.util.Zipper;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import org.apache.log4j.Logger;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.SAXException;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adam
 */
public class AreaUploadShapefile extends AreaToolComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaUploadShapefile.class);
    private Button fileUpload;
    private Textbox txtLayerName;

    private static String getKMLPolygonAsWKT(String kmldata) {
        try {
            Parser parser = new Parser(new org.geotools.kml.v22.KMLConfiguration());
            SimpleFeature f = (SimpleFeature) parser.parse(new StringReader(kmldata));
            Collection placemarks = (Collection) f.getAttribute(StringConstants.FEATURE);

            Geometry g = null;
            SimpleFeature sf = null;

            //for <Placemark>
            if (!placemarks.isEmpty() && !placemarks.isEmpty()) {
                sf = (SimpleFeature) placemarks.iterator().next();
                g = (Geometry) sf.getAttribute(StringConstants.GEOMETRY);
            }

            //for <Folder><Placemark>
            if (g == null && sf != null) {
                placemarks = (Collection) sf.getAttribute(StringConstants.FEATURE);
                if (placemarks != null && !placemarks.isEmpty()) {
                    g = (Geometry) ((SimpleFeature) placemarks.iterator().next()).getAttribute(StringConstants.GEOMETRY);
                }
            }

            if (g != null) {
                WKTWriter wr = new WKTWriter();
                String wkt = wr.write(g);
                return wkt.replace(" (", "(").replace(", ", ",").replace(") ", ")");
            }
        } catch (SAXException e) {
            LOGGER.error("KML spec parse error", e);
        } catch (ParserConfigurationException e) {
            LOGGER.error("error converting KML to WKT", e);
        } catch (IOException e) {
            LOGGER.error("error reading KML", e);
        }

        return null;
    }

    @Override
    public void afterCompose() {
        super.afterCompose();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName(CommonData.lang(StringConstants.DEFAULT_AREA_LAYER_NAME)));
        fileUpload.addEventListener("onUpload", new EventListener() {

            public void onEvent(Event event) throws Exception {
                onClick$btnOk(event);
            }
        });
    }

    public void onClick$btnOk(Event event) {
        onUpload$btnFileUpload(event);
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onUpload$btnFileUpload(Event event) {

        UploadEvent ue = null;
        if ("onUpload".equals(event.getName())) {
            ue = (UploadEvent) event;
        } else if ("onForward".equals(event.getName())) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            LOGGER.debug("unable to upload file");
            return;
        } else {
            LOGGER.debug("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            LOGGER.debug("m.getName(): " + m.getName());
            LOGGER.debug("getContentType: " + m.getContentType());
            LOGGER.debug("getFormat: " + m.getFormat());

            UserDataDTO ud = new UserDataDTO(txtLayerName.getValue());
            ud.setFilename(m.getName());

            byte[] kmldata = getKml(m);
            if (kmldata.length > 0) {
                loadUserLayerKML(m.getName(), kmldata, ud);

            } else if (m.getName().toLowerCase().endsWith("zip")) {
                Map args = new HashMap();
                args.put(StringConstants.LAYERNAME, txtLayerName.getValue());
                args.put(StringConstants.MEDIA, m);

                String windowname = "areashapewizard";
                if (getFellowIfAny(windowname) != null) {
                    getFellowIfAny(windowname).detach();
                }

                Window window = (Window) Executions.createComponents("WEB-INF/zul/add/area/AreaUploadShapefileWizard.zul", this.getParent(), args);
                try {
                    window.doModal();
                } catch (SuspendNotAllowedException e) {
                    // we are really closing the window without opening/displaying to the user
                }
            } else if (m.getName().toLowerCase().endsWith("zip_removeme")) {

                Map input = Zipper.unzipFile(m.getName(), m.getStreamData(), "/data/ala/runtime/output/layers/");
                String type = "";
                String file = "";
                if (input.containsKey(StringConstants.TYPE)) {
                    type = (String) input.get(StringConstants.TYPE);
                }
                if (input.containsKey(StringConstants.FILE)) {
                    file = (String) input.get(StringConstants.FILE);
                }
                if ("shp".equalsIgnoreCase(type)) {
                    LOGGER.debug("Uploaded file is a shapefile. Loading...");
                    Map shape = ShapefileUtils.loadShapefile(new File(file));

                    if (shape != null) {
                        String wkt = (String) shape.get(StringConstants.WKT);
                        LOGGER.debug("Got shapefile wkt...validating");
                        String msg = "";
                        boolean invalid = false;
                        try {
                            WKTReader wktReader = new WKTReader();
                            com.vividsolutions.jts.geom.Geometry g = wktReader.read(wkt);
                            //NC 20130319: Ensure that the WKT is valid according to the WKT standards.
                            IsValidOp op = new IsValidOp(g);
                            if (!op.isValid()) {
                                invalid = true;
                                LOGGER.warn(CommonData.lang(StringConstants.ERROR_WKT_INVALID) + " " + op.getValidationError().getMessage());
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
                            ok = false;
                            getMapComposer().showMessage(CommonData.lang(StringConstants.ERROR_WKT_INVALID) + " " + msg);
                        } else {

                            layerName = getMapComposer().getNextAreaLayerName(txtLayerName.getValue());
                            MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, layerName, txtLayerName.getValue());

                            ud.setUploadedTimeInMs(System.currentTimeMillis());
                            ud.setType("shapefile");

                            String metadata = "";
                            metadata += "User uploaded Shapefile \n";
                            metadata += "Name: " + ud.getName() + " <br />\n";
                            metadata += "Filename: " + ud.getFilename() + " <br />\n";
                            metadata += "Date: " + ud.getDisplayTime() + " <br />\n";

                            mapLayer.getMapLayerMetadata().setMoreInfo(metadata);
                            ok = true;
                        }
                    }
                } else {
                    LOGGER.debug("Unknown file type. ");
                    getMapComposer().showMessage(CommonData.lang("error_unknown_file_type"));
                }
            } else {
                LOGGER.debug("Unknown file type. ");
                getMapComposer().showMessage(CommonData.lang("error_unknown_file_type"));
            }
        } catch (Exception ex) {
            getMapComposer().showMessage(CommonData.lang("error_upload_failed"));
            LOGGER.error("unable to load user area file: ", ex);
        }
    }

    private byte[] getKml(Media m) {
        try {
            String kmlData = "";

            if (m.inMemory()) {
                kmlData = new String(m.getByteData());
            } else if (m.isBinary()) {
                InputStream data = m.getStreamData();
                if (data != null) {
                    Writer writer = new StringWriter();

                    char[] buffer = new char[1024];
                    Reader reader = null;
                    try {
                        reader = new BufferedReader(
                                new InputStreamReader(data));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        reader.close();
                        data.close();
                    }
                    kmlData = writer.toString();
                }
            } else if ("txt".equals(m.getFormat())) {
                Writer writer = new StringWriter();
                char[] buffer = new char[1024];
                Reader reader = m.getReaderData();
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }

                kmlData = writer.toString();
            }

            if (kmlData.contains("xml") && kmlData.contains("Document")) {
                return kmlData.getBytes();
            } else {
                return new byte[0];
            }
        } catch (Exception e) {
            LOGGER.error("Exception checking if kml file", e);
        }
        return new byte[0];
    }

    public void loadUserLayerKML(String name, byte[] kmldata, UserDataDTO ud) {
        try {

            String id = String.valueOf(System.currentTimeMillis());
            String kmlpath = "/data/ala/runtime/output/layers/" + id + "/";
            File kmlfilepath = new File(kmlpath);
            kmlfilepath.mkdirs();
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(kmlfilepath.getAbsolutePath() + "/" + name)));
            String kmlstr = new String(kmldata);
            out.write(kmlstr);
            out.close();

            MapComposer mc = getMapComposer();
            layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
            String wkt = getKMLPolygonAsWKT(kmlstr);


            boolean invalid = false;
            String msg = "";
            try {
                WKTReader wktReader = new WKTReader();
                com.vividsolutions.jts.geom.Geometry g = wktReader.read(wkt);
                //NC 20130319: Ensure that the WKT is valid according to the WKT standards.
                IsValidOp op = new IsValidOp(g);
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
                LOGGER.error(CommonData.lang(StringConstants.ERROR_WKT_INVALID), parseException);
            }

            if (invalid) {
                ok = false;
                getMapComposer().showMessage(CommonData.lang(StringConstants.ERROR_WKT_INVALID) + " " + msg);
            } else {
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

                ud.setUploadedTimeInMs(Long.parseLong(id));
                ud.setType("kml");

                String metadata = "";
                metadata += "User uploaded KML area \n";
                metadata += "Name: " + ud.getName() + " <br />\n";
                metadata += "Filename: " + ud.getFilename() + " <br />\n";
                metadata += "Date: " + ud.getDisplayTime() + " <br />\n";

                mapLayer.getMapLayerMetadata().setMoreInfo(metadata);

                if (mapLayer == null) {
                    LOGGER.debug("The layer " + name + " couldnt be created");
                    mc.showMessage(mc.getLanguagePack().getLang("ext_layer_creation_failure"));
                } else {
                    ok = true;
                    mc.addUserDefinedLayerToMenu(mapLayer, true);
                }
            }
        } catch (IOException e) {

            getMapComposer().showMessage(CommonData.lang("error_upload_failed"));

            LOGGER.debug("unable to load user kml: ", e);
        }
    }
}
