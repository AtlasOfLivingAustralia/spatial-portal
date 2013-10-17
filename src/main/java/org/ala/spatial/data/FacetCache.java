package org.ala.spatial.data;

import java.util.*;

public interface FacetCache {

    public Map<String,String[][]> getGroupedFacets();
    //public List<QueryField> getFacetQueryFieldList();
    public void reloadCache();
}
