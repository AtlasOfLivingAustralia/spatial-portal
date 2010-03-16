package org.ala.spatial.web;

import java.util.Arrays;
import java.util.Map;
import org.ala.spatial.analysis.maxent.MaxentSettings;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.Initiator;


/**
 *
 * @author ajay
 */
public class Maxent2Controller implements Initiator {

    @Override
    public void doInit(Page page, Map args) throws Exception {
        String el = "Hello world";
        MaxentSettings msets = new MaxentSettings();
        msets.setMaxentPath("maxent apth");
        msets.setEnvList(Arrays.asList(el.split("")));
        msets.setRandomTestPercentage(0);
        msets.setEnvPath("/path/to/env/vars/10minutes/");
        msets.setEnvVarToggler("world");
        msets.setEnvPrefix("world_10_bio");
        page.setVariable("msets", msets.toString()); 

    }

    @Override
    public void doAfterCompose(Page page) throws Exception {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean doCatch(Throwable ex) throws Exception {
        //throw new UnsupportedOperationException("Not supported yet.");
        return true; 
    }

    @Override
    public void doFinally() throws Exception {
        //throw new UnsupportedOperationException("Not supported yet.");
    }



}
