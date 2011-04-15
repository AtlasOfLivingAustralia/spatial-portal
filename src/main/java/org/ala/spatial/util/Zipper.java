package org.ala.spatial.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author ajay
 */
public class Zipper {
    public static Map unzipFile(String name, InputStream data, String basepath) {
        try {
            Map output = new HashMap(); 
            String id = String.valueOf(System.currentTimeMillis());
            //String outputpath = "/data/ala/runtime/output/layers/" + id + "/";
            //String outputpath = "/Users/ajay/projects/tmp/useruploads/" + id + "/";
            String outputpath = basepath + id + "/"; 

            String zipfilename = name.substring(0, name.lastIndexOf("."));
            outputpath += zipfilename + "/";
            File outputDir = new File(outputpath);
            outputDir.mkdirs();

            ZipInputStream zis = new ZipInputStream(data);
            ZipEntry ze = null;
            String shpfile = "";
            String type = "";

            while ((ze = zis.getNextEntry()) != null) {
                System.out.println("ze.file: " + ze.getName());
                if (ze.getName().endsWith(".shp")) {
                    shpfile = ze.getName();
                    type = "shp";
                }
                String fname = outputpath + ze.getName();
                copyInputStream(zis, new BufferedOutputStream(new FileOutputStream(fname)));
                zis.closeEntry();
            }
            zis.close();

//            if (type.equalsIgnoreCase("shp")) {
//                System.out.println("Uploaded file is a shapefile. Loading...");
//                //loadUserShapefile(new File(outputpath + shpfile));
//            } else {
//                System.out.println("Unknown file type. ");
//                //showMessage("Unknown file type. Please upload a valid CSV, KML or Shapefile. ");
//            }

            output.put("type", type);
            output.put("file", outputpath + shpfile);

            return output;

        } catch (Exception e) {
            //showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user kml: ");
            e.printStackTrace(System.out);

        }

        return null; 
    }

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException, Exception {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) > -1) {
            out.write(buffer, 0, len);
        }

        // no need to close the input stream as it gets closed
        // in the caller function.
        // just close the output stream.
        out.close();

    }

    public static void zipDirectory(String dirpath, String outpath) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outpath));
            zipDir(dirpath, zos, dirpath);
            //close the stream
            zos.close();
        } catch (Exception e) {
            //handle exception
        }
    }

    private static void zipDir(String dir2zip, ZipOutputStream zos, String parentDir) {
        try {
            File zipDir = new File(dir2zip);
            //get a listing of the directory content
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(filePath, zos, parentDir);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String fileToAdd = f.getAbsolutePath().substring(parentDir.length()+1);
                ZipEntry anEntry = new ZipEntry(fileToAdd);
                System.out.println("adding: " + anEntry.getName());
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                //close the Stream
                fis.close();
            }
        } catch (Exception e) {
            //handle exception
        }
    }


}
