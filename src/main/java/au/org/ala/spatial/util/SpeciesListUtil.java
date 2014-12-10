package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.SpeciesListDTO;
import au.org.ala.spatial.dto.SpeciesListItemDTO;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Provides some utility methods for working with species list.
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
@Component("listUtil")
public class SpeciesListUtil {

    private static final Logger LOGGER = Logger.getLogger(SpeciesListUtil.class);
    private static Map<String, String> speciesListMap = new java.util.HashMap<String, String>();

    public static Map<String, String> getSpeciesListMap() {
        return speciesListMap;
    }

    public static int getNumberOfPublicSpeciesLists(String user) {
        //TO DO retrive from lists.ala.org.au
        JSONObject jobject = getLists(user, 0, 0, null, null, null);
        if (jobject != null) {
            return jobject.getInt("listCount");
        } else {
            return 0;
        }
    }

    private static JSONObject getLists(String user, Integer offset, Integer max, String sort, String order, String searchTerm) {


        StringBuilder sb = new StringBuilder(CommonData.getSpeciesListServer());
        sb.append("/ws/speciesList");


        sb.append("?user=");
        if (user != null && !"guest@ala.org.au".equals(user)) {
            sb.append(user);
        }
        if (offset != null) {
            sb.append("&offset=").append(offset.toString());
        }
        if (max != null) {
            sb.append("&max=").append(max);
        }
        if (sort != null) {
            sb.append("&sort=").append(sort);
        }
        if (order != null) {
            sb.append("&order=").append(order);
        }
        if (searchTerm != null) {
            //sb.append("&listName=ilike:%25" + searchTerm + "%25");
            try {
                sb.append("&q=" + URLEncoder.encode(searchTerm, "UTF-8"));
            } catch (Exception e) {
            }
        }
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(sb.toString());
        try {

            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.TEXT_PLAIN);

            int result = client.executeMethod(get);
            if (result == 200) {
                String rawJSON = get.getResponseBodyAsString();

                return JSONObject.fromObject(rawJSON);
            } else {
                LOGGER.error("Unable to retrieve species list. " + result + " > " + get.getResponseBodyAsString());
            }

        } catch (Exception e) {
            LOGGER.error("Error retrieving public species list.", e);
        } finally {
            get.releaseConnection();
        }
        return null;
    }

    /**
     * Retrieves all the publically available species lists
     *
     * @return
     */
    public static Collection getPublicSpeciesLists(String user, Integer offset, Integer max, String sort, String order, String searchTerm, MutableInt listSize) {
        JSONObject jobject = getLists(user, offset, max, sort, order, searchTerm);
        JsonConfig cfg = new JsonConfig();
        if (listSize != null) {
            listSize.setValue(jobject.getInt("listCount"));
        }
        cfg.setPropertySetStrategy(new IgnoreUnknownPropsStrategyWrapper(PropertySetStrategy.DEFAULT));
        cfg.setRootClass(SpeciesListDTO.class);
        return JSONArray.toCollection(jobject.getJSONArray("lists"), cfg);

    }

    /**
     * Retrieves the items from the specified listUid
     *
     * @param listUid
     * @return
     */
    public static Collection<SpeciesListItemDTO> getListItems(String listUid) {
        String url = CommonData.getSpeciesListServer() + "/ws/speciesListItems/" + listUid;
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        try {

            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.TEXT_PLAIN);

            int result = client.executeMethod(get);
            if (result == 200) {
                String rawJSON = get.getResponseBodyAsString();

                return JSONArray.toCollection(JSONArray.fromObject(rawJSON), SpeciesListItemDTO.class);
            } else {
                LOGGER.error("Unable to retrieve species list items for " + listUid + ". " + result + " > " + get.getResponseBodyAsString());
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving list items.", e);
        } finally {
            get.releaseConnection();
        }
        return new ArrayList();
    }

    public static String createNewList(String name, String items, String description, String url, String user) {
        String postUrl = CommonData.getSpeciesListServer() + "/ws/speciesList/";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(postUrl);
        //set the cookie from the user
        try {
            post.setRequestHeader(StringConstants.COOKIE, "ALA-Auth=" + java.net.URLEncoder.encode(user, StringConstants.UTF_8));
        } catch (Exception e) {
            //should not happen as utf-8 is a supported encoding
            LOGGER.error("failed to encode user: " + user, e);
        }
        LOGGER.debug(post.getRequestHeader(StringConstants.COOKIE));

        if (name != null && items != null) {
            try {
                java.util.Map map = new java.util.HashMap();
                map.put(StringConstants.LISTNAME, name);
                if (description != null) {
                    map.put(StringConstants.DESCRIPTION, description);
                }
                if (url != null) {
                    map.put("url", url);
                }
                map.put("listItems", items);
                map.put("listType", "SPATIAL_PORTAL");
                String content = JSONObject.fromObject(map).toString();
                LOGGER.debug("create new list : " + content + " for user " + user);
                String contentType = StringConstants.APPLICATION_JSON;
                String charset = StringConstants.UTF_8;
                StringRequestEntity requestEntity = new StringRequestEntity(content, contentType, charset);
                post.setRequestEntity(requestEntity);
                int result = client.executeMethod(post);
                LOGGER.debug(result);
                LOGGER.debug(post.getResponseBodyAsString());

                if (result == 201) {
                    return post.getResponseHeader("druid").getValue();
                }

            } catch (Exception e) {
                LOGGER.error("Error uploading list", e);
            }
        }

        return null;
    }

    /**
     * We are caching the values that will change often. Used to display i18n values in area report etc..
     * <p/>
     * schedule to run every 12 hours
     */
    @Scheduled(fixedDelay = 43200000)
    public void reloadCache() {
        //get the number of lists
        int num = getNumberOfPublicSpeciesLists(null);
        int total = 0;
        int max = 50;
        Map<String, String> tmpMap = new java.util.HashMap<String, String>();
        while (total < num) {
            Collection<SpeciesListDTO> batch = getPublicSpeciesLists(null, total, max, null, null, null, null);
            for (SpeciesListDTO item : batch) {
                tmpMap.put(item.getDataResourceUid(), item.getListName());
                total++;
            }
            LOGGER.debug("Cached lists: " + tmpMap);
        }
        speciesListMap = tmpMap;
    }

    public static class IgnoreUnknownPropsStrategyWrapper extends PropertySetStrategy {

        private PropertySetStrategy original;

        public IgnoreUnknownPropsStrategyWrapper(PropertySetStrategy original) {
            this.original = original;
        }

        @Override
        public void setProperty(Object o, String string, Object o1) {
            try {
                original.setProperty(o, string, o1);
            } catch (JSONException ex) {
                //ignore
            }
        }
    }
}
