/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Adam
 */
public class SaveHttpServletRequest {

    static LinkedBlockingQueue<HttpServletRequestCopy> lbq = new LinkedBlockingQueue<HttpServletRequestCopy>();
    static SaveHttpServletRequestConsumer shsrc = null;

    public static void put(HttpServletRequest req) {
        try {
            //copy
            HttpServletRequestCopy copy = new HttpServletRequestCopy(req);
            
            //queue for save
            lbq.put(copy);

            //start consumer if required
            if (shsrc == null) {
                shsrc = new SaveHttpServletRequestConsumer(lbq);
                shsrc.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class HttpServletRequestCopy {
    //TODO: data here

    public HttpServletRequestCopy(HttpServletRequest req){
        //data variable = req.getSomething()
    }
}

class SaveHttpServletRequestConsumer extends Thread {

    LinkedBlockingQueue<HttpServletRequestCopy> lbq;

    public SaveHttpServletRequestConsumer(LinkedBlockingQueue<HttpServletRequestCopy> lbq_) {
        lbq = lbq_;
    }

    @Override
    public void run() {
        while (true) {
            try {
                save(lbq.take());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void save(HttpServletRequestCopy req) {
        try {
            File file = File.createTempFile("httpReq" + String.valueOf(System.currentTimeMillis()), ".dat");

            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(req);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
