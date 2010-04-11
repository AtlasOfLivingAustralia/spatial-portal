package org.ala.spatial.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * SpatialSettings reads in the settings file sets any default values
 * as necessary
 *
 * @author ajay
 */
public class SpatialSettings {

    private Hashtable settingKeys;
    private String filename;
    private XmlReader xr;
    private String envDataPath;

    public SpatialSettings() {
        try {
            filename = SpatialSettings.class.getResource("/tabulation_settings.xml").getFile();
            load();
        } catch (Exception e) {
            //sl.log("Tabulation Settings", e.toString());
            System.out.println("Error: ");
            e.printStackTrace(System.out);

        }
    }

    public SpatialSettings(String filename) {
        this.filename = filename;
    }

    public SpatialSettings(Hashtable settingKeys) {
        this.settingKeys = settingKeys;
    }

    private void setDefaultKeys() {
        if (settingKeys == null) {
            settingKeys = new Hashtable();
        }

    }

    public void load() {
        if (settingKeys == null) {
            settingKeys = new Hashtable();
        }
        xr = new XmlReader(filename);

        envDataPath = xr.getValue("environmental_data_path");
    }

    public String getEnvDataPath() {
        return envDataPath;
    }

    public String getValue(String key) {
        return xr.getValue(key);
    }

    public Layer[] getEnvironmentalLayers() {
        String s1, s2, s3, s4, s5, s6;
        List<Layer> layerlist = new ArrayList<Layer>();

        String[] edname = {"environmental_data_files", "environmental_data_file", "environmental_data_file_name"};
        String[] eddisplay = {"environmental_data_files", "environmental_data_file", "environmental_data_file_display_name"};
        String[] eddescription = {"environmental_data_files", "environmental_data_file", "environmental_data_file_description"};
        int[] edi = {0, 0, 0};
        layerlist.clear();
        s1 = xr.getValue(edname, edi, 3);
        s2 = xr.getValue(eddisplay, edi, 3);
        s3 = xr.getValue(eddescription, edi, 3);
        System.out.println(s1 + " : " + s2 + " : " + s3);
        while (s1 != null) {
            layerlist.add(new Layer(s1, s2, s3, "environmental", null));
            edi[1]++;

            s1 = xr.getValue(edname, edi, 3);
            s2 = xr.getValue(eddisplay, edi, 3);
            s3 = xr.getValue(eddescription, edi, 3);
            System.out.println(s1 + " : " + s2 + " : " + s3);

        }
        return (Layer[]) layerlist.toArray(new Layer[layerlist.size()]);
    }

    public Layer[] getContextualLayers() {
        String s1, s2, s3, s4, s5, s6;
        List<Layer> layerlist = new ArrayList<Layer>();
        List<Field> fieldlist = new ArrayList<Field>();

        String[] gtname = {"geo_tables", "geo_table", "geo_table_name"};
        String[] gtdisplay = {"geo_tables", "geo_table", "geo_table_display_name"};
        String[] gtdescription = {"geo_tables", "geo_table", "geo_table_description"};
        String[] gttype = {"geo_tables", "geo_table", "geo_table_type"};
        String[] gtfields = {"geo_tables", "geo_table", "geo_fields_to_return"};
        int[] gti = {0, 0, 0};

        String[] gfname = {"geo_tables", "geo_table", "geo_fields_to_return", "geo_field", "geo_field_name"};
        String[] gfdisplay = {"geo_tables", "geo_table", "geo_fields_to_return", "geo_field", "geo_field_display_name"};
        int[] gfi = {0, 0, 0, 0, 0};

        layerlist.clear();
        s1 = xr.getValue(gtname, gti, 3);
        s2 = xr.getValue(gtdisplay, gti, 3);
        s3 = xr.getValue(gtdescription, gti, 3);
        s4 = xr.getValue(gttype, gti, 3);
        System.out.println(s1 + " : " + s2 + " : " + s3 + " : " + s4);
        while (s1 != null) {

            fieldlist.clear();
            gfi[3] = 0;
            s5 = xr.getValue(gfname, gfi, 5);
            s6 = xr.getValue(gfdisplay, gfi, 5);
            System.out.println(s5 + " : " + s6);
            while (s5 != null) {
                fieldlist.add(new Field(s5, s6, ""));
                gfi[3]++;
                s5 = xr.getValue(gfname, gfi, 5);
                s6 = xr.getValue(gfdisplay, gfi, 5);
                System.out.println(s5 + " : " + s6);
            }

            layerlist.add(new Layer(s1, s2, s3, s4, (Field[]) fieldlist.toArray(new Field[fieldlist.size()])));
            gti[1]++;
            gfi[1]++;

            s1 = xr.getValue(gtname, gti, 3);
            s2 = xr.getValue(gtdisplay, gti, 3);
            s3 = xr.getValue(gtdescription, gti, 3);
            s4 = xr.getValue(gttype, gti, 3);
            System.out.println(s1 + " : " + s2 + " : " + s3 + " : " + s4);
        }

        return (Layer[]) layerlist.toArray(new Layer[layerlist.size()]);
    }

    public String getMaxentCmd() {
        String cmd = "";

        String[] maxent = {"maxent", "cmdpath"};
        int[] mi = {0, 0};
        cmd = xr.getValue(maxent, mi, 2);

        return cmd;
    }

    public Hashtable getGeoserverSettings() {
        Hashtable htGeoServer = new Hashtable();
        htGeoServer.put("geoserver_url", xr.getValue("geoserver_url"));
        htGeoServer.put("geoserver_username", xr.getValue("geoserver_username"));
        htGeoServer.put("geoserver_password", xr.getValue("geoserver_password"));
        return htGeoServer;
    }
}
