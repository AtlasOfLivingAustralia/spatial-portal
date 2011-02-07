package org.ala.spatial.web.services;

import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.index.OccurrencesCollection;

import org.ala.spatial.util.TabulationSettings;
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
            TabulationSettings.load();

        	String species = req.getParameter("sp").replaceAll("_"," ");
        	
			String [] list = OccurrencesCollection.findSpecies(species,40);
			
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
