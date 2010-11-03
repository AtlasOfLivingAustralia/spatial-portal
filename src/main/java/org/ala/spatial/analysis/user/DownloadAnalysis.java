package org.ala.spatial.analysis.user;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.ala.spatial.util.CitationService;
import org.ala.spatial.util.TabulationSettings;

/**
 * Analysis the user downloaded zip files and sends the data
 * to the ALA Logger
 * 
 * @author ajay
 */
public class DownloadAnalysis {

    private static final int BUFFER = 2048;

    public DownloadAnalysis() {
    }

    private void analyseZip(String path) {
        try {

            ZipFile zf = new ZipFile(path);
            ZipInputStream in = new ZipInputStream(new FileInputStream(path));

            File theZip = new File(path);

            String outputdir = theZip.getAbsolutePath().substring(0, theZip.getAbsolutePath().lastIndexOf("/") + 1);


            // iterate till we find a csv file
            Enumeration entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String ename = entry.getName();

                ename = outputdir + ename;

                System.out.println("File is csv:  " + ename + " > " + (ename.endsWith(".csv")));
                if (ename.equalsIgnoreCase("samples.csv")) {

                    System.out.println("outputdir: " + outputdir);

                    int len = 0;
                    byte data[] = new byte[BUFFER];

                    BufferedOutputStream out = new BufferedOutputStream(
                            new FileOutputStream(ename), BUFFER);

                    while ((len = in.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, len);
                    }

                    out.close();

                    analyseFile(ename);

                    Vector drList = CitationService.getDataResources(ename, TabulationSettings.occurrences_dr_uid);
                    StringBuilder sbDp = new StringBuilder();
                    sbDp.append("[");
                    for (int i = 0; i < drList.size(); i++) {
                        if (i > 0) {
                            sbDp.append(",");
                        }
                        sbDp.append("\"").append((String) drList.get(i)).append("\"");
                    }
                    sbDp.append("]");

                    Map params = new HashMap();
                    


                    CitationService.postInfo(TabulationSettings.ala_logger_url, params, true);

                    break;
                }
            }

            in.close();

        } catch (Exception e) {
            System.out.println("Unable to analyse zip file:");
            e.printStackTrace(System.out);
        }
    }

    private void analyseFile(String path) {
    }

    public static void main(String[] args) {
        DownloadAnalysis da = new DownloadAnalysis();
        da.analyseZip("/Users/ajay/projects/tmp/Sample_20101017_1287292213876.zip");
    }
}
