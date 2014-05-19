/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import org.zkoss.zk.ui.event.EventListener;

/**
 * @author adam
 */
public class Action {

    public String label;
    public EventListener eventListener;

    //defaults
    public Action(String label, EventListener eventListener) {
        this.label = label;
        this.eventListener = eventListener;
    }
}
