package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.SpeciesListDTO;
import au.org.ala.spatial.dto.SpeciesListItemDTO;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zkoss.json.JSONValue;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
            return Integer.parseInt(jobject.get("listCount").toString());
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
            //permission to get private lists
            if (user != null) {
                get.addRequestHeader(StringConstants.COOKIE, "ALA-Auth=" + java.net.URLEncoder.encode(user, StringConstants.UTF_8));
            }

            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.TEXT_PLAIN);

            int result = client.executeMethod(get);
            if (result == 200) {
                String rawJSON = get.getResponseBodyAsString();
                JSONParser jp = new JSONParser();
                return (JSONObject) jp.parse(rawJSON);
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
        List list = new ArrayList<SpeciesListDTO>();
        try {
            JSONArray ja = (JSONArray) jobject.get("lists");

            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                SpeciesListDTO sli = new SpeciesListDTO();
                if (jo.containsKey("dataResourceUid"))
                    sli.setDataResourceUid(jo.get("dataResourceUid") == null ? "" : jo.get("dataResourceUid").toString());
                if (jo.containsKey("dateCreated"))
                    sli.setDateCreated(jo.get("dateCreated") == null ? "" : jo.get("dateCreated").toString());
                if (jo.containsKey("firstName"))
                    sli.setFirstName(jo.get("firstName") == null ? "" : jo.get("firstName").toString());
                if (jo.containsKey("fullName"))
                    sli.setFullName(jo.get("fullName") == null ? "" : jo.get("fullName").toString());
                if (jo.containsKey("itemCount"))
                    sli.setItemCount(Integer.parseInt(jo.get("itemCount") == null ? "0" : jo.get("itemCount").toString()));
                if (jo.containsKey("listName"))
                    sli.setListName(jo.get("listName") == null ? "" : jo.get("listName").toString());
                if (jo.containsKey("listType"))
                    sli.setListType(jo.get("listType") == null ? "" : jo.get("listType").toString());
                if (jo.containsKey("surname"))
                    sli.setSurname(jo.get("surname") == null ? "" : jo.get("surname").toString());
                if (jo.containsKey("username"))
                    sli.setUsername(jo.get("username") == null ? "" : jo.get("username").toString());

                list.add(sli);
            }
        } catch (Exception e) {
            LOGGER.error("error getting species lists", e);
        }

        return list;
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

                JSONParser jp = new JSONParser();
                JSONArray ja = (JSONArray) jp.parse(rawJSON);
                List list = new ArrayList<SpeciesListItemDTO>();

                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = (JSONObject) ja.get(i);
                    SpeciesListItemDTO sli = new SpeciesListItemDTO();
                    if (jo.containsKey("lsid") && jo.get("lsid") != null) {
                        sli.setLsid(jo.get("lsid").toString());
                    }
                    if (jo.containsKey("name") && jo.get("name") != null) {
                        sli.setName(jo.get("name").toString());
                    }

                    list.add(sli);
                }

                return list;
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

    public static String createNewList(String name, String items, String description, String url, String user, boolean makePrivate) {
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
                map.put("isPrivate", makePrivate);

                String content = JSONValue.toJSONString(map);
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
}
