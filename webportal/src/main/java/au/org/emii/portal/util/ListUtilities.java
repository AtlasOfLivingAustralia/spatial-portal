/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.AbstractIdentifier;
import java.util.List;

/**
 *
 * @author geoff
 */
public class ListUtilities {
    /**
     * Simple searching in a list.  Does not support recursive inspection
     * @param <T>
     * @param id
     * @param search
     * @return
     */
    public <T extends AbstractIdentifier> T findInList(String id, List<? extends T> search) {
        T found = null;
        T inspect = null;
        String inspectId = null;
        int i = 0;
        if ((search != null) && (id != null)) {
            while ((found == null) && (i < search.size())) {
                inspect = search.get(i);
                if (inspect != null) {
                    inspectId = inspect.getId();
                    if (inspectId != null && inspectId.equals(id)) {
                        found = inspect;
                    }
                }
                i++;
            }
        }

        return found;

    }
}
