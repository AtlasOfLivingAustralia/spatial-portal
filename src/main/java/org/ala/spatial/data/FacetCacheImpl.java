package org.ala.spatial.data;

import java.util.*;

import javax.inject.Inject;



import org.ala.spatial.util.CommonData;
import org.apache.log4j.Logger;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
/**
 * A refreshable cache that populates the facets values from the biocache ws.
 * 
 * @author Natasha Carter (natasha.carter@csiro.au)
 *
 */
@Component("facetCache")
public class FacetCacheImpl implements FacetCache{
    private final static String facetSuffix="/search/grouped/facets"; 
    private final static String dataTypeSuffix="/index/fields";
    private Map<String,String[][]> groupedFacets;
    private static List<QueryField> facetQueryFieldList;
    private final static Logger logger = Logger.getLogger(FacetCacheImpl.class);
    @Inject
    private RestOperations restTemplate;
    @Inject
    private AbstractMessageSource messageSource;
    @Override
    public Map<String, String[][]> getGroupedFacets() {
        return groupedFacets;
    }
    
    //@Override
    /**
     * This method was made static to prevent the need to AOP to inject configuration into a BiocacheQuery
     * @return
     */
    public static List<QueryField> getFacetQueryFieldList(){
        return facetQueryFieldList;
    }
   
    /**
     * Reloads the facet cache every 12 hours. This will also be called when a refresh of the config is called.
     */
    @Override
    @Scheduled(fixedDelay = 43200000)// schedule to run every 12 hours
    public void reloadCache(){
        System.out.println();
        Map<String,String[][]> tmpMap = new LinkedHashMap<String, String[][]>();
        List<QueryField> tmpList = new ArrayList<QueryField>();
        List<Map<String,Object>> values = new ArrayList<Map<String,Object>>();
        //get the JSON from the WS
        //System.out.println("URLRL: " + CommonData.biocacheServer+facetSuffix);
        values =restTemplate.getForObject(CommonData.biocacheServer+facetSuffix, List.class);
        //System.out.println("FACETCACHE:::: " + values);
        logger.debug(values);
        Map<String, QueryField.FieldType> dataTypes = getDataTypes();
       for(Map<String,Object> value : values){
           //extract the group
           String title = value.get("title").toString();
           //now get the facets themselves
           List<Map<String,String>> facets = ( List<Map<String,String>>)value.get("facets");
           String[][] facetValues = new String[facets.size()][2];
           int i=0;
           for(Map<String,String> facet : facets){
               String field = facet.get("field");               
               String i18n = messageSource.getMessage("facet."+field, null, field, Locale.getDefault());
               facetValues[i][0] = field;
               facetValues[i][1] = i18n;
               QueryField.FieldType ftype = dataTypes.containsKey(field)? dataTypes.get(field):QueryField.FieldType.STRING;
               //new QueryField("taxon_name", "Scientific name", QueryField.GroupType.TAXONOMIC, QueryField.FieldType.STRING)
               QueryField qf = new QueryField(field, i18n, QueryField.GroupType.getGroupType(title), ftype);
               tmpList.add(qf);
               i++;
           }
           tmpMap.put(title, facetValues);
       }
       
       //add a bunch of configured extra fields from the default values
       for(String f : CommonData.customFacets){
           String i18n = messageSource.getMessage("facet."+f, null, f, Locale.getDefault());
           QueryField.FieldType ftype = dataTypes.containsKey(f)? dataTypes.get(f):QueryField.FieldType.STRING;
           tmpList.add(new QueryField(f,i18n, QueryField.GroupType.CUSTOM,QueryField.FieldType.STRING));
       }
       
       groupedFacets = tmpMap;
       facetQueryFieldList = tmpList;
       logger.info("Grouped Facets: " + groupedFacets); 
       logger.info("facet query list : " + facetQueryFieldList);
    }
    /**
     * Extracts the biocache data types from the webservice so that they can be used to dynamically load the facets
     * @return
     */
    private Map<String, QueryField.FieldType> getDataTypes(){
        Map<String, QueryField.FieldType> map = new HashMap<String, QueryField.FieldType>();
        List<Map<String,String>> values = new ArrayList<Map<String,String>>();
        //get the JSON from the WS
        //System.out.println("URLRL: " + CommonData.biocacheServer+facetSuffix);
        values =restTemplate.getForObject(CommonData.biocacheServer+facetSuffix, List.class);
        for(Map<String,String> mvalues : values){
            String name = mvalues.get("name");
            String dtype = mvalues.get("dataType");
            if("string".equals(dtype) || "textgen".equals(dtype)){
                map.put(name, QueryField.FieldType.STRING);
            } else if("int".equals(dtype) || "tint".equals(dtype) || "tdate".equals(dtype)){
                map.put(name, QueryField.FieldType.INT);
            } else if("double".equals(dtype) || "tdouble".equals(dtype)){
                map.put(name, QueryField.FieldType.DOUBLE);
            } else {
                map.put(name, QueryField.FieldType.STRING);
            }
        }
        return map;
    }

}
