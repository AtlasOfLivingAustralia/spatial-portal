package org.ala.spatial.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author ajayr
 */
@Controller
public class PageController {

    @RequestMapping("/index")
    public String index(ModelMap model) {
        System.out.println("index2 page method called");
        //return new ModelAndView("user", "message", "Add method called");
        //User user = new User();
        //model.addAttribute(user);
        return "index";
    }

    @RequestMapping("/home")
    public String home(ModelMap model) {
        //System.out.println("home page method called");
        return "index";
    }
}
