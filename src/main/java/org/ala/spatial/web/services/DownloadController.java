/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import javax.servlet.http.HttpServletResponse;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Zipper;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/download/")
public class DownloadController {

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public void test() {
        System.out.println("Testing download..");
    }

    @RequestMapping(value = "/{pid}", method = RequestMethod.GET)
    @ResponseBody
    public String download(@PathVariable String pid, HttpServletResponse response) {
        try {

            File dir = findFile(pid);

            if (dir != null) {
                //System.out.println("Found session data: " + dir.getAbsolutePath());
                //return "Found session data: " + dir.getAbsolutePath();

                String zipfile = dir.getParent() + "/" + pid + ".zip";
                Zipper.zipDirectory(dir.getAbsolutePath(), zipfile);

                System.out.println("Found " + dir.getName() + " in " + dir.getParent() + " and zipped at: " + zipfile);
                //return "Found " + dir.getName() + " in " + dir.getParent() + " and zipped at: " + zipfile;

                String parentName = "ALA_";
                String parentPath = dir.getParent().substring(dir.getParent().lastIndexOf("/")+1);
                if ("maxent".equals(parentPath)) {
                    parentName = "ALA_Prediction_";
                } else if ("sampling".equals(parentPath)) {
                    parentName = "ALA_Species_Samples_";
                } else if ("layers".equals(parentPath) || "aloc".equals(parentPath)) {
                    parentName = "ALA_Classification_";
                } else if ("gdm".equals(parentPath)) {
                    parentName = "ALA_GDM_";
                } else if ("filtering".equals(parentPath)) {
                    parentName = "ALA_EnvFilter_";
                } else if ("sitesbyspecies".equals(parentPath)) {
                    parentName = "ALA_SitesBySpecies_";
                }

                File file = new File(zipfile); 
                response.setContentType("application/zip");
                response.setContentLength(safeLongToInt(file.length()));
                response.setHeader("Content-Disposition", "attachment; filename=\"" + parentName + pid + ".zip\"");

                FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());

                return null;

            } else {
                System.out.println("Could not find session data");
                return "Could not find session data";
            }

        } catch (Exception e) {
            System.out.println("Unable to download:");
            e.printStackTrace(System.out);
        }

        return "";
    }

    private File findFile(String pid) {
        try {
            System.out.println("Looking for: " + pid + " to downloadd");

            // the 'pid's are unique, so lets figure out
            // which directory they live under.

            String basedir = TabulationSettings.base_output_dir + File.separator + "output" + File.separator;
            File baseDir = new File(basedir);
            FilenameFilter ff = DirectoryFileFilter.DIRECTORY;
            File[] files = baseDir.listFiles(ff);
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isDirectory() && !f.getName().equalsIgnoreCase("layers")) {
                    if (f.getName().equalsIgnoreCase(pid)) {
                        return f;
                    } else {
                        File[] files2 = f.listFiles(ff);
                        for (int j = 0; j < files2.length; j++) {
                            File f2 = files2[j];
                            if (f2.getName().equalsIgnoreCase(pid)) {
                                return f2;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error finding session data:");
            e.printStackTrace(System.out);
        }

        return null;
    }

    private int safeLongToInt(long l) {
    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
        throw new IllegalArgumentException
            (l + " cannot be cast to int without changing its value.");
    }
    return (int) l;
}
}
