package au.org.emii.portal.composer.legend;

import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;

/**
 * @author Adam
 */
public class MapOptions extends UtilityComposer {

    Radiogroup rgBaseMap;

    @Override
    public void afterCompose() {
        super.afterCompose();

        String baseMap = getMapComposer().getBaseMap();
        for (Radio r : rgBaseMap.getItems()) {
            if (r.getValue().equals(baseMap)) {
                rgBaseMap.setSelectedItem(r);
                break;
            }
        }
    }
}
