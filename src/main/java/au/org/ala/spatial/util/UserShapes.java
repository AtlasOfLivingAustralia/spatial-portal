package au.org.ala.spatial.util;

import au.org.ala.spatial.composer.results.DistributionsController;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import au.org.ala.spatial.dto.ShapeObjectDTO;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.apache.commons.httpclient.NameValuePair;

import java.util.ArrayList;

/**
 * Created by a on 5/05/2014.
 */
public class UserShapes {
    private static Logger logger = Logger.getLogger(UserShapes.class);

    public static String upload(String wkt, String name, String description, String user_id, String api_key) {

        String url = CommonData.layersServer + "/shape/upload/wkt";

        try {
            ShapeObjectDTO obj = new ShapeObjectDTO(wkt,name,description,user_id,api_key);
            JSONObject jo = JSONObject.fromObject(obj);

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);

            post.setParameter("namesearch","false");
            post.setRequestBody(jo.toString());

            post.setRequestHeader("Content-Type", "application/json");

            int result = client.executeMethod(post);
            if (result == 200) {
                JSONObject ja = JSONObject.fromObject(post.getResponseBodyAsString());
                ArrayList<String> found = new ArrayList();

                if (ja.containsKey("id")) {
                    return ja.getString("id");
                }
            } else {
                logger.debug(post.getResponseBodyAsString());
            }
        } catch (Exception e) {
            logger.error("error uploading shape: " + url);
        }

        return null;
    }
}
