package org.ala.rest;

import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.rest.MapResource;
import org.geotools.util.logging.Logging;

/**
 * This class handles requests for gazetteer features and returns a Map which
 * is serialized as GeoJSON
 * @author angus
 */
public class FeatureServiceResource extends MapResource {

    private static final Logger logger = Logging.getLogger("org.ala.rest.FeatureServiceResource");

    @Override
    public Map getMap() throws Exception {
        logger.info("Feature Service" + getRequest().getAttributes().toString());

        String help = null;
        String layer = null;
        String id1 = null;
        String id2 = null;


        if (getRequest().getAttributes().containsKey("layer")) {
            layer = getRequest().getAttributes().get("layer").toString();
            logger.finer("layer supplied is " + layer);
            if (getRequest().getAttributes().containsKey("id1")) {
                id1 = getRequest().getAttributes().get("id1").toString();
                logger.finer("id1 supplied is " + id1);
                if (getRequest().getAttributes().containsKey("id2")) {
                    id2 = getRequest().getAttributes().get("id2").toString();
                    logger.finer("id2 supplied is " + id2);
                    return new GazetteerFeature(layer, id1, id2).getJSONMap();
                } else {
                    return new GazetteerFeature(layer, id1).getJSONMap();
                }

            } else {
                // TODO: Handles layer only requests
                logger.info("layer details have been requested");
                return new GazetteerLayer(layer).getMap();
            }
        } else if (getRequest().getAttributes().containsKey("help")) {
            if (getRequest().getAttributes().get("help").toString().compareTo("gazetteer") == 0) // TODO: Handle root request
            {
                logger.info("gaz root has been requested - do something smart");
            }
            return null;
        } else {
            logger.severe("We can't handle this ...");
            return null;
        }
    }
}
