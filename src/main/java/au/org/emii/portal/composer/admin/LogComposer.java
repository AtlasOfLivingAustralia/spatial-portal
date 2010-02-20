/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.composer.admin;

import au.org.emii.portal.Validate;
import au.org.emii.portal.composer.GenericAutowireAutoforwardComposer;
import au.org.emii.portal.lang.LanguagePack;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

/**
 *
 * @author geoff
 */
public class LogComposer extends GenericAutowireAutoforwardComposer {

    /**
     * Filename to read catalina.out from - tomcat specific
     */
    private final String TOMCAT_LOG_DIR = System.getProperty("catalina.base") + "/logs/";

    /**
     * Log output for catalina.out - zk autowired
     */
    private Textbox output = null;

    /**
     * Log switcher control - zk autowired
     */
    private Listbox selectedLogfile = null;

    /**
     * Filename of selected log file
     */
    private String selectedLogFileName = null;

    /**
     * See what files are available in the log directory
     */
    private String[] availableLogfiles = new File(TOMCAT_LOG_DIR).list();

    /**
     * Error message - file unreadable - zk autwired
     */
    private Label errorFileUnreadable = null;

    /**
     * Error message - no log file selected - zk autowired
     */
    private Label errorNoLogfileSelected = null;

    /**
     * Error message to display when there are no tomcat logs found - zk autowired
     */
    private Label errorNoLogfiles = null;

    private LanguagePack languagePack = null;

    /**
     * Refresh button - re-read selected log file
     */
    public void onClick$refresh() {
        updateLog();
    }

    /**
     * Update log output for selected file
     */
    public void onSelect$selectedLogfile() {
        selectedLogFileName = (String) selectedLogfile.getSelectedItem().getValue();
        logger.info("selected log file: " + selectedLogFileName);
        updateLog();
    }

    private void hideMessages() {
        errorFileUnreadable.setVisible(false);
        errorNoLogfileSelected.setVisible(false);
    }

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (availableLogfiles.length > 0) {
            // load the available files into the drop down
            selectedLogfile.setModel(new SimpleListModel(availableLogfiles));

            // select the first file (only affects ui)
            selectedLogfile.setSelectedItem(selectedLogfile.getItemAtIndex(0));

            // get the log display to update
            selectedLogFileName = availableLogfiles[0];
            updateLog();
        } else {
            errorNoLogfiles.setValue(languagePack.getCompoundLang("admin_log_view_no_files", new Object[] {TOMCAT_LOG_DIR}));
        }
    }


    /**
     * Re-read the selected log file and update the display
     */
    private void updateLog() {
        hideMessages();

        if (selectedLogFileName == null) {
            errorNoLogfileSelected.setVisible(true);
        } else {
            String filename = TOMCAT_LOG_DIR + selectedLogFileName;
            try {
                String log = FileUtils.readFileToString(new File(filename));
                if (Validate.empty(log)) {
                    output.setValue("*** This file is empty ***");
                } else {
                    output.setValue(log);
                }
            } catch (IOException ex) {
                errorFileUnreadable.setVisible(true);
                logger.info(String.format(
                        "unable to read '%s': %s - are you using tomcat? If not, we do not support log output yet",
                        filename, ex.getMessage()));
            }
        }
    }

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }


}
