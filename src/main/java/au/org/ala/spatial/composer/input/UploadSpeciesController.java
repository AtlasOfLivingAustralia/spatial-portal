package au.org.ala.spatial.composer.input;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.UserDataDTO;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.UserDataQuery;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
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
import java.util.Map;

/**
 * @author ajay
 */
public class UploadSpeciesController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(UploadSpeciesController.class);
    private boolean addToMap;
    private Button btnOk;
    private Textbox tbDesc;
    private Textbox tbName;
    private Button fileUpload;
    private Label tbInstructions;
    private Html lsidinfo;
    private String uploadLSID;
    private String uploadType = "normal";
    private boolean defineArea;
    private EventListener callback;
    private EventListener eventListener;

    public static void loadUserPoints(UserDataDTO ud, Reader data, boolean addToMap
            , String name, String description, MapComposer mc, EventListener eventListener) throws Exception {
        LOGGER.debug("\n\n\nin loadUserPoints");
        // Read a line in to check if it's a valid file
        // if it throw's an error, then it's not a valid csv file

        StringBuilder sbProcessUrl = new StringBuilder();
        sbProcessUrl.append(CommonData.getLayersServer()).append("/userdata/add");

        //TODO: get signed in user's id
        sbProcessUrl.append("?user_id=").append("anonymous");
        sbProcessUrl.append("&name=").append(URLEncoder.encode(name, StringConstants.UTF_8));
        sbProcessUrl.append("&description=").append(URLEncoder.encode(description, StringConstants.UTF_8));


        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(sbProcessUrl.toString());

        post.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

        post.addParameter("csv", IOUtils.toString(data));

        LOGGER.debug("calling add user data ws: " + sbProcessUrl.toString());
        client.executeMethod(post);

        JSONObject jo = (JSONObject) new JSONParser().parse(post.getResponseBodyAsString());

        //this is loading user data there is no need for ud_header_id + facet_id for use in layer creation.
        UserDataQuery q = new UserDataQuery(String.valueOf(jo.get("ud_header_id")));

        if (addToMap) {
            MapLayer ml = mc.mapSpecies(q, ud.getName(), "user", ud.getFeatureCount(), LayerUtilitiesImpl.SPECIES_UPLOAD, null, -1,
                    MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);

            if (ml != null) {
                ml.getMapLayerMetadata().setMoreInfo(q.getMetadataHtml());
            }
        }

        if (eventListener != null) {
            eventListener.onEvent(new Event("", null, jo.get("ud_header_id") + "\t" + ud.getName()));
        }


        data.close();
    }

    @Override
    public void afterCompose() {
        super.afterCompose();

        setTbInstructions("3. Select file (text file, one LSID or name per line)");

        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object o : m.entrySet()) {
                if (((Map.Entry) o).getKey() instanceof String
                        && "setTbInstructions".equals(((Map.Entry) o).getKey())) {
                    setTbInstructions((String) ((Map.Entry) o).getValue());
                }
                if (((Map.Entry) o).getKey() instanceof String
                        && "addToMap".equals(((Map.Entry) o).getKey())) {
                    addToMap = true;
                }
            }
        }

        tbName.setConstraint(new Constraint() {

            @Override
            public void validate(Component comp, Object value) {
                String val = (String) value;
                Map htUserSpecies = (Map) getMapComposer().getSession().getAttribute(StringConstants.USERPOINTS);
                if (htUserSpecies != null && !htUserSpecies.isEmpty()) {
                    for (Object o : htUserSpecies.values()) {
                        if (((UserDataDTO) o).getName().equalsIgnoreCase(val.trim())) {
                            throw new WrongValueException(comp, "User dataset named '" + val + "' already exists. Please enter another name for your dataset.");
                        }
                    }
                }
            }
        });

        fileUpload.addEventListener("onUpload", new EventListener() {

            public void onEvent(Event event) throws Exception {
                UserDataDTO ud = new UserDataDTO(tbName.getValue(), tbDesc.getValue(), "points");
                addToMap = true;
                defineArea = true;
                doFileUpload(ud, event);
                onClick$btnCancel(null);
            }
        });
    }

    public void setCallback(EventListener callback) {
        this.callback = callback;
    }

    public void onClick$btnOk(Event event) {
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        if (callback != null) {
            try {
                callback.onEvent(new ForwardEvent("", null, null, new String[]{"", "cancel" }));
            } catch (Exception e) {
                LOGGER.error("failed to cancel species points upload", e);
            }
        }
        this.detach();
    }

    public void processMedia(Media media) {
        try {
            Messagebox.show("Processing...", "Error",
                    Messagebox.OK, Messagebox.ERROR);
            LOGGER.debug("Loading files");
            if (media != null) {
                if (media instanceof org.zkoss.util.media.AMedia) {
                    LOGGER.debug("Valid file successfully uploaded");
                } else {
                    Messagebox.show("Not a valid upload: " + media, "Error",
                            Messagebox.OK, Messagebox.ERROR);
                    LOGGER.debug("not a valid file");
                }
            } else {
                Messagebox.show("No attachment", "Error",
                        Messagebox.OK, Messagebox.ERROR);
                LOGGER.debug("No attachment");
            }
        } catch (Exception e) {
            LOGGER.error("Unable to process media: ", e);
        }
    }

    public void onUpload$btnFileUpload(Event event) {
        doFileUpload("", event);
    }

    public void doFileUpload(String name, Event event) {
        doFileUpload(new UserDataDTO(name), event);
    }

    public void doFileUpload(UserDataDTO ud, Event event) {
        UploadEvent ue = null;
        if (event instanceof UploadEvent) {
            ue = (UploadEvent) event;
        } else if (event instanceof ForwardEvent) {
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
            UserDataDTO u = ud;
            if (u == null) {
                u = new UserDataDTO(m.getName());
            }
            if (u.getName().trim().isEmpty()) {
                u.setName(m.getName());
            }
            u.setFilename(m.getName());

            if (u.getName() == null || u.getName().length() == 0) {
                u.setName(m.getName());
            }
            if (u.getDescription() == null || u.getDescription().length() == 0) {
                u.setDescription(m.getName());
            }

            u.setUploadedTimeInMs(System.currentTimeMillis());

            LOGGER.debug("Got file '" + u.getName() + "' with type '" + m.getContentType() + "'");

            //forget content types, do 'try'
            boolean loaded = false;
            try {
                loadUserPoints(u, m.getReaderData(), addToMap, tbName.getText(), tbDesc.getText(), getMapComposer(), eventListener);
                loaded = true;
                LOGGER.debug("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //failed to read uploaded data, will try another method
            }
            if (!loaded) {
                try {
                    loadUserPoints(u, new StringReader(new String(m.getByteData())), addToMap, tbName.getText(), tbDesc.getText(), getMapComposer(), eventListener);
                    loaded = true;
                    LOGGER.debug("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //failed to read uploaded data, will try another method
                }
            }
            if (!loaded) {
                try {
                    loadUserPoints(u, new InputStreamReader(m.getStreamData()), addToMap, tbName.getText(), tbDesc.getText(), getMapComposer(), eventListener);
                    loaded = true;
                    LOGGER.debug("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //failed to read uploaded data, will try another method

                }
            }
            if (!loaded) {
                try {
                    loadUserPoints(u, new StringReader(m.getStringData()), addToMap, tbName.getText(), tbDesc.getText(), getMapComposer(), eventListener);

                    LOGGER.debug("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage("Unable to load your file. Please try again.");
                    LOGGER.error("unable to load user points: ", e);
                }
            }

            //call reset window on caller to perform refresh'
            if (callback != null) {
                try {
                    callback.onEvent(new ForwardEvent("", null, null, new String[]{uploadLSID, uploadType}));
                } catch (Exception e) {
                    LOGGER.error("failed to cancel species points upload", e);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("unable to load user points", ex);
        }
    }

    void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setTbInstructions(String instructions) {
        tbInstructions.setValue(instructions);
        if (instructions.contains(StringConstants.LONGITUDE)) {
            lsidinfo.setVisible(false);
            ((Caption) getFellow(StringConstants.CTITLE)).setLabel("Import points");
        } else {
            lsidinfo.setVisible(true);
            ((Caption) getFellow(StringConstants.CTITLE)).setLabel("Import assemblage");
        }
    }

    public void setUploadType(String uploadType) {
        this.uploadType = uploadType;
    }

    public void setDefineArea(boolean defineArea) {
        this.defineArea = defineArea;
    }

    public void setAddToMap(boolean addToMap) {
        this.addToMap = addToMap;
    }
}
