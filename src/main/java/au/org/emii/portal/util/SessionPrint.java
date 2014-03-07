/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.util;

import au.org.ala.spatial.util.CommonData;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * @author Adam
 */
public class SessionPrint {

    private static Logger logger = Logger.getLogger(SessionPrint.class);


    final int dpi = 200;
    final double margin_north = .4;  //A4 portrait top
    final double margin_south = .4;  //A4 portrait bottom
    final double margin_east = .4;   //A4 portrait right
    final double margin_west = .4;   //A4 portrait left
    final double page_width = 8.27;  //A4 width
    final double page_height = 11.69;//A4 height
    String server;
    String query;
    String height;
    String width;
    String htmlpthfilename;
    String htmlurlfilename;
    String htmlpth;
    String uid;
    String imgFilename;
    String pdfFilename;
    String jpgFilename;
    String sessionid;
    String zoom;
    String header;
    int resolution;
    String format;
    double grid;
    double scaleBy;

    public SessionPrint(String server, String height, String width, String htmlpth, String htmlurl, String uid, String jsessionid, String zoom, String header, double grid, String format, int resolution) {
        this.server = server;
        this.query = null;
        this.height = height;
        this.width = width;
        this.htmlpth = htmlpth;
        this.htmlpthfilename = htmlpth + uid + ".html";
        this.htmlurlfilename = htmlurl + uid + ".html";
        this.imgFilename = htmlpth + uid + ".png";
        this.pdfFilename = htmlpth + uid + ".pdf";
        this.jpgFilename = htmlpth + uid + ".jpg";
        this.uid = uid;
        this.sessionid = jsessionid;
        this.zoom = zoom;
        this.header = header;
        this.resolution = resolution;
        this.format = format;
        this.grid = grid;

        //resolution == 0 (current)
        //resolution == 1 (print: width up to 4962px, height up to 7014px = A4 600dpi)
        if (resolution == 1) {
            int maxW = (int) ((page_width - margin_west - margin_east) * dpi);  //A4 in inches
            int maxH = (int) ((page_height - margin_north - margin_south) * dpi);
            double w = Double.parseDouble(width);
            double h = Double.parseDouble(height);
            if (w > h) {               //swap to produce largest possible A4 map
                int tmp = maxH;
                maxH = maxW;
                maxW = tmp;
            }
            if (w / h > maxW / maxH) {
                //limit by w
                scaleBy = maxW / w;
                h = h * maxW / w;
                w = maxW;
            } else {
                //limit by h
                scaleBy = maxH / h;
                w = w * maxH / h;
                h = maxH;
            }
            this.width = String.valueOf((int) w);
            this.height = String.valueOf((int) h);

        } else {
            scaleBy = 1.0;
        }
    }

