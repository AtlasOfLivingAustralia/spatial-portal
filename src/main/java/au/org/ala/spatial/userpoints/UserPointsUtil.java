/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.ala.spatial.userpoints;

import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.QueryField;
import au.org.ala.spatial.data.UploadQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.session.PortalSession;

import java.util.ArrayList;
import java.util.List;

/**
 * @author a
 */
public class UserPointsUtil {
    public static void addAnalysisLayerToUploadedCoordinates(PortalSession portalSession, String fieldId, String displayName) {
        ArrayList<QueryField> f = CommonData.getDefaultUploadSamplingFields();
        f.add(new QueryField(fieldId, displayName, QueryField.FieldType.AUTO));

        //identify all user uploaded coordinate layers
        List<MapLayer> allLayers = portalSession.getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            Query q = allLayers.get(i).getSpeciesQuery();
            if (q != null && q instanceof UploadQuery) {
                q.sample(f);
                ((UploadQuery) q).resetOriginalFieldCount(-1);
            }
        }
    }
}
