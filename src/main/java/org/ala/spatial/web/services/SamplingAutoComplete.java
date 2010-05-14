package org.ala.spatial.web.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.tabulation.SamplingService;
import org.ala.spatial.analysis.tabulation.SpeciesListIndex;
import org.ala.spatial.analysis.tabulation.TabulationSettings;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SpatialSettings;
import org.ala.spatial.util.Zipper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author adam
 */
@Controller
@RequestMapping("/ws/samplingac/")
public class SamplingAutoComplete {
    
    @RequestMapping(value = "/species", method = RequestMethod.GET)
    public
    @ResponseBody
    String species(HttpServletRequest req) {
        try {
        	String species = req.getParameter("sp").replaceAll("_"," ");
        	
        	SamplingService ss = new SamplingService();
        	
			String [] list = ss.filterSpecies(species,40);
			
			if(list != null){			
				StringBuffer sb = new StringBuffer();
				for(int i=0;i<list.length;i++){
					sb.append(list[i]);
					sb.append('\n');
				}
				return sb.toString();
			}			
        } catch (Exception e) {
            System.out.println("Error processing Sampling request:");
            e.printStackTrace(System.out);
        }

        return "";

    }
}
