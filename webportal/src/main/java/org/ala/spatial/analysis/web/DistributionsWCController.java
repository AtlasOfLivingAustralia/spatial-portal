package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.UtilityComposer;
import java.io.StringReader;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;

/**
 *
 * @author ajay
 */
public class DistributionsWCController extends UtilityComposer {
    public FilteringResultsWCController parent = null;
    Label distributionLabel;
    Listbox distributionListbox;

    public void onClick$btnDownload(Event event) {
        parent.onClick$sdDownload(event);
    }

    public void init(FilteringResultsWCController p) {
        parent = p;
        if (parent.speciesDistributionText != null) {
            distributionLabel.setValue("found " + parent.sdLabel.getValue() + " Expert Distributions in the Active Area");
            distributionListbox.setItemRenderer(new ListitemRenderer() {

                @Override
                public void render(Listitem li, Object data) {
                    StringReader sreader = new StringReader((String) data);
                    CSVReader reader = new CSVReader(sreader);
                    try {
                        String[] cells = reader.readNext(); //only one row

                        for (int i = 0; i < cells.length; i++) {
                            Listcell lc = new Listcell(cells[i]);
                            lc.setStyle("white-space: normal;");
                            lc.setParent(li);
                        }

                        reader.close();
                    } catch (Exception e) {
                    }
                    sreader.close();
                }
            });
            distributionListbox.setModel(new SimpleListModel(parent.speciesDistributionText));
        }
    }    
}
