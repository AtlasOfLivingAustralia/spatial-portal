/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.zkoss.zul.Radiogroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class ExportSpeciesExternalComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(ExportSpeciesExternalComposer.class);


    private Radiogroup exportFormat;

    private String redirectUrl;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Export species to BCCVL";
        this.totalSteps = 1;

        this.loadSpeciesLayers(false, true);

        this.updateWindowTitle();
    }

    @Override
    public boolean onFinish() {

        List<Query> list = getAllSelectedSpecies();
        List<Map> maps = new ArrayList<>();
        for (Query q : list) {
            Map m = new HashMap();
            m.put("name", q.getName());
            m.put("qid", ((BiocacheQuery) q).getQid());
            m.put("url", q.getBS());

            maps.add(m);
        }
        Map container = new HashMap();
        container.put("data", maps);
        String json = JSONObject.toJSONString(container);

        getMapComposer().getOpenLayersJavascript().execute("postToExternal('" + CommonData.getSettings().getProperty("bccvl.post.url") + "', " + json + ");");

        detach();
        return true;
    }
}
