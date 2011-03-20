/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.zk;

import au.com.bytecode.opencsv.CSVReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

/**
 *
 * @author ajay
 */
public class LogsZK extends GenericForwardComposer {

    private Label lblMessage;
    private Button refresh;
    private Grid grid;
    ArrayList logList = new ArrayList();
    String[] headers = {"date", "userip", "useremail", "processid", "sessionid", "actiontype", "lsid", "layers", "method", "params", "downloadfile", "message"};

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        //lblMessage.setValue("Log Analyser");
        System.out.println("Calling loadLogs");

        //grid.setVflex(true);
        //grid.setMold("paging");        

        loadLogs();
    }

    public void onClick$refresh(Event e) {
        System.out.println("Refreshing logs");
        //grid.setMold(null);
        //grid.getChildren().clear();
        logList.clear(); 
        //grid.setMold("paging");
        loadLogs();

    }

    private void loadLogs() {
        try {
            File logsDir = new File("/data/logs/"); // /Library/Tomcat/Home/logs/

            FileFilter ff = new WildcardFileFilter("useractions.*"); // useractions.log.2011-01-1*
            File[] files = logsDir.listFiles(ff);
            System.out.println("Data...");
            for (int i = 0; i < files.length; i++) {
                System.out.println(files[i]);

                CSVReader reader = new CSVReader(new FileReader(files[i]));
                logList.addAll(reader.readAll());

            }

            ListModelList lm = new ListModelList(logList);
            grid.setModel(lm);
            grid.setRowRenderer(new RowRenderer() {

                @Override
                public void render(Row row, Object data) throws Exception {
                    String[] recs = (String[]) data;
                    for (int i = 0; i < recs.length; i++) {
                        String col = recs[i].trim();
                        if (col.startsWith("\"")) {
                            col = col.substring(1); 
                        }
                        new Label(col).setParent(row);
                    }

                }
            });


        } catch (Exception e) {
            System.out.println("Error loading logs: ");
            e.printStackTrace(System.out);
        }
    }

}
