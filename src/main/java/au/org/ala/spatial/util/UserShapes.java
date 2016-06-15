package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.ShapeObjectDTO;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.json.JSONValue;

import java.util.HashMap;
import java.util.Map;

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
            Map m = new HashMap();
            m.put("wkt", wkt);
            m.put("name", name);
            m.put("description", description);
            m.put("api_key", apiKey);
            m.put("user_id", userId);

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);

            post.setParameter("namesearch", StringConstants.FALSE);
            post.setRequestBody(JSONValue.toJSONString(m));

            post.setRequestHeader("Content-Type", StringConstants.APPLICATION_JSON);

            int result = client.executeMethod(post);
            if (result == 200) {
                JSONParser jp = new JSONParser();
                JSONObject ja = (JSONObject) jp.parse(post.getResponseBodyAsString());
                if (ja.containsKey(StringConstants.ID)) {
                    return ja.get(StringConstants.ID).toString();
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
