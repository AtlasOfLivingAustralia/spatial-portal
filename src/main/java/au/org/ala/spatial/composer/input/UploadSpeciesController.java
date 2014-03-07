package au.org.ala.spatial.composer.input;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.composer.add.AddSpeciesController;
import au.org.ala.spatial.composer.add.AddSpeciesInArea;
import au.org.ala.spatial.composer.tool.ToolComposer;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.QueryField;
import au.org.ala.spatial.data.UploadQuery;
import au.org.ala.spatial.userpoints.RecordsLookup;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.UserData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import org.apache.log4j.Logger;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * @author ajay
 */
public class UploadSpeciesController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(UploadSpeciesController.class);

    Button btnOk;
    SettingsSupplementary settingsSupplementary;
    Textbox tbDesc;
    Textbox tbName;
    Button fileUpload;
    Label tbInstructions;
    Html lsidinfo;
    String uploadLSID;
    String uploadType = "normal";
    private EventListener eventListener;
    public boolean addToMap;
    boolean defineArea;
    Textbox tMultiple;

    @Override
    public void afterCompose() {
        super.afterCompose();

        setTbInstructions("3. Select file (text file, one LSID or name per line)");

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
                addToMap = true;
                defineArea = true;
                doFileUpload(ud, event);
                onClick$btnCancel(null);
            }
        });
    }

    public void onClick$btnOk(Event event) {
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        if (this.getParent().getId().equals("addtoolwindow")) {
            ToolComposer analysisParent = (ToolComposer) this.getParent();
            analysisParent.resetWindowFromSpeciesUpload("", "cancel");
        }
        this.detach();
    }

    public void processMedia(Media media) {
        try {
            Messagebox.show("Processing...", "Error",
                    Messagebox.OK, Messagebox.ERROR);
            logger.debug("Loading files");
            if (media != null) {
                if (media instanceof org.zkoss.util.media.AMedia) {
                    //lblFupload.setValue(media.getName());
                    logger.debug("Valid file successfully uploaded");
                } else {
                    Messagebox.show("Not a valid upload: " + media, "Error",
                            Messagebox.OK, Messagebox.ERROR);
                    logger.debug("not a valid file");
                }
            } else {
                Messagebox.show("No attachment", "Error",
                        Messagebox.OK, Messagebox.ERROR);
                logger.debug("No attachment");
            }
        } catch (Exception e) {
            logger.debug("Unable to process media: ");
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
        if (event instanceof UploadEvent) {
            ue = (UploadEvent) event;
        } else if (event instanceof ForwardEvent) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            logger.debug("unable to upload file");
            return;
        } else {
            logger.debug("fileUploaded()");
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

            ud.setUploadedTimeInMs(System.currentTimeMillis());

            logger.debug("Got file '" + ud.getName() + "' with type '" + m.getContentType() + "'");

            //forget content types, do 'try'
            boolean loaded = false;
            try {
                loadUserPoints(ud, m.getReaderData());
                loaded = true;
                logger.debug("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //e.printStackTrace();
            }
            if (!loaded) {
                try {
                    loadUserPoints(ud, new StringReader(new String(m.getByteData())));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    loadUserPoints(ud, new InputStreamReader(m.getStreamData()));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    loadUserPoints(ud, new StringReader(m.getStringData()));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage("Unable to load your file. Please try again.");
                    logger.debug("unable to load user points: ");
                    e.printStackTrace();
                }
            }

            //call reset window on caller to perform refresh'
            logger.debug("\n\n\nusc.addtoolwindow?: " + this.getParent().getId() + "\n\n\n");
            if (this.getParent().getId().equals("addtoolwindow")) {
                ToolComposer analysisParent = (ToolComposer) this.getParent();
                analysisParent.resetWindowFromSpeciesUpload(uploadLSID, uploadType);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadUserPoints(String name, Reader data) throws Exception {
        loadUserPoints(new UserData(name), data);
    }

    public void loadUserPoints(UserData ud, Reader data) throws Exception {
        logger.debug("\n\n\nin loadUserPoints");
        // Read a line in to check if it's a valid file
        // if it throw's an error, then it's not a valid csv file
        CSVReader reader = new CSVReader(data);

        List userPoints = reader.readAll();

        logger.debug("userPoints.size(): " + userPoints.size());
        //if only one column treat it as a list of LSID's
        if (userPoints.size() == 0) {
            throw (new RuntimeException("no data in csv"));
        }
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

        logger.debug("hasHeader: " + hasHeader);

        // check if the count of points goes over the threshold.
        int sizeToCheck = (hasHeader) ? userPoints.size() - 1 : userPoints.size();
        logger.debug("Checking user points size: " + sizeToCheck + " -> " + settingsSupplementary.getValueAsInt("max_record_count_upload"));
        if (sizeToCheck > settingsSupplementary.getValueAsInt("max_record_count_upload")) {
            getMapComposer().showMessage(settingsSupplementary.getValue("max_record_count_upload_message"));
            return;
        }

        ArrayList<QueryField> fields = new ArrayList<QueryField>();
        if (upHeader.length == 2) {
            //only points upload, add 'id' column at the start
            fields.add(new QueryField("id"));
            fields.get(0).ensureCapacity(sizeToCheck);
        }
        String[] defaultHeader = {"id", "longitude", "latitude"};
        for (int i = 0; i < upHeader.length; i++) {
            String name = upHeader[i];
            if (upHeader.length == 2 && i < 2) {
                name = defaultHeader[i + 1];
            } else if (upHeader.length > 2 && i < 3) {
                name = defaultHeader[i];
            }
            fields.add(new QueryField("f" + String.valueOf(i), name, QueryField.FieldType.AUTO));
            fields.get(fields.size() - 1).ensureCapacity(sizeToCheck);
        }

        double[] points = new double[sizeToCheck * 2];
        int counter = 1;
        int hSize = hasHeader ? 1 : 0;
        for (int i = 0; i < userPoints.size() - hSize; i++) {
            String[] up = (String[]) userPoints.get(i + hSize);
            if (up.length > 2) {
                for (int j = 0; j < up.length && j < fields.size(); j++) {
                    //replace anything that may interfere with webportal facet parsing
                    String s = up[j].replace("\"", "'").replace(" AND ", " and ").replace(" OR ", " or ");
                    if (s.length() > 0 && s.charAt(0) == '*') {
                        s = "_" + s;
                    }
                    fields.get(j).add(s);
                }
                try {
                    points[i * 2] = Double.parseDouble(up[1]);
                    points[i * 2 + 1] = Double.parseDouble(up[2]);
                } catch (Exception e) {
                }
            } else if (up.length > 1) {
                fields.get(0).add(ud.getName() + "-" + counter);
                for (int j = 0; j < up.length && j < fields.size(); j++) {
                    fields.get(j + 1).add(up[j]);
                }
                try {
                    points[i * 2] = Double.parseDouble(up[0]);
                    points[i * 2 + 1] = Double.parseDouble(up[1]);
                } catch (Exception e) {
                }
                counter++;
            }
        }

        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).store();
        }

        String pid = String.valueOf(System.currentTimeMillis());

        ud.setFeatureCount(userPoints.size() - hSize);

        String metadata = "";
        metadata += "User uploaded points \n";
        metadata += "Name: " + ud.getName() + " <br />\n";
        metadata += "Description: " + ud.getDescription() + " <br />\n";
        metadata += "Date: " + ud.getDisplayTime() + " <br />\n";
        metadata += "Number of Points: " + ud.getFeatureCount() + " <br />\n";

        ud.setMetadata(metadata);
        ud.setSubType(LayerUtilities.SPECIES_UPLOAD);
        ud.setLsid(pid);
        uploadLSID = pid + "\t" + ud.getName();

        Query q = new UploadQuery(pid, ud.getName(), points, fields, metadata);
        ud.setQuery(q);
        RecordsLookup.putData(pid, points, fields, metadata);

        // add it to the user session
        Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
        if (htUserSpecies == null) {
            htUserSpecies = new Hashtable<String, UserData>();
        }
        htUserSpecies.put(pid, ud);
        getMapComposer().getSession().setAttribute("userpoints", htUserSpecies);

        logger.debug("addToMap: " + addToMap);
        logger.debug("uploadLSID: " + uploadLSID);
        logger.debug("metadata: " + metadata);

        if (addToMap) {
            if (!defineArea) {
                //do default sampling now
                ArrayList<QueryField> f = CommonData.getDefaultUploadSamplingFields();
                //add any analysis layers from the layers list
                for (MapLayer ml : getMapComposer().getAnalysisLayers()) {
                    f.add(new QueryField(ml.getName(), ml.getDisplayName(), QueryField.FieldType.AUTO));
                }
                if (CommonData.getDefaultUploadSamplingFields().size() > 0) {
                    q.sample(f);
                    ((UploadQuery) q).resetOriginalFieldCount(-1);
                }
            }
            MapLayer ml = null;
            if (ud.getFeatureCount() > settingsSupplementary.getValueAsInt(MapComposer.POINTS_CLUSTER_THRESHOLD)) {
                if (defineArea) {
                    mapFilterGrid(q, ud.getName(), ud.getFeatureCount(), metadata);
                } else {
                    ml = getMapComposer().mapSpecies(q, ud.getName(), "user", ud.getFeatureCount(), LayerUtilities.SPECIES_UPLOAD, null, -1,
                            MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
                }
            } else {
                if (defineArea) {
                    mapFilter(q, ud.getName(), ud.getFeatureCount(), metadata);
                } else {
                    ml = getMapComposer().mapSpecies(q, ud.getName(), "user", ud.getFeatureCount(), LayerUtilities.SPECIES_UPLOAD, null, -1,
                            MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
                }
            }
            if (ml != null) {
                ml.getMapLayerMetadata().setMoreInfo(metadata);
            }
        }

        if (eventListener != null) {
            eventListener.onEvent(new Event("", null, pid + "\t" + ud.getName()));
        }

        // close the reader and data streams
        reader.close();
        data.close();
    }

    public void continueLoadUserLSIDs(UserData ud, Reader data, CSVReader reader, List userPoints) {
        logger.debug("\n\n\nin continueLoadUserLSIDs");
        try {
            //don't care if it has a header

            // check if the count of LSIDs goes over the threshold (+1).
            int sizeToCheck = userPoints.size();
            logger.debug("Checking user LSIDs size: " + sizeToCheck + " -> " + 50);
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

            if (this.getParent().getId().equals("addtoolwindow")) {
                uploadLSID = lsids;
                uploadType = "assemblage";
            } else {
                AddSpeciesController window = (AddSpeciesController) Executions.createComponents("WEB-INF/zul/add/AddSpecies.zul", getMapComposer(), null);
                try {
                    window.setMultipleSpecies(lsids, ud.getName());
                    window.doModal();
                } catch (Exception e) {
                    logger.error("error opening AddSpecies.zul", e);
                }
            }
        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");

            logger.error("unable to load user LSIDs: ", e);
        }
    }

    void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setTbInstructions(String instructions) {
        tbInstructions.setValue(instructions);
        if (instructions.contains("longitude")) {
            lsidinfo.setVisible(false);
            this.setTitle("Import points");
        } else {
            lsidinfo.setVisible(true);
            this.setTitle("Import assemblage");
        }
    }

    public void setUploadType(String uploadType) {
        this.uploadType = uploadType;
    }

    public void setDefineArea(boolean defineArea) {
        this.defineArea = defineArea;
    }

    private void mapFilterGrid(Query q, String name, int featureCount, String metadata) {
        AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/add/AddSpeciesInArea.zul", getMapComposer(), null);
        window.setSpeciesFilterGridParams(q, name, "user", featureCount, LayerUtilities.SPECIES_UPLOAD, metadata, "User");
        window.loadAreaLayers();
        try {
            window.doModal();
        } catch (Exception e) {
            logger.error("error opening AddSpeciesInArea.zul", e);
        }
    }

    private void mapFilter(Query q, String name, int featureCount, String metadata) {
        AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/add/AddSpeciesInArea.zul", getMapComposer(), null);
        window.setSpeciesFilterParams(q, name, "user", featureCount, LayerUtilities.SPECIES_UPLOAD, metadata, "User");
        window.loadAreaLayers();
        try {
            window.doModal();
        } catch (Exception e) {
            logger.error("error opening AddSpeciesInArea.zul", e);
        }
    }
}
