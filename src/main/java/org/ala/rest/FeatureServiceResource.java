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

        logger.finer(getRequest().getResourceRef().toString());


        //Legacy support for basic class details - when requested as /layer.json
        if (getRequest().getAttributes().containsKey("type") && !(getRequest().getAttributes().containsKey("id1"))) {
            layer = getRequest().getAttributes().get("layer").toString();
            return new GazetteerLayer(layer).getLegacyMap();
        }

        //TODO: refactor into separate resource object?
        if (getRequest().getResourceRef().toString().contains("features")) {
            logger.finer("request for feature list");
            FeatureList fl;
            layer = getRequest().getAttributes().get("layer").toString();
            if (getRequest().getAttributes().containsKey("page")) {
                int page = Integer.parseInt(getRequest().getAttributes().get("page").toString());
                System.out.println(getRequest().getAttributes().toString());
                fl =  new FeatureList(layer,page);
            }
            else
                fl =  new FeatureList(layer);
            if (getRequest().getAttributes().containsKey("format")) {
                    String format = (getRequest().getAttributes().get("format").toString());
                    fl.setFormat(format);
            }
            return fl.getMap();
        }


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
                logger.info("layer details have been requested");
                return new GazetteerLayer(layer).getMap();
            }
        } else {
            //everything else, we just render layer details (keep it simple)
            logger.info("gaz root has been requested - rendering layer details");
            return new GazetteerCapabilities().getJSONMap();
        }
    }
}
