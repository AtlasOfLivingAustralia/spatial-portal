/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.web;

import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.ala.layers.dao.LayerPidDAO;
import org.ala.layers.dto.LayerPid;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author ajay
 */
@Controller
public class LayerPidService {

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());

    @Resource(name = "layerPidDao")
    private LayerPidDAO layerPidDao;

    @RequestMapping(value = "/layerpids", method = RequestMethod.GET)
    public
    @ResponseBody
    List<LayerPid> listLayerPids(HttpServletRequest req) {
        return layerPidDao.getLayers();

    }

//    @RequestMapping(value = "/layerpids/populate", method = RequestMethod.GET)
//    public void populateLayerPids(HttpServletRequest req, HttpServletResponse res) {
//
//        System.out.println("started populate");
//
//        //String query = "SELECT * FROM layerpids;";
//
//        //ResultSet r = DBConnection.query(query);
//
//        //return Utils.resultSetToJSON(r);
//
//        PGBridge.getLayer();
//
//        System.out.println("table population completed");
//    }
//
//    @RequestMapping(value = "/layerpids/generate", method = RequestMethod.GET)
//    public void generateLayerPids(HttpServletRequest req, HttpServletResponse res) {
//
//        System.out.println("started generate");
//
//        //String query = "SELECT * FROM layerpids;";
//
//        //ResultSet r = DBConnection.query(query);
//
//        //return Utils.resultSetToJSON(r);
//
//        // https://test.ands.org.au:8443/pids/
//        // 2c6ed180e966774eee8409f7152b0cc885d07f71
//
//        try {
//            AndsPidIdentity andsid = new AndsPidIdentity();
//            andsid.setAppId("2c6ed180e966774eee8409f7152b0cc885d07f71");
//            andsid.setAuthDomain("csiro.au");
//            andsid.setIdentifier("ran126");
//
//            AndsPidClient ands = new AndsPidClient();
//            ands.setPidServiceHost("test.ands.org.au");
//            ands.setPidServicePath("pids");
//            ands.setPidServicePort(8443);
//            ands.setRequestorIdentity(andsid);
//            AndsPidResponse mintHandleFormattedResponse = ands.mintHandleFormattedResponse(AndsPidClient.HandleType.DESC, "test");
//
//            System.out.println("handle creation status: " + mintHandleFormattedResponse.isSuccess());
//
//        } catch (Exception e) {
//        }
//
//
//
//        System.out.println("PID generation completed");
//    }

}
