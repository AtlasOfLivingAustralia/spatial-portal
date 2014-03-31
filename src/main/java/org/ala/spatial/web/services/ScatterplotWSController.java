/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.services;

import net.sf.json.JSONArray;
import org.ala.spatial.analysis.scatterplot.Scatterplot;
import org.ala.spatial.analysis.scatterplot.ScatterplotDTO;
import org.ala.spatial.analysis.scatterplot.ScatterplotStore;
import org.ala.spatial.analysis.scatterplot.ScatterplotStyleDTO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adam
 */
@Controller
public class ScatterplotWSController  {

    private static Logger logger = Logger.getLogger(ScatterplotWSController.class);

    @RequestMapping(value = {"/ws/scatterplot/new", "/ws/scatterplotlist"}, method = {RequestMethod.POST, RequestMethod.GET})
    public @ResponseBody
    Map envelope(@ModelAttribute ScatterplotDTO desc,
                 @ModelAttribute ScatterplotStyleDTO style,
                 HttpServletRequest req) {

//        ScatterplotDTO desc = new ScatterplotDTO(
//                "lsid%3Aurn%3Alsid%3Abiodiversity.org.au%3Aapni.taxon%3A257590",
//                "http://biocache.ala.org.au/ws",
//                "species",
//                null,null,null,null
//                ,"bioclim_bio1", "bioclim_bio1"
//                ,"bioclim_bio2", "bioclim_bio2"
//                ,20,null);
//
//        ScatterplotStyleDTO style = new ScatterplotStyleDTO();

        Scatterplot scat = new Scatterplot(desc, style, null);

        ScatterplotStore.addData(scat.getScatterplotDTO().getId(), scat);

        HashMap<String, Object> map = new HashMap<String, Object>();

        if (desc.getLayers() != null && desc.getLayers().length > 2) {
            //scatterplot list
            map.put("htmlUrl", scat.getHtmlURL());
            map.put("downloadUrl", scat.getDownloadURL());
        } else {
            //scatterplot
            map.put("missingCount", scat.getScatterplotDataDTO().getMissingCount());
            map.put("extents", scat.getScatterplotDataDTO().getExtents());
            map.put("layers", scat.getScatterplotDTO().getLayers());
            map.put("imageUrl", scat.getImageURL());
        }
        map.put("id",scat.getScatterplotDTO().getId());

        return map;

    }

    @RequestMapping(value = "/ws/scatterplot/{id}", method = {RequestMethod.POST, RequestMethod.GET})
    public @ResponseBody
    void update(@PathVariable("id") String id,
                @RequestParam(value="minx",required=false) Integer minx,
               @RequestParam(value="miny",required=false) Integer miny,
               @RequestParam(value="maxx",required=false) Integer maxx,
               @RequestParam(value="maxy",required=false) Integer maxy,
               HttpServletRequest req, HttpServletResponse response) {

        Scatterplot scat = ScatterplotStore.getData(id);

        if (scat != null && minx != null & miny != null && maxx != null && maxy != null) {
            scat.annotatePixelBox(minx, miny, maxx, maxy);
        }

        try {
            OutputStream os = response.getOutputStream();

            if (req.getRequestURI().toUpperCase().endsWith(".PNG")) {
                response.setHeader("Cache-Control", "max-age=86400"); //age == 1 day
                response.setContentType("image/png");

                os.write(scat.getAsBytes());
            } else {
                response.setContentType("application/json");

                HashMap<String, Object> map = new HashMap<String, Object>();

                JSONArray jo = JSONArray.fromObject(scat.getScatterplotStyleDTO().getSelection());

                os.write(jo.toString().getBytes());
            }

            os.flush();
            os.close();
        } catch (IOException e) {
            logger.error("error in outputting annotated scatterplot image as png", e);
        }

    }

    @RequestMapping(value = "/ws/scatterplot/style/{id}", method = {RequestMethod.POST, RequestMethod.GET})
    public @ResponseBody
    Map style(@PathVariable("id") String id,
              @ModelAttribute ScatterplotStyleDTO style,
               HttpServletRequest req) {

        Scatterplot scat = ScatterplotStore.getData(id);

        if (scat != null) {
            scat.reStyle(style);
        }

        HashMap<String, Object> map = new HashMap<String, Object>();

        map.put("scatterplot", scat.getScatterplotDTO());
        map.put("style", scat.getScatterplotStyleDTO());

        return map;

    }

    @RequestMapping(value = "/ws/scatterplot/csv/{id}", method = {RequestMethod.POST, RequestMethod.GET})
    public @ResponseBody
    String csv(@PathVariable("id") String id,
              HttpServletRequest req) {

        Scatterplot scat = ScatterplotStore.getData(id);

        return scat.getCSV();
    }




}
