package org.ala.spatial.data;


import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;

import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.apache.commons.httpclient.Cookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zkoss.zk.ui.Executions;

/**
 * 
 * Provides some utility methods for working with species list.  
 * 
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 *
 */
@Component("listUtil")
public class SpeciesListUtil {
  
    private static Collection<SpeciesListDTO> publicSpeciesLists = null;
    private final static Logger logger = Logger.getLogger(SpeciesListUtil.class);
    private static Map<String,String> speciesListMap = new java.util.HashMap<String,String>();
    
    /**
     * We are caching the values that will change often. Used to display i18n values in area report etc..
     */
    @Scheduled(fixedDelay = 43200000)// schedule to run every 12 hours
    public void reloadCache(){
        //get the number of lists
        int num = getNumberOfPublicSpeciesLists(null);
        int total = 0;
        int max=50;
        Map<String,String> tmpMap = new java.util.HashMap<String,String>();
        while(total<num){
            Collection<SpeciesListDTO> batch = getPublicSpeciesLists(null, total, max, null, null);
            for(SpeciesListDTO item:batch){
                tmpMap.put(item.getDataResourceUid(), item.getListName());
                total++;
            }
            logger.debug("Cached lists: " + tmpMap);            
        }
        speciesListMap=tmpMap;
    }
    public static Map<String,String> getSpeciesListMap(){
        return speciesListMap;
    }
    public static int getNumberOfPublicSpeciesLists(String user){
        //TO DO retrive from lists.ala.org.au
        JSONObject jobject = getLists(user, 0,0,null,null);
        if(jobject != null)
            return jobject.getInt("listCount");
        else
            return 0;
    }
    
    private static JSONObject getLists(String user,Integer offset, Integer max, String sort, String order){
        //String url = CommonData.speciesListServer + "/ws/speciesList" ;
        StringBuilder sb = new StringBuilder(CommonData.speciesListServer);
        sb.append("/ws/speciesList");
        
        
        sb.append("?user=");
        if(user != null){
            sb.append(user);        
        }
        if(offset != null){
            sb.append("&offset=").append(offset.toString());
        }
        if(max != null)
            sb.append("&max=").append(max);
        if(sort!=null)
          sb.append("&sort=").append(sort);
        if(order != null)
            sb.append("&order=").append(order);
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(sb.toString());
        try{
            
            get.addRequestHeader("Content-type", "text/plain");
      
            int result = client.executeMethod(get);
            if(result == 200){
                String rawJSON = get.getResponseBodyAsString();
                //System.out.println(rawJSON);
                JSONObject object = JSONObject.fromObject(rawJSON);
                
                return object;
            }
            else{
                logger.error("Unable to retrieve species list. " + result);
                logger.info("Extra information about the error: " + get.getResponseBodyAsString());
            }
           
        }
        catch(Exception e){
            logger.error("Error retrieving public species list.",e);
        }
        finally{
            get.releaseConnection();
        }
        return null;
    }
    
