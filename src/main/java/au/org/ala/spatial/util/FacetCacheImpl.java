package au.org.ala.spatial.util;

import au.org.ala.legend.QueryField;
import au.org.ala.spatial.StringConstants;
import org.apache.log4j.Logger;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import javax.inject.Inject;
import java.util.*;

/**
 * A refreshable cache that populates the facets values from the biocache ws.
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */

@Component("facetCache")
public class FacetCacheImpl implements FacetCache {
    private static final String FACET_SUFFIX = "/search/grouped/facets";
    private static final Logger LOGGER = Logger.getLogger(FacetCacheImpl.class);
    private static List<QueryField> facetQueryFieldList;

    @Inject
    private RestOperations restTemplate;
    @Inject
    private AbstractMessageSource messageSource;

    /**
     * This method was made static to prevent the need to AOP to inject configuration into a BiocacheQuery
     *
     * @return
     */
    public static List<QueryField> getFacetQueryFieldList() {
        return facetQueryFieldList;
    }

    /**
     * Reloads the facet cache every 12 hours. This will also be called when a refresh of the config is called.
     */
    @Override
    @Scheduled(fixedDelay = 43200000)
    public void reloadCache() {
        Map<String, String[][]> tmpMap = new LinkedHashMap<String, String[][]>();
        List<QueryField> tmpList = new ArrayList<QueryField>();
        List<Map<String, Object>> values;
        //get the JSON from the WS\
        values = restTemplate.getForObject(CommonData.getBiocacheServer() + FACET_SUFFIX, List.class);

        LOGGER.debug(values);
        Map<String, QueryField.FieldType> dataTypes = getDataTypes();
        for (Map<String, Object> value : values) {
            //extract the group
            String title = value.get(StringConstants.TITLE).toString();
            //now get the facets themselves
            List<Map<String, String>> facets = (List<Map<String, String>>) value.get("facets");
            String[][] facetValues = new String[facets.size()][2];
            int i = 0;
            for (Map<String, String> facet : facets) {
                String field = facet.get("field");
                //Only add if it is not included in the ignore list
                if (!CommonData.ignoredFacets.contains(field)) {
                    String i18n = messageSource.getMessage("facet." + field, null, field, Locale.getDefault());

                    //TODO: update biocache i18n instead of doing this
                    if ("data_provider".equals(field)) {
                        i18n = "Data Provider";
                    }

                    //use current layer names for facets
                    try {
                        String layername = CommonData.getFacetLayerName(field);
                        if (i18n == null || layername != null) {
                            i18n = CommonData.getLayerDisplayName(layername);
                        }
                    } catch (Exception e) {
                        
                    }

                    facetValues[i][0] = field;
                    facetValues[i][1] = i18n;
                    QueryField.FieldType ftype = dataTypes.containsKey(field) ? dataTypes.get(field) : QueryField.FieldType.STRING;


                    QueryField qf = new QueryField(field, i18n, QueryField.GroupType.getGroupType(title), ftype);
                    tmpList.add(qf);
                    i++;
                }
            }
            tmpMap.put(title, facetValues);
        }

        //add a bunch of configured extra fields from the default values
        for (String f : CommonData.customFacets) {
            String i18n = messageSource.getMessage("facet." + f, null, f, Locale.getDefault());
            tmpList.add(new QueryField(f, i18n, QueryField.GroupType.CUSTOM, QueryField.FieldType.STRING));
        }

        facetQueryFieldList = tmpList;
        LOGGER.debug("Grouped Facets: " + tmpMap);
        LOGGER.debug("facet query list : " + facetQueryFieldList);
    }

    /**
     * Extracts the biocache data types from the webservice so that they can be used to dynamically load the facets
     *
     * @return
     */
    private Map<String, QueryField.FieldType> getDataTypes() {
        Map<String, QueryField.FieldType> map = new HashMap<String, QueryField.FieldType>();
        List<Map<String, String>> values;
        //get the JSON from the WS
        values = restTemplate.getForObject(CommonData.getBiocacheServer() + FACET_SUFFIX, List.class);
        for (Map<String, String> mvalues : values) {
            String name = mvalues.get(StringConstants.NAME);
            String dtype = mvalues.get("dataType");
            if ("string".equals(dtype) || "textgen".equals(dtype)) {
                map.put(name, QueryField.FieldType.STRING);
            } else if ("int".equals(dtype) || "tint".equals(dtype) || "tdate".equals(dtype)) {
                map.put(name, QueryField.FieldType.INT);
            } else if ("double".equals(dtype) || "tdouble".equals(dtype)) {
                map.put(name, QueryField.FieldType.DOUBLE);
            } else {
                map.put(name, QueryField.FieldType.STRING);
            }
        }
        return map;
    }

}
