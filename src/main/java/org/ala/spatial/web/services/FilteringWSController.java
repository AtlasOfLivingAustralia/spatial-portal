package org.ala.spatial.web.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.ala.spatial.analysis.tabulation.FilteringImage;
import org.ala.spatial.analysis.tabulation.SPLFilter;
import org.ala.spatial.analysis.tabulation.SpeciesListIndex;
import org.ala.spatial.util.SpatialSettings;
import org.jboss.serial.io.JBossObjectInputStream;
import org.jboss.serial.io.JBossObjectOutputStream;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/filtering/")
public class FilteringWSController {

    private SpatialSettings ssets;
    private Map<String, Object> userMap;
    private FilteringImage filteringImage;
    private List _layer_filters_selected;

    @RequestMapping(value = "/init", method = RequestMethod.GET)
    public
    @ResponseBody
    String doInit(HttpServletRequest req) {
        try {
            String pid = "";
            long currTime = System.currentTimeMillis();
            pid = "" + currTime;

            HttpSession session = req.getSession(true);

            userMap = new Hashtable();

            File workingDir = new File(session.getServletContext().getRealPath("/output/filtering/" + currTime + "/"));
            workingDir.mkdirs();

            File file = File.createTempFile("spl", ".png", workingDir);
            filteringImage = new FilteringImage(file.getPath());

            System.out.println("Created initial image at: " + file.getPath());

            _layer_filters_selected = new ArrayList();

            userMap.put("pid", pid);
            //userMap.put("filteringImage", filteringImage);
            userMap.put("imgpath", file.getPath());
            //userMap.put("selectedLayerFilters", _layer_filters_selected);


            //session.setAttribute(pid, userMap);
            writeUserBytes(session.getServletContext().getRealPath("/output/filtering/" + currTime + "/usermap.ser"));
            //writeUserMapXML(session.getServletContext().getRealPath("/output/filtering/" + currTime + "/usermap.xml"));

            return pid;
        } catch (Exception ex) {
            Logger.getLogger(FilteringWSController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";
    }

    @RequestMapping(value = "/apply/pid/{pid}/layers/{layers}/types/{types}/val1s/{val1s}/val2s/{val2s}", method = RequestMethod.GET)
    public
    @ResponseBody
    String apply2(@PathVariable String pid,
            @PathVariable String layers,
            @PathVariable String types,
            @PathVariable String val1s,
            @PathVariable String val2s,
            HttpServletRequest req) {
        try {

            // Undecode them first
            layers = URLDecoder.decode(layers, "UTF-8");
            types = URLDecoder.decode(types, "UTF-8");
            val1s = URLDecoder.decode(val1s, "UTF-8");
            val2s = URLDecoder.decode(val2s, "UTF-8");

            // grab and split the layers
            String[] aLayers = layers.split(";");
            String[] aTypes = types.split(";");
            String[] aVal1s = val1s.split(";");
            String[] aVal2s = val2s.split(";");


            // Now lets apply the filters, one at a time
            // grab the existing imgpath to re-init the filteringimage obj
            filteringImage = new FilteringImage((String) userMap.get("imgpath"));

            // apply the filters by iterating thru' the layers from client
            for (int i = 0; i < aLayers.length; i++) {
                String cLayer = aLayers[i];
                String cType = aTypes[i];
                String cVal1 = aVal1s[i];
                String cVal2 = aVal2s[i];

                System.out.println("Applying filter for " + cLayer + " with " + cVal1 + " - " + cVal2); 

                if (cType.equalsIgnoreCase("environmental")) {
                    filteringImage.applyFilter(cLayer, Double.parseDouble(cVal1), Double.parseDouble(cVal2));
                } else {
                    filteringImage.applyFilterCtx(cLayer, Integer.parseInt(cVal1), Boolean.parseBoolean(cVal2));
                }
            }


        } catch (Exception e) {
            e.printStackTrace(System.out);
        }



        return "";
    }

    @RequestMapping(value = "/apply/pid/{pid}/layer/{layer}/type/{type}/val1/{val1}/val2/{val2}", method = RequestMethod.GET)
    public
    @ResponseBody
    String apply(@PathVariable String pid,
            @PathVariable String layer,
            @PathVariable String type,
            @PathVariable String val1,
            @PathVariable String val2,
            HttpServletRequest req) {
        try {

            String sessionfile = req.getSession().getServletContext().getRealPath("/output/filtering/" + pid + "/usermap.ser");

            SPLFilter selLayerFilter = null;

            layer = URLDecoder.decode(layer, "UTF-8");

            //userMap = (Map) req.getSession().getAttribute("pid");
            readUserBytes(sessionfile);
            //readUserMapXML(req.getSession().getServletContext().getRealPath("/output/filtering/" + pid + "/usermap.xml"));
            System.out.println("Read usermap... ");
            System.out.println("working with pid: " + userMap.get("pid"));
            //filteringImage = (FilteringImage) userMap.get("filteringImage");
            filteringImage = new FilteringImage((String) userMap.get("imgpath"));

            if (filteringImage == null) {
                System.out.println("Opps filteringimage is null");
            } else {
                System.out.println("got filteringimage");
            }

            if (type.equalsIgnoreCase("environmental")) {
                selLayerFilter = filteringImage.applyFilter(layer, Double.parseDouble(val1), Double.parseDouble(val2));
            } else {
                selLayerFilter = filteringImage.applyFilterCtx(layer, Integer.parseInt(val1), Boolean.parseBoolean(val2));
            }

            //_layer_filters_selected = (List) userMap.get("selectedLayerFilters");
            //_layer_filters_selected.add(selLayerFilter);

            List<String> _filtersNames = (List) userMap.get("fnames");
            List<String> _filtersNamesMin = (List) userMap.get("fnamesmin");
            List<String> _filtersNamesMax = (List) userMap.get("fnamesmax");

            _layer_filters_selected = new ArrayList();
            Iterator it = _filtersNames.iterator();
            while (it.hasNext()) {
                _layer_filters_selected.add(selLayerFilter);
            }


            // re-write the session file 
            writeUserBytes(sessionfile);

            return "";
        } catch (Exception ex) {
            Logger.getLogger(FilteringWSController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";
    }

    @RequestMapping(value = "/apply/pid/{pid}/species/count", method = RequestMethod.GET)
    public
    @ResponseBody
    String getSpeciesCount(@PathVariable String pid, HttpServletRequest req) {
        try {
            String sessionfile = req.getSession().getServletContext().getRealPath("/output/filtering/" + pid + "/usermap.ser");
            readUserBytes(sessionfile);

            //_layer_filters_selected = (List) userMap.get("selectedLayerFilters");
            List<String> _filtersNames = (List) userMap.get("fnames");
            List<String> _filtersNamesMin = (List) userMap.get("fnamesmin");
            List<String> _filtersNamesMax = (List) userMap.get("fnamesmax");

            //SPLFilter[] layer_filters = getSelectedFilters();
            SPLFilter[] layer_filters = getSelectedFilters(_filtersNames);
            for (int i = 0; i < layer_filters.length; i++) {
                if (layer_filters[i].getLayer().type.equalsIgnoreCase("environmental")) {
                    layer_filters[i].maximum_value = Double.parseDouble(_filtersNamesMax.get(i));
                    layer_filters[i].maximum_value = Double.parseDouble(_filtersNamesMin.get(i));
                } else {
                    // TODO: write code for categorical 
                }
            }

            int c = 0;
            if (layer_filters != null) {
                c = SpeciesListIndex.listSpeciesCountGeo(layer_filters);
                System.out.println("Got count: " + c);
            } else {
                System.out.println("No count, layers filters is null");
            }

            return "" + c;

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return "";
    }

    private SPLFilter[] getSelectedFilters() {
        SPLFilter[] f = new SPLFilter[_layer_filters_selected.size()];
        _layer_filters_selected.toArray(f);
        return f;
    }

    private SPLFilter[] getSelectedFilters(List filterNames) {
        SPLFilter[] f = new SPLFilter[_layer_filters_selected.size()];
        _layer_filters_selected.toArray(f);
        return f;
    }

    private void writeUserMap(String filename) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {

            fos = new FileOutputStream(filename);
            out = new ObjectOutputStream(fos);
            /*
            byte[] b = getByteArrayFromObject(userMap);
            System.out.println("Writing length: " + b.length);
            out.write(b.length);
            out.write(b);
             *
             */
            out.writeObject(userMap);

            out.flush();
            out.close();

            fos.flush();
            fos.close();

        } catch (Exception e) {
            System.out.println("Error writing usermap to filesystem: ");
            e.printStackTrace(System.out);
        }
    }

    private void readUserMap(String filename) {

        FileInputStream fis = null;
        ObjectInputStream in = null;

        try {

            fis = new FileInputStream(filename);
            in = new ObjectInputStream(fis);
            userMap = (Map) in.readObject();
            /*
            int length = in.readInt();
            System.out.println("Got length: " + length);
            byte[] b = new byte[length];
            in.read(b);
            userMap = (Map) getObjectFromByteArray(b);
             */
            in.close();

            fis.close();

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    /*
    private void writeUserMapXML(String filename) {
    try {
    System.out.println("writing out the data");
    XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(filename)));
    encoder.writeObject(userMap);
    encoder.flush();
    encoder.close();
    System.out.println("done writing data");
    } catch (Exception ex) {
    //Logger.getLogger(FilteringWSController.class.getName()).log(Level.SEVERE, null, ex);
    ex.printStackTrace(System.out);
    }
    }

    private void readUserMapXML(String filename) {
    try {
    System.out.println("reading in the data");
    XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(
    new FileInputStream(filename)));
    userMap = (Map) decoder.readObject();
    decoder.close();
    System.out.println("done reading data");
    } catch (Exception ex) {
    ex.printStackTrace(System.out);
    }
    }
     *
     */
    private void writeUserBytes(String filename) {
        try {

            // save it to the session
            // userMap.put("pid", pid);
            // userMap.put("filteringImage", filteringImage);
            /*
            if (userMap.get("selectedLayerFilters") != null) {
            //_layer_filters_selected = new ArrayList();
            userMap.remove("selectedLayerFilters");
            }
            userMap.put("selectedLayerFilters", _layer_filters_selected);
             * 
             */



            FileOutputStream fos = new FileOutputStream(filename);
            JBossObjectOutputStream out = new JBossObjectOutputStream(fos);
            System.out.println("writing user bytes");
            out.writeObject(userMap);
            out.flush();
            out.close();
            fos.flush();
            fos.close();
            System.out.println("bytes written");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private void readUserBytes(String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            JBossObjectInputStream in = new JBossObjectInputStream(fis);
            System.out.println("reading user bytes");
            userMap = (Map) in.readObject();
            in.close();
            fis.close();
            System.out.println("bytes read");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Converts an object to a serialized byte array.
     *
     * @param obj Object to be converted.
     * @return byte[] Serialized array representing the object.
     */
    public byte[] getByteArrayFromObject(Object obj) {
        byte[] result = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new JBossObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            baos.close();
            result = baos.toByteArray();
        } catch (IOException ioEx) {
            ioEx.printStackTrace(System.out);
        }

        return result;
    }

    /**
     * Utility method to un-serialize objects from byte arrays.
     *
     * @param bytes The input byte array.
     * @return The output object.
     */
    public Object getObjectFromByteArray(byte[] bytes) {
        Object result = null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new JBossObjectInputStream(bais);
            result = ois.readObject();
            ois.close();
        } catch (IOException ioEx) {
            ioEx.printStackTrace(System.out);
        } catch (ClassNotFoundException cnfEx) {
            cnfEx.printStackTrace(System.out);
        }

        return result;
    }
}
