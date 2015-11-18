/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.gazetteer.GazetteerAutoComplete;
import au.org.ala.spatial.util.CommonData;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;

;

/**
 * @author angus
 */
public class AreaRegionSelection extends AreaToolComposer {

    private static final Logger LOGGER = Logger.getLogger(AreaRegionSelection.class);
    private Button btnOk;
    private Hbox hbRadius;
    private Doublebox dRadius;
    private Checkbox displayAsWms;
    private GazetteerAutoComplete gazetteerAuto;

    public void onClick$btnOk(Event event) {
        Comboitem ci = gazetteerAuto.getSelectedItem();

        if (!validate()) {
            return;
        }

        //exit if no match found
        if (ci == null) {
            return;
        }

        JSONObject jo = ci.getValue();

        getMapComposer().addObjectByPid(jo.get(StringConstants.PID).toString(), ci.getLabel(), dRadius.getValue());

        ok = true;

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    /**
     * Adds the currently selected gazetteer feature to the map
     */
    public void onChange$gazetteerAuto() {

        Comboitem ci = gazetteerAuto.getSelectedItem();

        //when no item selected find an exact match from listed items
        if (ci == null) {
            String txt = gazetteerAuto.getText();
            for (Object o : gazetteerAuto.getItems()) {
                Comboitem c = (Comboitem) o;
                if (c.getLabel().equalsIgnoreCase(txt)) {
                    gazetteerAuto.setSelectedItem(c);
                    ci = c;
                    break;
                }
            }
        }

        if (ci == null) {
            btnOk.setDisabled(true);
        } else {
            btnOk.setDisabled(false);

            //specifically for the gazetteer layer point detection
            boolean point = false;
            if (ci.getDescription() != null && ci.getDescription().contains("Gazetteer")) {
                String[] s = ci.getDescription().split(",");
                try {
                    double lat = Double.parseDouble(s[2].trim());
                    point = !Double.isNaN(lat);
                } catch (NumberFormatException e) {
                    //not a double value
                }
            }

            if (point) {
                hbRadius.setVisible(true);
            } else {
                hbRadius.setVisible(false);
            }
        }
    }

    private boolean validate() {
        StringBuilder sb = new StringBuilder();

        double radius = dRadius.getValue();
        if (radius <= 0) {
            sb.append("\n").append(CommonData.lang(StringConstants.ERROR_INVALID_RADIUS));
        }

        if (sb.length() > 0) {
            getMapComposer().showMessage(sb.toString());
        }

        return sb.length() == 0;
    }
}
