package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author ajay
 */
public final class Zipper {
    private static final Logger LOGGER = Logger.getLogger(Zipper.class);

    private Zipper() {
        //to hide public constructor
    }

    public static Map unzipFile(String name, InputStream data, String basepath) {
        try {
            Map output = new HashMap();
            String id = String.valueOf(System.currentTimeMillis());
            String outputpath = basepath + id + "/";

            String zipfilename = name.substring(0, name.lastIndexOf('.'));
            outputpath += zipfilename + "/";
            File outputDir = new File(outputpath);
            outputDir.mkdirs();

            ZipInputStream zis = new ZipInputStream(data);
            ZipEntry ze;
            String shpfile = "";
            String type = "";

            while ((ze = zis.getNextEntry()) != null) {
                String fname = outputpath + ze.getName();
                File destFile = new File(fname);
                if (destFile.isHidden()) {
                    continue;
                }
                destFile.getParentFile().mkdirs();
                LOGGER.debug("ze.file: " + ze.getName());
                if (ze.getName().endsWith(".shp")) {
                    shpfile = ze.getName();
                    type = "shp";
                }
                if (!ze.isDirectory()) {
                    copyInputStream(zis, new BufferedOutputStream(new FileOutputStream(fname)));
                }
                zis.closeEntry();
            }
            zis.close();

            output.put(StringConstants.TYPE, type);
            output.put(StringConstants.FILE, outputpath + shpfile);

            return output;

        } catch (Exception e) {
            LOGGER.error("unable to load user kml: ", e);

        }

        return null;
    }

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
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
            int bytesIn;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(filePath, zos, parentDir);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String fileToAdd = f.getName();
                ZipEntry anEntry = new ZipEntry(fileToAdd);
                LOGGER.debug("zipDirAdd: " + anEntry.getName());
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
