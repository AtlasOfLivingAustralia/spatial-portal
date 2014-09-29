package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.ShapeObjectDTO;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * Created by a on 5/05/2014.
 */
public final class UserShapes {
    private static final Logger LOGGER = Logger.getLogger(UserShapes.class);

    private UserShapes() {
        //to hide public constructor
    }

    public static String upload(String wkt, String name, String description, String userId, String apiKey) {

        String url = CommonData.getLayersServer() + "/shape/upload/wkt";

        try {
            ShapeObjectDTO obj = new ShapeObjectDTO(wkt, name, description, userId, apiKey);
            JSONObject jo = JSONObject.fromObject(obj);

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);

            post.setParameter("namesearch", StringConstants.FALSE);
            post.setRequestBody(jo.toString());

            post.setRequestHeader("Content-Type", StringConstants.APPLICATION_JSON);

            int result = client.executeMethod(post);
            if (result == 200) {
                JSONObject ja = JSONObject.fromObject(post.getResponseBodyAsString());
                if (ja.containsKey(StringConstants.ID)) {
                    return ja.getString(StringConstants.ID);
                }
            } else {
                LOGGER.debug(post.getResponseBodyAsString());
            }
        } catch (Exception e) {
            LOGGER.error("error uploading shape: " + url);
        }

        return null;
    }
}
