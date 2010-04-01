package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;

/**
 * Controller class for the Analysis tab
 * 
 * @author ajay
 */
public class AnalysisController extends UtilityComposer {

    private Session sess = (Session) Sessions.getCurrent();

    @Override
    public void afterCompose() {
        super.afterCompose();
        try {
            Messagebox.show("hello world!!");
        } catch (InterruptedException ex) {
            Logger.getLogger(AnalysisController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
