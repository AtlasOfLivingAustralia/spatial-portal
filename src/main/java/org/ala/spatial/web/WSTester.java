/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web;

import java.net.URLEncoder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 *
 * @author ajay
 */
public class WSTester {

    public static void main(String[] args) {
        try {
            System.out.println("Hello world");
            String base_url = "http://spatial-dev.ala.org.au/alaspatial/species/cluster/";
            String lsid = "urn:lsid:biodiversity__org__au:apni__taxon:295864";
            lsid = "urn:lsid:biodiversity__org__au:afd__taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537";
            //lsid = lsid.replaceAll("/./", "__");
            
            String csurl = base_url + URLEncoder.encode(lsid, "UTF-8");
            csurl += "?z=3&d=40";
            //csurl = "http://spatial-dev.ala.org.au/alaspatial//species/cluster/urn%3Alsid%3Abiodiversity__org__au%3Aafd__taxon%3A558a729a-789b-4b00-a685-8843dc447319";
            System.out.println("Calling: " + csurl); 

            HttpClient client = new HttpClient();
            GetMethod post = new GetMethod(csurl);
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();

            System.out.println("slist:::");
            System.out.println(slist); 
        } catch (Exception ex) {
            //Logger.getLogger(WSTester.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error: ");
            ex.printStackTrace(System.out);

        }

    }
}
