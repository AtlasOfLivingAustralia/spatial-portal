package org.ala.spatial.util;

import au.com.bytecode.opencsv.CSVReader;
import org.ala.spatial.analysis.layers.Records;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by a on 19/03/2014.
 */
public class OccurrenceData {

    static final String SPECIES_LIST_SERVICE_CSV = "/webportal/species.csv?";

    public String [] getSpeciesData(String q, String bs, String records_filename) {
            HashMap<String, Object> result = new HashMap<String, Object>();

            HashSet<String> sensitiveSpeciesFound = new HashSet<String>();

            //add to 'identified' sensitive list
            try {
                CSVReader csv = new CSVReader(new StringReader(getSpecies(q + "&fq=" + URLEncoder.encode("-sensitive:[* TO *]", "UTF-8"),bs)));
                List<String[]> fullSpeciesList = csv.readAll();
                csv.close();
                for (int i = 0; i < fullSpeciesList.size(); i++) {
                    sensitiveSpeciesFound.add(fullSpeciesList.get(i)[0]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //remove sensitive records that will not be LSID matched
            try {
                Records r = new Records(bs,q + "&fq=" + URLEncoder.encode("-sensitive:[* TO *]","UTF-8"),null,records_filename,null);

                StringBuilder sb = null;
                if (r.getRecordsSize() > 0) {
                    sb = new StringBuilder();
                    for (int i = 0; i <  r.getRecordsSize(); i ++) {
                        if (sb.length() == 0) {
                            //header
                            sb.append("species,longitude,latitude");
                        }
                        sb.append("\nspecies,").append(r.getLongitude(i)).append(",").append(r.getLatitude(i));
                    }
                }

                //collate sensitive species found, no header
                StringBuilder sen = new StringBuilder();
                for (String s : sensitiveSpeciesFound) {
                    sen.append(s).append("\n");
                }

                String[] out = {((sb == null) ? null : sb.toString()), (sen.length() == 0) ? null : sen.toString()};

                return out;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


    public String getSpecies (String q, String bs) {
        HttpClient client = new HttpClient();
        String url = bs
                + SPECIES_LIST_SERVICE_CSV
                + "&q=" + q;

        System.out.println("getting species list: " + url);

        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            return get.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }
}
