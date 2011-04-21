package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zul.Textbox;
import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilities;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.UserData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class UploadSpeciesController extends UtilityComposer {
    
    SettingsSupplementary settingsSupplementary;
    Textbox tbDesc;
    Textbox tbName;
    Fileupload fileUpload;
    private EventListener eventListener;

    @Override
    public void afterCompose() {
        super.afterCompose();

        tbName.setConstraint(new Constraint() {

            @Override
            public void validate(Component comp, Object value) throws WrongValueException {
                String val = (String) value;
                Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
                if (htUserSpecies != null) {
                    if (htUserSpecies.size() > 0) {
                        Enumeration e = htUserSpecies.keys();
                        while (e.hasMoreElements()) {
                            String k = (String) e.nextElement();
                            UserData ud = htUserSpecies.get(k);

                            if (ud.getName().toLowerCase().equals(val.trim().toLowerCase())) {
                                throw new WrongValueException(comp, "User dataset named '" + val + "' already exists. Please enter another name for your dataset.");
                            }
                        }
                    }
                }
            }
        });

        fileUpload.addEventListener("onUpload", new EventListener() {

            public void onEvent(Event event) throws Exception {
                UserData ud = new UserData(tbName.getValue(), tbDesc.getValue(), "points");
                doFileUpload(ud, event);
            }
        });
    }

    public void onClick$btnOk(Event event) {
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void processMedia(Media media) {
        try {
            Messagebox.show("Processing...", "Error",
                    Messagebox.OK, Messagebox.ERROR);
            System.out.println("Loading files");
            if (media != null) {
                if (media instanceof org.zkoss.util.media.AMedia) {
                    //lblFupload.setValue(media.getName());
                    System.out.println("Valid file successfully uploaded");
                } else {
                    Messagebox.show("Not a valid upload: " + media, "Error",
                            Messagebox.OK, Messagebox.ERROR);
                    System.out.println("not a valid file");
                }
            } else {
                Messagebox.show("No attachment", "Error",
                        Messagebox.OK, Messagebox.ERROR);
                System.out.println("No attachment");
            }
        } catch (Exception e) {
            System.out.println("Unable to process media: ");
            e.printStackTrace(System.out);
        }
    }

    public void onUpload$btnFileUpload(Event event) {
        doFileUpload("", event);
    }

    public void doFileUpload(String name, Event event) {
        doFileUpload(new UserData(name), event);
    }

    public void doFileUpload(UserData ud, Event event) {
        UploadEvent ue = null;
        if (event.getName().equals("onUpload")) {
            ue = (UploadEvent) event;
        } else if (event.getName().equals("onForward")) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            System.out.println("unable to upload file");
            return;
        } else {
            System.out.println("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();
            if (ud == null) {
                ud = new UserData(m.getName());
            }
            if (ud.getName().trim().equals("")) {
                ud.setName(m.getName());
            }
            ud.setFilename(m.getName());

            if (ud.getName() == null || ud.getName().length() == 0) {
                ud.setName(m.getName());
            }
            if (ud.getDescription() == null || ud.getDescription().length() == 0) {
                ud.setDescription(m.getName());
            }

            String name = ud.getName();

            System.out.println("Got file '" + ud.getName() + "' with type '" + m.getContentType() + "'");

            // check the content-type
            // TODO: check why LB is sending 'application/spc' mime-type. remove from future use.
            if (m.getContentType().equalsIgnoreCase("text/plain") || m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_CSV) || m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_CSV_EXCEL)) {
                loadUserPoints(ud, m.getReaderData());
            } else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_EXCEL) || m.getContentType().equalsIgnoreCase("application/spc")) {
                byte[] csvdata = m.getByteData();
                loadUserPoints(ud, new StringReader(new String(csvdata)));
            } else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_KML)) {
                if (m.inMemory()) {
                    loadUserLayerKML(name, m.getByteData());
                } else {
                    loadUserLayerKML(name, m.getStreamData());
                }

            } else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_ZIP)) {
                unzipFile(m.getName(), m.getStreamData());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadUserPoints(String name, Reader data) {
        loadUserPoints(new UserData(name), data);
    }

    public void loadUserPoints(UserData ud, Reader data) {  
        try {
            // Read a line in to check if it's a valid file
            // if it throw's an error, then it's not a valid csv file
            CSVReader reader = new CSVReader(data);

            List userPoints = reader.readAll();

            //if only one column treat it as a list of LSID's
            if (((String[]) userPoints.get(0)).length == 1) {
                continueLoadUserLSIDs(ud, data, reader, userPoints);
                return;
            }

            boolean hasHeader = false;

            // check if it has a header
            String[] upHeader = (String[]) userPoints.get(0);
            try {
                Double d1 = new Double(upHeader[1]);
                Double d2 = new Double(upHeader[2]);
            } catch (Exception e) {
                hasHeader = true;
            }

            System.out.println("hasHeader: " + hasHeader);

            // check if the count of points goes over the threshold.
            int sizeToCheck = (hasHeader) ? userPoints.size() - 1 : userPoints.size();
            System.out.println("Checking user points size: " + sizeToCheck + " -> " + settingsSupplementary.getValueAsInt("max_record_count_upload"));
            if (sizeToCheck > settingsSupplementary.getValueAsInt("max_record_count_upload")) {
                getMapComposer().showMessage(settingsSupplementary.getValue("max_record_count_upload_message"));
                return;
            }

            StringBuffer sbUIds = new StringBuffer();
            StringBuffer sbUPoints = new StringBuffer();
            int counter = 1;
            for (int i = 0; i < userPoints.size(); i++) {
                String[] up = (String[]) userPoints.get(i);
                if (up.length > 2) {
                    sbUIds.append(up[0] + "\n");
                    sbUPoints.append(up[1] + "," + up[2] + "\n");
                } else if (up.length > 1) {
                    sbUIds.append(counter + "\n");
                    sbUPoints.append(up[0] + "," + up[1] + "\n");
                    counter++;
                }
            }

            System.out.println("Loading points into alaspatial");
            System.out.println(sbUPoints.toString());

            // Post it to alaspatial app
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.satServer + "/alaspatial/ws/points/register"); // testurl
            post.addRequestHeader("Accept", "text/plain");
            post.addParameter("name", ud.getName());
            post.addParameter("points", sbUPoints.toString());
            post.addParameter("ids", sbUIds.toString());

            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();

            System.out.println("uploaded points name: " + ud.getName() + " lsid: " + slist);

            ud.setFeatureCount(userPoints.size());
            Long did = new Long(slist);
            System.out.println("lval: " + did.longValue());
            ud.setUploadedTimeInMs(did.longValue());

            String metadata = "";
            metadata += "User uploaded points \n";
            metadata += "Name: " + ud.getName() + " \n";
            metadata += "Description: " + ud.getDescription() + " \n";
            metadata += "Date: " + ud.getDisplayTime() + " \n";
            metadata += "Number of Points: " + ud.getFeatureCount() + " \n";

//            MapLayer ml = null;
//            if (ud.getFeatureCount() > settingsSupplementary.getValueAsInt(getMapComposer().POINTS_CLUSTER_THRESHOLD)) {
//                //ml = mapSpeciesByLsidCluster(slist, ud.getName(), "user");
//                ml = getMapComposer().mapSpeciesByLsidFilterGrid(slist, ud.getName(), "user", ud.getFeatureCount());
//            } else {
//                ml = getMapComposer().mapSpeciesByLsidFilter(slist, ud.getName(), "user", ud.getFeatureCount());
//            }
//            MapLayerMetadata md = ml.getMapLayerMetadata();
//            if (md == null) {
//                md = new MapLayerMetadata();
//                ml.setMapLayerMetadata(md);
//            }
//            md.setMoreInfo(metadata);
            //md.setSpeciesRank("User");


            // add it to the user session
            Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
            if (htUserSpecies == null) {
                htUserSpecies = new Hashtable<String, UserData>();
            }
            htUserSpecies.put(slist, ud);
            getMapComposer().getSession().setAttribute("userpoints", htUserSpecies);

            if(eventListener != null) {
                eventListener.onEvent(new Event("",null,slist + "\t" + ud.getName()));
            }

            // close the reader and data streams
            reader.close();
            data.close();

        } catch (Exception e) {

            getMapComposer().showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user points: ");
            e.printStackTrace(System.out);
        }
    }

    public void continueLoadUserLSIDs(UserData ud, Reader data, CSVReader reader, List userPoints) {
        try {
            //don't care if it has a header

            // check if the count of LSIDs goes over the threshold (+1).
            int sizeToCheck = userPoints.size();
            System.out.println("Checking user LSIDs size: " + sizeToCheck + " -> " + 50);
            if (sizeToCheck > 50) {
                getMapComposer().showMessage("Cannot upload more than 50 LSIDs");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < userPoints.size(); i++) {
                String[] up = (String[]) userPoints.get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(up[0].replace(",", "").trim().toLowerCase());
            }

            String lsids = sb.toString();

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/species/lsid/register");
            sbProcessUrl.append("?lsids=" + URLEncoder.encode(lsids.replace(".", "__"), "UTF-8"));

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString()); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            String pid = get.getResponseBodyAsString();

            System.out.println("uploaded points name: " + ud.getName() + " lsid: " + pid);

            ud.setFeatureCount(userPoints.size());
            Long did = new Long(pid);
            System.out.println("lval: " + did.longValue());
            ud.setUploadedTimeInMs(did.longValue());

            String metadata = "";
            metadata += "User uploaded points \n";
            metadata += "Name: " + ud.getName() + " \n";
            metadata += "Description: " + ud.getDescription() + " \n";
            metadata += "Date: " + ud.getDisplayTime() + " \n";
            metadata += "Number of Points: " + ud.getFeatureCount() + " \n";

            MapLayer ml = getMapComposer().mapSpeciesByLsid(pid, ud.getName(), "user", ud.getFeatureCount(), LayerUtilities.SPECIES);
            MapLayerMetadata md = ml.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
                ml.setMapLayerMetadata(md);
            }
            md.setMoreInfo(metadata);

            // add it to the user session
            Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
            if (htUserSpecies == null) {
                htUserSpecies = new Hashtable<String, UserData>();
            }
            htUserSpecies.put(pid, ud);
            getMapComposer().getSession().setAttribute("userpoints", htUserSpecies);

            // close the reader and data streams
            reader.close();
            data.close();
        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user LSIDs: ");
            e.printStackTrace(System.out);
        }
    }

    public void loadUserLayerKML(String name, InputStream data) {
        try {
            String kmlData = "";

            if (data != null) {
                Writer writer = new StringWriter();

                char[] buffer = new char[1024];
                try {
                    Reader reader = new BufferedReader(
                            new InputStreamReader(data));
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                } finally {
                    data.close();
                }
                kmlData = writer.toString();
            }

            loadUserLayerKML(name, kmlData.getBytes());

        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user kml: ");
            e.printStackTrace(System.out);
        }
    }

    public void loadUserLayerKML(String name, byte[] kmldata) {
        try {

            String id = String.valueOf(System.currentTimeMillis());
            String kmlpath = "/data/ala/runtime/output/layers/" + id + "/";
            File kmlfilepath = new File(kmlpath);
            kmlfilepath.mkdirs();
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(kmlfilepath.getAbsolutePath() + "/" + name)));
            String kmlstr = new String(kmldata);
            out.write(kmlstr);
            out.close();

            String kmlurl = "http://spatial-dev.ala.org.au/output/layers/" + id + "/" + name;

            MapLayer mapLayer = getMapComposer().getGenericServiceAndBaseLayerSupport().createMapLayer("User-defined kml layer", "User-defined layer", "KML", kmlurl);

            if (mapLayer == null) {
                logger.debug("The layer " + name + " couldnt be created");
                getMapComposer().showMessage(getMapComposer().getLanguagePack().getLang("ext_layer_creation_failure"));
            } else {
                getMapComposer().addUserDefinedLayerToMenu(mapLayer, true);
            }


        } catch (Exception e) {

            getMapComposer().showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user kml: ");
            e.printStackTrace(System.out);
        }
    }

    private void unzipFile(String name, InputStream data) {
        try {
            String id = String.valueOf(System.currentTimeMillis());
            String outputpath = "/data/ala/runtime/output/layers/" + id + "/";
            //String outputpath = "/Users/ajay/projects/tmp/useruploads/" + id + "/";

            String zipfilename = name.substring(0, name.lastIndexOf("."));
            outputpath += zipfilename + "/";
            File outputDir = new File(outputpath);
            outputDir.mkdirs();

            ZipInputStream zis = new ZipInputStream(data);
            ZipEntry ze = null;
            String shpfile = "";
            String type = "";

            while ((ze = zis.getNextEntry()) != null) {
                System.out.println("ze.file: " + ze.getName());
                if (ze.getName().endsWith(".shp")) {
                    shpfile = ze.getName();
                    type = "shp";
                }
                String fname = outputpath + ze.getName();
                copyInputStream(zis, new BufferedOutputStream(new FileOutputStream(fname)));
                zis.closeEntry();
            }
            zis.close();

            if (type.equalsIgnoreCase("shp")) {
                System.out.println("Uploaded file is a shapefile. Loading...");
                loadUserShapefile(new File(outputpath + shpfile));
            } else {
                System.out.println("Unknown file type. ");
                getMapComposer().showMessage("Unknown file type. Please upload a valid CSV, KML or Shapefile. ");
            }

        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user kml: ");
            e.printStackTrace(System.out);

        }
    }

    private void copyInputStream(InputStream in, OutputStream out) throws IOException, Exception {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) > -1) {
            out.write(buffer, 0, len);
        }

        // no need to close the input stream as it gets closed
        // in the caller function.
        // just close the output stream.
        out.close();
    }

    private void loadUserShapefile(File shpfile) {
        try {
            FileDataStore store = FileDataStoreFinder.getDataStore(shpfile);

            System.out.println("Loading shapefile. Reading content:");
            System.out.println(store.getTypeNames()[0]);

            FeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);

            FeatureCollection featureCollection = featureSource.getFeatures();
            FeatureIterator it = featureCollection.features();
            while (it.hasNext()) {
                SimpleFeature feature = (SimpleFeature) it.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                WKTWriter wkt = new WKTWriter();
                getMapComposer().addWKTLayer(wkt.write(geom), feature.getID(), feature.getID());
                break;
            }
            featureCollection.close(it);
        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user shapefile: ");
            e.printStackTrace(System.out);
        }
    }

    void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }
}