    /**
     * Retrieves all the publically available species lists  
     * @return
     */
    public static Collection  getPublicSpeciesLists(String user,Integer offset, Integer max, String sort, String order){
        JSONObject jobject = getLists(user, offset, max, sort, order);
        JsonConfig cfg = new JsonConfig();
        cfg.setPropertySetStrategy(new IgnoreUnknownPropsStrategyWrapper(PropertySetStrategy.DEFAULT));
        cfg.setRootClass(SpeciesListDTO.class);
        return JSONArray.toCollection(jobject.getJSONArray("lists"), cfg);
//        String url = CommonData.speciesListServer + "/ws/speciesList" ;
//        if(user != null)
//          url += "?user="+user;
//        HttpClient client = new HttpClient();
//        GetMethod get = new GetMethod(url);
//        try{
//            
//            get.addRequestHeader("Content-type", "text/plain");
//      
//            int result = client.executeMethod(get);
//            if(result == 200){
//                String rawJSON = get.getResponseBodyAsString();
//                //System.out.println(rawJSON);
//                publicSpeciesLists = JSONArray.toCollection(JSONArray.fromObject(rawJSON), SpeciesListDTO.class);
//            }
//            else{
//                logger.error("Unable to retrieve species list. " + result);
//                logger.info("Extra information about the error: " + get.getResponseBodyAsString());
//            }
//           
//        }
//        catch(Exception e){
//            logger.error("Error retrieving public species list.",e);
//        }
//        finally{
//            get.releaseConnection();
//        }
//        return publicSpeciesLists;
    }
    /**
     * Retrieves the items from the specified listUid
     * @param listUid
     * @return
     */
    public static Collection<SpeciesListItemDTO> getListItems(String listUid){
        String url = CommonData.speciesListServer + "/ws/speciesListItems/" +listUid;//+"?nonulls=true";
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        try{
            
            get.addRequestHeader("Content-type", "text/plain");
      
            int result = client.executeMethod(get);
            if(result == 200){
                String rawJSON = get.getResponseBodyAsString();
              //System.out.println(rawJSON);
                return JSONArray.toCollection(JSONArray.fromObject(rawJSON), SpeciesListItemDTO.class);
            }
            else{
                logger.error("Unable to retrieve species list items for "+listUid+ ". " + result);
                logger.info("Extra information about the error: " + get.getResponseBodyAsString());
          }
        }
        catch(Exception e){
            logger.error("Error retrieving list items.",e);
        }
        finally{
            get.releaseConnection();
        }
        return null;
    }
    
    public static String createNewList(String name, String items, String description, String url, String user){
        String postUrl = CommonData.speciesListServer+"/ws/speciesList/";
        HttpClient client = new HttpClient();        
        PostMethod post = new PostMethod(postUrl);
        //set the cookie from the user
        try{
            post.setRequestHeader("Cookie", "ALA-Auth="+java.net.URLEncoder.encode(user,"utf-8"));
        }
        catch(Exception e){
          //should not happen as utf-8 is a supported encoding          
        }        
        logger.debug(post.getRequestHeader("Cookie"));
        //((HttpServletRequest) Executions.getCurrent().getNativeRequest()).getCookies()
        if(name != null && items!=null){
            try{
                java.util.Map map = new java.util.HashMap();
                map.put("listName", name);
                if(description !=null) map.put("description", description);
                if(url != null) map.put("url",url);
                map.put("listItems", items);
                map.put("listType","SPATIAL_PORTAL");
                String content=JSONObject.fromObject( map ).toString();
                logger.debug("create new list : " + content + " for user " + user);
                String contentType="application/json";
                String charset="UTF-8";
                StringRequestEntity requestEntity = new StringRequestEntity(content, contentType,charset);
                post.setRequestEntity(requestEntity);
                int result = client.executeMethod(post);
                logger.debug(result);
                logger.debug(post.getResponseBodyAsString());
//                for(org.apache.commons.httpclient.Header header : post.getResponseHeaders()){
//                    logger.debug(header.getName() + " ::: " + header.getValue());
//                }
                //logger.debug("Response Header " + post.getResponseHeaders());
                
                if(result == 201)
                    return post.getResponseHeader("druid").getValue();
                
            }
            catch(Exception e){
                logger.error("Error uploading list",e);
            }
            return null;
        }
        else
            return null;
    }
    
    public static void main(String[] args){
        CommonData.speciesListServer="http://natasha.ala.org.au:8080/specieslist-webapp";
        System.out.println(getPublicSpeciesLists(null, null,null,null,null));
        //System.out.println(createNewList("FirstTestList","Callocephalon fimbriatum,Ornithorhynchus anatinus","The description","The url","natasha.carter@csiro"));
    }
    
   
     
    public static class IgnoreUnknownPropsStrategyWrapper extends PropertySetStrategy {
     
        private PropertySetStrategy original;
     
        public IgnoreUnknownPropsStrategyWrapper(PropertySetStrategy original) {
            this.original = original;
        }
     
        @Override
        public void setProperty(Object o, String string, Object o1) throws JSONException {
            try {
                original.setProperty(o, string, o1);
            } catch (Exception ex) {
                //ignore
            }
        }
    }
}
