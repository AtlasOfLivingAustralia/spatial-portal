/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.layers.web;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Layer;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author jac24n
 */
@Controller
public class LayersService {
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());

    @Resource(name = "layerDao")
    private LayerDAO layerDao;

    /**
     * This method returns all layers
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/layers", method = RequestMethod.GET)
    public
    @ResponseBody
    List<Layer> layerObjects(HttpServletRequest req) {
        logger.info("Retriving enabled layers");
        return layerDao.getLayers();
    }

    /**
     * This method returns a single layer, provided an id
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/layer/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    Layer layerObject(@PathVariable("id") String id, HttpServletRequest req) {
        Layer l = null;
        try {
            l = layerDao.getLayerById(Integer.parseInt(id));
        } catch (Exception e) {
        }

        if (l == null) {
            l = layerDao.getLayerByName(id);
        }
        return l;
    }

    /**
     * This method returns all layers
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/layers/search", method = RequestMethod.GET)
    public
    @ResponseBody
    List<Layer> layerObjects(@RequestParam("q") String q, HttpServletRequest req) {
        logger.info("search enabled layers for " + q);
        return layerDao.getLayersByCriteria(q);
    }

    @RequestMapping(value = "/layers/grids", method = RequestMethod.GET)
    public
    @ResponseBody
    List<Layer> gridsLayerObjects(HttpServletRequest req) {
        // String query =
        // "SELECT * FROM layers WHERE enabled='TRUE' and type='Environmental';";
        // ResultSet r = DBConnection.query(query);
        // return Utils.resultSetToJSON(r);
        return layerDao.getLayersByEnvironment();
    }

    @RequestMapping(value = "/layers/shapes", method = RequestMethod.GET)
    public
    @ResponseBody
    List<Layer> shapesLayerObjects(HttpServletRequest req) {
        // String query =
        // "SELECT * FROM layers WHERE enabled='TRUE' and type='Contextual';";
        // ResultSet r = DBConnection.query(query);
        // return Utils.resultSetToJSON(r);
        return layerDao.getLayersByContextual();

    }

    /**
     * Return layers list if RIF-CS XML format
     *
     * @param req
     * @param res
     * @throws Exception
     */
    @RequestMapping(value = "/layers/rif-cs", method = RequestMethod.GET)
    public void layersRifCs(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setContentType("text/xml");

        // Build XML by hand here because JSP processing seems to omit CDATA sections from the output
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        sb.append("<registryObjects xmlns=\"http://ands.org.au/standards/rif-cs/registryObjects\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://ands.org.au/standards/rif-cs/registryObjects http://services.ands.org.au/documentation/rifcs/schema/registryObjects.xsd\">");
        for (Layer layer : layerDao.getLayers()) {
            sb.append("<registryObject group=\"Atlas of Living Australia\">");
            sb.append("<key>ala.org.au/uid_" + layer.getUid() + "</key>");
            sb.append("<originatingSource><![CDATA[" + layer.getMetadatapath() + "]]></originatingSource>");
            sb.append("<collection type=\"dataset\">");
            sb.append("<identifier type=\"local\">ala.org.au/uid_" + layer.getUid() + "</identifier>");
            sb.append("<name type=\"abbreviated\">");
            sb.append("<namePart>" + layer.getName() + "</namePart>");
            sb.append("</name>");
            sb.append("<name type=\"alternative\">");
            sb.append("<namePart><![CDATA[" + layer.getDescription() + "]]></namePart>");
            sb.append("</name>");
            sb.append("<name type=\"primary\">");
            sb.append("<namePart><![CDATA[" + layer.getDescription() + "]]></namePart>");
            sb.append("</name>");
            sb.append("<location>");
            sb.append("<address>");
            sb.append("<electronic type=\"url\">");
            sb.append("<value>http://spatial.ala.org.au/layers</value>");
            sb.append("</electronic>");
            sb.append("</address>");
            sb.append("</location>");
            sb.append("<relatedObject>");
            sb.append("<key>Contributor:Atlas of Living Australia</key>");
            sb.append("<relation type=\"hasCollector\" />");
            sb.append("</relatedObject>");
            sb.append("<subject type=\"anzsrc-for\">0502</subject>");
            sb.append("<subject type=\"local\">" + layer.getClassification1() + "</subject>");
            if (!StringUtils.isEmpty(layer.getClassification2())) {
                sb.append("<subject type=\"local\">" + layer.getClassification2() + "</subject>");
            }
            sb.append("<description type=\"full\"><![CDATA[" + layer.getNotes() + "]]></description>");
            sb.append("<relatedInfo type=\"website\">");
            sb.append("<identifier type=\"uri\"><![CDATA[" + layer.getMetadatapath() + "]]></identifier>");
            sb.append("<title>Further metadata</title>");
            sb.append("</relatedInfo>");
            sb.append("<relatedInfo type=\"website\">");
            sb.append("<identifier type=\"uri\"><![CDATA[" + layer.getSource_link() + "]]></identifier>");
            sb.append("<title>Original source of this data</title>");
            sb.append("</relatedInfo>");
            sb.append("<rights>");
            sb.append("<licence ");
            if (!StringUtils.isEmpty(layer.getLicence_link())) {
                sb.append("rightsUri=\"" + StringEscapeUtils.escapeXml(layer.getLicence_link()) + "\">");
            } else {
                sb.append(">");
            }
            sb.append("<![CDATA[" + layer.getLicence_notes() + "]]></licence>");
            sb.append("</rights>");
            sb.append("<coverage>");
            sb.append("<spatial type=\"iso19139dcmiBox\">northlimit=" + layer.getMaxlatitude() + "; southlimit=" + layer.getMinlatitude() + "; westlimit=" + layer.getMinlongitude() + "; eastLimit=" + layer.getMaxlongitude() + "; projection=WGS84</spatial>");
            sb.append("</coverage>");
            sb.append("</collection>");
            sb.append("</registryObject>");
        }
        sb.append("</registryObjects>");

        res.getWriter().append(sb.toString());
        res.getWriter().flush();
        res.getWriter().close();
    }

}
