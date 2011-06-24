/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Adam
 */
public class SamplingIndexThread implements Runnable {

    Thread t;
    LinkedBlockingQueue<String> lbq;
    String index_path;
    double[][] points;

    public SamplingIndexThread(LinkedBlockingQueue<String> lbq_, String index_path_, double[][] points_) {
        t = new Thread(this);
        lbq = lbq_;
        index_path = index_path_;
        points = points_;
        t.start();
    }

    public void run() {

        /* get next batch */
        String next;
        try {
            synchronized (lbq) {
                if (lbq.size() > 0) {
                    next = lbq.take();
                } else {
                    next = null;
                }
            }

            SamplingIndex si = new SamplingIndex(index_path, points);

            while (next != null) {
                /* update for this layer */
                si.layersUpdate(next);

                /* get next available */
                synchronized (lbq) {
                    if (lbq.size() > 0) {
                        next = lbq.take();
                    } else {
                        next = null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAlive() {
        return t.isAlive();
    }
}
