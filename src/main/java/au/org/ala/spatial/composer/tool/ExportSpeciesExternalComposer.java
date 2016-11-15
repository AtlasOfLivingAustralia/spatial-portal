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
import org.zkoss.zk.ui.event.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class ExportSpeciesExternalComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(ExportSpeciesExternalComposer.class);


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
            m.put("query", "qid:" + ((BiocacheQuery) q).getQid());
            m.put("url", q.getBS());

            maps.add(m);
        }
        Map container = new HashMap();
        container.put("data", maps);
        String json = JSONObject.toJSONString(container);

        getMapComposer().getOpenLayersJavascript().execute("postToExternal('" + CommonData.getSettings().getProperty("bccvl.post.url") + "', " + json + ");");

        login(null);

        loggingIn = false;

        return true;
    }

    private boolean loggingIn = false;

    public void login(Event event) {
        if (!loggingIn) {
            loggingIn = true;

            getMapComposer().activateLink("*<div id='login_start'>Sending to BCCVL...</div><div id='login_required' style='display:none'><a target='_blank' href='" + CommonData.getSettings().getProperty("bccvl.login.url") + "'>Click here to login to BCCVL (opens a new tab)</a>. <br/><br/>Return to this page after logging in. </div><div id='end_of_login'/>", "Export to BCCVL", false, null);

            detach();
        }
    }
}
