package au.org.ala.spatial.composer.input;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.composer.add.AddSpeciesController;
import au.org.ala.spatial.composer.add.AddSpeciesInArea;
import au.org.ala.spatial.composer.tool.ToolComposer;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.UserDataQuery;
import au.org.ala.spatial.util.UserData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
import java.net.URLEncoder;
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
            logger.error("Unable to process media: ", e);
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
                    logger.error("unable to load user points: ", e);
                }
            }

            //call reset window on caller to perform refresh'
            logger.debug("\n\n\nusc.addtoolwindow?: " + this.getParent().getId() + "\n\n\n");
            if (this.getParent().getId().equals("addtoolwindow")) {
                ToolComposer analysisParent = (ToolComposer) this.getParent();
                analysisParent.resetWindowFromSpeciesUpload(uploadLSID, uploadType);
            }
        } catch (Exception ex) {
            logger.error("unable to load user points", ex);
        }
    }

    public void loadUserPoints(String name, Reader data) throws Exception {
        loadUserPoints(new UserData(name), data);
    }

    public void loadUserPoints(UserData ud, Reader data) throws Exception {
        logger.debug("\n\n\nin loadUserPoints");
        // Read a line in to check if it's a valid file
        // if it throw's an error, then it's not a valid csv file

        StringBuilder sbProcessUrl = new StringBuilder();
        sbProcessUrl.append(/*CommonData.layersServer*/ "http://localhost:8080/layers-service" + "/userdata/add");

        //TODO: get signed in user's id
        sbProcessUrl.append("?user_id=" + "anonymous");
        sbProcessUrl.append("&name=").append(URLEncoder.encode(this.tbName.getText(), "UTF-8"));
        sbProcessUrl.append("&description=").append(URLEncoder.encode(this.tbDesc.getText(), "UTF-8"));


        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(sbProcessUrl.toString());

        post.addRequestHeader("Accept", "application/json");

        post.addParameter("csv", IOUtils.toString(data));

        logger.debug("calling add user data ws: " + sbProcessUrl.toString());
        int result = client.executeMethod(post);

        JSONObject jo = (JSONObject) new JSONParser().parse(post.getResponseBodyAsString());

        if (!jo.containsKey("error")) {

        }

        //this is loading user data there is no need for ud_header_id + facet_id for use in layer creation.
        UserDataQuery q = new UserDataQuery(String.valueOf(jo.get("ud_header_id")));

        if (addToMap) {
            MapLayer ml = null;

            if (defineArea) {
                mapFilterGrid(q, ud.getName(), ud.getFeatureCount(), q.getMetadataHtml());
            } else {
                ml = getMapComposer().mapSpecies(q, ud.getName(), "user", ud.getFeatureCount(), LayerUtilities.SPECIES_UPLOAD, null, -1,
                        MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour());
            }

            if (ml != null) {
                ml.getMapLayerMetadata().setMoreInfo(q.getMetadataHtml());
            }
        }

        if (eventListener != null) {
            eventListener.onEvent(new Event("", null, jo.get("ud_header_id") + "\t" + ud.getName()));
        }


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
