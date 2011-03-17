package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.event.ForwardEvent;

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
    private SettingsSupplementary settingsSupplementary = null;
    //private HtmlMacroComponent speciesListForm;
    private HtmlMacroComponent scatterplotForm;
    private HtmlMacroComponent asf;
    private HtmlMacroComponent mf;
    private HtmlMacroComponent af;
    private HtmlMacroComponent sf;
    //boolean speciesListTabActive = false;
    boolean samplingTabActive = true;   //TODO: tie to default in .zul
    boolean maxentTabActive = false;
    boolean alocTabActive = false;
    boolean scatterplotActive = false;

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary == null) {
            if (getMapComposer() != null) {
                settingsSupplementary = getMapComposer().getSettingsSupplementary();
            }
        }
    }

    public void onActivateLink(ForwardEvent event) {
        getMapComposer().onActivateLink(event);
    }

//    public void onSelect$speciesListTab() {
//        getMapComposer().setWestWidth(MENU_HALF_WIDTH);
//    }
    public void onSelect$filteringTab() {
        //getMapComposer().setWestWidth(MENU_HALF_WIDTH);
    }

    public void onSelect$samplingTab() {
        //getMapComposer().setWestWidth(MENU_HALF_WIDTH);
    }

//    public void onClick$speciesListTab() {
//        speciesListTabActive = true;
//        samplingTabActive = false;
//        maxentTabActive = false;
//        alocTabActive = false;
//        scatterplotActive = false;
//        ((FilteringResultsWCController) speciesListForm.getFellow("popup_results")).refreshCount();
//    }
    public void onClick$samplingTab() {
        //speciesListTabActive = false;
        samplingTabActive = true;
        maxentTabActive = false;
        alocTabActive = false;
        scatterplotActive = false;
        ((SamplingWCController) asf.getFellow("samplingwindow")).callPullFromActiveLayers();
    }

    public void onSelect$maxentTab() {
        //getMapComposer().setWestWidth(MENU_HALF_WIDTH);
    }

    public void onClick$maxentTab() {
        //speciesListTabActive = false;
        samplingTabActive = false;
        maxentTabActive = true;
        alocTabActive = false;
        scatterplotActive = false;
        ((MaxentWCController) mf.getFellow("maxentwindow")).callPullFromActiveLayers();
    }

    public void onSelect$alocTab() {
        //speciesListTabActive = false;
        samplingTabActive = false;
        maxentTabActive = false;
        alocTabActive = true;
        scatterplotActive = false;
        //getMapComposer().setWestWidth(MENU_HALF_WIDTH);

        ((ALOCWCController) af.getFellow("alocwindow")).callPullFromActiveLayers();
    }

    public void onSelect$scatterplotTab() {
        //speciesListTabActive = false;
        samplingTabActive = false;
        maxentTabActive = false;
        alocTabActive = false;
        scatterplotActive = true;
        //getMapComposer().setWestWidth(MENU_HALF_WIDTH);

        ((ScatterplotWCController) scatterplotForm.getFellow("scatterplotwindow")).callPullFromActiveLayers();
    }

    public void callPullFromActiveLayers() {
        ((SelectionController) sf.getFellow("selectionwindow")).checkForAreaRemoval();
        ((SelectionController) sf.getFellow("selectionwindow")).updateActiveAreaInfo();
        if (samplingTabActive) {
            ((SamplingWCController) asf.getFellow("samplingwindow")).callPullFromActiveLayers();
        } else if (maxentTabActive) {
            ((MaxentWCController) mf.getFellow("maxentwindow")).callPullFromActiveLayers();
        } else if (alocTabActive) {
            ((ALOCWCController) af.getFellow("alocwindow")).callPullFromActiveLayers();
        } else if (scatterplotActive) {
            ((ScatterplotWCController) scatterplotForm.getFellow("scatterplotwindow")).callPullFromActiveLayers();
        }
    }

    public HtmlMacroComponent getSelectionHtmlMacroComponent() {
        return sf;
    }
}
