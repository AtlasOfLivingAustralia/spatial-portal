/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Adam
 */
public class SessionPrint {

    String server;
    String height;
    String width;
    String htmlpthfilename;
    String htmlurlfilename;
    String uid;
    String imgFilename;
    String pdfFilename;
    String sessionid;
    String zoom;

    public SessionPrint(String server, String height, String width, String htmlpth, String htmlurl, String uid, String sessionid, String zoom) {
        this.server = server;
        this.height = height;
        this.width = width;
        this.htmlpthfilename = htmlpth + uid + ".html";
        this.htmlurlfilename = htmlurl + uid + ".html";
        this.imgFilename = htmlpth + uid + ".jpg";
        this.pdfFilename = htmlpth + uid + ".jpg";
        this.uid = uid;
        this.sessionid = sessionid;
        this.zoom = zoom;
    }

    public String getImageFilename() {
       return imgFilename;
    }

    public void print() {
        makeHtmlFile();

        makeImageOfHtmlFile();
    }

    String getHtmlContent() {
        //html wrapper
        StringBuffer html = new StringBuffer();
        html.append("<html><script>cookie='");
        html.append(sessionid);
        html.append("';d = new Date(); d.setTime(d.getTime()+(1000000));");
        html.append("document.cookie = 'JSESSIONID=' + cookie + '; expires=' + d.toGMTString() + '; path=/';");
        html.append("</script><body style='margin:0px'>");
        html.append("<iframe style='border:none' src='");
        html.append(server + "?p=" + width + "," + height + "," + zoom);
        html.append("' width='");
        html.append(width);
        html.append("px' height='");
        html.append(height);
        html.append("px' /></body></html>");

        return html.toString();
    }

    void makeHtmlFile() {
        //write html to load to a file
        try {
            FileWriter fw = new FileWriter(htmlpthfilename);
            fw.append(getHtmlContent());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeImageOfHtmlFile() {
        //TODO: dynamic path and settings
        String cmd = "/mnt/ala/printing/wkhtmltoimage"
                + " --debug-javascript"
                + " --load-error-handling ignore"
                + " --javascript-delay 15000" //delay 15s for tiles to load
                + " " + htmlurlfilename + "?p=" + width + "," + height + "," + zoom + " " + imgFilename;

        try {
            /* Xml to Jpg */
            System.out.println("Setting up output stream readers: " + cmd);

            //delete any existing output files
            File img = new File(imgFilename);
            if (img.exists()) {
                img.delete();
            }

            Runtime runtime = Runtime.getRuntime();

            //wait for output file to be created (max 45s then retry twice)
            int retry = 0;
            long now;
            while(!img.exists() && retry < 3){
                Process proc = runtime.exec(cmd);

                now = System.currentTimeMillis() + 45000;
                while (now > System.currentTimeMillis() && !img.exists());

                if (img.exists()) {
                    break;
                } else {
                    System.out.println("failed (" + retry + ") cmd: " + cmd);
                    proc.destroy();
                }
                retry++;
            }

            //wait 5 seconds to render (hope it is enough!)
            now = System.currentTimeMillis() + 5000;
            while(now > System.currentTimeMillis());

            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
