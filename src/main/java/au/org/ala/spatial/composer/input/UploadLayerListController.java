package au.org.ala.spatial.composer.input;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.composer.tool.ToolComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.apache.log4j.Logger;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
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

    private static Logger logger = Logger.getLogger(UploadLayerListController.class);
    SettingsSupplementary settingsSupplementary;
    String layerList;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

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
            logger.debug("unable to upload file");
            return;
        } else {
            logger.debug("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            boolean loaded = false;
            try {
                loadLayerList(m.getReaderData());
                loaded = true;
                logger.debug("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //e.printStackTrace();
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(new String(m.getByteData())));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new InputStreamReader(m.getStreamData()));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(m.getStringData()));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage("Unable to load your file.");
                    logger.error("unable to load user layer list: ", e);
                }
            }
        } catch (Exception ex) {
            logger.error("error reading uploaded file", ex);
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
        layerList = sb.toString();
        reader.close();
        if (getParent() instanceof ToolComposer) {
            ((ToolComposer) getParent()).selectLayerFromList(layerList);
            ((ToolComposer) getParent()).updateLayerSelectionCount();
        }
    }
}
