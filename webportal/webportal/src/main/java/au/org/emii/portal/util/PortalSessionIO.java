/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.session.PortalSession;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Adam
 */
public class PortalSessionIO {

    /**
     * Write a portal session to a file.
     *
     * @param filename
     */
    static public String writePortalSession(PortalSession ps, String path, String id) {
        if (id == null) {
            id = String.valueOf(System.currentTimeMillis());
        }

        try {
            File file = new File(path + id + ".session");

            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(ps);
            oos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return id;
    }

    /**
     * Read a portal session from a file.
     *
     * @param filename
     * @return new PortalSession
     */
    static public PortalSession readPortalSession(String path, String id) {
        PortalSession ps = null;
        try {
            File file = new File(path + id + ".session");

            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            ps = (PortalSession) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ps;
    }


}
