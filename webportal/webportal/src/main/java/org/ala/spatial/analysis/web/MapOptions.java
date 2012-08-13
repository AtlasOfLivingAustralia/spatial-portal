package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import java.util.List;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;

/**
 *
 * @author Adam
 */
public class MapOptions extends UtilityComposer {
    
    Radiogroup rgBaseMap;

    @Override
    public void afterCompose() {
        super.afterCompose();

        String baseMap = getMapComposer().getBaseMap();
        for(Radio r : (List<Radio>) rgBaseMap.getItems()) {
            if(r.getValue().equals(baseMap)) {
                rgBaseMap.setSelectedItem(r);
                break;
            }
        }
    }
}
