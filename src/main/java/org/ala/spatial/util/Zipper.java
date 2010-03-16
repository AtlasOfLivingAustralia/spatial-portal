package org.ala.spatial.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zipper helper class to zip up files.
 * Quick zipper helper class
 * 
 * @author ajay
 */
public class Zipper {

    /**
     * zipFile method for zipping a single file. Provide an zip filename. 
     * 
     * @param infile Input file to be zipped
     * @param outfile Output zipped filename
     */
    public static void zipFile(String infile, String outfile) {
        String[] infiles = {infile};
        zipFiles(infiles, outfile);
    }

    /**
     * zipFile method for zipping a single file. Output filename generated
     * based on the input file
     * 
     * @param infile Input file to be zipped
     * @return Output zipped filename
     */
    public static String zipFile(String infile) {
        String outfile = infile + ".zip";

        String[] infiles = {infile};
        zipFiles(infiles, outfile);

        return outfile;
    }

    /**
     * zipFiles method to zip a bunch of files. Output filename to be provided.
     * 
     * @param infiles Input files to be zipped
     * @param outfile Output zipped filename
     */
    public static void zipFiles(String[] infiles, String outfile) {
        byte[] buf = new byte[1024];
        try {
            // Create the ZIP file
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outfile));
            // Compress the files
            for (int i = 0; i < infiles.length; i++) {
                File f = new File(infiles[i]);
                FileInputStream in = new FileInputStream(f); 
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(f.getName()));
                // Transfer bytes from the file to the ZIP file
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                // Complete the entry
                out.closeEntry();
                in.close();
            }
            // Complete the ZIP file
            out.close();
        } catch (IOException e) {
        }

    }

    /**
     * Run as a separate class
     * 
     * @param args command line or method inputs 
     */
    public static void main(String[] args) {
        String infile = "/Users/ajay/projects/ala/code/alageospatialportal/alaspatial/target/ala-spatial-1.0-SNAPSHOT/output/maxent/F3F24BCAEE7732FBD6D93E21A4B65E81/species.asc";
        String outfile = "";

        outfile = zipFile(infile);
        System.out.println("outfile: " + outfile);
    }
}