    public SessionPrint(String server, String query, String height, String width, String htmlpth, String htmlurl, String uid, String zoom, String header, double grid, String format) {
        this.server = server;
        this.query = query;
        this.height = height;
        this.width = width;
        this.htmlpth = htmlpth;
        this.htmlpthfilename = htmlpth + uid + ".html";
        this.htmlurlfilename = htmlurl + uid + ".html";
        this.imgFilename = htmlpth + uid + ".png";
        this.pdfFilename = htmlpth + uid + ".pdf";
        this.jpgFilename = htmlpth + uid + ".jpg";
        this.uid = uid;
        this.sessionid = null;
        this.zoom = zoom;
        this.header = header;

        this.format = format;
        this.grid = grid;

        double h = Double.parseDouble(height);
        double w = Double.parseDouble(width);
        int sizelimit = 1200 * 600;
        if (h * w > sizelimit) {
            this.resolution = 1;
            scaleBy = h * w / sizelimit;
        } else {
            this.resolution = 0;
            scaleBy = 1.0;
        }
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public String getImageFilename() {
        if (format.equalsIgnoreCase("png")) {
            return imgFilename;
        } else if (format.equalsIgnoreCase("pdf")) {
            return pdfFilename;
        } else {
            return jpgFilename;
        }
    }

    public void print() {
        makeHtmlFile();

        makeImageOfHtmlFile();

        makeConversionsOfImage();
    }

    String getHtmlContent(boolean setCookie) {
        //html wrapper
        StringBuilder html = new StringBuilder();
        html.append("<html><script>");
        if (setCookie) {
            html.append("cookie='");
            html.append(sessionid);
            html.append("';");
            html.append("d = new Date(); d.setTime(d.getTime()+(1000000));");
            html.append("document.cookie = 'JSESSIONID=' + cookie + '; expires=' + d.toGMTString() + '; path=/';");
        }
        html.append("</script><body style='margin:0px'>");
        if (header.length() > 0) {
            html.append("<div style='padding:5px;text-align:center'>");
            html.append(header);    //TODO: formatting, cleaning
            html.append("</div><br>");
        }
        html.append("<iframe style='border:none' src='");
        html.append(server).append("?p=").append(width).append(",").append(height).append(",").append(zoom).append(",").append(grid);
        if (query != null) {
            html.append(query);
        }

        //if printing put out scale factor
        if (resolution > 0) {
            html.append(",").append(scaleBy);
        }
        html.append("' width='");
        html.append(width);
        html.append("px' height='");
        html.append(height);
        html.append("px' /></body></html>");

        return html.toString();
    }

    private void makeHtmlFile() {
        //write html to load to a file
        try {
            FileWriter fw = new FileWriter(htmlpthfilename);
            fw.append(getHtmlContent(sessionid != null));
            fw.close();
        } catch (Exception e) {
            logger.error("error writing html file: " + htmlpthfilename, e);
        }
    }

    private void makeHtmlPreviewFile() {
        //write html to load to a file
        try {
            FileWriter fw = new FileWriter(htmlpthfilename);
            fw.append(getHtmlContent(false));
            fw.close();
        } catch (Exception e) {
            logger.error("error writing preview file: " + htmlpthfilename);
        }
    }

    private void makeImageOfHtmlFile() {
        int delay = 15000;
        if (resolution == 1) { //print resolution
            delay += 35000;
        }
        //TODO: dynamic path and settings
        String cmd = CommonData.wkhtmltoimage_cmd
                + " --debug-javascript"
                + " --load-error-handling ignore"
                + " --javascript-delay " + delay //delay 15s for tiles to load
                + " " + htmlurlfilename + " " + imgFilename;

        try {
            /* webpage to PNG */
            logger.debug("Running cmd: " + cmd);

            //delete any existing output files
            File img = new File(imgFilename);
            if (img.exists()) {
                img.delete();
            }

            Runtime runtime = Runtime.getRuntime();

            //wait for output file to be created (max 45s then retry twice)
            int retry = 0;
            long now;
            while (!img.exists() && retry < 3) {
                Process proc = runtime.exec(cmd);
                //StreamReaderThread srtError = new StreamReaderThread(proc.getErrorStream());
                //StreamReaderThread srtInput = new StreamReaderThread(proc.getInputStream());

                now = System.currentTimeMillis() + 45000;
                if (resolution == 1) { //print resolution
                    now += 25000;
                }
                while (now > System.currentTimeMillis() && !img.exists()) Thread.yield();

                //int exitVal = proc.waitFor(); //should work here but does not

                if (img.exists()) {
                    logger.debug("success (" + retry + ") cmd: " + cmd);
                    break;
                } else {
                    logger.debug("failed (" + retry + ") cmd: " + cmd);
                    proc.destroy();
                }
                retry++;
            }

            //wait 5 seconds to render (hope it is enough!)
            now = System.currentTimeMillis() + 5000;
            if (resolution == 1) { //print resolution
                now += 15000;
            }
            while (now > System.currentTimeMillis()) Thread.yield();

            return;
        } catch (Exception e) {
            logger.error("error producing map print sessionid:" + sessionid, e);
        }
    }

    private void makeConversionsOfImage() {
        File img = new File(imgFilename);
        if (!img.exists()) {
            return;
        }


        String[][] cmdsScreen = {
                {CommonData.convert_cmd, "-crop", width + "x" + height, imgFilename, imgFilename},
                {CommonData.convert_cmd, "-crop", width + "x" + height, imgFilename, jpgFilename},
                {CommonData.convert_cmd, imgFilename, pdfFilename}};

        String[][] cmdsPrint = {
                {CommonData.convert_cmd, "-crop", width + "x" + height, imgFilename, imgFilename},
                {CommonData.convert_cmd, "-crop", width + "x" + height, imgFilename, jpgFilename},
                {CommonData.convert_cmd, "-density", dpi + "x" + dpi, "-units", "pixelsperinch", imgFilename, pdfFilename}};

        try {
            String[][] cmds = cmdsScreen;
            if (resolution == 1) {
                cmds = cmdsPrint;
            }

            if (format.equals("png")) {
                cmds[1] = cmds[2] = null;
            }
            if (format.equals("jpg")) {
                cmds[0] = cmds[2] = null;
            }
            if (format.equals("pdf")) {
                cmds[1] = null;
            }

            for (String[] cmd : cmds) {
                if (cmd == null) {
                    continue;
                }
                logger.debug("running cmd: " + cmd[0] + " " + cmd[1] + " " + cmd[2]);

                Runtime runtime = Runtime.getRuntime();

                Process proc = runtime.exec(cmd, null, new File(this.htmlpth));
                StreamReaderThread srtInput = new StreamReaderThread(proc.getInputStream());
                StreamReaderThread srtError = new StreamReaderThread(proc.getErrorStream());

                //int exitVal = proc.waitFor(); //should work here

                long now = System.currentTimeMillis() + 1000;
                if (resolution == 1) { //print resolution
                    now += 2000;
                }
                while (now > System.currentTimeMillis()) Thread.yield();

                //manage for potential crop output file renaming
                try {
                    File f = new File(imgFilename.replace(".png", "-0.png"));
                    if (f.exists()) {
                        try {
                            new File(imgFilename).delete();
                        } catch (Exception e) {
                        }
                        FileUtils.moveFile(f, new File(imgFilename));
                    }
                    f = new File(jpgFilename.replace(".jpg", "-0.jpg"));
                    if (f.exists()) {
                        try {
                            new File(jpgFilename).delete();
                        } catch (Exception e) {
                        }
                        FileUtils.moveFile(f, new File(jpgFilename));
                    }
                } catch (Exception e) {
                }
            }

            return;
        } catch (Exception e) {
            logger.error("error transforming file for output format sessionid=" + sessionid, e);
        }
    }

    public String getPreviewUrl() {
        makeHtmlPreviewFile();
        return htmlurlfilename;
    }
}

class StreamReaderThread implements Runnable {
    private static Logger logger = Logger.getLogger(StreamReaderThread.class);

    Thread t;
    InputStream inputStream;

    public StreamReaderThread(InputStream is) {
        t = new Thread(this);
        inputStream = is;
        t.run();
    }

    @Override
    public void run() {
        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            isr = new InputStreamReader(inputStream);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                logger.info(line);
            }
        } catch (Exception e) {

            if (br != null) {
                try {
                    br.close();
                } catch (Exception ex) {
                    logger.error("failed to close buffered line reader", ex);
                }
            }

            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception ex) {
                    logger.error("failed to close input stream reader", ex);
                }
            }
        }
    }
}
