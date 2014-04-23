package org.ala.layers.util;

import org.ala.layers.dto.AttributionDTO;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A small cache for data resource attribution.
 * This should be revisited if this cache grows or needs regular refreshing.
 */
public class AttributionCache {

    private AttributionCache() {
    }

    private static AttributionCache attributionCache;

    private Map<String, AttributionDTO> cache = new HashMap<String, AttributionDTO>();

    public AttributionDTO getAttributionFor(String dataResourceUid) throws Exception {
        AttributionDTO a = cache.get(dataResourceUid);
        if (a == null) {
            ObjectMapper om = new ObjectMapper();
            om.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            a = om.readValue(new URL("http://collections.ala.org.au/ws/dataResource/" + dataResourceUid), AttributionDTO.class);
            cache.put(dataResourceUid, a);
        }
        return a;
    }

    public static AttributionCache getCache() {
        if (attributionCache == null)
            attributionCache = new AttributionCache();
        return attributionCache;
    }

    public void clear() {
        cache.clear();
    }
}
