/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.intersect;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import org.ala.layers.dao.LayerIntersectDAO;
import org.ala.layers.util.IntersectUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Adam
 */
public class Test {

    public static void main(String[] args) {

        try {

            ApplicationContext ac = new ClassPathXmlApplicationContext("spring/app-config.xml");
            LayerIntersectDAO layerIntersect = (LayerIntersectDAO) ac.getBean("layerIntersectDao");

            String fieldList = "el798,el805,el663,el674";
            String pointsList = "132,-22,133,-24,133,21";
            //String pointsList = loadPoints();

            ArrayList<String> sample = layerIntersect.sampling(fieldList, pointsList);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            String[] ps = pointsList.split(",");

            IntersectUtil.writeSampleToStream(fieldList.split(","), ps, sample, baos);

            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    static String loadPoints() {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader("d:\\coordinates.txt"));
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(s[1]).append(",").append(s[0]);
            }
            br.close();
        } catch (Exception e) {
        }
        return sb.toString();
    }
}
