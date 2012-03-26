/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.zk;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.Zipper;
import org.apache.commons.io.IOUtils;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;

/**
 *
 * @author ajay
 */
public class FilesZK extends GenericForwardComposer {

    private Fileupload btnFileUpload;
    private Label lblmsg;
    private String UPLOAD_PATH = "/Users/ajay/projects/tmp/useruploads/";

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        UPLOAD_PATH = AlaspatialProperties.getBaseOutputDir();
    }

    /**
     * Upload file. All files uploaded are treated as binary
     * so don't have to check for file type.
     * This is set in the zul via native="true" attribute
     *
     * @param ue
     */
    public void onUpload$btnFileUpload(UploadEvent ue) {

        BufferedInputStream in = null;
        BufferedOutputStream out = null;


        if (ue == null) {
            System.out.println("unable to upload file");
            return;
        } else {
            System.out.println("fileUploaded()");
        }

        try {

            Media m = ue.getMedia();
            System.out.println("m.getContentType(): " + m.getContentType());
            System.out.println("m.getFormat(): " + m.getFormat());

            try {
                InputStream is = m.getStreamData();
                in = new BufferedInputStream(is);

                File baseDir = new File(UPLOAD_PATH);
                if (!baseDir.exists()) {
                    baseDir.mkdirs();
                }

                final File file = new File(UPLOAD_PATH + m.getName());

                OutputStream fout = new FileOutputStream(file);
                out = new BufferedOutputStream(fout);
                /*
                byte buffer[] = new byte[1024];
                int ch = in.read(buffer);
                while (ch != -1) {
                out.write(buffer, 0, ch);
                ch = in.read(buffer);
                }
                 *
                 */

                IOUtils.copy(in, out);

                if (m.getFormat().equals("zip") || m.getFormat().equals("x-gzip")) {
                    //final String basepath = UPLOAD_PATH + m.getName().substring(0, m.getName().lastIndexOf(".")) + "/";
                    final String filename = m.getName();
                    Messagebox.show("Archive file detected. Would you like to unzip this file?", "ALA Spatial Portal", Messagebox.YES + Messagebox.NO, Messagebox.QUESTION, new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            //System.out.println("got user response: " + event.getName());
                            //System.out.println("data: " + event.getData() + " ( " + Messagebox.YES + " | " + Messagebox.NO + " )");
                            try {
                                int response = ((Integer) event.getData()).intValue();
                                if (response == Messagebox.YES) {
                                    System.out.println("unzipping file to: " + UPLOAD_PATH);
                                    boolean success = Zipper.unzipFile(filename, new FileInputStream(file), UPLOAD_PATH, false);
                                    if (success) {
                                        Messagebox.show("File unzipped: '" + filename + "'");
                                    } else {
                                        Messagebox.show("Unable to unzip '" + filename + "' ");
                                    }
                                } else {
                                    System.out.println("leaving archive file alone");
                                }
                            } catch (NumberFormatException nfe) {
                                System.out.println("Not a valid response");
                            }
                        }
                    });
                } else {
                    Messagebox.show("File '" + m.getName() + "' successfully uploaded");
                }


            } catch (IOException e) {
                System.out.println("IO Exception while saving file: ");
                e.printStackTrace(System.out);
            } catch (Exception e) {
                System.out.println("General Exception: ");
                e.printStackTrace(System.out);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }

                    if (in != null) {
                        in.close();
                    }

                } catch (IOException e) {
                    System.out.println("IO Exception while closing stream: ");
                    e.printStackTrace(System.out);
                }
            }

        } catch (Exception e) {
            System.out.println("Error uploading file.");
            e.printStackTrace(System.out);
        }
    }
}
