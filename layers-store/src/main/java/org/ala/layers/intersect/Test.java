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

package org.ala.layers.intersect;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import org.ala.layers.dao.LayerIntersectDAO;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Adam
 */

public class Test {
    
    public static void main(String [] args) {

        try {

            ApplicationContext ac = new ClassPathXmlApplicationContext("app-config.xml");
            LayerIntersectDAO layerIntersect = (LayerIntersectDAO) ac.getBean("layerIntersectDao");
            
            String fieldList = "el799,cl22";
            String pointsList = "132,-22,133,-24,133,21";
            //String pointsList = loadPoints();

            List<String [] > sample = layerIntersect.sampling(fieldList, pointsList);

            //header
            String [] pointsArray = pointsList.split(",");
            String [] fields = fieldList.split(",");
            StringBuilder sb = new StringBuilder();
            sb.append("longitude,latitude");
            for(int i=0;i<fields.length;i++) {
                sb.append(",");
                sb.append(fields[i]);
            }
            //rows
            int rows = sample.get(0).length;
            for(int i=0;i<rows;i++) {
                sb.append("\n");
                sb.append(pointsArray[i*2]);
                sb.append(",");
                sb.append(pointsArray[i*2+1]);
                for(int j=0;j<sample.size();j++) {
                    sb.append(",");
                    sb.append(sample.get(j)[i]);
                }
            }

            System.out.println(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String loadPoints() {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader("d:\\coordinates.txt"));
            String line;
            br.readLine();
            while((line = br.readLine()) != null) {
                String [] s = line.split(",");
                if(sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(s[1]).append(",").append(s[0]);
            }
            br.close();
        } catch(Exception e) {

        }
        return sb.toString();
    }
}
