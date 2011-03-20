/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import org.ala.spatial.analysis.legend.Legend;
import org.ala.spatial.analysis.legend.LegendEvaluate;
import org.ala.spatial.analysis.service.LoadedPointsService;
import org.ala.spatial.util.TabulationSettings;

/**
 * LSID + one attribute/contextual/environmental intersection
 * for categorical = value & count
 * for continous = min, max, [scale bar] 
 *
 * @author Adam
 */
public class LayerVariableDistribution {
//TODO: bounding box hashmap cleanup

    /*
     * categorical is [String[] values, int[] counts]
     * continous is [double min, double max, String scaleType]
     */
    static HashMap<String, Object[]> lsidDistribution = new HashMap<String, Object[]>();

    /**
     *
     * categorical is [String[] values, int[] counts]
     * continous is [Legend legend]
     *
     * @param lsid
     * @return categorical or continous as Object[]
     */
    static public Object[] getLsidDistribution(String lsid, String colourMode) {
        Object[] o = lsidDistribution.get(lsid + colourMode);
        if (o == null) {
            ArrayList<SpeciesColourOption> extra = new ArrayList<SpeciesColourOption>();
            extra.add(SpeciesColourOption.fromName(colourMode, true));

            double[] p = OccurrencesCollection.getPoints(new OccurrencesFilter(lsid, TabulationSettings.MAX_RECORD_COUNT_CLUSTER), extra);

            if (p != null) {
                SpeciesColourOption sco = extra.get(0);                
                if(sco.isContinous()) {
                    Legend legend;
                    o = new Object[1];
                    if(sco.isDbl()) {
                        legend = LegendEvaluate.buildFrom(sco.getDblArray());
                        o[0] = legend;
                    } else {
                        //int
                        legend = LegendEvaluate.buildFrom(sco.getIntArray());
                        o[0] = legend;
                    }
                } else {
                    //categorical
                    o = sco.getCategoryBreakdown();
                }
                lsidDistribution.put(lsid + colourMode, o);
            }
        }
        return o;
    }

    static public void putlsidDistribution(String id, Object[] o) {
        lsidDistribution.put(id, o);
    }
}
