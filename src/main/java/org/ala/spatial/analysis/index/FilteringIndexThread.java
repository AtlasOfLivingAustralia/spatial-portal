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
class FilteringIndexThread implements Runnable {

    Thread t;
    LinkedBlockingQueue<String> lbq;
    int step;
    int[] target;
    String index_path;

    public FilteringIndexThread(LinkedBlockingQueue<String> lbq_, String index_path_) {
        t = new Thread(this);
        lbq = lbq_;
        index_path = index_path_;
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

            System.out.println("A*: " + next);

            FilteringIndex fi = new FilteringIndex(index_path);

            while (next != null) {
                /* update for this layer */
                fi.layersUpdate(next);

                /* report */
                System.out.println("D*: " + next);

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
