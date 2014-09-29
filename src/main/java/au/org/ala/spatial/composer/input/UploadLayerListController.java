package au.org.ala.spatial.composer.input;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.UtilityComposer;
import org.apache.log4j.Logger;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author ajay
 */
public class UploadLayerListController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(UploadLayerListController.class);
    private EventListener callback;

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onUpload$fileUpload(Event event) {
        doFileUpload(event);
        this.detach();
    }

    public void doFileUpload(Event event) {
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

            boolean loaded = false;
            try {
                loadLayerList(m.getReaderData());
                loaded = true;
                LOGGER.debug(m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //failed to read, will try another method
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(new String(m.getByteData())));
                    loaded = true;
                    LOGGER.debug(m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //failed to read, will try another method
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new InputStreamReader(m.getStreamData()));
                    loaded = true;
                    LOGGER.debug(m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //failed to read, will try another method
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(m.getStringData()));

                    LOGGER.debug(m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage(CommonData.lang("error_uploading_file"));
                    LOGGER.error("unable to load user layer list: ", e);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("error reading uploaded file", ex);
        }
    }

    private void loadLayerList(Reader r) throws IOException {
        CSVReader reader = new CSVReader(r);
        //one line, read it
        StringBuilder sb = new StringBuilder();
        for (String s : reader.readNext()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s);
        }

        reader.close();

        if (callback != null) {
            try {
                callback.onEvent(new ForwardEvent("", this, null, sb.toString()));
            } catch (Exception e) {
                LOGGER.error("failed when calling ToolComposer callback", e);
            }
        }
    }

    public void setCallback(EventListener callback) {
        this.callback = callback;
    }
}
