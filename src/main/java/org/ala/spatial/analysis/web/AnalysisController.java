package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;

/**
 * Controller class for the Analysis tab
 * 
 * @author ajay
 */
public class AnalysisController extends UtilityComposer {

    private static final String MENU_DEFAULT_WIDTH = "380px";
    private static final String MENU_MIN_WIDTH = "22px"; // 380px
    private static final String MENU_HALF_WIDTH = "50%";
    private static final String MENU_MAX_WIDTH = "100%";

    private Session sess = (Session) Sessions.getCurrent();



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
        mc.setWestWidth(MENU_MAX_WIDTH);
    }

    public void onSelect$samplingTab() {
        MapComposer mc = getThisMapComposer();
        mc.setWestWidth(MENU_HALF_WIDTH);
    }

    public void onSelect$maxentTab() {
        MapComposer mc = getThisMapComposer();
        mc.setWestWidth(MENU_HALF_WIDTH);
    }

    public void onSelect$alocTab() {
        MapComposer mc = getThisMapComposer();
        mc.setWestWidth(MENU_HALF_WIDTH);
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


}
