package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;

/**
 *
 * @author ajay
 */
public class UploadLayerListController extends UtilityComposer {

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
            System.out.println("unable to upload file");
            return;
        } else {
            System.out.println("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            boolean loaded = false;
            try {
                loadLayerList(m.getReaderData());
                loaded = true;
                System.out.println("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //e.printStackTrace();
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(new String(m.getByteData())));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new InputStreamReader(m.getStreamData()));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(m.getStringData()));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage("Unable to load your file.");
                    System.out.println("unable to load user layer list: ");
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadLayerList(Reader r) throws IOException {
        CSVReader reader = new CSVReader(r);
        //one line, read it
        StringBuilder sb = new StringBuilder();
        for(String s : reader.readNext()) {
            if(sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s);
        }
        layerList = sb.toString();
        reader.close();
        if (getParent() instanceof AddToolComposer) {
            ((AddToolComposer) getParent()).selectLayerFromList(layerList);
            ((AddToolComposer) getParent()).updateLayerSelectionCount();
        }
    }
}
