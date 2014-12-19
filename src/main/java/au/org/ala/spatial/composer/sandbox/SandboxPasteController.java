package au.org.ala.spatial.composer.sandbox;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.ala.layers.legend.Facet;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.io.IOUtils;
import org.apache.xmlbeans.impl.common.ReaderInputStream;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Div;
import org.zkoss.zul.Iframe;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ajay
 */
public class SandboxPasteController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(SandboxPasteController.class);

    private String dataResourceUid;
    private Div sandboxReady;
    private Iframe sandboxFrame;
    private BiocacheQuery query;
    private boolean addtoMap;
    private boolean defineArea;
    private EventListener callback;
    private String uploadId;
    private String uploadFn;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void setAddToMap(boolean addtoMap) {
        this.addtoMap = addtoMap;
    }

    public void gotDrUid(Event event) {
        dataResourceUid = ((String) event.getData());

        sandboxReady.setVisible(true);

        sandboxFrame.setVisible(false);

        List<Facet> facetList = new ArrayList<Facet>();
        facetList.add(new Facet("data_resource_uid", dataResourceUid, true));
        query = new BiocacheQuery(null, null, null, facetList, true, null,
                CommonData.getSettings().getProperty("sandbox.biocache.url"),
                CommonData.getSettings().getProperty("sandbox.biocache.ws.url"),
                true);

        if (addtoMap) {
            getMapComposer().mapSpecies(query, query.getSolrName(), StringConstants.SPECIES, query.getOccurrenceCount(),
                    LayerUtilitiesImpl.SPECIES, null,
                    0, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
        }

        //call reset window on caller to perform refresh'
        if (callback != null) {
            try {
                callback.onEvent(new ForwardEvent("", null, null, new String[]{dataResourceUid, "normal"}));
            } catch (Exception e) {
                LOGGER.error("failed to cancel species points upload", e);
            }
        }

        onClick$btnOk(null);
    }

    public void setDefineArea(boolean defineArea) {
        this.defineArea = defineArea;
    }

    public void onClick$btnOk(Event event) {
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        if (callback != null) {
            try {
                callback.onEvent(new ForwardEvent("", null, null, new String[]{"", "cancel"}));
            } catch (Exception e) {
                LOGGER.error("failed to cancel species points upload", e);
            }
        }
        this.detach();
    }

    private boolean upload(byte[] bytes, String filename, String contentType) throws Exception {
        //create tmp file
        File tmp = File.createTempFile("pointsUpload", "_" + filename);
        FileUtils.writeByteArrayToFile(tmp, bytes);

        String url = CommonData.getSettings().getProperty("sandbox.url") + "upload/uploadFile";

        HttpClient httpClient = new HttpClient();

        PostMethod filePost = new PostMethod(url);
        Part[] parts = {
                new FilePart("myFile", filename, tmp, contentType, null)
        };
        filePost.setRequestEntity(
                new MultipartRequestEntity(parts, filePost.getParams())
        );

        int status = httpClient.executeMethod(filePost);

        if (status == 302) {
            String responseText = filePost.getResponseHeader("Location").getValue();
            uploadId = responseText.substring(responseText.indexOf("preview/") + "preview/".length(), responseText.lastIndexOf('?'));
            uploadFn = responseText.substring(responseText.indexOf('?') + 4);

            System.out.println(responseText);

            return true;
        }

        return false;
    }


    public void onUpload$fileUpload(Event event) {
        doFileUpload(event);
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

            String filename = m.getName();
            String contentType = m.getContentType();
            String format = m.getFormat();


            boolean loaded = false;
            if (!loaded) {
                try {
                    loaded = upload(m.getByteData(), filename, contentType);
                    LOGGER.debug(m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //failed to read, will try another method
                }
            }
            if (!loaded) {
                try {
                    loaded = upload(IOUtils.toByteArray(m.getStreamData()), filename, contentType);
                    LOGGER.debug(m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //failed to read, will try another method
                }
            }
            try {
                Reader r = m.getReaderData();
                InputStream is = new ReaderInputStream(r, "UTF-8");

                loaded = upload(IOUtils.toByteArray(is), filename, contentType);
                LOGGER.debug(m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //failed to read, will try another method
            }
            if (!loaded) {
                try {
                    loaded = upload(m.getStringData().getBytes(), filename, contentType);
                    LOGGER.debug(m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage(CommonData.lang("error_uploading_file"));
                    LOGGER.error("unable to load user layer list: ", e);
                }
            }

            //process if successful upload
            if (loaded) {
                ((Iframe) getFellow("sandboxFrame")).setSrc("sandboxPreview.html?uploadId=" + uploadId + "&uploadFn=" + uploadFn);

                //display frame and trigger javascript setup function
                getMapComposer().getOpenLayersJavascript().execute("$('#sandboxContainer')[0].style.display = \"inline\";setTimeout(\"setUrls()\",500)");

                getFellow("fileUpload").setVisible(false);
                getFellow("fileUpload").detach();
                getFellow("divSandbox").setVisible(false);

                setTop("10px");
            }
        } catch (Exception ex) {
            LOGGER.error("error reading uploaded file", ex);
        }
    }
}
