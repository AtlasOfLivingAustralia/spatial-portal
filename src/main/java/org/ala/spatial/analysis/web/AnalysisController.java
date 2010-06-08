package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;

import au.org.emii.portal.composer.UtilityComposer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zul.Window;
import org.ala.spatial.analysis.web.SamplingWCController;
import org.ala.spatial.analysis.web.MaxentWCController;
import org.ala.spatial.analysis.web.ALOCWCController;

/**
 * Controller class for the Analysis tab
 * 
 * @author ajay
 */
public class AnalysisController extends UtilityComposer {

    private static final String MENU_DEFAULT_WIDTH = "380px";
    private static final String MENU_MIN_WIDTH = "22px"; // 380px
    private static final String MENU_HALF_WIDTH = "30%";
    private static final String MENU_MAX_WIDTH = "100%";

    private Session sess = (Session) Sessions.getCurrent();
    
    private HtmlMacroComponent sf;
    private HtmlMacroComponent mf;
    private HtmlMacroComponent af;
    
    boolean samplingTabActive = false;	
	boolean maxentTabActive = false;
	boolean alocTabActive = true;		//default


    @Override
    public void afterCompose() {
        super.afterCompose();
        try {
            //Messagebox.show("hello SAT world!!");
        } catch (Exception ex) {
            Logger.getLogger(AnalysisController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void onSelect$filteringTab() {
        MapComposer mc = getThisMapComposer();
        mc.setWestWidth(MENU_HALF_WIDTH);
    }

    public void onSelect$samplingTab() {
        MapComposer mc = getThisMapComposer();
        mc.setWestWidth(MENU_HALF_WIDTH);
    }
    
    public void onClick$samplingTab() {
    	samplingTabActive = true;
    	maxentTabActive = false;
    	alocTabActive = false;
    	((SamplingWCController)sf.getFellow("samplingwindow")).callPullFromActiveLayers();
    }

    public void onSelect$maxentTab() {
        MapComposer mc = getThisMapComposer();
        mc.setWestWidth(MENU_HALF_WIDTH);
    }
    
    public void onClick$maxentTab() {    
    	samplingTabActive = false;
    	maxentTabActive = true;
    	alocTabActive = false;
        ((MaxentWCController)mf.getFellow("maxentwindow")).callPullFromActiveLayers();
    }

    public void onSelect$alocTab() {
    	samplingTabActive = false;
    	maxentTabActive = false;
    	alocTabActive = true;
        MapComposer mc = getThisMapComposer();
        mc.setWestWidth(MENU_HALF_WIDTH);
        
        ((ALOCWCController)af.getFellow("alocwindow")).callPullFromActiveLayers();
    }

     /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    public void callPullFromActiveLayers() {
    	if (samplingTabActive) {
    		((SamplingWCController)sf.getFellow("samplingwindow")).callPullFromActiveLayers();
    	} else if(maxentTabActive) {
    		((MaxentWCController)mf.getFellow("maxentwindow")).callPullFromActiveLayers();
    	} else if(alocTabActive) {
    		((ALOCWCController)af.getFellow("alocwindow")).callPullFromActiveLayers();
    	}
    }
}
