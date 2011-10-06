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

package org.ala.layers.client;

import java.util.Iterator;
import java.util.List;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Layer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Main CLI class to hook into layers-store
 * 
 * @author ajay
 */
public class Client
{
    public static void main( String[] args ) {
        System.out.println( "Layers Store CLI client" );

        ApplicationContext context =
                new ClassPathXmlApplicationContext("spring/app-config.xml");

        LayerDAO layerDao = (LayerDAO) context.getBean("layerDao");
        List<Layer> layers = layerDao.getLayers();
        System.out.println("Got " + layers.size() + " layers");
        Iterator<Layer> it = layers.iterator();
        while(it.hasNext()) {
            Layer l = it.next();
            System.out.println(" > " + l.getName());
        }
    }
}
